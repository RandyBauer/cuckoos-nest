package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TokenGeneratorTests {

    private final TokenGenerator tokens = new TokenGenerator();

    @ParameterizedTest
    @ValueSource(ints = {1, 22, 64})
    void tokensHaveTheRequestedLengthAndOnlyBase62Characters(int length) {
        String token = tokens.base62(length);

        assertThat(token).hasSize(length).matches("[0-9A-Za-z]+");
    }

    @Test
    void tokensDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertThat(seen.add(tokens.base62(22)))
                    .as("token %d was a repeat", i)
                    .isTrue();
        }
    }

    @Test
    void everyCharacterOfTheAlphabetGetsUsed() {
        // 20,000 characters drawn from 62: the chance that any one never shows up is negligible.
        Set<Character> used = new HashSet<>();
        for (char c : tokens.base62(20_000).toCharArray()) {
            used.add(c);
        }
        assertThat(used).hasSize(TokenGenerator.ALPHABET.length());
    }

    @Test
    void lengthMustBePositive() {
        assertThatIllegalArgumentException().isThrownBy(() -> tokens.base62(0));
    }
}
