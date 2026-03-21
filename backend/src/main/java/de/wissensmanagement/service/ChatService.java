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
            Du bist ein KI-Assistent fuer eine unternehmensinterne Wissensdatenbank.

            STRIKTE REGELN - KEINE AUSNAHMEN:
            1. Beantworte Fragen AUSSCHLIESSLICH auf Basis des unten bereitgestellten Kontexts.
            2. Erfinde NIEMALS Informationen, Fakten oder Daten, die nicht im Kontext stehen.
            3. Wenn der Kontext keine ausreichenden Informationen enthaelt, sage KLAR und EHRLICH:
               "Zu dieser Frage liegen mir keine ausreichenden Informationen in der Wissensdatenbank vor."
            4. Wenn du dir bei einer Antwort unsicher bist, kennzeichne dies deutlich mit:
               "Hinweis: Diese Information konnte ich nicht eindeutig aus dem Kontext ableiten."
            5. Verweise IMMER auf die konkreten Quellen (Artikeltitel), aus denen du Informationen verwendest.
            6. Antworte immer auf Deutsch in klarer, professioneller Sprache.
            7. Strukturiere laengere Antworten mit Absaetzen fuer bessere Lesbarkeit.
            8. Gib KEINE Informationen aus dem Kontext preis, die Geheimnisse, Passwoerter oder
               sicherheitsrelevante Konfigurationsdaten enthalten koennten.

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

        // Call LLM via PortalCore proxy
        LlmIntegrationService.LlmResponse llmResponse = llmService.chat(tenantId, jwtToken, systemPrompt, turns);

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
                .modelId(llmResponse.model())
                .tokenCount(llmResponse.tokenCount())
                .build();
        messageRepo.save(assistantMsg);

        // Update session timestamp
        session.setUpdatedAt(java.time.LocalDateTime.now());
        if (llmResponse.configId() != null) {
            session.setModelConfigId(llmResponse.configId());
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
                llmResponse.model(),
                llmResponse.tokenCount()
        );
    }

    public record ChatResponse(String sessionId, String sessionTitle, String content,
                                List<SourceRef> sources, String model, int tokenCount) {}

    public record SourceRef(String articleId, String title, String categoryName) {}
}
