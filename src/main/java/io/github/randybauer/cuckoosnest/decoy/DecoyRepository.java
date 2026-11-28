package io.github.randybauer.cuckoosnest.decoy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecoyRepository extends JpaRepository<Decoy, UUID> {

    Optional<Decoy> findByToken(String token);

    List<Decoy> findAllByOrderByCreatedAtDesc();
}
