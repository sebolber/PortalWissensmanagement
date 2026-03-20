package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.entity.ChatMessage;
import de.wissensmanagement.entity.ChatSession;
import de.wissensmanagement.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final SecurityHelper securityHelper;

    public ChatController(ChatService chatService, SecurityHelper securityHelper) {
        this.chatService = chatService;
        this.securityHelper = securityHelper;
    }

    @GetMapping("/sessions")
    public List<SessionDto> listSessions() {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return chatService.listSessions(tenantId, userId).stream()
                .map(this::toSessionDto)
                .toList();
    }

    @PostMapping("/sessions")
    public SessionDto createSession(@RequestBody(required = false) Map<String, String> body) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String title = body != null ? body.get("title") : null;
        return toSessionDto(chatService.createSession(tenantId, userId, title));
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionDto getSession(@PathVariable String sessionId) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return toSessionDto(chatService.getSession(sessionId, tenantId, userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        chatService.deleteSession(sessionId, tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<MessageDto> getMessages(@PathVariable String sessionId) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        // Validate access
        chatService.getSession(sessionId, tenantId, userId);
        return chatService.getMessages(sessionId).stream()
                .map(this::toMessageDto)
                .toList();
    }

    @PostMapping("/send")
    public ChatResponseDto sendMessage(@RequestBody SendRequest request, HttpServletRequest httpRequest) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String jwtToken = extractToken(httpRequest);

        ChatService.ChatResponse response = chatService.sendMessage(
                request.sessionId(), tenantId, userId, request.message(), jwtToken);

        return new ChatResponseDto(
                response.sessionId(),
                response.sessionTitle(),
                response.content(),
                response.sources(),
                response.model(),
                response.tokenCount()
        );
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
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

    record SendRequest(String sessionId, String message) {}

    record ChatResponseDto(String sessionId, String sessionTitle, String content,
                           List<ChatService.SourceRef> sources, String model, int tokenCount) {}

    record SessionDto(String id, String title, String modelConfigId, String createdAt, String updatedAt) {}

    record MessageDto(String id, String role, String content, String sourceRefs,
                      String modelId, Integer tokenCount, String createdAt) {}
}
