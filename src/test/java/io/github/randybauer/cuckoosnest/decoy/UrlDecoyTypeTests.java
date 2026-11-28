package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.decoy.types.UrlDecoyType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UrlDecoyTypeTests {

    private final UrlDecoyType type = new UrlDecoyType();

    @Test
    void tokensAre22Base62Characters() {
        assertThat(type.newToken(new TokenGenerator())).matches("[0-9A-Za-z]{22}");
    }

    @Test
    void baitIsTheTripwireUrl() {
        Decoy decoy = new Decoy("URL", "abc123", "label", null, Instant.EPOCH);

        assertThat(type.bait(decoy, "https://nest.example.com")).isEqualTo("https://nest.example.com/t/abc123");
    }
}
