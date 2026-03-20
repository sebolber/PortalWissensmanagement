package de.wissensmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls PortalCore's internal API to retrieve the active LLM config,
 * then calls the LLM provider directly for RAG-based chat.
 */
@Service
public class LlmIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(LlmIntegrationService.class);

    private final RestClient restClient;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    public LlmIntegrationService() {
        this.restClient = RestClient.create();
    }

    /**
     * Fetches the active LLM config from PortalCore for the given tenant.
     */
    @SuppressWarnings("unchecked")
    public LlmConfig getActiveLlmConfig(String tenantId, String jwtToken) {
        try {
            List<Map<String, Object>> configs = restClient.get()
                    .uri(portalCoreBaseUrl + "/tenants/" + tenantId + "/profile/llm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .body(List.class);

            if (configs == null || configs.isEmpty()) {
                return null;
            }

            for (Map<String, Object> cfg : configs) {
                Boolean active = (Boolean) cfg.get("active");
                if (Boolean.TRUE.equals(active)) {
                    return new LlmConfig(
                            (String) cfg.get("id"),
                            (String) cfg.get("provider"),
                            (String) cfg.get("model"),
                            (String) cfg.get("baseUrl"),
                            (String) cfg.get("apiKeyEncrypted"),
                            cfg.get("maxTokens") != null ? ((Number) cfg.get("maxTokens")).intValue() : 2048,
                            cfg.get("temperature") != null ? ((Number) cfg.get("temperature")).doubleValue() : 0.7
                    );
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch LLM config from PortalCore: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calls the LLM with a RAG prompt combining context and user question.
     */
    public LlmResponse chat(LlmConfig config, String systemPrompt, List<ChatTurn> conversationHistory) {
        if (config == null) {
            return new LlmResponse("Kein LLM konfiguriert. Bitte konfigurieren Sie eine KI-Verbindung in den Mandanteneinstellungen.", 0);
        }

        try {
            String provider = config.provider() != null ? config.provider().toLowerCase() : "openai";

            return switch (provider) {
                case "anthropic" -> callAnthropic(config, systemPrompt, conversationHistory);
                default -> callOpenAiCompatible(config, systemPrompt, conversationHistory);
            };
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            return new LlmResponse("Fehler bei der KI-Verarbeitung: " + e.getMessage(), 0);
        }
    }

    @SuppressWarnings("unchecked")
    private LlmResponse callOpenAiCompatible(LlmConfig config, String systemPrompt, List<ChatTurn> history) {
        String baseUrl = resolveBaseUrl(config.provider(), config.baseUrl());

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }

        Map<String, Object> body = Map.of(
                "model", config.model(),
                "messages", messages,
                "max_tokens", config.maxTokens(),
                "temperature", config.temperature()
        );

        Map<String, Object> response = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        int tokens = 0;
        if (response.get("usage") instanceof Map<?, ?> usage) {
            tokens = usage.get("total_tokens") != null ? ((Number) usage.get("total_tokens")).intValue() : 0;
        }

        return new LlmResponse(content, tokens);
    }

    @SuppressWarnings("unchecked")
    private LlmResponse callAnthropic(LlmConfig config, String systemPrompt, List<ChatTurn> history) {
        String baseUrl = config.baseUrl() != null && !config.baseUrl().isBlank() ? config.baseUrl() : "https://api.anthropic.com/v1";

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }

        Map<String, Object> body = Map.of(
                "model", config.model(),
                "system", systemPrompt,
                "max_tokens", config.maxTokens(),
                "messages", messages
        );

        Map<String, Object> response = restClient.post()
                .uri(baseUrl + "/messages")
                .header("x-api-key", config.apiKey())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) response.get("content");
        String content = (String) contentBlocks.get(0).get("text");

        int tokens = 0;
        if (response.get("usage") instanceof Map<?, ?> usage) {
            int input = usage.get("input_tokens") != null ? ((Number) usage.get("input_tokens")).intValue() : 0;
            int output = usage.get("output_tokens") != null ? ((Number) usage.get("output_tokens")).intValue() : 0;
            tokens = input + output;
        }

        return new LlmResponse(content, tokens);
    }

    private String resolveBaseUrl(String provider, String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) return baseUrl;
        return switch (provider != null ? provider.toLowerCase() : "openai") {
            case "ollama" -> "http://localhost:11434/v1";
            case "azure" -> "";
            default -> "https://api.openai.com/v1";
        };
    }

    public record LlmConfig(String id, String provider, String model, String baseUrl,
                             String apiKey, int maxTokens, double temperature) {}

    public record LlmResponse(String content, int tokenCount) {}

    public record ChatTurn(String role, String content) {}
}
