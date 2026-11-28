package io.github.randybauer.cuckoosnest.trip;

import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertService;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.github.randybauer.cuckoosnest.decoy.types.UrlDecoyType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The tripwire for URL decoys. Any request to {@code /t/{token}}, with any HTTP method, is checked.
 *
 * <p>The response is always the same plain 404, whether the token is a live decoy, a retired one,
 * or random text. So a visitor can't learn which links are traps.
 */
@RestController
public class UrlTripController {

    static final String BODY = "Not Found";

    private final DecoyService decoys;
    private final AlertService alerts;

    public UrlTripController(DecoyService decoys, AlertService alerts) {
        this.decoys = decoys;
        this.alerts = alerts;
    }

    @RequestMapping(UrlDecoyType.PATH_PREFIX + "{token}")
    public ResponseEntity<String> touch(@PathVariable String token, HttpServletRequest request) {
        decoys.findActive(UrlDecoyType.ID, token)
                .ifPresent(decoy -> alerts.recordTouch(
                        decoy,
                        Alert.Channel.HTTP,
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"),
                        details(request)));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_PLAIN)
                .body(BODY);
    }

    private static Map<String, String> details(HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("method", request.getMethod());
        details.put("path", request.getRequestURI());
        putIfPresent(details, "query", request.getQueryString());
        putIfPresent(details, "referer", request.getHeader("Referer"));
        putIfPresent(details, "forwardedFor", request.getHeader("X-Forwarded-For"));
        return details;
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
