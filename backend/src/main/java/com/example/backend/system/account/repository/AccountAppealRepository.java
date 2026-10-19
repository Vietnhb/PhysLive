package com.example.backend.system.account.repository;

import com.example.backend.system.account.model.entity.AccountAppeal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountAppealRepository extends JpaRepository<AccountAppeal, UUID> {
    List<AccountAppeal> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<AccountAppeal> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
