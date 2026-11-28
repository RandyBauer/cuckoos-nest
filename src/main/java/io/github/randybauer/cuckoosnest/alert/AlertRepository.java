package io.github.randybauer.cuckoosnest.alert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    /** Recent alerts with their decoys loaded, so callers can read decoy details. */
    @EntityGraph(attributePaths = "decoy")
    List<Alert> findByTouchedAtAfterOrderByTouchedAtDesc(Instant since, Limit limit);

    @EntityGraph(attributePaths = "decoy")
    Optional<Alert> findWithDecoyById(Long id);

    long countByDecoyId(UUID decoyId);
}
