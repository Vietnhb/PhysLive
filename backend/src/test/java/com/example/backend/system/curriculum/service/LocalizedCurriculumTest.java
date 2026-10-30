package com.example.backend.system.curriculum.service;

import com.example.backend.base.crud.model.enums.LifecycleStatus;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.curriculum.dto.CurriculumTopicRequest;
import com.example.backend.system.curriculum.dto.LocalizedName;
import com.example.backend.system.curriculum.model.entity.Topic;
import com.example.backend.system.curriculum.repository.TopicRepository;
import com.example.backend.system.physics.repository.SchemaVersionRepository;
import com.example.backend.system.physics.service.SchemaDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalizedCurriculumTest {
    @Test
    void namesRoundTripAsOneJsonObjectAndValidateBothLocales() throws Exception {
        var json = new ObjectMapper();
        var name = new LocalizedName(" Kinematics ", " Động học ");
        assertEquals(name, json.readValue(json.writeValueAsString(name), LocalizedName.class));
        assertEquals("Động học", json.readTree(json.writeValueAsString(name)).path("vi").asText());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new CurriculumTopicRequest(name, null, null)).isEmpty());
            assertFalse(validator.validate(new CurriculumTopicRequest(new LocalizedName("Kinematics", " "), null, null)).isEmpty());
            assertFalse(validator.validate(new CurriculumTopicRequest(new LocalizedName(null, "Động học"), null, null)).isEmpty());
            assertFalse(validator.validate(new CurriculumTopicRequest(new LocalizedName("x".repeat(81), "Tên"), null, null)).isEmpty());
        }
    }

    @Test
    void renamingEitherLocaleDoesNotChangeTheSimulationIdentity() {
        Topic topic = new Topic(); topic.setSlug("electric-current");
        topic.setName(new LocalizedName("Electric current", "Dòng điện"));
        assertTrue(topic.matchesCode("ELECTRIC_CURRENT"));
        topic.setName(new LocalizedName("Circuits", "Dòng điện và mạch điện"));
        assertEquals("ELECTRIC_CURRENT", topic.getCode());
        assertTrue(topic.matchesCode("electric_current"));
        assertFalse(topic.matchesCode("ELECTRIC_FIELD"));
        assertFalse(topic.matchesCode(null));
    }

    @Test
    void schemaLookupUsesSlugAndReturnsBothStoredLanguages() {
        var topics = mock(TopicRepository.class);
        Topic topic = new Topic(); topic.setSlug("kinematics"); topic.setEnabled(true);
        topic.setName(new LocalizedName("Kinematics", "Động học"));
        when(topics.findBySlugIgnoreCaseAndEnabledTrueOrderBySortOrderAsc("kinematics")).thenReturn(List.of(topic));
        var service = new CurriculumService(topics, mock(SchemaDefinitionService.class), mock(CurrentUserService.class));
        var item = service.getActiveTreeForTopic("KINEMATICS").topics().getFirst();
        assertEquals("Động học", item.name().vi()); assertEquals("Kinematics", item.name().en());
    }

    @Test
    void approvedSchemasAreSelectedByCodeRegardlessOfDisplayNames() {
        var topics = mock(TopicRepository.class); var schemas = mock(SchemaVersionRepository.class);
        Topic topic = new Topic(); topic.setSlug("electric-current");
        topic.setName(new LocalizedName("Electric current", "Dòng điện và mạch điện"));
        when(topics.findByEnabledTrueOrderBySortOrderAsc()).thenReturn(List.of(topic));
        new SchemaDefinitionService(schemas, topics, new ObjectMapper()).approvedSchemas();
        verify(schemas).findAllByLifecycleStatusAndTopicInOrderByTopicAscSchemaIdAscCreatedAtDesc(
                LifecycleStatus.APPROVED, List.of("ELECTRIC_CURRENT"));
    }

    @Test
    void creatingNewTopicPersistsUserSuppliedTranslationsWithoutDictionary() {
        var topics = mock(TopicRepository.class);
        var service = new CurriculumAdminService(topics, null, null, null, mock(CurriculumService.class));
        var name = new LocalizedName("Custom new topic", "Chủ đề tự nhập");
        service.createTopic(new CurriculumTopicRequest(name, null, null));
        verify(topics).save(argThat(topic -> topic.getName().equals(name)
                && topic.getSlug().equals("custom-new-topic") && topic.isEnabled()));
    }
}
