package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Something a teacher's simulation computed that no law of the catalogues could check. One row per kind of gap
 * (the same role and unit in the same topic), counted each time it is met, for the reviewer who adds the law.
 */
@Entity
@Table(name = "law_gaps")
@Getter
@Setter
public class LawGap extends AuditedEntity {

    @Column(name = "gap_key", nullable = false, length = 240, unique = true)
    private String gapKey;

    /** UNCHECKED: a role no law reached; NO_ROLE: a quantity no role fitted; AMBIGUOUS: a law that could not tell which quantity was meant. */
    @Column(nullable = false, length = 16)
    private String kind;

    @Column(name = "schema_id", length = 80)
    private String schemaId;

    @Column(length = 200)
    private String topic;

    @Column(length = 80)
    private String role;

    @Column(length = 40)
    private String unit;

    @Column(columnDefinition = "text")
    private String label;

    @Column(nullable = false)
    private int occurrences = 1;

    @Column(name = "example_description", columnDefinition = "text")
    private String exampleDescription;

    @Column(name = "example_formula", columnDefinition = "text")
    private String exampleFormula;

    /** OPEN, RESOLVED (a law was added) or DISMISSED (nothing to add). */
    @Column(nullable = false, length = 16)
    private String status = "OPEN";

    @Column(columnDefinition = "text")
    private String note;
}
