package com.example.backend.system.school.repository;

import com.example.backend.system.school.model.entity.SchoolPayment;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SchoolPaymentRepository extends JpaRepository<SchoolPayment, UUID> {
    interface PaymentAnalyticsRow {
        String getPlanCode();
        Long getAmountVnd();
        String getStatus();
        Instant getCreatedAt();
        Instant getPaidAt();
    }

    interface RevenueTotals {
        Long getPaidTransactions();
        Long getPendingTransactions();
        Long getReviewTransactions();
        Long getGrossPaidVnd();
    }

    @Query("""
            select
                coalesce(sum(case when p.status = 'PAID' then 1L else 0L end), 0L) as paidTransactions,
                coalesce(sum(case when p.status = 'PENDING' then 1L else 0L end), 0L) as pendingTransactions,
                coalesce(sum(case when p.status = 'REQUIRES_REVIEW' then 1L else 0L end), 0L) as reviewTransactions,
                coalesce(sum(case when p.status = 'PAID' then p.amountVnd else 0L end), 0L) as grossPaidVnd
            from SchoolPayment p
            """)
    RevenueTotals summarizeRevenue();

    @Query("""
            select p.planCode as planCode, p.amountVnd as amountVnd, p.status as status,
                   p.createdAt as createdAt, p.paidAt as paidAt
            from SchoolPayment p
            where (p.status = 'PAID' and coalesce(p.paidAt, p.createdAt) >= :from and coalesce(p.paidAt, p.createdAt) < :to)
               or (p.status <> 'PAID' and p.createdAt >= :from and p.createdAt < :to)
            """)
    List<PaymentAnalyticsRow> findAnalyticsRows(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select p from SchoolPayment p
            left join fetch p.school
            left join fetch p.manager
            order by p.createdAt desc
            """)
    List<SchoolPayment> findAllForAdmin();

    @Query("""
            select p from SchoolPayment p
            left join fetch p.school
            left join fetch p.manager
            where (p.status = 'PAID' and coalesce(p.paidAt, p.createdAt) >= :from and coalesce(p.paidAt, p.createdAt) < :to)
               or (p.status <> 'PAID' and p.createdAt >= :from and p.createdAt < :to)
            order by case when p.status = 'PAID' then coalesce(p.paidAt, p.createdAt) else p.createdAt end desc
            """)
    List<SchoolPayment> findAllForAdminPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SchoolPayment p where p.id = :id")
    Optional<SchoolPayment> findLockedById(@Param("id") UUID id);
    List<SchoolPayment> findTop20ByStatusInOrderByPaidAtDesc(List<String> statuses);
    Optional<SchoolPayment> findFirstByManagerIdOrderByCreatedAtDesc(Integer managerId);
    Optional<SchoolPayment> findFirstByRegistrationEmailIgnoreCaseOrderByCreatedAtDesc(String registrationEmail);
    @Query("select count(p) > 0 from SchoolPayment p where p.purpose = 'REGISTRATION' and p.status = 'PENDING' and (lower(p.registrationEmail) = lower(:email) or upper(p.registrationSchoolCode) = upper(:code) or lower(p.registrationSchoolName) = lower(:name))")
    boolean hasPendingRegistrationConflict(@Param("email") String email, @Param("code") String code, @Param("name") String name);
    List<SchoolPayment> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);
    List<SchoolPayment> findByStatusAndCreatedAtBefore(String status, java.time.Instant cutoff);
}
