package io.github.randybauer.cuckoosnest.decoy;

import java.util.UUID;

/** Thrown when a decoy id does not exist. */
public class DecoyNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DecoyNotFoundException(UUID id) {
        super("No decoy with id " + id);
    }
}
