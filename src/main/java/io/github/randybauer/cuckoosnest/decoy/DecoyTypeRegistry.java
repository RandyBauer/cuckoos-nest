package io.github.randybauer.cuckoosnest.decoy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Finds every {@link DecoyType} plugin at startup and looks them up by id. */
@Component
public class DecoyTypeRegistry {

    private static final Pattern ID = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");

    private final Map<String, DecoyType> byId;

    public DecoyTypeRegistry(List<DecoyType> types) {
        Map<String, DecoyType> map = new TreeMap<>();
        for (DecoyType type : types) {
            if (!ID.matcher(type.id()).matches()) {
                throw new IllegalStateException("Bad decoy type id '" + type.id() + "' in " + type.getClass());
            }
            DecoyType previous = map.putIfAbsent(type.id(), type);
            if (previous != null) {
                throw new IllegalStateException("Two decoy types share the id '" + type.id() + "': "
                        + previous.getClass().getName() + " and "
                        + type.getClass().getName());
            }
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public Optional<DecoyType> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Looks up a type the caller expects to exist. */
    public DecoyType get(String id) {
        return find(id).orElseThrow(() -> new UnknownDecoyTypeException(id, byId.keySet()));
    }

    /** All types, sorted by id. */
    public Collection<DecoyType> all() {
        return byId.values();
    }
}
