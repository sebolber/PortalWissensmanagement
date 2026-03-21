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
        try {
            List<Map<String, String>> messages = conversationHistory.stream()
                    .map(turn -> Map.of("role", turn.role(), "content", turn.content()))
                    .toList();

            Map<String, Object> requestBody = Map.of(
                    "systemPrompt", systemPrompt,
                    "messages", messages
            );

            Map<String, Object> response = restClient.post()
                    .uri(portalCoreBaseUrl + "/api/tenants/" + tenantId + "/profile/llm/chat")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return new LlmResponse("Keine Antwort vom LLM-Service erhalten.", null, null, 0);
            }

            String content = (String) response.get("content");
            String model = (String) response.get("model");
            String configId = (String) response.get("configId");
            int tokenCount = response.get("tokenCount") != null
                    ? ((Number) response.get("tokenCount")).intValue() : 0;

            if (content == null || content.isBlank()) {
                return new LlmResponse("Keine Antwort vom LLM erhalten. Bitte pruefen Sie die LLM-Konfiguration.", model, configId, tokenCount);
            }

            return new LlmResponse(content, model, configId, tokenCount);
        } catch (Exception e) {
            log.error("LLM chat proxy call failed: {}", e.getMessage(), e);
            return new LlmResponse(
                    "Kein LLM konfiguriert. Bitte konfigurieren Sie eine KI-Verbindung in den Mandanteneinstellungen.",
                    null, null, 0);
        }
    }

    public record LlmResponse(String content, String model, String configId, int tokenCount) {}

    public record ChatTurn(String role, String content) {}
}
