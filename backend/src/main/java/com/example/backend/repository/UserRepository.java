package com.example.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findBySchoolId(java.util.UUID schoolId);

    @org.springframework.data.jpa.repository.Query("select count(u) from User u where u.school.id = :schoolId and u.role.name = 'STUDENT' and u.active = true and u.enabled = true")
    long countActiveStudents(@org.springframework.data.repository.query.Param("schoolId") java.util.UUID schoolId);

    long countBySchoolIdAndRoleNameAndActive(java.util.UUID schoolId, String roleName, Boolean active);

    List<User> findBySchoolIdAndRoleName(java.util.UUID schoolId, String roleName);
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByEmailContainingIgnoreCase(String email);
}
