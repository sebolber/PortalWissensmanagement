package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.entity.ChatMessage;
import de.wissensmanagement.entity.ChatSession;
import de.wissensmanagement.service.ChatService;
import de.wissensmanagement.service.LlmIntegrationService;
import de.wissensmanagement.service.PermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;
    private final RestClient restClient = RestClient.create();

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    public ChatController(ChatService chatService, SecurityHelper securityHelper,
                          PermissionService permissionService) {
        this.chatService = chatService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping("/sessions")
    public List<SessionDto> listSessions() {
        permissionService.requireChat(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return chatService.listSessions(tenantId, userId).stream()
                .map(this::toSessionDto)
                .toList();
    }

    @PostMapping("/sessions")
    public SessionDto createSession(@RequestBody(required = false) Map<String, String> body) {
        permissionService.requireChat(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String title = body != null ? body.get("title") : null;
        return toSessionDto(chatService.createSession(tenantId, userId, title));
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionDto getSession(@PathVariable String sessionId) {
        permissionService.requireChat(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return toSessionDto(chatService.getSession(sessionId, tenantId, userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        permissionService.requireChat(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        chatService.deleteSession(sessionId, tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<MessageDto> getMessages(@PathVariable String sessionId) {
        permissionService.requireChat(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        // Validate access
        chatService.getSession(sessionId, tenantId, userId);
        return chatService.getMessages(sessionId).stream()
                .map(this::toMessageDto)
                .toList();
    }

    @PostMapping("/send")
    public ChatResponseDto sendMessage(@RequestBody SendRequest request) {
        String jwtToken = securityHelper.getCurrentToken();
        permissionService.requireChat(jwtToken);
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();

        ChatService.ChatResponse response = chatService.sendMessage(
                request.sessionId(), tenantId, userId, request.message(), jwtToken);

        return new ChatResponseDto(
                response.sessionId(),
                response.sessionTitle(),
                response.content(),
                response.sources(),
                response.model(),
                response.tokenCount(),
                response.retrievalTrace()
        );
    }

    private SessionDto toSessionDto(ChatSession s) {
        return new SessionDto(s.getId(), s.getTitle(), s.getModelConfigId(),
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null,
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
    }

    private MessageDto toMessageDto(ChatMessage m) {
        return new MessageDto(m.getId(), m.getRole().name().toLowerCase(), m.getContent(),
                m.getSourceRefs(), m.getModelId(), m.getTokenCount(),
                m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/llm-models")
    public ResponseEntity<List<Map<String, Object>>> listLlmModels() {
        String jwtToken = securityHelper.getCurrentToken();
        permissionService.requireChat(jwtToken);
        String tenantId = securityHelper.getCurrentTenantId();
        try {
            List<Map<String, Object>> configs = restClient.get()
                    .uri(portalCoreBaseUrl + "/api/tenants/" + tenantId + "/profile/llm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .body(List.class);
            if (configs == null) return ResponseEntity.ok(List.of());
            List<Map<String, Object>> result = configs.stream()
                    .map(c -> Map.<String, Object>of(
                            "id", c.getOrDefault("id", ""),
                            "name", c.getOrDefault("name", ""),
                            "provider", c.getOrDefault("provider", ""),
                            "model", c.getOrDefault("model", ""),
                            "isActive", c.getOrDefault("isActive", false)
                    ))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Failed to fetch LLM models from PortalCore: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    record SendRequest(String sessionId, String message, String modelConfigId) {}

    record ChatResponseDto(String sessionId, String sessionTitle, String content,
                           List<ChatService.SourceRef> sources, String model, int tokenCount,
                           List<ChatService.TraceInfo> retrievalTrace) {}

    record SessionDto(String id, String title, String modelConfigId, String createdAt, String updatedAt) {}

    record MessageDto(String id, String role, String content, String sourceRefs,
                      String modelId, Integer tokenCount, String createdAt) {}
}
