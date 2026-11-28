package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class Base62Tests {

    @Test
    void encodesKnownValuesWithZeroPadding() {
        assertThat(Base62.encode(0, 3)).isEqualTo("000");
        assertThat(Base62.encode(61, 2)).isEqualTo("0z");
        assertThat(Base62.encode(62, 2)).isEqualTo("10");
        assertThat(Base62.encode(62L * 62 - 1, 2)).isEqualTo("zz");
    }

    @Test
    void theLargestCrc32FitsInSixDigits() {
        // CRC32 values are below 2^32; 62^6 is about 5.7e10, so six digits always suffice.
        assertThat(Base62.encode(0xFFFF_FFFFL, 6)).hasSize(6);
    }

    @Test
    void rejectsNegativesAndOverflow() {
        assertThatIllegalArgumentException().isThrownBy(() -> Base62.encode(-1, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> Base62.encode(62, 1));
    }
}
