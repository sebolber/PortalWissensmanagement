package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.service.DictationService;
import de.wissensmanagement.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diktat")
public class DictationController {

    private final DictationService dictationService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public DictationController(DictationService dictationService,
                                SecurityHelper securityHelper,
                                PermissionService permissionService) {
        this.dictationService = dictationService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    /**
     * Structures raw/dictated text using LLM.
     * Input: { "text": "raw dictated text..." }
     * Output: { "title": "...", "summary": "...", "content": "..." }
     */
    @PostMapping("/strukturieren")
    public DictationService.StructuredResult structureText(@RequestBody Map<String, String> body) {
        String jwtToken = securityHelper.getCurrentToken();
        permissionService.requireSchreiben(jwtToken);
        String tenantId = securityHelper.getCurrentTenantId();
        String rawText = body.get("text");

        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Text darf nicht leer sein.");
        }

        return dictationService.structureText(tenantId, jwtToken, rawText);
    }
}
