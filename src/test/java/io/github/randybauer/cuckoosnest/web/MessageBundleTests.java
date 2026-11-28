package io.github.randybauer.cuckoosnest.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every piece of page text exists in English and Spanish, and every key a page uses is defined. */
class MessageBundleTests {

    private static final Pattern KEY_IN_TEMPLATE = Pattern.compile("#\\{([a-zA-Z0-9_.]+)[}(]");

    @Test
    void englishAndSpanishHaveExactlyTheSameKeys() throws IOException {
        assertThat(load("messages_es.properties").stringPropertyNames())
                .isNotEmpty()
                .containsExactlyInAnyOrderElementsOf(load("messages.properties").stringPropertyNames());
    }

    @Test
    void noSpanishValueIsBlank() throws IOException {
        Properties es = load("messages_es.properties");
        assertThat(es.stringPropertyNames())
                .allSatisfy(key -> assertThat(es.getProperty(key)).isNotBlank());
    }

    @Test
    void everyKeyUsedInATemplateIsDefined() throws IOException {
        Properties en = load("messages.properties");
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/templates"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".html")).toList()) {
                Matcher m = KEY_IN_TEMPLATE.matcher(Files.readString(file));
                while (m.find()) {
                    assertThat(en.getProperty(m.group(1)))
                            .as("%s uses #{%s}", file.getFileName(), m.group(1))
                            .isNotNull();
                }
            }
        }
        for (String type : new String[] {"URL", "CREDENTIAL", "MCP_TOOL"}) {
            assertThat(en.getProperty("decoy.type." + type))
                    .as("decoy.type.%s", type)
                    .isNotNull();
        }
    }

    private static Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                MessageBundleTests.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
