package com.example.backend.system.reviewer.dto;

import com.example.backend.system.reviewer.model.entity.LawGap;
import java.util.UUID;
import java.time.Instant;

public final class LawGapContracts {
    private LawGapContracts() { }
    public record Item(UUID id, String kind, String schemaId, String topic, String role, String unit, String label, int occurrences,
            String exampleDescription, String exampleFormula, String status, String note, Instant createdAt, Instant updatedAt) {
        public static Item from(LawGap gap) {
            return new Item(gap.getId(), gap.getKind(), gap.getSchemaId(), gap.getTopic(), gap.getRole(), gap.getUnit(), gap.getLabel(), gap.getOccurrences(),
                    gap.getExampleDescription(), gap.getExampleFormula(), gap.getStatus(), gap.getNote(), gap.getCreatedAt(), gap.getUpdatedAt());
        }
    }

    public record Decision(String status, String note) { }

}
