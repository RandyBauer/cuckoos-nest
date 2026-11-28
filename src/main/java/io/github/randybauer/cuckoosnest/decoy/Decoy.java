package io.github.randybauer.cuckoosnest.decoy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A decoy planted in one place. Nobody has a reason to touch it, so any touch is an alarm.
 *
 * <p>The {@code type} is the id of the plugin that created it (for example {@code URL}), and the
 * {@code token} is the secret part an intruder would have to find: a URL path, a fake key, or a
 * tool name.
 */
@Entity
@Table(name = "decoy")
public class Decoy {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(length = 500)
    private String location;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    protected Decoy() {
        // for JPA
    }

    public Decoy(String type, String token, String label, String location, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.type = Objects.requireNonNull(type, "type");
        this.token = Objects.requireNonNull(token, "token");
        this.label = Objects.requireNonNull(label, "label");
        this.location = location;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Takes the decoy out of service. Retiring twice keeps the first retirement time. */
    public void retire(Instant when) {
        if (retiredAt == null) {
            retiredAt = Objects.requireNonNull(when, "when");
        }
    }

    public boolean isActive() {
        return retiredAt == null;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public String getLabel() {
        return label;
    }

    public String getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRetiredAt() {
        return retiredAt;
    }
}
