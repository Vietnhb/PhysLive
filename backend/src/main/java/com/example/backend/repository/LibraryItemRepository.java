package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.LibraryItem;
import com.example.backend.entity.enums.LibraryModerationStatus;
import jakarta.persistence.LockModeType;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"owner", "owner.school", "specification"})
    @Query("SELECT i FROM LibraryItem i WHERE i.id = :id")
    Optional<LibraryItem> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"owner", "owner.school", "specification"})
    @Query("SELECT i FROM LibraryItem i WHERE i.visibility = 'PUBLIC' AND i.moderationStatus = :status "
            + "AND (i.active = true OR i.moderationStatus = 'REMOVED') ORDER BY i.createdAt ASC")
    List<LibraryItem> findPublicModerationItems(@Param("status") LibraryModerationStatus status);

    @EntityGraph(attributePaths = {"owner", "owner.school", "specification"})
    @Query("SELECT i FROM LibraryItem i WHERE i.visibility = 'SHARED' AND i.owner.school.id = :schoolId "
            + "AND i.moderationStatus = :status AND (i.active = true OR i.moderationStatus = 'REMOVED') "
            + "ORDER BY i.createdAt ASC")
    List<LibraryItem> findSchoolModerationItems(@Param("schoolId") UUID schoolId,
            @Param("status") LibraryModerationStatus status);

    @EntityGraph(attributePaths = {"owner", "owner.school", "specification"})
    @Query("SELECT i FROM LibraryItem i LEFT JOIN i.owner.school s WHERE i.active = true AND "
            + "(i.ownerId = :userId OR (i.moderationStatus IN ('APPROVED','FEATURED') AND "
            + "(i.visibility = 'PUBLIC' OR (i.visibility = 'SHARED' AND s.id = :schoolId)))) AND "
            + "(:title IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%'))) ORDER BY i.createdAt DESC")
    List<LibraryItem> findVisibleItems(@Param("userId") Integer userId, @Param("schoolId") UUID schoolId,
            @Param("title") String title);

    @EntityGraph(attributePaths = {"owner", "owner.school", "specification"})
    @Query("SELECT i FROM LibraryItem i WHERE i.active = true AND i.visibility = 'SHARED' "
            + "AND i.owner.school.id = :schoolId AND i.moderationStatus IN ('APPROVED','FEATURED') "
            + "ORDER BY i.createdAt DESC")
    List<LibraryItem> findPublishedSharedForSchool(@Param("schoolId") UUID schoolId);

    @EntityGraph(attributePaths = {"owner", "owner.school", "specification"})
    @Query("SELECT i FROM LibraryItem i WHERE i.active = true AND i.visibility = 'PUBLIC' "
            + "AND i.moderationStatus IN ('APPROVED','FEATURED') ORDER BY i.createdAt DESC")
    List<LibraryItem> findPublishedPublic();
}
