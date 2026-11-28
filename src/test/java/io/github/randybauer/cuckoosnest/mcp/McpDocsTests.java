package io.github.randybauer.cuckoosnest.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;

/** docs/MCP.md documents every MCP tool and every parameter the code actually has. */
class McpDocsTests {

    private static final Path DOC = Path.of("docs/MCP.md");

    @Test
    void everyToolAndParameterIsDocumented() throws IOException {
        String doc = Files.readString(DOC);
        List<Method> tools = Arrays.stream(NestMcpTools.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(McpTool.class))
                .toList();

        assertThat(tools).hasSize(5);
        for (Method tool : tools) {
            String name = tool.getAnnotation(McpTool.class).name();
            assertThat(doc).as("tool %s", name).contains("### `" + name + "`");
            for (Parameter parameter : tool.getParameters()) {
                assertThat(parameter.isNamePresent())
                        .as("compiled with -parameters")
                        .isTrue();
                assertThat(doc)
                        .as("parameter %s of %s", parameter.getName(), name)
                        .contains("| `" + parameter.getName() + "` |");
            }
        }
    }

    @Test
    void theDocumentedDenialIsTheRealOne() throws IOException {
        assertThat(Files.readString(DOC)).contains(McpDecoyTools.DENIED);
    }
}
