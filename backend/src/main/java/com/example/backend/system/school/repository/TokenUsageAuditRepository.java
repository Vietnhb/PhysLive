package com.example.backend.system.school.repository;

import com.example.backend.system.school.model.entity.TokenUsageAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenUsageAuditRepository extends JpaRepository<TokenUsageAudit, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TokenUsageAudit> {
    List<TokenUsageAudit> findTop200BySchoolIdOrderByRecordedAtDesc(UUID schoolId);

    @org.springframework.data.jpa.repository.Query("select distinct a.operation from TokenUsageAudit a where a.school.id = :schoolId order by a.operation")
    List<String> operations(@org.springframework.data.repository.query.Param("schoolId") UUID schoolId);

    @org.springframework.data.jpa.repository.Query("select distinct a.usageMonth from TokenUsageAudit a where a.school.id = :schoolId order by a.usageMonth desc")
    List<java.time.LocalDate> months(@org.springframework.data.repository.query.Param("schoolId") UUID schoolId);
}
