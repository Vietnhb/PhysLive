package com.example.backend.service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.ContentModuleRequest;
import com.example.backend.dto.ContentModuleResponse;
import com.example.backend.dto.GradeLevelResponse;
import com.example.backend.dto.LessonResponse;
import com.example.backend.dto.TopicRequest;
import com.example.backend.dto.TopicResponse;
import com.example.backend.dto.curriculum.CurriculumTreeResponse;
import com.example.backend.dto.curriculum.CurriculumTreeResponse.LessonItem;
import com.example.backend.dto.curriculum.CurriculumTreeResponse.LevelItem;
import com.example.backend.dto.curriculum.CurriculumTreeResponse.ModuleItem;
import com.example.backend.dto.curriculum.CurriculumTreeResponse.TopicItem;
import com.example.backend.entity.ContentModule;
import com.example.backend.entity.GradeLevel;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.Topic;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.ContentModuleRepository;
import com.example.backend.repository.GradeLevelRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service managing curriculum hierarchy (Topics, Content Modules, Grade Levels, Lessons).
 * Allows Administrator to toggle modules on/off for schools, as well as tree queries.
 */
@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final TopicRepository topicRepository;
    private final ContentModuleRepository moduleRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final LessonRepository lessonRepository;

    // ------------------------------------------------------------------ //
    // Tree Query
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public CurriculumTreeResponse getTree(boolean includeInactive) {
        List<TopicItem> topics = topicRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(topic -> includeInactive || topic.isEnabled())
                .map(topic -> toTopic(topic, includeInactive))
                .toList();
        return new CurriculumTreeResponse(topics);
    }

    private TopicItem toTopic(Topic topic, boolean includeInactive) {
        return new TopicItem(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.isEnabled(),
                topic.getSortOrder(),
                topic.getModules().stream()
                        .filter(module -> includeInactive || module.isActive())
                        .map(module -> toModule(module, includeInactive))
                        .toList());
    }

    private ModuleItem toModule(ContentModule module, boolean includeInactive) {
        return new ModuleItem(
                module.getId(),
                module.getName(),
                module.getSlug(),
                module.isActive(),
                module.getSortOrder(),
                module.getLevels().stream()
                        .filter(level -> includeInactive || level.isActive())
                        .map(level -> toLevel(level, includeInactive))
                        .toList());
    }

    private LevelItem toLevel(GradeLevel level, boolean includeInactive) {
        return new LevelItem(
                level.getId(),
                level.getName(),
                level.isActive(),
                level.getSortOrder(),
                level.getLessons().stream()
                        .filter(lesson -> includeInactive || lesson.isActive())
                        .map(this::toLesson)
                        .toList());
    }

    private LessonItem toLesson(Lesson lesson) {
        return new LessonItem(
                lesson.getId(),
                lesson.getName(),
                lesson.getSlug(),
                lesson.isActive(),
                lesson.getSortOrder());
    }

    // ------------------------------------------------------------------ //
    // Topics CRUD
    // ------------------------------------------------------------------ //

    public List<TopicResponse> getAllTopics() {
        return topicRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toTopicResponse)
                .toList();
    }

    public TopicResponse getTopicById(UUID id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy topic với id: " + id));
        return toTopicResponse(topic);
    }

    @Transactional
    public TopicResponse createTopic(TopicRequest request) {
        if (topicRepository.existsBySlug(request.getSlug().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slug đã tồn tại: " + request.getSlug());
        }

        Topic topic = new Topic();
        topic.setName(request.getName().trim());
        topic.setSlug(request.getSlug().trim().toLowerCase());
        topic.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        topic.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        topicRepository.save(topic);
        return toTopicResponse(topic);
    }

    @Transactional
    public TopicResponse updateTopic(UUID id, TopicRequest request) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy topic với id: " + id));

        if (!topic.getSlug().equalsIgnoreCase(request.getSlug().trim()) && topicRepository.existsBySlug(request.getSlug().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slug đã tồn tại: " + request.getSlug());
        }

        topic.setName(request.getName().trim());
        topic.setSlug(request.getSlug().trim().toLowerCase());
        if (request.getSortOrder() != null) topic.setSortOrder(request.getSortOrder());
        if (request.getEnabled() != null) topic.setEnabled(request.getEnabled());

        topicRepository.save(topic);
        return toTopicResponse(topic);
    }

    @Transactional
    public TopicResponse toggleTopicEnabled(UUID id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy topic với id: " + id));

        topic.setEnabled(!topic.isEnabled());
        topicRepository.save(topic);
        return toTopicResponse(topic);
    }

    @Transactional
    public void deleteTopic(UUID id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy topic với id: " + id));
        topicRepository.delete(topic);
    }

    // ------------------------------------------------------------------ //
    // Content Modules CRUD
    // ------------------------------------------------------------------ //

    public List<ContentModuleResponse> getModulesByTopic(UUID topicId) {
        return moduleRepository.findByTopicIdOrderBySortOrderAsc(topicId).stream()
                .map(this::toModuleResponse)
                .toList();
    }

    @Transactional
    public ContentModuleResponse createModule(ContentModuleRequest request) {
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy topic với id: " + request.getTopicId()));

        if (moduleRepository.existsBySlug(request.getSlug().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slug module đã tồn tại: " + request.getSlug());
        }

        ContentModule module = new ContentModule();
        module.setName(request.getName().trim());
        module.setSlug(request.getSlug().trim().toLowerCase());
        module.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        module.setActive(request.getActive() != null ? request.getActive() : true);
        module.setTopic(topic);

        moduleRepository.save(module);
        return toModuleResponse(module);
    }

    @Transactional
    public ContentModuleResponse updateModule(UUID id, ContentModuleRequest request) {
        ContentModule module = moduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy module với id: " + id));

        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy topic với id: " + request.getTopicId()));

        if (!module.getSlug().equalsIgnoreCase(request.getSlug().trim()) && moduleRepository.existsBySlug(request.getSlug().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slug module đã tồn tại: " + request.getSlug());
        }

        module.setName(request.getName().trim());
        module.setSlug(request.getSlug().trim().toLowerCase());
        if (request.getSortOrder() != null) module.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) module.setActive(request.getActive());
        module.setTopic(topic);

        moduleRepository.save(module);
        return toModuleResponse(module);
    }

    @Transactional
    public ContentModuleResponse toggleModuleActive(UUID id) {
        ContentModule module = moduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy module với id: " + id));

        module.setActive(!module.isActive());
        moduleRepository.save(module);
        return toModuleResponse(module);
    }

    // ------------------------------------------------------------------ //
    // Grade Levels & Lessons CRUD
    // ------------------------------------------------------------------ //

    public List<GradeLevelResponse> getLevelsByModule(UUID moduleId) {
        return gradeLevelRepository.findByModuleIdOrderBySortOrderAsc(moduleId).stream()
                .map(this::toLevelResponse)
                .toList();
    }

    @Transactional
    public GradeLevelResponse toggleLevelActive(UUID id) {
        GradeLevel level = gradeLevelRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy grade level với id: " + id));
        level.setActive(!level.isActive());
        gradeLevelRepository.save(level);
        return toLevelResponse(level);
    }

    public List<LessonResponse> getLessonsByLevel(UUID levelId) {
        return lessonRepository.findByLevelIdOrderBySortOrderAsc(levelId).stream()
                .map(this::toLessonResponse)
                .toList();
    }

    @Transactional
    public LessonResponse toggleLessonActive(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học với id: " + id));
        lesson.setActive(!lesson.isActive());
        lessonRepository.save(lesson);
        return toLessonResponse(lesson);
    }

    // ------------------------------------------------------------------ //
    // Helper Mappers
    // ------------------------------------------------------------------ //

    private TopicResponse toTopicResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.isEnabled(),
                topic.getSortOrder(),
                topic.getCreatedAt() != null ? topic.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                topic.getUpdatedAt() != null ? topic.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    private ContentModuleResponse toModuleResponse(ContentModule module) {
        return new ContentModuleResponse(
                module.getId(),
                module.getName(),
                module.getSlug(),
                module.isActive(),
                module.getSortOrder(),
                module.getTopic() != null ? module.getTopic().getId() : null,
                module.getTopic() != null ? module.getTopic().getName() : null,
                module.getCreatedAt() != null ? module.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                module.getUpdatedAt() != null ? module.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    private GradeLevelResponse toLevelResponse(GradeLevel level) {
        return new GradeLevelResponse(
                level.getId(),
                level.getName(),
                level.getSortOrder(),
                level.isActive(),
                level.getModule() != null ? level.getModule().getId() : null,
                level.getCreatedAt() != null ? level.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                level.getUpdatedAt() != null ? level.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    private LessonResponse toLessonResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getName(),
                lesson.getSlug(),
                lesson.getSortOrder(),
                lesson.isActive(),
                lesson.getLevel() != null ? lesson.getLevel().getId() : null,
                lesson.getCreatedAt() != null ? lesson.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                lesson.getUpdatedAt() != null ? lesson.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }
}
