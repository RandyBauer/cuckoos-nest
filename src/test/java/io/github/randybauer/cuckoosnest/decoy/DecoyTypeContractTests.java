package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.decoy.types.CredentialDecoyType;
import io.github.randybauer.cuckoosnest.decoy.types.McpToolDecoyType;
import io.github.randybauer.cuckoosnest.decoy.types.UrlDecoyType;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The rules every decoy plugin must follow. A new plugin goes in {@link #plugins()} and is checked
 * the same way.
 */
class DecoyTypeContractTests {

    static Stream<DecoyType> plugins() {
        return Stream.of(new UrlDecoyType(), new CredentialDecoyType(), new McpToolDecoyType());
    }

    @ParameterizedTest
    @MethodSource("plugins")
    void idIsAValidRegistryIdAndMatchesThePluginsConstant(DecoyType type) {
        assertThat(type.id()).matches("[A-Z][A-Z0-9_]{0,31}");
        String constant = switch (type) {
            case UrlDecoyType u -> UrlDecoyType.ID;
            case CredentialDecoyType c -> CredentialDecoyType.ID;
            case McpToolDecoyType m -> McpToolDecoyType.ID;
            default -> throw new AssertionError("unknown plugin " + type);
        };
        assertThat(type.id()).isEqualTo(constant);
    }

    @ParameterizedTest
    @MethodSource("plugins")
    void tokensFitTheDatabaseColumnAndBaitContainsTheToken(DecoyType type) {
        String token = type.newToken(new TokenGenerator());
        Decoy decoy = new Decoy(type.id(), token, "label", null, Instant.EPOCH);

        assertThat(token).isNotBlank().hasSizeLessThanOrEqualTo(128);
        assertThat(type.bait(decoy, "https://nest.example.com")).contains(token);
    }

    @Test
    void displayNamesAreDistinctAndReadable() {
        List<String> names = plugins().map(DecoyType::displayName).toList();

        assertThat(names).containsExactly("URL decoy", "Fake API key", "Fake MCP tool");
        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    void aDecoyKeepsWhatItWasCreatedWith() {
        Instant created = Instant.parse("2026-12-01T12:00:00Z");
        Decoy decoy = new Decoy("URL", "tok123", "Fake admin page", "docs/ADMIN.md", created);

        assertThat(decoy.getType()).isEqualTo("URL");
        assertThat(decoy.getToken()).isEqualTo("tok123");
        assertThat(decoy.getLabel()).isEqualTo("Fake admin page");
        assertThat(decoy.getLocation()).isEqualTo("docs/ADMIN.md");
        assertThat(decoy.getCreatedAt()).isEqualTo(created);
    }
}
