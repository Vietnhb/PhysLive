package com.example.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.backend.dto.LibraryItemResponse;
import com.example.backend.dto.library.LibraryModerationContracts;
import com.example.backend.entity.LibraryItem;
import com.example.backend.entity.LibraryModerationAudit;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.LibraryModerationStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.LibraryItemRepository;
import com.example.backend.repository.LibraryModerationAuditRepository;
import com.example.backend.service.account.AccountAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibraryModerationService {
    private final LibraryItemRepository items;
    private final LibraryModerationAuditRepository audits;
    private final CurrentUserService currentUser;
    private final AccountAccessService access;

    @Transactional(readOnly = true)
    public List<LibraryItemResponse> publicQueue(LibraryModerationStatus status) {
        requirePublicReviewer();
        return items.findPublicModerationItems(defaultStatus(status)).stream().map(this::response).toList();
    }

    @Transactional
    public LibraryItemResponse moderatePublic(UUID id, LibraryModerationStatus status, String comment) {
        User actor = requirePublicReviewer();
        LibraryItem item = locked(id);
        if (!"PUBLIC".equalsIgnoreCase(item.getVisibility())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Reviewer can only moderate public content");
        }
        return decide(actor, item, status, comment, true);
    }

    @Transactional(readOnly = true)
    public List<LibraryItemResponse> schoolQueue(UUID schoolId, LibraryModerationStatus status) {
        requireSchoolReviewer(schoolId);
        return items.findSchoolModerationItems(schoolId, defaultStatus(status)).stream()
                .filter(item -> !StringUtils.hasText(item.getSharedInstitutionId())
                        || schoolId.toString().equals(item.getSharedInstitutionId()))
                .map(this::response).toList();
    }

    @Transactional
    public LibraryItemResponse moderateSchool(UUID schoolId, UUID id,
            LibraryModerationStatus status, String comment) {
        User actor = requireSchoolReviewer(schoolId);
        LibraryItem item = locked(id);
        if (!"SHARED".equalsIgnoreCase(item.getVisibility()) || item.getOwner() == null
                || item.getOwner().getSchool() == null
                || !schoolId.equals(item.getOwner().getSchool().getId())
                || StringUtils.hasText(item.getSharedInstitutionId())
                    && !schoolId.toString().equals(item.getSharedInstitutionId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Department heads can only moderate content shared inside their school");
        }
        if (status == LibraryModerationStatus.FEATURED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "School content cannot be featured publicly");
        }
        return decide(actor, item, status, comment, false);
    }

    @Transactional(readOnly = true)
    public List<LibraryModerationContracts.Audit> history(UUID id) {
        requirePublicReviewer();
        LibraryItem item = items.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Library item not found"));
        if (!"PUBLIC".equalsIgnoreCase(item.getVisibility())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Public library item not found");
        }
        return audits.findTop100ByLibraryItemIdOrderByDecidedAtDesc(id).stream()
                .map(row -> new LibraryModerationContracts.Audit(row.getId(), id,
                        row.getReviewer().getEmail(), row.getFromStatus(), row.getToStatus(),
                        row.getComment(), row.getDecidedAt()))
                .toList();
    }

    private LibraryItemResponse decide(User actor, LibraryItem item, LibraryModerationStatus status,
            String comment, boolean publicDecision) {
        if (item.getOwnerId() != null && item.getOwnerId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Users cannot approve their own content");
        }
        if (!item.isActive() && item.getModerationStatus() != LibraryModerationStatus.REMOVED) {
            throw new ApiException(HttpStatus.CONFLICT, "The owner removed this library item");
        }
        if (status == null || status == LibraryModerationStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A moderation decision is required");
        }
        if (!publicDecision && status == LibraryModerationStatus.FEATURED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "School content cannot be featured publicly");
        }
        if ((status == LibraryModerationStatus.REJECTED || status == LibraryModerationStatus.REMOVED)
                && !StringUtils.hasText(comment)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A reason is required when rejecting or removing content");
        }
        LibraryModerationAudit audit = new LibraryModerationAudit();
        audit.setLibraryItem(item);
        audit.setReviewer(actor);
        audit.setFromStatus(item.getModerationStatus());
        audit.setToStatus(status);
        audit.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
        audits.save(audit);

        item.setModerationStatus(status);
        item.setModerationComment(audit.getComment());
        item.setModeratedAt(Instant.now());
        item.setModeratedBy(actor);
        item.setActive(status != LibraryModerationStatus.REMOVED);
        return response(items.save(item));
    }

    private User requirePublicReviewer() {
        User actor = currentUser.requireCurrentUser();
        if (!access.canReviewPublic(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Public review permission is required");
        }
        return actor;
    }

    private User requireSchoolReviewer(UUID schoolId) {
        User actor = currentUser.requireCurrentUser();
        if (!access.isDepartmentHead(actor) || actor.getSchool() == null
                || !schoolId.equals(actor.getSchool().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only the school's department head can moderate internal content");
        }
        return actor;
    }

    private LibraryItem locked(UUID id) {
        return items.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Library item not found"));
    }

    private static LibraryModerationStatus defaultStatus(LibraryModerationStatus status) {
        return status == null ? LibraryModerationStatus.PENDING : status;
    }

    private LibraryItemResponse response(LibraryItem item) {
        var school = item.getOwner() == null ? null : item.getOwner().getSchool();
        return LibraryItemResponse.builder()
                .id(item.getId()).title(item.getTitle()).visibility(item.getVisibility())
                .moderationStatus(item.getModerationStatus().name())
                .moderationComment(item.getModerationComment()).moderatedAt(item.getModeratedAt())
                .moderatedBy(item.getModeratedBy() == null ? null : item.getModeratedBy().getId())
                .sharedInstitutionId(item.getSharedInstitutionId())
                .active(item.isActive()).ownerId(item.getOwnerId())
                .ownerName(item.getOwner() == null ? null : item.getOwner().getFullName())
                .ownerEmail(item.getOwner() == null ? null : item.getOwner().getEmail())
                .schoolId(school == null ? null : school.getId())
                .schoolName(school == null ? null : school.getName())
                .specificationId(item.getSpecificationId())
                .topic(item.getSpecification() == null ? null : item.getSpecification().getTopic())
                .specificationDetails(item.getSpecification() == null ? null : item.getSpecification().getObjects())
                .createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt()).build();
    }
}
