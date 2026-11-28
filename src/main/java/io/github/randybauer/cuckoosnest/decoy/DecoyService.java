package io.github.randybauer.cuckoosnest.decoy;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, lists, and retires decoys. */
@Service
public class DecoyService {

    static final int MAX_LABEL = 200;
    static final int MAX_LOCATION = 500;
    private static final int TOKEN_ATTEMPTS = 5;

    private final DecoyRepository decoys;
    private final DecoyTypeRegistry types;
    private final TokenGenerator tokens;
    private final Clock clock;

    public DecoyService(DecoyRepository decoys, DecoyTypeRegistry types, TokenGenerator tokens, Clock clock) {
        this.decoys = decoys;
        this.types = types;
        this.tokens = tokens;
        this.clock = clock;
    }

    /**
     * Creates a decoy of the given type.
     *
     * @throws UnknownDecoyTypeException if no plugin provides {@code typeId}
     * @throws IllegalArgumentException if the label is blank or a field is too long
     */
    @Transactional
    public Decoy create(String typeId, String label, String location) {
        DecoyType type = types.get(typeId);
        String cleanLabel = requireText(label, "label", MAX_LABEL);
        String cleanLocation = optionalText(location, "location", MAX_LOCATION);
        return decoys.save(new Decoy(type.id(), uniqueToken(type), cleanLabel, cleanLocation, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<Decoy> list() {
        return decoys.findAllByOrderByCreatedAtDesc();
    }

    /** Takes a decoy out of service. Touches after this are ignored. */
    @Transactional
    public Decoy retire(UUID id) {
        Decoy decoy = decoys.findById(id).orElseThrow(() -> new DecoyNotFoundException(id));
        decoy.retire(clock.instant());
        return decoy;
    }

    /** Finds an active decoy of the given type by its secret token. */
    @Transactional(readOnly = true)
    public Optional<Decoy> findActive(String typeId, String token) {
        return decoys.findByToken(token).filter(d -> d.getType().equals(typeId)).filter(Decoy::isActive);
    }

    /** Tokens are random enough that a clash is practically impossible, but check anyway. */
    private String uniqueToken(DecoyType type) {
        for (int i = 0; i < TOKEN_ATTEMPTS; i++) {
            String token = type.newToken(tokens);
            if (decoys.findByToken(token).isEmpty()) {
                return token;
            }
        }
        throw new IllegalStateException("Could not make a unique token for " + type.id());
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return optionalText(value, field, max);
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return trimmed;
    }
}
