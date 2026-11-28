package io.github.randybauer.cuckoosnest.decoy;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Makes unguessable tokens for decoys.
 *
 * <p>Tokens use base62 (digits and both letter cases) so they are safe in URLs, file names, and
 * environment variables without escaping. Each character carries log2(62) ≈ 5.95 bits, so a
 * 22-character token has about 131 bits of randomness.
 */
@Component
public class TokenGenerator {

    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random;

    public TokenGenerator() {
        this(new SecureRandom());
    }

    TokenGenerator(SecureRandom random) {
        this.random = random;
    }

    /** Returns {@code length} random base62 characters. */
    public String base62(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be at least 1, was " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
