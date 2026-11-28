package io.github.randybauer.cuckoosnest.alert;

import io.github.randybauer.cuckoosnest.decoy.Decoy;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public AlertService(AlertRepository alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
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
        LOG.warn(
                "Decoy touched: decoy={} type={} label=\"{}\" channel={} sourceIp={}",
                decoy.getId(),
                decoy.getType(),
                decoy.getLabel(),
                channel,
                alert.getSourceIp());
        return alert;
    }

    /** Alerts from the last {@code window}, newest first, at most {@code limit} of them. */
    @Transactional(readOnly = true)
    public List<Alert> recent(Duration window, int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return alerts.findByTouchedAtAfterOrderByTouchedAtDesc(clock.instant().minus(window), Limit.of(safeLimit));
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
