package com.example.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.CreateLibraryItemRequest;
import com.example.backend.dto.LibraryItemResponse;
import com.example.backend.dto.UpdateLibraryItemRequest;
import com.example.backend.entity.LibraryItem;
import com.example.backend.entity.Specification;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.LibraryModerationStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.LibraryItemRepository;
import com.example.backend.repository.SpecificationRepository;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LibraryService {

    private final LibraryItemRepository libraryItemRepository;
    private final SpecificationRepository specificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LibraryItemResponse> getLibraryItems(String userEmail, String visibility, String search) {
        User user = getUserByEmail(userEmail);

        List<LibraryItem> items;
        if ("PERSONAL".equalsIgnoreCase(visibility)) {
            items = libraryItemRepository.findByOwnerIdAndActiveOrderByCreatedAtDesc(user.getId(), true);
            if (search != null && !search.trim().isEmpty()) {
                String keyword = search.trim().toLowerCase();
                items = items.stream()
                        .filter(i -> i.getTitle().toLowerCase().contains(keyword))
                        .collect(Collectors.toList());
            }
        } else if ("SHARED".equalsIgnoreCase(visibility)) {
            items = user.getSchool() == null ? List.of()
                    : libraryItemRepository.findPublishedSharedForSchool(user.getSchool().getId());
            if (search != null && !search.trim().isEmpty()) {
                String keyword = search.trim().toLowerCase();
                items = items.stream()
                        .filter(i -> i.getTitle().toLowerCase().contains(keyword))
                        .collect(Collectors.toList());
            }
        } else if ("PUBLIC".equalsIgnoreCase(visibility)) {
            items = libraryItemRepository.findPublishedPublic();
            if (search != null && !search.trim().isEmpty()) {
                String keyword = search.trim().toLowerCase();
                items = items.stream().filter(i -> i.getTitle().toLowerCase().contains(keyword)).toList();
            }
        } else {
            items = libraryItemRepository.findVisibleItems(user.getId(),
                    user.getSchool() == null ? null : user.getSchool().getId(), search);
        }

        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LibraryItemResponse> getCommunityItems(String topic) {
        String normalizedTopic = topic == null || topic.isBlank() ? null : topic.trim();
        return libraryItemRepository.findCommunityItems(normalizedTopic).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LibraryItemResponse getLibraryItemById(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        LibraryItem item = libraryItemRepository.findByIdAndActive(id, true)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy mục thư viện với ID: " + id));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
        if (!item.getOwnerId().equals(user.getId()) && !isAdmin && !isPublishedFor(user, item)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem mục thư viện cá nhân này");
        }

        return toResponse(item);
    }

    @Transactional
    public LibraryItemResponse createLibraryItem(CreateLibraryItemRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Specification spec = specificationRepository.findById(request.getSpecificationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc tả mô phỏng (Specification) với ID: " + request.getSpecificationId()));

        if (!isAdmin(user) && (spec.getSubmission() == null || !user.getId().equals(spec.getSubmission().getOwner().getId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Specification belongs to another teacher");
        LibraryItem item = new LibraryItem();
        item.setTitle(request.getTitle().trim());
        String visibility = request.getVisibility() != null ? request.getVisibility().toUpperCase() : "SHARED";
        if ("SHARED".equals(visibility) && user.getSchool() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "School sharing requires a school account");
        }
        item.setVisibility(visibility);
        item.setSharedInstitutionId("SHARED".equals(visibility) ? user.getSchool().getId().toString() : null);
        item.setModerationStatus("PERSONAL".equals(visibility)
                ? LibraryModerationStatus.APPROVED : LibraryModerationStatus.PENDING);
        item.setActive(true);
        item.setOwnerId(user.getId());
        item.setOwner(user);
        item.setSpecificationId(spec.getId());
        item.setSpecification(spec);

        LibraryItem saved = libraryItemRepository.save(item);
        return toResponse(saved);
    }

    @Transactional
    public LibraryItemResponse updateLibraryItem(UUID id, UpdateLibraryItemRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        LibraryItem item = libraryItemRepository.findByIdAndActive(id, true)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy mục thư viện với ID: " + id));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
        if (!item.getOwnerId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa mục thư viện này");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getVisibility() != null) {
            String visibility = request.getVisibility().toUpperCase();
            if ("SHARED".equals(visibility) && user.getSchool() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "School sharing requires a school account");
            }
            if (!visibility.equalsIgnoreCase(item.getVisibility())) {
                item.setVisibility(visibility);
                item.setSharedInstitutionId("SHARED".equals(visibility)
                        ? user.getSchool().getId().toString() : null);
                item.setModerationStatus("PERSONAL".equals(visibility)
                        ? LibraryModerationStatus.APPROVED : LibraryModerationStatus.PENDING);
                item.setModerationComment(null);
                item.setModeratedAt(null);
                item.setModeratedBy(null);
            }
            if (!"PERSONAL".equals(visibility)
                    && item.getModerationStatus() == LibraryModerationStatus.REJECTED) {
                item.setModerationStatus(LibraryModerationStatus.PENDING);
                item.setModerationComment(null);
                item.setModeratedAt(null);
                item.setModeratedBy(null);
            }
        }
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }

        LibraryItem updated = libraryItemRepository.save(item);
        return toResponse(updated);
    }

    @Transactional
    public void deleteLibraryItem(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        LibraryItem item = libraryItemRepository.findByIdAndActive(id, true)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy mục thư viện với ID: " + id));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
        if (!item.getOwnerId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xoá mục thư viện này");
        }

        item.setActive(false);
        libraryItemRepository.save(item);
    }

    @Transactional
    public LibraryItemResponse cloneLibraryItem(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        LibraryItem source = libraryItemRepository.findByIdAndActive(id, true)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy mục thư viện với ID: " + id));

        if (!user.getId().equals(source.getOwnerId()) && !isAdmin(user) && !isPublishedFor(user, source))
            throw new ApiException(HttpStatus.FORBIDDEN, "Private library item is not accessible");
        LibraryItem clone = new LibraryItem();
        clone.setTitle(source.getTitle() + " (Bản sao)");
        clone.setVisibility("PERSONAL");
        clone.setModerationStatus(LibraryModerationStatus.APPROVED);
        clone.setActive(true);
        clone.setOwnerId(user.getId());
        clone.setOwner(user);
        clone.setSpecificationId(source.getSpecificationId());
        clone.setSpecification(source.getSpecification());

        LibraryItem saved = libraryItemRepository.save(clone);
        return toResponse(saved);
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private LibraryItemResponse toResponse(LibraryItem item) {
        Specification spec = item.getSpecification();
        String topic = spec != null ? spec.getTopic() : null;
        Object details = spec != null ? spec.getObjects() : null;

        return LibraryItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .visibility(item.getVisibility())
                .moderationStatus(item.getModerationStatus().name())
                .moderationComment(item.getModerationComment())
                .moderatedAt(item.getModeratedAt())
                .moderatedBy(item.getModeratedBy() == null ? null : item.getModeratedBy().getId())
                .sharedInstitutionId(item.getSharedInstitutionId())
                .active(item.isActive())
                .ownerId(item.getOwnerId())
                .ownerName(item.getOwner() != null ? item.getOwner().getFullName() : null)
                .ownerEmail(item.getOwner() != null ? item.getOwner().getEmail() : null)
                .schoolId(item.getOwner() == null || item.getOwner().getSchool() == null
                        ? null : item.getOwner().getSchool().getId())
                .schoolName(item.getOwner() == null || item.getOwner().getSchool() == null
                        ? null : item.getOwner().getSchool().getName())
                .specificationId(item.getSpecificationId())
                .topic(topic)
                .specificationDetails(details)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    static boolean isPublishedFor(User user, LibraryItem item) {
        if (!item.isActive() || item.getModerationStatus() != LibraryModerationStatus.APPROVED
                && item.getModerationStatus() != LibraryModerationStatus.FEATURED) return false;
        if ("PUBLIC".equalsIgnoreCase(item.getVisibility())) return true;
        return sharedWithSchool(user, item);
    }

    static boolean sharedWithSchool(User user, LibraryItem item) {
        return user != null && "SHARED".equalsIgnoreCase(item.getVisibility()) && user.getSchool() != null
                && item.getOwner() != null && item.getOwner().getSchool() != null
                && user.getSchool().getId().equals(item.getOwner().getSchool().getId())
                && (!org.springframework.util.StringUtils.hasText(item.getSharedInstitutionId())
                    || user.getSchool().getId().toString().equals(item.getSharedInstitutionId()));
    }
}
