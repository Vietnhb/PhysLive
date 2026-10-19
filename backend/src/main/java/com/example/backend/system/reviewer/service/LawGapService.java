package com.example.backend.system.reviewer.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.reviewer.model.entity.LawGap;
import com.example.backend.system.reviewer.repository.LawGapRepository;
import com.example.backend.system.simulation.service.LawGapSink;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** The reviewers' list of what the law catalogues are missing, fed by the simulations teachers create. */
@Service
@RequiredArgsConstructor
public class LawGapService implements LawGapSink {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LawGapService.class);
    private static final Set<String> STATUSES = Set.of("OPEN", "RESOLVED", "DISMISSED");
    private final LawGapRepository gaps;

    @Override
    public int record(String schemaId, String topic, String description, JsonNode spec, JsonNode validation) {
        try {
            Map<String, JsonNode> quantities = new LinkedHashMap<>();
            spec.path("model").path("quantities").forEach(quantity -> quantities.put(quantity.path("id").asText(), quantity));
            Map<String, LawGap> found = new LinkedHashMap<>();
            for (JsonNode id : validation.path("uncheckedQuantities")) {
                JsonNode quantity = quantities.get(id.asText());
                if (quantity == null) continue;
                String role = quantity.path("role").asText("other"), unit = quantity.path("unit").asText("");
                boolean named = !role.equals("other");
                // a named role is one gap wherever it is met; a quantity no role fitted is told apart by topic and unit
                String key = named ? "UNCHECKED|" + role + "|" + unit : "NO_ROLE|" + schemaId + "|" + unit;
                found.computeIfAbsent(key, ignored -> draft(key, named ? "UNCHECKED" : "NO_ROLE", schemaId, topic, named ? role : null, unit,
                        quantity.path("label").asText(id.asText()), description, formula(quantity)));
            }
            for (JsonNode law : validation.path("ambiguousLaws")) {
                String key = "AMBIGUOUS|" + law.path("law").asText();
                found.computeIfAbsent(key, ignored -> draft(key, "AMBIGUOUS", schemaId, topic, null, null, law.path("title").asText(law.path("law").asText()),
                        description, "Các đại lượng cùng khớp: " + law.path("quantities")));
            }
            // a law that still refuses the model after the repair loop: the model was wrong, or the law is too narrow
            if ("FLAGGED".equals(validation.path("status").asText()))
                for (JsonNode check : validation.path("lawChecks")) {
                    if (!"FAILED".equals(check.path("status").asText())) continue;
                    String key = "VIOLATION|" + check.path("law").asText();
                    found.computeIfAbsent(key, ignored -> draft(key, "VIOLATION", schemaId, topic, null, null,
                            check.path("title").asText(check.path("law").asText()), description, check.path("message").asText("")));
                }
            for (LawGap gap : found.values()) {
                LawGap known = gaps.findByGapKey(gap.getGapKey()).orElse(null);
                if (known == null) { gaps.save(gap); continue; }
                known.setOccurrences(known.getOccurrences() + 1);
                known.setExampleDescription(gap.getExampleDescription());
                known.setExampleFormula(gap.getExampleFormula());
                gaps.save(known);
            }
            return found.size();
        } catch (RuntimeException failure) {
            // the teacher's simulation never waits on the reviewers' list
            log.warn("LAW_GAP_NOT_RECORDED {}", failure.getMessage());
            return 0;
        }
    }

    public List<LawGap> list(String status) {
        return gaps.findTop300ByStatusOrderByOccurrencesDescUpdatedAtDesc(STATUSES.contains(status) ? status : "OPEN");
    }

    public long open() {
        return gaps.countByStatus("OPEN");
    }

    public LawGap decide(UUID id, String status, String note) {
        if (!STATUSES.contains(status)) throw ApiException.badRequest("Trạng thái không hợp lệ");
        LawGap gap = gaps.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy mục cần xử lý"));
        gap.setStatus(status);
        gap.setNote(note == null || note.isBlank() ? null : note.strip());
        return gaps.save(gap);
    }

    private static LawGap draft(String key, String kind, String schemaId, String topic, String role, String unit, String label, String description, String formula) {
        LawGap gap = new LawGap();
        gap.setGapKey(key.length() > 240 ? key.substring(0, 240) : key);
        gap.setKind(kind);
        gap.setSchemaId(schemaId);
        gap.setTopic(topic);
        gap.setRole(role);
        gap.setUnit(unit);
        gap.setLabel(label);
        gap.setExampleDescription(description);
        gap.setExampleFormula(formula);
        return gap;
    }

    private static String formula(JsonNode quantity) {
        String id = quantity.path("id").asText();
        return quantity.has("rate") ? "d" + id + "/dt = " + quantity.path("rate") + "; " + id + "(0) = " + quantity.path("initial") : id + " = " + quantity.path("value");
    }
}
