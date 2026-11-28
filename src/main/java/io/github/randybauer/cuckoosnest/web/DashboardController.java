package io.github.randybauer.cuckoosnest.web;

import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertService;
import io.github.randybauer.cuckoosnest.config.NestProperties;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.github.randybauer.cuckoosnest.decoy.DecoyType;
import io.github.randybauer.cuckoosnest.decoy.DecoyTypeRegistry;
import io.github.randybauer.cuckoosnest.decoy.UnknownDecoyTypeException;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The signed-in dashboard: plant decoys, see their bait, retire them, and watch alerts. */
@Controller
public class DashboardController {

    static final Duration ALERT_WINDOW = Duration.ofHours(24);
    static final int ALERT_LIMIT = 50;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final DecoyService decoys;
    private final AlertService alerts;
    private final DecoyTypeRegistry types;
    private final NestProperties properties;

    public DashboardController(
            DecoyService decoys, AlertService alerts, DecoyTypeRegistry types, NestProperties properties) {
        this.decoys = decoys;
        this.alerts = alerts;
        this.types = types;
        this.properties = properties;
    }

    /** One row of the decoy table. */
    public record DecoyRow(
            UUID id,
            String label,
            String type,
            String location,
            String created,
            boolean active,
            long alerts,
            String bait) {}

    /** One row of the alert table. */
    public record AlertRow(String when, String decoyLabel, String channel, String sourceIp, String client) {}

    @GetMapping("/")
    public String dashboard(Principal principal, Model model) {
        Map<UUID, Long> counts = alerts.countsByDecoy();
        List<DecoyRow> rows = decoys.list().stream()
                .map(d -> new DecoyRow(
                        d.getId(),
                        d.getLabel(),
                        d.getType(),
                        d.getLocation(),
                        format(d.getCreatedAt()),
                        d.isActive(),
                        counts.getOrDefault(d.getId(), 0L),
                        bait(d)))
                .toList();
        model.addAttribute("username", principal.getName());
        model.addAttribute("types", types.all().stream().map(DecoyType::id).toList());
        model.addAttribute("decoys", rows);
        model.addAttribute("alerts", alertRows());
        return "dashboard";
    }

    /** Just the alert table, for the page's 15-second refresh. */
    @GetMapping("/alerts/fragment")
    public String alertsFragment(Model model) {
        model.addAttribute("alerts", alertRows());
        return "fragments/alerts :: alerts";
    }

    @PostMapping("/decoys")
    public String create(
            @RequestParam String type,
            @RequestParam String label,
            @RequestParam(required = false) String location,
            RedirectAttributes redirect) {
        try {
            Decoy decoy = decoys.create(type, label, location);
            redirect.addFlashAttribute("createdLabel", decoy.getLabel());
            redirect.addFlashAttribute("createdBait", bait(decoy));
        } catch (UnknownDecoyTypeException e) {
            redirect.addFlashAttribute("errorKey", "error.type.unknown");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(
                    "errorKey", label == null || label.isBlank() ? "error.label.required" : "error.field.tooLong");
        }
        return "redirect:/";
    }

    @PostMapping("/decoys/{id}/retire")
    public String retire(@PathVariable UUID id, RedirectAttributes redirect) {
        decoys.retire(id);
        redirect.addFlashAttribute("retired", true);
        return "redirect:/";
    }

    private List<AlertRow> alertRows() {
        return alerts.recent(ALERT_WINDOW, ALERT_LIMIT).stream()
                .map(a -> new AlertRow(
                        format(a.getTouchedAt()),
                        a.getDecoy().getLabel(),
                        a.getChannel().name(),
                        a.getSourceIp(),
                        clientOf(a)))
                .toList();
    }

    private String bait(Decoy decoy) {
        return types.find(decoy.getType())
                .map(t -> t.bait(decoy, properties.publicBaseUrl()))
                .orElse("");
    }

    private static String clientOf(Alert alert) {
        return alert.getUserAgent() == null ? "" : alert.getUserAgent();
    }

    static String format(Instant instant) {
        return WHEN.withLocale(Locale.ROOT).format(instant.atZone(ZoneOffset.UTC));
    }
}
