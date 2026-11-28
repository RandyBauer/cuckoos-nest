package io.github.randybauer.cuckoosnest.alert;

import io.github.randybauer.cuckoosnest.decoy.Decoy;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records decoy touches and lists recent ones. */
@Service
public class AlertService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);

    static final int MAX_IP = 64;
    static final int MAX_USER_AGENT = 500;
    static final int MAX_DETAIL_VALUE = 500;
    static final int MAX_LIMIT = 500;

    private final AlertRepository alerts;
    private final Clock clock;
    private final RepeatFilter repeats;

    public AlertService(AlertRepository alerts, Clock clock, RepeatFilter repeats) {
        this.alerts = alerts;
        this.clock = clock;
        this.repeats = repeats;
    }

    /**
     * Records a touch from a tripwire, unless the same source touched the same decoy in the last
     * minute (see {@link RepeatFilter}). Returns the new alert, or empty for a repeat.
     */
    @Transactional
    public Optional<Alert> recordTouch(
            Decoy decoy, Alert.Channel channel, String sourceIp, String userAgent, Map<String, ?> details) {
        String source = sourceIp != null ? sourceIp : userAgent;
        if (!repeats.isFirst(decoy.getId(), channel, source)) {
            LOG.info(
                    "Repeat touch of decoy {} from {} within {}s; not recorded again.",
                    decoy.getId(),
                    forLog(source),
                    RepeatFilter.WINDOW.toSeconds());
            return Optional.empty();
        }
        return Optional.of(record(decoy, channel, sourceIp, userAgent, details));
    }

    /**
     * Records one touch of a decoy. Input comes from strangers, so every field is cut to a safe
     * length before it is stored.
     */
    @Transactional
    public Alert record(Decoy decoy, Alert.Channel channel, String sourceIp, String userAgent, Map<String, ?> details) {
        Alert alert = alerts.save(new Alert(
                decoy,
                clock.instant(),
                channel,
                truncate(sourceIp, MAX_IP),
                truncate(userAgent, MAX_USER_AGENT),
                cleanDetails(details)));
        // The key-value pairs become separate JSON fields when structured logging is on (prod),
        // so alerts can be searched by decoy, type, channel, or source IP.
        LOG.atWarn()
                .addKeyValue("event", "decoy.touched")
                .addKeyValue("decoyId", decoy.getId())
                .addKeyValue("decoyType", decoy.getType())
                .addKeyValue("channel", channel)
                .addKeyValue("sourceIp", forLog(alert.getSourceIp()))
                .log(
                        "Decoy touched: \"{}\" ({}) via {} from {}",
                        forLog(decoy.getLabel()),
                        decoy.getType(),
                        channel,
                        forLog(alert.getSourceIp()));
        return alert;
    }

    /** Alerts from the last {@code window}, newest first, at most {@code limit} of them. */
    @Transactional(readOnly = true)
    public List<Alert> recent(Duration window, int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return alerts.findByTouchedAtAfterOrderByTouchedAtDesc(clock.instant().minus(window), Limit.of(safeLimit));
    }

    /** How many alerts each decoy has. Decoys with none are absent from the map. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> countsByDecoy() {
        return alerts.countPerDecoy().stream()
                .collect(Collectors.toMap(
                        AlertRepository.DecoyAlertCount::getDecoyId, AlertRepository.DecoyAlertCount::getAlerts));
    }

    /** One alert with its decoy loaded. */
    @Transactional(readOnly = true)
    public Optional<Alert> find(long id) {
        return alerts.findWithDecoyById(id);
    }

    /**
     * Makes a stranger-supplied value safe to put in a log line. A user agent such as
     * {@code "curl\r\n2026-12-01 ERROR fake entry"} would otherwise forge a second line in plain-text
     * logs (log injection). Line breaks and other control characters become '_'.
     */
    static String forLog(String value) {
        if (value == null) {
            return null;
        }
        String noBreaks = value.replace('\r', '_').replace('\n', '_');
        StringBuilder sb = new StringBuilder(Math.min(noBreaks.length(), MAX_USER_AGENT));
        for (int i = 0; i < noBreaks.length() && i < MAX_USER_AGENT; i++) {
            char c = noBreaks.charAt(i);
            sb.append(Character.isISOControl(c) ? '_' : c);
        }
        return sb.toString();
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static Map<String, Object> cleanDetails(Map<String, ?> details) {
        Map<String, Object> clean = new LinkedHashMap<>();
        if (details != null) {
            details.forEach((key, value) -> {
                if (key != null && value != null) {
                    clean.put(truncate(key, 64), truncate(value.toString(), MAX_DETAIL_VALUE));
                }
            });
        }
        return clean;
    }
}
