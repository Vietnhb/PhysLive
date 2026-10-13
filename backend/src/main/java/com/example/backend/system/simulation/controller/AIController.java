package com.example.backend.system.simulation.controller;

import com.example.backend.config.UploadProperties;
import com.example.backend.exception.ApiException;
import com.example.backend.system.simulation.dto.SimulationRequests;
import com.example.backend.system.simulation.service.AIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "physlive.staff.preview-enabled", havingValue = "true")
@org.springframework.security.access.prepost.PreAuthorize("@staffAccess.requireTeaching() != null")
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class AIController {
    private final AIService ai;
    private final UploadProperties upload;

    @PostMapping(path = "/understand", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode understandText(@Valid @RequestBody SimulationRequests.Understand request) {
        return ai.understandText(request.description(), request.sessionId(), request.recognizedText(), request.correctedText());
    }

    @PostMapping(path = "/understand", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ObjectNode understandImage(@RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String text) {
        if (file.isEmpty() || file.getSize() > upload.maxImageBytes())
            throw ApiException.badRequest("Ảnh trống hoặc vượt quá giới hạn dung lượng");
        try { return ai.understandImage(file.getBytes(), file.getContentType(), text); }
        catch (IOException ex) { throw ApiException.badRequest("Không thể đọc ảnh đã tải lên"); }
    }

    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ObjectNode generate(@RequestBody JsonNode request) { return ai.generate(request); }

    @PostMapping(path = "/revise", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode revise(@Valid @RequestBody SimulationRequests.Revision request) {
        return ai.revise(request.intent(), request.text());
    }

    @PostMapping(path = "/render-diagnostics", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void renderDiagnostic(@Valid @RequestBody SimulationRequests.RenderDiagnostic request) {
        ai.reportRenderDiagnostic(request.simulation(), request.message(), request.runtime());
    }

    @PostMapping(path = "/compute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ObjectNode compute(@RequestBody JsonNode request) { return ai.compute(request); }

}
