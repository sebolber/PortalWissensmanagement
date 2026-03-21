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
 * Calls PortalCore's LLM chat proxy to send messages to the tenant's active LLM.
 * The API key never leaves PortalCore.
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
     * Sends a chat request through PortalCore's LLM proxy.
     */
    @SuppressWarnings("unchecked")
    public LlmResponse chat(String tenantId, String jwtToken, String systemPrompt,
                             List<ChatTurn> conversationHistory) {
        List<Map<String, String>> messages = conversationHistory.stream()
                .map(turn -> Map.of("role", turn.role(), "content", turn.content()))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "systemPrompt", systemPrompt,
                "messages", messages
        );

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(portalCoreBaseUrl + "/api/tenants/" + tenantId + "/profile/llm/chat")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("LLM chat proxy call failed: {}", e.getMessage(), e);
            throw new LlmException("Kein LLM konfiguriert oder LLM-Aufruf fehlgeschlagen. "
                    + "Bitte pruefen Sie die KI-Verbindung in den Mandanteneinstellungen.");
        }

        if (response == null) {
            throw new LlmException("Keine Antwort vom LLM-Service erhalten.");
        }

        String content = (String) response.get("content");
        String model = (String) response.get("model");
        String configId = (String) response.get("configId");
        int tokenCount = response.get("tokenCount") != null
                ? ((Number) response.get("tokenCount")).intValue() : 0;

        if (content == null || content.isBlank()) {
            throw new LlmException("Keine Antwort vom LLM erhalten. Bitte pruefen Sie die LLM-Konfiguration.");
        }

        return new LlmResponse(content, model, configId, tokenCount);
    }

    public static class LlmException extends RuntimeException {
        public LlmException(String message) {
            super(message);
        }
    }

    public record LlmResponse(String content, String model, String configId, int tokenCount) {}

    public record ChatTurn(String role, String content) {}
}
