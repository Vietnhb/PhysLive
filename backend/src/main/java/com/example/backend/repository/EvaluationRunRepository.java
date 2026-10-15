package com.example.backend.repository;

import com.example.backend.entity.EvaluationRun;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {
    @Query("""
            select run from EvaluationRun run
            where (:status is null or run.status = :status)
            order by run.createdAt desc
            """)
    Page<EvaluationRun> search(@Param("status") String status, Pageable pageable);
}
