package io.github.randybauer.cuckoosnest.decoy;

import java.util.Collection;

/** Thrown when someone asks for a decoy type that no plugin provides. */
public class UnknownDecoyTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownDecoyTypeException(String id, Collection<String> known) {
        super("Unknown decoy type '" + id + "'. Known types: " + String.join(", ", known));
    }
}
