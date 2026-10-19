package com.example.backend.system.reviewer.repository;

import com.example.backend.system.reviewer.model.entity.LawGap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawGapRepository extends JpaRepository<LawGap, UUID> {
    Optional<LawGap> findByGapKey(String gapKey);
    List<LawGap> findTop300ByStatusOrderByOccurrencesDescUpdatedAtDesc(String status);
    long countByStatus(String status);
}
