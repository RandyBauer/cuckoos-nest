package io.github.randybauer.cuckoosnest.alert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByTouchedAtAfterOrderByTouchedAtDesc(Instant since, Limit limit);

    long countByDecoyId(UUID decoyId);
}
