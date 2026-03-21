package de.wissensmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles dictation raw text processing and LLM-based structuring.
 * Converts raw/dictated text into well-structured article content.
 */
@Service
public class DictationService {

    private static final Logger log = LoggerFactory.getLogger(DictationService.class);

    private static final String STRUCTURE_PROMPT = """
            Du bist ein Textstrukturierungs-Assistent fuer eine Wissensdatenbank.

            STRIKTE REGELN:
            1. Strukturiere den folgenden Rohtext zu einem klar gegliederten Artikel.
            2. Verwende Absaetze, Ueberschriften und Aufzaehlungen wo sinnvoll.
            3. Verbessere Grammatik und Ausdruck, behalte aber den fachlichen Inhalt EXAKT bei.
            4. Erfinde KEINE neuen Inhalte, Fakten oder Informationen.
            5. Ergaenze KEINE eigenen Erklaerungen oder Interpretationen.
            6. Entferne Fuellwoerter und Wiederholungen aus diktiertem Text.
            7. Erstelle eine kurze Zusammenfassung (max. 2 Saetze) am Anfang.

            Antworte im folgenden Format:
            ZUSAMMENFASSUNG: [Zusammenfassung]
            ---
            TITEL: [Vorgeschlagener Titel]
            ---
            INHALT:
            [Strukturierter Inhalt]

            Rohtext:
            ---
            %s
            ---
            """;

    private final LlmIntegrationService llmService;

    public DictationService(LlmIntegrationService llmService) {
        this.llmService = llmService;
    }

    /**
     * Structures raw/dictated text using LLM.
     */
    public StructuredResult structureText(String tenantId, String jwtToken, String rawText) {
        String prompt = String.format(STRUCTURE_PROMPT, rawText);

        LlmIntegrationService.LlmResponse response = llmService.chat(
                tenantId, jwtToken, prompt,
                List.of(new LlmIntegrationService.ChatTurn("user", "Bitte strukturiere diesen Text."))
        );

        return parseStructuredResponse(response.content());
    }

    private StructuredResult parseStructuredResponse(String response) {
        String summary = "";
        String title = "";
        String content = response; // fallback: use full response as content

        try {
            if (response.contains("ZUSAMMENFASSUNG:") && response.contains("---")) {
                String[] parts = response.split("---");
                if (parts.length >= 3) {
                    summary = parts[0].replace("ZUSAMMENFASSUNG:", "").trim();
                    title = parts[1].replace("TITEL:", "").trim();
                    // Content is everything after the second ---
                    StringBuilder contentBuilder = new StringBuilder();
                    for (int i = 2; i < parts.length; i++) {
                        if (i > 2) contentBuilder.append("---");
                        contentBuilder.append(parts[i]);
                    }
                    content = contentBuilder.toString().replace("INHALT:", "").trim();
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse structured LLM response, using raw content");
        }

        return new StructuredResult(title, summary, content);
    }

    public record StructuredResult(String title, String summary, String content) {}
}
