package com.example.backend.repository;

import com.example.backend.entity.LibraryComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LibraryCommentRepository extends JpaRepository<LibraryComment, UUID> {
    @EntityGraph(attributePaths = "author")
    Slice<LibraryComment> findByItemIdOrderByCreatedAtDescIdDesc(UUID itemId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "item", "item.owner", "item.owner.school"})
    Optional<LibraryComment> findByIdAndItemId(UUID id, UUID itemId);

    long countByItemId(UUID itemId);
}
