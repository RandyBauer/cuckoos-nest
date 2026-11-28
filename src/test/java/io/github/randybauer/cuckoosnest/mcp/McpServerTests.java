package io.github.randybauer.cuckoosnest.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertRepository;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Drives the real MCP endpoint over HTTP with the MCP Java SDK's own client, the way an AI agent
 * would.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nest.mcp.token=" + McpServerTests.TOKEN, "nest.public-base-url=https://nest.test"})
class McpServerTests {

    static final String TOKEN = "test-mcp-token-0123456789";

    @LocalServerPort
    private int port;

    @Autowired
    private DecoyService decoys;

    @Autowired
    private AlertRepository alerts;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        client = clientWithToken(TOKEN);
        client.initialize();
    }

    @AfterEach
    void disconnect() {
        client.closeGracefully();
    }

    @Test
    void requestsWithoutTheRightTokenAreRefused() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        for (String auth : new String[] {null, "Bearer wrong-token", "Basic dXNlcjpwYXNz"}) {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base() + "/mcp"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"));
            if (auth != null) {
                request.header("Authorization", auth);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as("auth=%s", auth).isEqualTo(401);
            assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
        }
    }

    @Test
    void theRealToolsAreListed() {
        assertThat(toolNames()).contains("create_decoy", "list_decoys", "list_alerts", "explain_alert", "retire_decoy");
    }

    @Test
    void anAgentCanPlantAUrlDecoyAndGetItsBait() {
        McpSchema.CallToolResult result = call("create_decoy", Map.of("type", "url", "label", "Fake wiki page"));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(text(result)).contains("\"type\":\"URL\"").contains("https://nest.test/t/");
    }

    @Test
    void badInputComesBackAsAToolError() {
        McpSchema.CallToolResult result = call("create_decoy", Map.of("type", "FAX", "label", "x"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("FAX");
    }

    @Test
    void aDecoyToolAppearsIsCaughtWhenCalledAndDisappearsWhenRetired() {
        Decoy decoy = decoys.create("MCP_TOOL", "Tempting tool", "Nest MCP server");
        String name = decoy.getToken();
        assertThat(toolNames()).contains(name);

        McpSchema.CallToolResult result = call(name, Map.of("environment", "production"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo(McpDecoyTools.DENIED);
        List<Alert> caught = alerts.findAll().stream()
                .filter(a -> a.getDecoy().getId().equals(decoy.getId()))
                .toList();
        assertThat(caught).hasSize(1);
        assertThat(caught.getFirst().getChannel()).isEqualTo(Alert.Channel.MCP);
        assertThat(caught.getFirst().getUserAgent()).isEqualTo("nest-test-agent/1.0");
        assertThat(caught.getFirst().getDetails()).containsEntry("tool", name);

        assertThat(text(call("list_alerts", Map.of()))).contains(name).contains("\"channel\":\"MCP\"");
        String explanation =
                text(call("explain_alert", Map.of("alertId", caught.getFirst().getId())));
        assertThat(explanation).contains("Tempting tool").contains("AI agent called a tool");

        call("retire_decoy", Map.of("decoyId", decoy.getId().toString()));
        assertThat(toolNames()).doesNotContain(name);
    }

    @Test
    void retiredDecoysAreHiddenFromListDecoysUnlessAsked() {
        Decoy decoy = decoys.create("URL", "Soon retired", null);
        decoys.retire(decoy.getId());

        assertThat(text(call("list_decoys", Map.of())))
                .doesNotContain(decoy.getId().toString());
        assertThat(text(call("list_decoys", Map.of("includeRetired", true))))
                .contains(decoy.getId().toString());
    }

    private McpSyncClient clientWithToken(String token) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(base())
                .endpoint("/mcp")
                .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer " + token))
                .build();
        return McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("nest-test-agent", "1.0"))
                .requestTimeout(Duration.ofSeconds(20))
                .build();
    }

    private List<String> toolNames() {
        return client.listTools().tools().stream().map(McpSchema.Tool::name).toList();
    }

    private McpSchema.CallToolResult call(String tool, Map<String, Object> arguments) {
        return client.callTool(new McpSchema.CallToolRequest(tool, arguments));
    }

    private static String text(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(c -> ((McpSchema.TextContent) c).text())
                .reduce("", String::concat);
    }

    private String base() {
        return "http://localhost:" + port;
    }
}
