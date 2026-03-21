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
 * Integrates with PortalCore's Nachrichtencenter to create tasks linked to knowledge articles.
 */
@Service
public class TaskIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(TaskIntegrationService.class);

    private final RestClient restClient;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    public TaskIntegrationService() {
        this.restClient = RestClient.create();
    }

    /**
     * Creates a task in PortalCore's Nachrichtencenter linked to an article.
     */
    @SuppressWarnings("unchecked")
    public String createArticleTask(String jwtToken, String tenantId, String articleId,
                                     String articleTitle, String betreff, String inhalt,
                                     String assigneeUserId) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("typ", "AUFGABE");
            body.put("betreff", betreff);
            body.put("inhalt", inhalt);
            body.put("referenzTyp", "WISSENSARTIKEL");
            body.put("referenzId", articleId);
            body.put("sourceApp", "wissensmanagement");
            body.put("sourceType", "artikel");
            body.put("systemGeneriert", true);
            if (assigneeUserId != null) {
                body.put("empfaengerIds", List.of(assigneeUserId));
            }

            Map<String, Object> response = restClient.post()
                    .uri(portalCoreBaseUrl + "/api/nachrichten")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return response != null ? (String) response.get("id") : null;
        } catch (Exception e) {
            log.error("Failed to create task in PortalCore: {}", e.getMessage());
            return null;
        }
    }
}
