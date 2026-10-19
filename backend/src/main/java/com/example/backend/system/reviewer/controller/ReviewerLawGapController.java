package com.example.backend.system.reviewer.controller;

import com.example.backend.system.reviewer.dto.LawGapContracts.*;
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

    @GetMapping
    public List<Item> list(@RequestParam(defaultValue = "OPEN") String status) {
        return service.list(status).stream().map(Item::from).toList();
    }

    @PutMapping("/{id}")
    public Item decide(@PathVariable UUID id, @RequestBody Decision decision) {
        return Item.from(service.decide(id, decision.status(), decision.note()));
    }
}
