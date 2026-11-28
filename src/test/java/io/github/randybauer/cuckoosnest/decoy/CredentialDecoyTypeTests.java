package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.decoy.types.CredentialDecoyType;
import java.time.Instant;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialDecoyTypeTests {

    /** Computed independently with Python's zlib.crc32, so this checks our CRC32 + base62 code. */
    private static final String KNOWN_GOOD = "nest_testkeyfortheunitchecksum000000k8S6V";

    private final CredentialDecoyType type = new CredentialDecoyType();

    @RepeatedTest(20)
    void newKeysAreWellFormed() {
        String key = type.newToken(new TokenGenerator());

        assertThat(key).startsWith("nest_").hasSize(5 + 30 + 6);
        assertThat(CredentialDecoyType.isWellFormed(key)).isTrue();
    }

    @Test
    void acceptsAKeyWithAnIndependentlyComputedChecksum() {
        assertThat(CredentialDecoyType.isWellFormed(KNOWN_GOOD)).isTrue();
    }

    @Test
    void changingAnyOneCharacterBreaksTheChecksum() {
        for (int i = CredentialDecoyType.PREFIX.length(); i < KNOWN_GOOD.length(); i++) {
            char original = KNOWN_GOOD.charAt(i);
            char replacement = original == 'a' ? 'b' : 'a';
            String tampered = KNOWN_GOOD.substring(0, i) + replacement + KNOWN_GOOD.substring(i + 1);
            assertThat(CredentialDecoyType.isWellFormed(tampered))
                    .as("changed position %d", i)
                    .isFalse();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "nest_",
                "nest_testkeyfortheunitchecksum000000k8S6", // one character short
                "nest_testkeyfortheunitchecksum000000k8S6VV", // one character long
                "NEST_testkeyfortheunitchecksum000000k8S6V", // wrong prefix case
                "nest_testkeyfortheunitchecksum-00000k8S6V", // character outside base62
                "AKIAIOSFODNN7EXAMPLE", // shaped like a cloud provider key
            })
    void rejectsAnythingElse(String candidate) {
        assertThat(CredentialDecoyType.isWellFormed(candidate)).isFalse();
    }

    @Test
    void baitIsAConfigSnippetWithTheDecoyApiAddress() {
        Decoy decoy = new Decoy("CREDENTIAL", KNOWN_GOOD, "label", null, Instant.EPOCH);

        assertThat(type.bait(decoy, "https://nest.example.com"))
                .isEqualTo("API_BASE_URL=https://nest.example.com/api/v1\nAPI_KEY=" + KNOWN_GOOD);
    }
}
