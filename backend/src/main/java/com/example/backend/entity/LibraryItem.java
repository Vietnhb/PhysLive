package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "library_items", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LibraryItem {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private LibraryFolder folder;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "visibility", nullable = false)
    private String visibility = "PERSONAL"; // PERSONAL, SHARED

    @Column(name = "shared_institution_id", length = 120)
    private String sharedInstitutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 16)
    private com.example.backend.entity.enums.LibraryModerationStatus moderationStatus =
            com.example.backend.entity.enums.LibraryModerationStatus.APPROVED;

    @Column(name = "moderation_comment", columnDefinition = "text")
    private String moderationComment;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    @Version
    @Column(name = "record_version", nullable = false)
    private long recordVersion;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "owner_id", nullable = false)
    private Integer ownerId;

    @ManyToOne
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @Column(name = "specification_id", nullable = false)
    private UUID specificationId;

    @ManyToOne
    @JoinColumn(name = "specification_id", insertable = false, updatable = false)
    private Specification specification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.visibility == null) {
            this.visibility = "PERSONAL";
        }
        if (this.moderationStatus == null) {
            this.moderationStatus = com.example.backend.entity.enums.LibraryModerationStatus.APPROVED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
