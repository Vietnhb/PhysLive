package com.example.backend.system.curriculum.model.entity;

import com.example.backend.base.crud.model.entity.AuditedEntity;
import com.example.backend.system.curriculum.dto.LocalizedName;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topics", uniqueConstraints = @UniqueConstraint(name = "uk_topics_slug", columnNames = "slug"))
@Getter
@Setter
public class Topic extends AuditedEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private LocalizedName name;

    @Column(nullable = false, length = 80)
    private String slug;

    public String getCode() {
        return slug.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    public boolean matchesCode(String code) {
        return code != null && getCode().equalsIgnoreCase(code);
    }

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ContentModule> modules = new ArrayList<>();

}
