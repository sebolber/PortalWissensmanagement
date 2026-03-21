package de.wissensmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.wissensmanagement.entity.ChatMessage;
import de.wissensmanagement.entity.ChatSession;
import de.wissensmanagement.enums.ChatRole;
import de.wissensmanagement.repository.ChatMessageRepository;
import de.wissensmanagement.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepo;

    @Mock
    private ChatMessageRepository messageRepo;

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private LlmIntegrationService llmService;

    private ChatService chatService;

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-1";
    private static final String JWT_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        chatService = new ChatService(sessionRepo, messageRepo, retrievalService, llmService, new ObjectMapper());
    }

    @Test
    void sendMessage_createsSessionForNewChat() {
        ChatSession session = ChatSession.builder()
                .tenantId(TENANT_ID).userId(USER_ID).title("Frage...").build();
        session.setId("session-1");
        session.setCreatedAt(LocalDateTime.now());

        when(sessionRepo.save(any())).thenReturn(session);
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(retrievalService.retrieve(eq(TENANT_ID), anyString()))
                .thenReturn(new RetrievalService.RetrievalResult("Kontext", List.of()));
        when(llmService.chat(eq(TENANT_ID), eq(JWT_TOKEN), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse("Antwort", "gpt-4", "cfg-1", 100));
        when(messageRepo.findBySessionIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());

        ChatService.ChatResponse response = chatService.sendMessage(null, TENANT_ID, USER_ID, "Frage?", JWT_TOKEN);

        assertEquals("session-1", response.sessionId());
        assertEquals("Antwort", response.content());
    }

    @Test
    void sendMessage_usesRetrievedContext() {
        ChatSession session = ChatSession.builder()
                .tenantId(TENANT_ID).userId(USER_ID).title("Test").build();
        session.setId("session-1");
        session.setCreatedAt(LocalDateTime.now());

        when(sessionRepo.findByIdAndTenantIdAndUserId("session-1", TENANT_ID, USER_ID))
                .thenReturn(Optional.of(session));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.findBySessionIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());
        when(retrievalService.retrieve(TENANT_ID, "Frage?"))
                .thenReturn(new RetrievalService.RetrievalResult(
                        "Relevanter Kontext aus Artikel",
                        List.of(new RetrievalService.SourceReference("art-1", "Quell-Artikel", "Kategorie"))
                ));
        when(llmService.chat(eq(TENANT_ID), eq(JWT_TOKEN), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse("Antwort basierend auf Kontext", "gpt-4", "cfg-1", 100));

        ChatService.ChatResponse response = chatService.sendMessage("session-1", TENANT_ID, USER_ID, "Frage?", JWT_TOKEN);

        // Verify the system prompt includes the retrieved context
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmService).chat(eq(TENANT_ID), eq(JWT_TOKEN), promptCaptor.capture(), anyList());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Relevanter Kontext aus Artikel"));
        assertTrue(prompt.contains("AUSSCHLIESSLICH")); // Anti-hallucination instruction

        // Verify sources are returned
        assertEquals(1, response.sources().size());
        assertEquals("art-1", response.sources().get(0).articleId());
    }

    @Test
    void sendMessage_handlesEmptyContext() {
        ChatSession session = ChatSession.builder()
                .tenantId(TENANT_ID).userId(USER_ID).title("Test").build();
        session.setId("s-1");
        session.setCreatedAt(LocalDateTime.now());

        when(sessionRepo.findByIdAndTenantIdAndUserId("s-1", TENANT_ID, USER_ID))
                .thenReturn(Optional.of(session));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.findBySessionIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());
        when(retrievalService.retrieve(TENANT_ID, "Unknown?"))
                .thenReturn(new RetrievalService.RetrievalResult("", List.of()));
        when(llmService.chat(any(), any(), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse("No context found", null, null, 0));

        ChatService.ChatResponse response = chatService.sendMessage("s-1", TENANT_ID, USER_ID, "Unknown?", JWT_TOKEN);

        // Verify system prompt mentions no context
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmService).chat(any(), any(), promptCaptor.capture(), anyList());
        assertTrue(promptCaptor.getValue().contains("Kein relevanter Kontext"));
    }

    @Test
    void listSessions_enforceTenantIsolation() {
        chatService.listSessions(TENANT_ID, USER_ID);
        verify(sessionRepo).findByTenantIdAndUserIdOrderByUpdatedAtDesc(TENANT_ID, USER_ID);
    }

    @Test
    void getSession_enforceTenantAndUserIsolation() {
        when(sessionRepo.findByIdAndTenantIdAndUserId("s-1", "other-tenant", USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> chatService.getSession("s-1", "other-tenant", USER_ID));
    }

    @Test
    void deleteSession_enforceTenantIsolation() {
        when(sessionRepo.findByIdAndTenantIdAndUserId("s-1", "wrong-tenant", USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> chatService.deleteSession("s-1", "wrong-tenant", USER_ID));
    }

    @Test
    void systemPrompt_containsAntiHallucinationInstructions() {
        ChatSession session = ChatSession.builder()
                .tenantId(TENANT_ID).userId(USER_ID).title("Test").build();
        session.setId("s-1");
        session.setCreatedAt(LocalDateTime.now());

        when(sessionRepo.findByIdAndTenantIdAndUserId("s-1", TENANT_ID, USER_ID))
                .thenReturn(Optional.of(session));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.findBySessionIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());
        when(retrievalService.retrieve(any(), any()))
                .thenReturn(new RetrievalService.RetrievalResult("context", List.of()));
        when(llmService.chat(any(), any(), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse("answer", null, null, 0));

        chatService.sendMessage("s-1", TENANT_ID, USER_ID, "Test?", JWT_TOKEN);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmService).chat(any(), any(), promptCaptor.capture(), anyList());
        String prompt = promptCaptor.getValue();

        // Verify anti-hallucination guardrails
        assertTrue(prompt.contains("AUSSCHLIESSLICH"));
        assertTrue(prompt.contains("NIEMALS"));
        assertTrue(prompt.contains("Quellen"));
        assertTrue(prompt.contains("Geheimnisse"));
    }
}
