package com.example.backend.extraction;

import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenRouterClient {

    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenRouterClient(RestClient.Builder builder, ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.apiKey = environment.getProperty("OPENROUTER_API_KEY", "").trim();
        String baseUrl = environment.getProperty("OPENROUTER_BASE_URL", DEFAULT_BASE_URL).trim();
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    public Completion complete(String model, List<Map<String, Object>> messages) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenRouter is not configured.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0,
                "max_tokens", 3000,
                "response_format", Map.of("type", "json_object"));

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-OpenRouter-Title", "PhysLive")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.path("choices").isEmpty()) {
            throw new IllegalStateException("OpenRouter returned an empty response.");
        }
        String content = response.path("choices").path(0).path("message").path("content").asText();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("OpenRouter returned empty content.");
        }
        return new Completion(response.path("model").asText(model), content, response);
    }

    public Map<String, Object> textMessage(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    public Map<String, Object> imageMessage(String prompt, String contentType, byte[] content) {
        String dataUrl = "data:" + contentType + ";base64," + java.util.Base64.getEncoder().encodeToString(content);
        return Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))));
    }

    public JsonNode parseJson(String content) {
        String normalized = content.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return objectMapper.readTree(normalized);
        } catch (Exception exception) {
            throw new IllegalStateException("OpenRouter returned invalid JSON.", exception);
        }
    }

    public record Completion(String model, String content, JsonNode rawResponse) {
    }
}
