package io.github.randybauer.cuckoosnest.alert;

import io.github.randybauer.cuckoosnest.decoy.Decoy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One touch of a decoy. Alerts are append-only: nothing in the app updates or deletes them. */
@Entity
@Table(name = "alert")
public class Alert {

    /** How the decoy was touched. */
    public enum Channel {
        HTTP,
        MCP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decoy_id", nullable = false, updatable = false)
    private Decoy decoy;

    @Column(name = "touched_at", nullable = false, updatable = false)
    private Instant touchedAt;

    @Column(nullable = false, length = 32, updatable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "source_ip", length = 64, updatable = false)
    private String sourceIp;

    @Column(name = "user_agent", length = 500, updatable = false)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    protected Alert() {
        // for JPA
    }

    public Alert(
            Decoy decoy,
            Instant touchedAt,
            Channel channel,
            String sourceIp,
            String userAgent,
            Map<String, Object> details) {
        this.decoy = Objects.requireNonNull(decoy, "decoy");
        this.touchedAt = Objects.requireNonNull(touchedAt, "touchedAt");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public Long getId() {
        return id;
    }

    public Decoy getDecoy() {
        return decoy;
    }

    public Instant getTouchedAt() {
        return touchedAt;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
