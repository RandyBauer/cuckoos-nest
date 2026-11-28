package io.github.randybauer.cuckoosnest.trip;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits how fast one IP address can hit the public tripwires ({@code /t/} and {@code /api/v1/}).
 * They have to be public, so this keeps a flood from turning into a flood of database work.
 *
 * <p>Fixed one-minute windows counted in Redis ({@code INCR} plus {@code EXPIRE}). Over the limit,
 * every tripwire path answers the same {@code 429}, so the limit can't be used to tell traps
 * apart. If Redis is down, requests pass: the tripwires stay up.
 */
@Component
public class TripwireRateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TripwireRateLimitFilter.class);
    static final String PREFIX = "nest:rate:";

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final int perMinute;

    public TripwireRateLimitFilter(
            StringRedisTemplate redis, Clock clock, @Value("${nest.tripwire.requests-per-minute:120}") int perMinute) {
        this.redis = redis;
        this.clock = clock;
        this.perMinute = perMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/t/") || path.startsWith("/api/v1/") || path.equals("/api/v1"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (overLimit(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, "60");
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Too Many Requests");
            return;
        }
        chain.doFilter(request, response);
    }

    /** True if this IP has used up its requests for the current minute. Counts this request. */
    public boolean overLimit(String ip) {
        long minute = clock.millis() / 60_000;
        String key = PREFIX + (ip == null ? "unknown" : ip) + ":" + minute;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofMinutes(2));
            }
            return count != null && count > perMinute;
        } catch (DataAccessException e) {
            LOG.warn("Redis unavailable; tripwire rate limit skipped: {}", e.getMessage());
            return false;
        }
    }
}
