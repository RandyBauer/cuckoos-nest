package io.github.randybauer.cuckoosnest.decoy.types;

import io.github.randybauer.cuckoosnest.decoy.Base62;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyType;
import io.github.randybauer.cuckoosnest.decoy.TokenGenerator;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.springframework.stereotype.Component;

/**
 * A fake API key. Plant it in a config file next to the decoy API's address; using it anywhere
 * against that API is an alarm.
 *
 * <p>Format: {@code nest_} + 30 random base62 characters + a 6-character base62 CRC32 checksum of
 * those 30. The {@code nest_} prefix keeps it from ever looking like a real cloud provider's key.
 * The checksum (the same idea GitHub uses for its tokens) lets the Nest reject random strings
 * without a database lookup.
 */
@Component
public class CredentialDecoyType implements DecoyType {

    public static final String ID = "CREDENTIAL";
    public static final String PREFIX = "nest_";

    /** Base path of the decoy API that the fake keys are "for". */
    public static final String API_PATH = "/api/v1";

    static final int BODY_LENGTH = 30;
    static final int CHECKSUM_LENGTH = 6;
    private static final Pattern SHAPE = Pattern.compile("nest_[0-9A-Za-z]{" + (BODY_LENGTH + CHECKSUM_LENGTH) + "}");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Fake API key";
    }

    @Override
    public String newToken(TokenGenerator tokens) {
        String body = tokens.base62(BODY_LENGTH);
        return PREFIX + body + checksum(body);
    }

    @Override
    public String bait(Decoy decoy, String baseUrl) {
        return "API_BASE_URL=" + baseUrl + API_PATH + "\nAPI_KEY=" + decoy.getToken();
    }

    /** True if {@code candidate} has the right prefix, length, alphabet, and checksum. */
    public static boolean isWellFormed(String candidate) {
        if (candidate == null || !SHAPE.matcher(candidate).matches()) {
            return false;
        }
        String body = candidate.substring(PREFIX.length(), PREFIX.length() + BODY_LENGTH);
        String check = candidate.substring(PREFIX.length() + BODY_LENGTH);
        return checksum(body).equals(check);
    }

    static String checksum(String body) {
        CRC32 crc = new CRC32();
        crc.update(body.getBytes(StandardCharsets.US_ASCII));
        return Base62.encode(crc.getValue(), CHECKSUM_LENGTH);
    }
}
