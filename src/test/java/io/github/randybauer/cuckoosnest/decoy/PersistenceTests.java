package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;

/** Runs against a real Postgres, migrated by Flyway, with Hibernate validating the entities. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PersistenceTests {

    private static final Instant NOW = Instant.parse("2026-12-01T12:00:00Z");

    @Autowired
    private DecoyRepository decoys;

    @Autowired
    private AlertRepository alerts;

    @Test
    void testProfileStartsWithNoSeedData() {
        assertThat(decoys.count()).isZero();
    }

    @Test
    void savesAndFindsADecoyByToken() {
        decoys.saveAndFlush(new Decoy("URL", "abc123", "Fake admin page", "docs/ADMIN.md", NOW));

        assertThat(decoys.findByToken("abc123")).get().satisfies(d -> {
            assertThat(d.getLabel()).isEqualTo("Fake admin page");
            assertThat(d.getLocation()).isEqualTo("docs/ADMIN.md");
            assertThat(d.isActive()).isTrue();
        });
        assertThat(decoys.findByToken("nope")).isEmpty();
    }

    @Test
    void tokensAreUnique() {
        decoys.saveAndFlush(new Decoy("URL", "same-token", "First", null, NOW));

        assertThatThrownBy(() -> decoys.saveAndFlush(new Decoy("URL", "same-token", "Second", null, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void alertsKeepJsonDetailsAndListNewestFirst() {
        Decoy decoy = decoys.saveAndFlush(new Decoy("URL", "t1", "Bait", null, NOW));
        alerts.saveAndFlush(alert(decoy, NOW.minus(2, ChronoUnit.HOURS), Map.of("path", "/t/t1")));
        alerts.saveAndFlush(alert(decoy, NOW.minus(1, ChronoUnit.HOURS), Map.of("method", "POST")));
        alerts.saveAndFlush(alert(decoy, NOW.minus(3, ChronoUnit.DAYS), Map.of()));

        List<Alert> recent =
                alerts.findByTouchedAtAfterOrderByTouchedAtDesc(NOW.minus(1, ChronoUnit.DAYS), Limit.of(10));

        assertThat(recent)
                .extracting(Alert::getTouchedAt)
                .containsExactly(NOW.minus(1, ChronoUnit.HOURS), NOW.minus(2, ChronoUnit.HOURS));
        assertThat(recent.get(0).getDetails()).containsEntry("method", "POST");
        assertThat(alerts.countByDecoyId(decoy.getId())).isEqualTo(3);
    }

    @Test
    void recentAlertsRespectTheLimit() {
        Decoy decoy = decoys.saveAndFlush(new Decoy("URL", "t2", "Bait", null, NOW));
        for (int i = 1; i <= 5; i++) {
            alerts.saveAndFlush(alert(decoy, NOW.minus(i, ChronoUnit.MINUTES), Map.of()));
        }

        assertThat(alerts.findByTouchedAtAfterOrderByTouchedAtDesc(NOW.minus(1, ChronoUnit.DAYS), Limit.of(2)))
                .hasSize(2);
    }

    private static Alert alert(Decoy decoy, Instant when, Map<String, Object> details) {
        return new Alert(decoy, when, Alert.Channel.HTTP, "198.51.100.4", "test-agent", details);
    }
}
