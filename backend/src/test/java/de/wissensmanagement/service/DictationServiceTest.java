package de.wissensmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictationServiceTest {

    @Mock
    private LlmIntegrationService llmService;

    @InjectMocks
    private DictationService dictationService;

    @Test
    void structureText_parsesWellFormattedResponse() {
        String llmOutput = """
                ZUSAMMENFASSUNG: Dies ist eine Zusammenfassung des Artikels.
                ---
                TITEL: Strukturierter Artikel
                ---
                INHALT:
                Dies ist der strukturierte Inhalt.

                Mit Absaetzen und klarer Gliederung.
                """;

        when(llmService.chat(any(), any(), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse(llmOutput, "gpt-4", null, 100));

        DictationService.StructuredResult result = dictationService.structureText("tenant-1", "token", "raw text");

        assertEquals("Dies ist eine Zusammenfassung des Artikels.", result.summary());
        assertEquals("Strukturierter Artikel", result.title());
        assertTrue(result.content().contains("strukturierte Inhalt"));
    }

    @Test
    void structureText_handlesUnparsableResponse() {
        String llmOutput = "Hier ist der Text einfach formatiert ohne Marker.";

        when(llmService.chat(any(), any(), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse(llmOutput, "gpt-4", null, 50));

        DictationService.StructuredResult result = dictationService.structureText("tenant-1", "token", "raw text");

        // Should use the raw response as content
        assertNotNull(result.content());
        assertFalse(result.content().isBlank());
    }

    @Test
    void structureText_passesRawTextToLlm() {
        String rawText = "Dies ist ein diktierter Text mit Fuellwoertern und so weiter und so fort.";

        when(llmService.chat(any(), any(), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse("result", null, null, 0));

        dictationService.structureText("tenant-1", "token", rawText);

        verify(llmService).chat(eq("tenant-1"), eq("token"), contains(rawText), anyList());
    }

    @Test
    void structureText_promptContainsNoInventionRules() {
        when(llmService.chat(any(), any(), anyString(), anyList()))
                .thenReturn(new LlmIntegrationService.LlmResponse("result", null, null, 0));

        dictationService.structureText("tenant-1", "token", "some text");

        verify(llmService).chat(any(), any(), argThat(prompt -> {
            // Verify the prompt enforces no content invention
            return prompt.contains("Erfinde KEINE") && prompt.contains("Ergaenze KEINE");
        }), anyList());
    }
}
