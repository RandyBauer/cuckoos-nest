package io.github.randybauer.cuckoosnest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps docs/ARCHITECTURE.md honest: every class it names must exist, and every controller,
 * service, and MCP tool class in the code must appear in it. Renaming a class without updating the
 * diagrams fails the build.
 */
class ArchitectureDocsTests {

    private static final Path DOC = Path.of("docs/ARCHITECTURE.md");
    private static final Pattern CLASS_NAME = Pattern.compile(
            "\\b([A-Z][A-Za-z0-9]*(?:Controller|Service|Filter|Config|Registry|Type|Tools|Cache|Repository|Event|Generator|Properties))\\b");
    private static final Pattern MUST_BE_DOCUMENTED = Pattern.compile(".*(Controller|Service|Tools|Filter)");

    @Test
    void everyClassTheDiagramsNameExists() throws IOException {
        Set<String> documented = classNamesIn(Files.readString(DOC));
        Set<String> inCode = mainClassNames();

        assertThat(documented).isNotEmpty();
        assertThat(inCode).containsAll(documented);
    }

    @Test
    void everyEntryPointAndServiceIsDocumented() throws IOException {
        Set<String> documented = classNamesIn(Files.readString(DOC));
        Set<String> mustAppear = mainClassNames().stream()
                .filter(name -> MUST_BE_DOCUMENTED.matcher(name).matches())
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(documented).containsAll(mustAppear);
    }

    @Test
    void decisionRecordsAreNumberedAndIndexed() throws IOException {
        String index = Files.readString(Path.of("docs/adr/README.md"));
        try (Stream<Path> files = Files.list(Path.of("docs/adr"))) {
            for (Path adr : files.filter(p -> p.getFileName().toString().matches("\\d{4}-.*\\.md"))
                    .toList()) {
                String text = Files.readString(adr);
                assertThat(text).as("%s", adr).contains("## Context", "## Decision", "## Consequences");
                assertThat(index).as("index links %s", adr).contains("(" + adr.getFileName() + ")");
            }
        }
    }

    private static Set<String> classNamesIn(String text) {
        Set<String> names = new TreeSet<>();
        Matcher m = CLASS_NAME.matcher(text);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    private static Set<String> mainClassNames() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".java"))
                    .map(n -> n.substring(0, n.length() - ".java".length()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
