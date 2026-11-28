package io.github.randybauer.cuckoosnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.RepeatFilter;
import io.github.randybauer.cuckoosnest.decoy.MissCache;
import io.github.randybauer.cuckoosnest.trip.TripwireRateLimitFilter;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * If Redis goes down, the Nest keeps catching touches. Every Redis user fails open: the cache
 * reports no miss, every touch counts as new, and the rate limit lets requests through.
 */
class RedisDownTests {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    RedisDownTests() {
        RedisConnectionFailureException down = new RedisConnectionFailureException("Redis is down");
        when(redis.opsForValue()).thenReturn(values);
        when(redis.hasKey(anyString())).thenThrow(down);
        when(redis.delete(anyString())).thenThrow(down);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenThrow(down);
        when(values.increment(anyString())).thenThrow(down);
        org.mockito.Mockito.doThrow(down).when(values).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void theMissCacheFallsBackToPostgres() {
        MissCache cache = new MissCache(redis);

        assertThat(cache.isKnownMiss("URL", "abc")).isFalse();
        cache.rememberMiss("URL", "abc"); // must not throw
        cache.forget("URL", "abc"); // must not throw
    }

    @Test
    void everyTouchCountsAsNew() {
        RepeatFilter filter = new RepeatFilter(redis);

        assertThat(filter.isFirst(UUID.randomUUID(), Alert.Channel.HTTP, "198.51.100.1"))
                .isTrue();
    }

    @Test
    void theRateLimitLetsRequestsThrough() {
        TripwireRateLimitFilter filter = new TripwireRateLimitFilter(redis, Clock.systemUTC(), 1);

        assertThat(filter.overLimit("198.51.100.1")).isFalse();
    }
}
