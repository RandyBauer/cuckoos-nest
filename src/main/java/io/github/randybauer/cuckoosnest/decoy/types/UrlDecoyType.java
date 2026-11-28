package io.github.randybauer.cuckoosnest.decoy.types;

import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyType;
import io.github.randybauer.cuckoosnest.decoy.TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * A link nobody should ever follow. Plant it in docs or code comments with a tempting label, such
 * as "internal admin dashboard". Any request to it is an alarm.
 */
@Component
public class UrlDecoyType implements DecoyType {

    public static final String ID = "URL";

    /** Path prefix for URL decoys. Kept short and bland on purpose. */
    public static final String PATH_PREFIX = "/t/";

    private static final int TOKEN_LENGTH = 22;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "URL decoy";
    }

    @Override
    public String newToken(TokenGenerator tokens) {
        return tokens.base62(TOKEN_LENGTH);
    }

    @Override
    public String bait(Decoy decoy, String baseUrl) {
        return baseUrl + PATH_PREFIX + decoy.getToken();
    }
}
