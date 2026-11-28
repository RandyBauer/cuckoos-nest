package io.github.randybauer.cuckoosnest.alert;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** A burst of touches from one source becomes one alert (real Redis, real Postgres). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RepeatTouchTests {

    @Autowired
    private AlertService alerts;

    @Autowired
    private DecoyService decoys;

    @Autowired
    private AlertRepository repository;

    @Test
    void repeatsFromTheSameSourceInsideTheWindowAreNotStoredAgain() {
        Decoy decoy = decoys.create("URL", "Burst target", null);

        assertThat(alerts.recordTouch(decoy, Alert.Channel.HTTP, "198.51.100.1", "bot", Map.of()))
                .isPresent();
        for (int i = 0; i < 25; i++) {
            assertThat(alerts.recordTouch(decoy, Alert.Channel.HTTP, "198.51.100.1", "bot", Map.of()))
                    .isEmpty();
        }

        assertThat(repository.countByDecoyId(decoy.getId())).isEqualTo(1);
    }

    @Test
    void differentSourcesAndDifferentDecoysAreSeparateAlerts() {
        Decoy first = decoys.create("URL", "First", null);
        Decoy second = decoys.create("URL", "Second", null);

        assertThat(alerts.recordTouch(first, Alert.Channel.HTTP, "198.51.100.2", null, Map.of()))
                .isPresent();
        assertThat(alerts.recordTouch(first, Alert.Channel.HTTP, "198.51.100.3", null, Map.of()))
                .isPresent();
        assertThat(alerts.recordTouch(second, Alert.Channel.HTTP, "198.51.100.2", null, Map.of()))
                .isPresent();

        assertThat(repository.countByDecoyId(first.getId())).isEqualTo(2);
        assertThat(repository.countByDecoyId(second.getId())).isEqualTo(1);
    }

    @Test
    void mcpTouchesWithNoIpAreGroupedByClient() {
        Decoy tool = decoys.create("MCP_TOOL", "Tool", null);

        assertThat(alerts.recordTouch(tool, Alert.Channel.MCP, null, "agent-a/1.0", Map.of()))
                .isPresent();
        assertThat(alerts.recordTouch(tool, Alert.Channel.MCP, null, "agent-a/1.0", Map.of()))
                .isEmpty();
        assertThat(alerts.recordTouch(tool, Alert.Channel.MCP, null, "agent-b/2.0", Map.of()))
                .isPresent();
    }
}
