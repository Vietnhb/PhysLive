package com.example.backend.repository;

import com.example.backend.entity.Permission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    List<Permission> findByCodeIn(Collection<String> codes);
}
