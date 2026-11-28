package io.github.randybauer.cuckoosnest.decoy;

/** Base62 (0-9, A-Z, a-z) encoding of non-negative numbers, used for token checksums. */
public final class Base62 {

    public static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private Base62() {}

    /**
     * Encodes {@code value} in base62, left-padded with '0' to exactly {@code width} characters.
     *
     * @throws IllegalArgumentException if the value is negative or does not fit in {@code width}
     */
    public static String encode(long value, int width) {
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative: " + value);
        }
        char[] out = new char[width];
        long rest = value;
        for (int i = width - 1; i >= 0; i--) {
            out[i] = ALPHABET.charAt((int) (rest % 62));
            rest /= 62;
        }
        if (rest != 0) {
            throw new IllegalArgumentException(value + " does not fit in " + width + " base62 digits");
        }
        return new String(out);
    }
}
