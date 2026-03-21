package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.entity.PromptConfig;
import de.wissensmanagement.enums.PromptType;
import de.wissensmanagement.service.LlmIntegrationService;
import de.wissensmanagement.service.PermissionService;
import de.wissensmanagement.service.PromptConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
public class PromptConfigController {

    private final PromptConfigService promptConfigService;
    private final LlmIntegrationService llmService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public PromptConfigController(PromptConfigService promptConfigService,
                                   LlmIntegrationService llmService,
                                   SecurityHelper securityHelper,
                                   PermissionService permissionService) {
        this.promptConfigService = promptConfigService;
        this.llmService = llmService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<PromptDto> list(@RequestParam(required = false) PromptType type) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        List<PromptConfig> configs = type != null
                ? promptConfigService.listByType(tenantId, type)
                : promptConfigService.listAll(tenantId);
        return configs.stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public PromptDto getById(@PathVariable String id) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return toDto(promptConfigService.getById(tenantId, id));
    }

    @PostMapping
    public PromptDto create(@RequestBody PromptRequest request) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        PromptConfig config = promptConfigService.create(tenantId,
                request.name(), request.description(), request.promptText(), request.promptType());
        return toDto(config);
    }

    @PutMapping("/{id}")
    public PromptDto update(@PathVariable String id, @RequestBody PromptRequest request) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        PromptConfig config = promptConfigService.update(tenantId, id,
                request.name(), request.description(), request.promptText(), request.promptType());
        return toDto(config);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        promptConfigService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Wendet einen konfigurierten Prompt auf den uebergebenen Inhalt an.
     * Der Prompt-Text wird als System-Prompt verwendet, der Inhalt als User-Nachricht.
     */
    @PostMapping("/{id}/anwenden")
    public ResponseEntity<Map<String, String>> applyPrompt(@PathVariable String id,
                                                             @RequestBody Map<String, String> body) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String jwtToken = securityHelper.getCurrentToken();

        PromptConfig config = promptConfigService.getById(tenantId, id);
        String content = body.get("content");
        String title = body.getOrDefault("title", "");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Inhalt ist erforderlich"));
        }

        String userMessage = title.isBlank() ? content : "Titel: " + title + "\n\n" + content;

        LlmIntegrationService.LlmResponse response = llmService.chat(tenantId, jwtToken,
                config.getPromptText(),
                List.of(new LlmIntegrationService.ChatTurn("user", userMessage)));

        return ResponseEntity.ok(Map.of("result", response.content()));
    }

    private PromptDto toDto(PromptConfig config) {
        return new PromptDto(config.getId(), config.getName(), config.getDescription(),
                config.getPromptText(), config.getPromptType().name(),
                config.getCreatedAt() != null ? config.getCreatedAt().toString() : null,
                config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);
    }

    record PromptRequest(String name, String description, String promptText, PromptType promptType) {}

    record PromptDto(String id, String name, String description, String promptText,
                     String promptType, String createdAt, String updatedAt) {}
}
