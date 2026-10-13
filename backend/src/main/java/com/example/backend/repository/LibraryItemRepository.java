package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.LibraryItem;

@Repository
public interface LibraryItemRepository extends JpaRepository<LibraryItem, UUID> {

    boolean existsByFolderIdAndActiveTrue(UUID folderId);

    long countByFolderIdAndActiveTrue(UUID folderId);

    List<LibraryItem> findByVisibilityAndActiveOrderByCreatedAtDesc(String visibility, boolean active);

    List<LibraryItem> findByOwnerIdAndActiveOrderByCreatedAtDesc(Integer ownerId, boolean active);

    @Query("SELECT l FROM LibraryItem l WHERE l.active = true AND (l.visibility = 'SHARED' OR l.ownerId = :userId) " +
           "AND (:title IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "ORDER BY l.createdAt DESC")
    List<LibraryItem> searchAccessibleItems(@Param("userId") Integer userId, @Param("title") String title);

    Optional<LibraryItem> findByIdAndActive(UUID id, boolean active);
}
