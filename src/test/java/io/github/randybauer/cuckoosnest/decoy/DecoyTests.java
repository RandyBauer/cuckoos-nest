package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DecoyTests {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");

    @Test
    void newDecoyIsActiveAndHasAnId() {
        Decoy decoy = new Decoy("URL", "tok", "label", null, T0);

        assertThat(decoy.isActive()).isTrue();
        assertThat(decoy.getId()).isNotNull();
        assertThat(decoy.getRetiredAt()).isNull();
    }

    @Test
    void retiringKeepsTheFirstRetirementTime() {
        Decoy decoy = new Decoy("URL", "tok", "label", null, T0);

        decoy.retire(T0.plusSeconds(60));
        decoy.retire(T0.plusSeconds(120));

        assertThat(decoy.isActive()).isFalse();
        assertThat(decoy.getRetiredAt()).isEqualTo(T0.plusSeconds(60));
    }

    @Test
    void requiredFieldsAreRequired() {
        assertThatNullPointerException().isThrownBy(() -> new Decoy(null, "tok", "label", null, T0));
        assertThatNullPointerException().isThrownBy(() -> new Decoy("URL", null, "label", null, T0));
        assertThatNullPointerException().isThrownBy(() -> new Decoy("URL", "tok", null, null, T0));
        assertThatNullPointerException().isThrownBy(() -> new Decoy("URL", "tok", "label", null, null));
    }
}
