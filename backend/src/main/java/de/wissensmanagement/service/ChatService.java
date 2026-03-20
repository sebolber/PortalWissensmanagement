package de.wissensmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wissensmanagement.entity.ChatMessage;
import de.wissensmanagement.entity.ChatSession;
import de.wissensmanagement.enums.ChatRole;
import de.wissensmanagement.repository.ChatMessageRepository;
import de.wissensmanagement.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_HISTORY_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = """
            Du bist ein hilfreicher KI-Assistent fuer eine Wissensdatenbank.
            Beantworte Fragen ausschliesslich auf Basis des bereitgestellten Kontexts.
            Wenn der Kontext keine relevanten Informationen enthaelt, sage das ehrlich.
            Antworte immer auf Deutsch. Verwende eine klare, professionelle Sprache.
            Verweise auf die Quellen, wenn du Informationen daraus verwendest.

            Kontext aus der Wissensdatenbank:
            ---
            %s
            ---
            """;

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final RetrievalService retrievalService;
    private final LlmIntegrationService llmService;
    private final ObjectMapper objectMapper;

    public ChatService(ChatSessionRepository sessionRepo, ChatMessageRepository messageRepo,
                       RetrievalService retrievalService, LlmIntegrationService llmService,
                       ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public List<ChatSession> listSessions(String tenantId, String userId) {
        return sessionRepo.findByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId);
    }

    public ChatSession getSession(String sessionId, String tenantId, String userId) {
        return sessionRepo.findByIdAndTenantIdAndUserId(sessionId, tenantId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session nicht gefunden"));
    }

    public List<ChatMessage> getMessages(String sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ChatSession createSession(String tenantId, String userId, String title) {
        ChatSession session = ChatSession.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(title != null && !title.isBlank() ? title : "Neue Unterhaltung")
                .build();
        return sessionRepo.save(session);
    }

    @Transactional
    public void deleteSession(String sessionId, String tenantId, String userId) {
        ChatSession session = getSession(sessionId, tenantId, userId);
        messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .forEach(messageRepo::delete);
        sessionRepo.delete(session);
    }

    @Transactional
    public ChatResponse sendMessage(String sessionId, String tenantId, String userId,
                                     String userMessage, String jwtToken) {
        // Get or validate session
        ChatSession session;
        if (sessionId == null || sessionId.isBlank()) {
            // Auto-create session with first message as title
            String title = userMessage.length() > 60 ? userMessage.substring(0, 60) + "..." : userMessage;
            session = createSession(tenantId, userId, title);
        } else {
            session = getSession(sessionId, tenantId, userId);
        }

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .role(ChatRole.USER)
                .content(userMessage)
                .build();
        messageRepo.save(userMsg);

        // Retrieve relevant context
        RetrievalService.RetrievalResult retrieval = retrievalService.retrieve(tenantId, userMessage);

        // Build system prompt with context
        String systemPrompt = String.format(SYSTEM_PROMPT,
                retrieval.context().isEmpty() ? "Kein relevanter Kontext gefunden." : retrieval.context());

        // Build conversation history
        List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<LlmIntegrationService.ChatTurn> turns = new ArrayList<>();
        int startIdx = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (int i = startIdx; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = msg.getRole() == ChatRole.USER ? "user" : "assistant";
            turns.add(new LlmIntegrationService.ChatTurn(role, msg.getContent()));
        }

        // Get LLM config and call
        LlmIntegrationService.LlmConfig llmConfig = llmService.getActiveLlmConfig(tenantId, jwtToken);
        LlmIntegrationService.LlmResponse llmResponse = llmService.chat(llmConfig, systemPrompt, turns);

        // Serialize source references
        String sourceRefsJson = null;
        if (!retrieval.sources().isEmpty()) {
            try {
                sourceRefsJson = objectMapper.writeValueAsString(retrieval.sources());
            } catch (Exception e) {
                log.warn("Failed to serialize source refs", e);
            }
        }

        // Save assistant message
        ChatMessage assistantMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .role(ChatRole.ASSISTANT)
                .content(llmResponse.content())
                .sourceRefs(sourceRefsJson)
                .modelId(llmConfig != null ? llmConfig.model() : null)
                .tokenCount(llmResponse.tokenCount())
                .build();
        messageRepo.save(assistantMsg);

        // Update session timestamp
        session.setUpdatedAt(java.time.LocalDateTime.now());
        if (llmConfig != null) {
            session.setModelConfigId(llmConfig.id());
        }
        sessionRepo.save(session);

        // Build response
        List<SourceRef> sources = retrieval.sources().stream()
                .map(s -> new SourceRef(s.articleId(), s.title(), s.categoryName()))
                .collect(Collectors.toList());

        return new ChatResponse(
                session.getId(),
                session.getTitle(),
                assistantMsg.getContent(),
                sources,
                llmConfig != null ? llmConfig.model() : null,
                llmResponse.tokenCount()
        );
    }

    public record ChatResponse(String sessionId, String sessionTitle, String content,
                                List<SourceRef> sources, String model, int tokenCount) {}

    public record SourceRef(String articleId, String title, String categoryName) {}
}
