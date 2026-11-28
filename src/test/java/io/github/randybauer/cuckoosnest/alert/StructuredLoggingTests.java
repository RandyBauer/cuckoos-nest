package io.github.randybauer.cuckoosnest.alert;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * An alert is logged at WARN with its facts as key-value pairs. In production, Spring Boot's ECS
 * formatter turns each pair into its own JSON field (see application-prod.properties).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class StructuredLoggingTests {

    @Autowired
    private DecoyService decoys;

    @Autowired
    private AlertService alerts;

    private final ListAppender<ILoggingEvent> captured = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AlertService.class);

    @BeforeEach
    void attach() {
        captured.start();
        logger.addAppender(captured);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(captured);
    }

    @Test
    void anAlertIsLoggedAtWarnWithSearchableFields() {
        Decoy decoy = decoys.create("URL", "Fake admin page", null);

        alerts.record(decoy, Alert.Channel.HTTP, "203.0.113.77", "curl/8", Map.of());

        ILoggingEvent event = captured.list.getLast();
        Map<String, Object> fields =
                event.getKeyValuePairs().stream().collect(Collectors.toMap(kv -> kv.key, kv -> kv.value));
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("Fake admin page").contains("203.0.113.77");
        assertThat(fields)
                .containsEntry("event", "decoy.touched")
                .containsEntry("decoyId", decoy.getId())
                .containsEntry("decoyType", "URL")
                .containsEntry("channel", Alert.Channel.HTTP)
                .containsEntry("sourceIp", "203.0.113.77");
    }
}
