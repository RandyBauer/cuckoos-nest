package io.github.randybauer.cuckoosnest.trip;

import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertService;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.github.randybauer.cuckoosnest.decoy.types.CredentialDecoyType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The decoy API that fake keys are "for". Every request under {@code /api/v1/} is checked for a key
 * in the three places clients usually send one. A live fake key records an alert.
 *
 * <p>Like the URL tripwire, the answer never changes: always {@code 401} with the same body, so a
 * caller can't tell a trapped key from a wrong one.
 */
@RestController
public class CredentialTripController {

    static final String BODY = "{\"error\":\"invalid_api_key\"}";

    private final DecoyService decoys;
    private final AlertService alerts;

    public CredentialTripController(DecoyService decoys, AlertService alerts) {
        this.decoys = decoys;
        this.alerts = alerts;
    }

    @RequestMapping(CredentialDecoyType.API_PATH + "/**")
    public ResponseEntity<String> call(HttpServletRequest request) {
        presentedKey(request)
                .ifPresent(presented -> decoys.findActive(CredentialDecoyType.ID, presented.key())
                        .ifPresent(decoy -> record(decoy, presented.via(), request)));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BODY);
    }

    /** A key the caller sent, and where it was found. */
    record PresentedKey(String key, String via) {}

    /** Looks for a well-formed key in the Authorization header, X-API-Key, then ?api_key=. */
    static Optional<PresentedKey> presentedKey(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String key = auth.substring(7).strip();
            if (CredentialDecoyType.isWellFormed(key)) {
                return Optional.of(new PresentedKey(key, "authorization-header"));
            }
        }
        String header = request.getHeader("X-API-Key");
        if (CredentialDecoyType.isWellFormed(header)) {
            return Optional.of(new PresentedKey(header, "x-api-key-header"));
        }
        String param = request.getParameter("api_key");
        if (CredentialDecoyType.isWellFormed(param)) {
            return Optional.of(new PresentedKey(param, "query-parameter"));
        }
        return Optional.empty();
    }

    private void record(Decoy decoy, String via, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("method", request.getMethod());
        details.put("path", request.getRequestURI());
        details.put("presentedVia", via);
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            details.put("forwardedFor", forwarded);
        }
        alerts.recordTouch(
                decoy, Alert.Channel.HTTP, request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT), details);
    }
}
