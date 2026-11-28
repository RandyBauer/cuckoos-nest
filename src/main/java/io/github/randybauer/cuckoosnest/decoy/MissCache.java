package io.github.randybauer.cuckoosnest.decoy;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Remembers in Redis which tokens are <em>not</em> decoys (a "negative cache").
 *
 * <p>Scanners and bots throw thousands of random paths and keys at the public tripwires. Without
 * this, every one of them is a Postgres query. With it, a repeated miss is answered from Redis.
 * Real decoy hits always go to Postgres, so they never see stale data.
 *
 * <p>If Redis is down, every method fails open: the lookup goes to Postgres as if nothing were
 * cached. An alarm system must not lose alarms because its cache broke.
 */
@Component
public class MissCache {

    private static final Logger LOG = LoggerFactory.getLogger(MissCache.class);
    static final Duration TTL = Duration.ofMinutes(10);
    static final String PREFIX = "nest:miss:";

    private final StringRedisTemplate redis;

    public MissCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** True if this token was recently looked up and was not a decoy of this type. */
    public boolean isKnownMiss(String typeId, String token) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(typeId, token)));
        } catch (DataAccessException e) {
            LOG.warn("Redis unavailable for miss lookup; falling back to Postgres: {}", e.getMessage());
            return false;
        }
    }

    public void rememberMiss(String typeId, String token) {
        try {
            redis.opsForValue().set(key(typeId, token), "1", TTL);
        } catch (DataAccessException e) {
            LOG.warn("Redis unavailable; miss not cached: {}", e.getMessage());
        }
    }

    /** Called when a decoy is created, so an earlier miss for its token can't hide it. */
    public void forget(String typeId, String token) {
        try {
            redis.delete(key(typeId, token));
        } catch (DataAccessException e) {
            LOG.warn("Redis unavailable; could not clear a cached miss: {}", e.getMessage());
        }
    }

    static String key(String typeId, String token) {
        // Tokens come from strangers: bound the key length so they can't fill Redis with huge keys.
        String bounded = token.length() > 128 ? token.substring(0, 128) : token;
        return PREFIX + typeId + ":" + bounded;
    }
}
