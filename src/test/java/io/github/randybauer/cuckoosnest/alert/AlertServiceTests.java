package io.github.randybauer.cuckoosnest.alert;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AlertServiceTests {

    @Autowired
    private AlertService alerts;

    @Autowired
    private DecoyService decoys;

    @Test
    void recordsATouchWithItsDetails() {
        Decoy decoy = decoys.create("URL", "bait", null);

        Alert alert = alerts.record(decoy, Alert.Channel.HTTP, "198.51.100.9", "curl/8", Map.of("method", "GET"));

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.getDecoy().getId()).isEqualTo(decoy.getId());
        assertThat(alert.getDetails()).containsEntry("method", "GET");
        assertThat(alerts.recent(Duration.ofMinutes(5), 10))
                .extracting(Alert::getId)
                .contains(alert.getId());
    }

    @Test
    void strangerSuppliedFieldsAreCutToSafeLengths() {
        Decoy decoy = decoys.create("URL", "bait", null);
        Map<String, Object> details = new HashMap<>();
        details.put("path", "/t/" + "a".repeat(5_000));
        details.put("ignored", null);

        Alert alert = alerts.record(decoy, Alert.Channel.HTTP, "9".repeat(200), "U".repeat(5_000), details);

        assertThat(alert.getSourceIp()).hasSize(AlertService.MAX_IP);
        assertThat(alert.getUserAgent()).hasSize(AlertService.MAX_USER_AGENT);
        assertThat((String) alert.getDetails().get("path")).hasSize(AlertService.MAX_DETAIL_VALUE);
        assertThat(alert.getDetails()).doesNotContainKey("ignored");
    }

    @Test
    void recentClampsTheLimit() {
        Decoy decoy = decoys.create("URL", "bait", null);
        alerts.record(decoy, Alert.Channel.HTTP, null, null, null);
        alerts.record(decoy, Alert.Channel.HTTP, null, null, null);

        assertThat(alerts.recent(Duration.ofMinutes(5), 0)).hasSize(1);
        assertThat(alerts.recent(Duration.ofMinutes(5), 10_000)).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void forLogStopsForgedLogLines() {
        String attack = "curl/8\r\n2026-12-01 ERROR Admin password changed\u001b[31m";

        assertThat(AlertService.forLog(attack))
                .doesNotContain("\r")
                .doesNotContain("\n")
                .doesNotContain("\u001b")
                .isEqualTo("curl/8__2026-12-01 ERROR Admin password changed_[31m");
        assertThat(AlertService.forLog(null)).isNull();
        assertThat(AlertService.forLog("x".repeat(5_000))).hasSize(AlertService.MAX_USER_AGENT);
    }

    @Test
    void truncateLeavesShortValuesAlone() {
        assertThat(AlertService.truncate(null, 3)).isNull();
        assertThat(AlertService.truncate("abc", 3)).isEqualTo("abc");
        assertThat(AlertService.truncate("abcd", 3)).isEqualTo("abc");
    }
}
