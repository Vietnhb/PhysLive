package com.example.backend.system.curriculum.repository;

import com.example.backend.system.curriculum.model.entity.Topic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    /** How many rows there are and when the latest changed: moves whenever a topic is added, enabled or disabled. */
    interface ChangeMark { Long getCount(); java.time.Instant getUpdated(); }

    @org.springframework.data.jpa.repository.Query("select count(t) as count, max(t.updatedAt) as updated from Topic t")
    ChangeMark changeMark();

    Optional<Topic> findBySlug(String slug);
    List<Topic> findByNameIgnoreCaseAndEnabledTrueOrderBySortOrderAsc(String name);

    List<Topic> findAllByOrderBySortOrderAsc();

    List<Topic> findByEnabledTrueOrderBySortOrderAsc();
}
