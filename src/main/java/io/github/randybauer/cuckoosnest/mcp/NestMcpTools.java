package io.github.randybauer.cuckoosnest.mcp;

import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertService;
import io.github.randybauer.cuckoosnest.config.NestProperties;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.github.randybauer.cuckoosnest.decoy.DecoyType;
import io.github.randybauer.cuckoosnest.decoy.DecoyTypeRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The real tools an AI agent gets over MCP: plant decoys, list them, read alerts, and retire
 * decoys. Fake decoy tools are added beside these at runtime (see {@link McpDecoyTools}).
 */
@Component
public class NestMcpTools {

    static final int DEFAULT_ALERT_HOURS = 24;
    static final int DEFAULT_ALERT_LIMIT = 50;

    private final DecoyService decoys;
    private final AlertService alerts;
    private final DecoyTypeRegistry types;
    private final NestProperties properties;

    public NestMcpTools(DecoyService decoys, AlertService alerts, DecoyTypeRegistry types, NestProperties properties) {
        this.decoys = decoys;
        this.alerts = alerts;
        this.types = types;
        this.properties = properties;
    }

    /** A decoy as an agent sees it, including the bait to plant. */
    public record DecoyView(
            UUID id, String type, String label, String location, boolean active, Instant createdAt, String bait) {}

    /** One recorded touch. */
    public record AlertView(
            long id,
            UUID decoyId,
            String decoyLabel,
            String decoyType,
            Instant touchedAt,
            String channel,
            String sourceIp,
            String userAgent,
            Map<String, Object> details) {}

    @McpTool(
            name = "create_decoy",
            description = "Create a decoy (honeytoken) and get the bait to plant. Types: URL (a link nobody "
                    + "should follow), CREDENTIAL (a fake API key), MCP_TOOL (a tempting fake tool on this "
                    + "server). Plant the returned bait where intruders or misbehaving agents would find it.")
    public DecoyView createDecoy(
            @McpToolParam(description = "Decoy type: URL, CREDENTIAL, or MCP_TOOL") String type,
            @McpToolParam(description = "Your name for it, e.g. 'Fake admin page in README'") String label,
            @McpToolParam(description = "Where you will plant it, e.g. 'docs/OPERATIONS.md'", required = false)
                    String location) {
        return view(decoys.create(type == null ? null : type.strip().toUpperCase(), label, location));
    }

    @McpTool(name = "list_decoys", description = "List decoys, newest first, with the bait for each.")
    public List<DecoyView> listDecoys(
            @McpToolParam(description = "Include retired decoys (default false)", required = false)
                    Boolean includeRetired) {
        boolean all = Boolean.TRUE.equals(includeRetired);
        return decoys.list().stream()
                .filter(d -> all || d.isActive())
                .map(this::view)
                .toList();
    }

    @McpTool(
            name = "list_alerts",
            description = "List recent decoy touches, newest first. Each one means something touched bait "
                    + "that nobody had a reason to touch.")
    public List<AlertView> listAlerts(
            @McpToolParam(description = "How many hours back to look (default 24)", required = false) Integer hours,
            @McpToolParam(description = "Maximum number of alerts (default 50, at most 500)", required = false)
                    Integer limit) {
        Duration window = Duration.ofHours(hours == null || hours < 1 ? DEFAULT_ALERT_HOURS : hours);
        return alerts.recent(window, limit == null ? DEFAULT_ALERT_LIMIT : limit).stream()
                .map(NestMcpTools::view)
                .toList();
    }

    @McpTool(
            name = "explain_alert",
            description = "Explain one alert in plain language: what was touched, where it was planted, "
                    + "who touched it, and what to check next.")
    public String explainAlert(@McpToolParam(description = "The alert id from list_alerts") Long alertId) {
        Alert alert = alerts.find(alertId == null ? -1 : alertId)
                .orElseThrow(() -> new IllegalArgumentException("No alert with id " + alertId));
        return AlertExplainer.explain(alert);
    }

    @McpTool(
            name = "retire_decoy",
            description = "Retire a decoy so touches stop raising alerts. Use when the bait is removed or burned.")
    public DecoyView retireDecoy(@McpToolParam(description = "The decoy id from list_decoys") String decoyId) {
        UUID id;
        try {
            id = UUID.fromString(decoyId == null ? "" : decoyId.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Not a decoy id: " + decoyId, e);
        }
        return view(decoys.retire(id));
    }

    private DecoyView view(Decoy decoy) {
        String bait = types.find(decoy.getType())
                .map((DecoyType t) -> t.bait(decoy, properties.publicBaseUrl()))
                .orElse(null);
        return new DecoyView(
                decoy.getId(),
                decoy.getType(),
                decoy.getLabel(),
                decoy.getLocation(),
                decoy.isActive(),
                decoy.getCreatedAt(),
                bait);
    }

    private static AlertView view(Alert alert) {
        Decoy decoy = alert.getDecoy();
        return new AlertView(
                alert.getId(),
                decoy.getId(),
                decoy.getLabel(),
                decoy.getType(),
                alert.getTouchedAt(),
                alert.getChannel().name(),
                alert.getSourceIp(),
                alert.getUserAgent(),
                alert.getDetails());
    }

    /** Kept separate so the wording can be tested without MCP. */
    static final class AlertExplainer {

        private AlertExplainer() {}

        static String explain(Alert alert) {
            Decoy decoy = alert.getDecoy();
            StringBuilder sb = new StringBuilder();
            sb.append("Alert ")
                    .append(alert.getId())
                    .append(": the ")
                    .append(decoy.getType())
                    .append(" decoy \"")
                    .append(decoy.getLabel())
                    .append("\" was touched at ")
                    .append(alert.getTouchedAt())
                    .append(" over ")
                    .append(alert.getChannel())
                    .append(".\n");
            sb.append("It was planted at: ")
                    .append(decoy.getLocation() == null ? "(no location recorded)" : decoy.getLocation())
                    .append(".\n");
            if (alert.getSourceIp() != null) {
                sb.append("Source IP: ").append(alert.getSourceIp()).append(".\n");
            }
            if (alert.getUserAgent() != null) {
                sb.append("Client: ").append(alert.getUserAgent()).append(".\n");
            }
            if (!alert.getDetails().isEmpty()) {
                sb.append("Details: ").append(alert.getDetails()).append(".\n");
            }
            sb.append("Nobody has a legitimate reason to touch this decoy. ");
            sb.append(
                    switch (decoy.getType()) {
                        case "URL" ->
                            "Check who can read the place it was planted, and look for other requests from"
                                    + " the same source.";
                        case "CREDENTIAL" ->
                            "Someone read the file holding the fake key and tried to use it. Treat"
                                    + " any real secrets stored beside it as exposed.";
                        case "MCP_TOOL" ->
                            "An AI agent called a tool it never needed. Review that agent's recent"
                                    + " prompts and inputs for injected instructions.";
                        default -> "Investigate the source.";
                    });
            return sb.toString();
        }
    }
}
