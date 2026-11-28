package io.github.randybauer.cuckoosnest.alert;

import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Collapses bursts. A script that requests the same decoy URL 500 times in a minute should make one
 * alert, not 500. The first touch from a given source starts a 60-second window in Redis
 * ({@code SET NX EX}); repeats inside it are counted in the log but not stored.
 *
 * <p>If Redis is down, every touch is treated as new: duplicate alerts are better than lost ones.
 */
@Component
public class RepeatFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RepeatFilter.class);
    static final Duration WINDOW = Duration.ofSeconds(60);
    static final String PREFIX = "nest:seen:";

    private final StringRedisTemplate redis;

    public RepeatFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** True if this is the first touch of the decoy by this source in the current window. */
    public boolean isFirst(UUID decoyId, Alert.Channel channel, String source) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(decoyId, channel, source), "1", WINDOW));
        } catch (DataAccessException e) {
            LOG.warn("Redis unavailable; recording the touch without repeat filtering: {}", e.getMessage());
            return true;
        }
    }

    static String key(UUID decoyId, Alert.Channel channel, String source) {
        String who = source == null ? "unknown" : source;
        if (who.length() > 200) {
            who = who.substring(0, 200);
        }
        return PREFIX + decoyId + ":" + channel + ":" + who;
    }
}
