package com.example.backend.system.reviewer.controller;

import com.example.backend.system.reviewer.model.entity.LawGap;
import com.example.backend.system.reviewer.service.LawGapService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** What teachers' simulations needed that no law checks yet; open to accounts that edit the catalogues. */
@RestController
@RequestMapping("/api/reviewer/law-gaps")
@RequiredArgsConstructor
public class ReviewerLawGapController {
    private final LawGapService service;

    public record Item(UUID id, String kind, String schemaId, String topic, String role, String unit, String label, int occurrences,
            String exampleDescription, String exampleFormula, String status, String note, Instant createdAt, Instant updatedAt) {
        static Item from(LawGap gap) {
            return new Item(gap.getId(), gap.getKind(), gap.getSchemaId(), gap.getTopic(), gap.getRole(), gap.getUnit(), gap.getLabel(), gap.getOccurrences(),
                    gap.getExampleDescription(), gap.getExampleFormula(), gap.getStatus(), gap.getNote(), gap.getCreatedAt(), gap.getUpdatedAt());
        }
    }

    public record Decision(String status, String note) { }

    @GetMapping
    public List<Item> list(@RequestParam(defaultValue = "OPEN") String status) {
        return service.list(status).stream().map(Item::from).toList();
    }

    @PutMapping("/{id}")
    public Item decide(@PathVariable UUID id, @RequestBody Decision decision) {
        return Item.from(service.decide(id, decision.status(), decision.note()));
    }
}
