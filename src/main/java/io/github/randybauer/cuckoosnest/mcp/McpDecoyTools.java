package io.github.randybauer.cuckoosnest.mcp;

import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertService;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyEvent;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.github.randybauer.cuckoosnest.decoy.types.McpToolDecoyType;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Puts every active {@code MCP_TOOL} decoy on the MCP server as a real, callable tool, and keeps
 * the tool list in sync as decoys are created and retired.
 *
 * <p>A call to one of these tools records an alert (channel MCP) naming the client that called it,
 * then returns a dull "permission denied" error, so the caller learns nothing useful.
 */
@Component
public class McpDecoyTools {

    private static final Logger LOG = LoggerFactory.getLogger(McpDecoyTools.class);
    static final String DENIED = "Permission denied: this operation requires an approved change ticket.";

    private final McpSyncServer server;
    private final DecoyService decoys;
    private final AlertService alerts;
    private final Set<String> registered = ConcurrentHashMap.newKeySet();

    public McpDecoyTools(McpSyncServer server, DecoyService decoys, AlertService alerts) {
        this.server = server;
        this.decoys = decoys;
        this.alerts = alerts;
    }

    /** Registers decoys that already exist when the app starts. */
    @EventListener(ApplicationReadyEvent.class)
    public void registerExisting() {
        decoys.activeOfType(McpToolDecoyType.ID).forEach(this::add);
    }

    /** Keeps the tool list in step with decoys, once the change is committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDecoyEvent(DecoyEvent event) {
        Decoy decoy = event.decoy();
        if (!McpToolDecoyType.ID.equals(decoy.getType())) {
            return;
        }
        switch (event) {
            case DecoyEvent.Created created -> add(created.decoy());
            case DecoyEvent.Retired retired -> remove(retired.decoy());
        }
    }

    /** Names of decoy tools currently on the server. */
    public Set<String> registeredNames() {
        return Set.copyOf(registered);
    }

    private void add(Decoy decoy) {
        String name = decoy.getToken();
        if (!registered.add(name)) {
            return;
        }
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(name)
                .description(McpToolDecoyType.descriptionFor(name))
                .inputSchema(Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                                "environment",
                                Map.of(
                                        "type", "string",
                                        "description", "Which environment, for example 'production'"))))
                .build();
        server.addTool(SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> touched(name, exchange, request))
                .build());
        server.notifyToolsListChanged();
        // Log the decoy id, never its token. (For this type the token is the tool name.)
        LOG.info("Decoy MCP tool added for decoy {}", decoy.getId());
    }

    private void remove(Decoy decoy) {
        String name = decoy.getToken();
        if (registered.remove(name)) {
            server.removeTool(name);
            server.notifyToolsListChanged();
            LOG.info("Decoy MCP tool removed for decoy {}", decoy.getId());
        }
    }

    private McpSchema.CallToolResult touched(
            String name, McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        decoys.findActive(McpToolDecoyType.ID, name).ifPresent(decoy -> {
            McpSchema.Implementation client = exchange.getClientInfo();
            String clientName = client == null ? null : client.name() + "/" + client.version();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("tool", name);
            details.put("arguments", String.valueOf(request.arguments()));
            details.put("sessionId", exchange.sessionId());
            alerts.record(decoy, Alert.Channel.MCP, null, clientName, details);
        });
        return McpSchema.CallToolResult.builder()
                .addTextContent(DENIED)
                .isError(true)
                .build();
    }
}
