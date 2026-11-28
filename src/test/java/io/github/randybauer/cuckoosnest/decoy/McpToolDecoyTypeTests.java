package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.decoy.types.McpToolDecoyType;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class McpToolDecoyTypeTests {

    private final McpToolDecoyType type = new McpToolDecoyType();

    @Test
    void namesAreValidMcpToolNamesFromTheTemptingList() {
        TokenGenerator tokens = new TokenGenerator();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String name = type.newToken(tokens);
            assertThat(name).matches("[a-z][a-z_0-9]{2,63}");
            assertThat(McpToolDecoyType.descriptionFor(name)).isNotEqualTo("Administrative operation.");
            seen.add(name);
        }
        // 8 names x 5 suffixes = 40 combinations; 500 draws should find almost all of them.
        assertThat(seen).hasSizeGreaterThan(30);
    }

    @Test
    void descriptionsMatchByPrefixSoSuffixedNamesKeepTheirMeaning() {
        assertThat(McpToolDecoyType.descriptionFor("get_production_credentials_v2"))
                .contains("production database and cloud credentials");
        assertThat(McpToolDecoyType.descriptionFor("something_else")).isEqualTo("Administrative operation.");
    }

    @Test
    void baitSaysWhereTheToolIsListed() {
        Decoy decoy = new Decoy("MCP_TOOL", "read_vault_secrets_legacy", "label", null, Instant.EPOCH);

        assertThat(type.bait(decoy, "https://nest.example.com"))
                .isEqualTo("Listed automatically as the MCP tool 'read_vault_secrets_legacy' on "
                        + "https://nest.example.com/mcp");
    }
}
