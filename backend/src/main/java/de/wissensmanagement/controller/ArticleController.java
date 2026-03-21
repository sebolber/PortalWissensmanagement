package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.dto.*;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.service.ArticleService;
import de.wissensmanagement.service.FeedbackService;
import de.wissensmanagement.service.LlmIntegrationService;
import de.wissensmanagement.service.PermissionService;
import de.wissensmanagement.service.TaskIntegrationService;
import de.wissensmanagement.service.UsageTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artikel")
public class ArticleController {

    private final ArticleService articleService;
    private final FeedbackService feedbackService;
    private final LlmIntegrationService llmService;
    private final TaskIntegrationService taskService;
    private final UsageTrackingService usageService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public ArticleController(ArticleService articleService,
                              FeedbackService feedbackService,
                              LlmIntegrationService llmService,
                              TaskIntegrationService taskService,
                              UsageTrackingService usageService,
                              SecurityHelper securityHelper,
                              PermissionService permissionService) {
        this.articleService = articleService;
        this.feedbackService = feedbackService;
        this.llmService = llmService;
        this.taskService = taskService;
        this.usageService = usageService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Page<ArticleDto> list(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String groupingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        return articleService.listArticles(tenantId, status, q, categoryId, groupingId, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public ArticleDto getById(@PathVariable String id,
                               @RequestParam(defaultValue = "true") boolean trackView) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        if (trackView) {
            return articleService.getArticleAndTrackView(tenantId, id);
        }
        return articleService.getArticle(tenantId, id);
    }

    @PostMapping
    public ArticleDto create(@Valid @RequestBody ArticleCreateRequest req) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return articleService.createArticle(tenantId, userId, req);
    }

    @PutMapping("/{id}")
    public ArticleDto update(@PathVariable String id,
                              @Valid @RequestBody ArticleUpdateRequest req) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return articleService.updateArticle(tenantId, userId, id, req);
    }

    @PutMapping("/{id}/publish")
    public ArticleDto publish(@PathVariable String id) {
        permissionService.requireVeroeffentlichen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.publishArticle(tenantId, id);
    }

    @PutMapping("/{id}/archive")
    public ArticleDto archive(@PathVariable String id) {
        permissionService.requireVeroeffentlichen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.archiveArticle(tenantId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        permissionService.requireBerechtigung(securityHelper.getCurrentToken(),
                PermissionService.UC_SCHREIBEN, "loeschen");
        String tenantId = securityHelper.getCurrentTenantId();
        articleService.deleteArticle(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versionen")
    public List<ArticleVersionDto> getVersions(@PathVariable String id) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getVersionHistory(tenantId, id);
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> submitFeedback(@PathVariable String id,
                                                @Valid @RequestBody FeedbackRequest req) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        feedbackService.submitFeedback(tenantId, userId, id, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistik")
    public StatistikDto statistik() {
        permissionService.requireAdmin(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getStatistik(tenantId);
    }

    @GetMapping("/neueste")
    public List<ArticleDto> newest(@RequestParam(defaultValue = "5") int limit) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getNewestArticles(tenantId, limit);
    }

    @GetMapping("/beliebt")
    public List<ArticleDto> popular(@RequestParam(defaultValue = "5") int limit) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getMostViewedArticles(tenantId, limit);
    }

    @GetMapping("/zuletzt-genutzt")
    public List<ArticleDto> recentlyUsed(@RequestParam(defaultValue = "5") int limit) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getRecentlyUsedArticles(tenantId, limit);
    }

    @GetMapping("/aufgabe/{taskId}")
    public List<ArticleDto> forTask(@PathVariable String taskId) {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getArticlesForTask(tenantId, taskId);
    }

    @PostMapping("/{id}/aufgabe")
    public ResponseEntity<Map<String, String>> createTask(@PathVariable String id,
                                                           @RequestBody Map<String, String> body,
                                                           HttpServletRequest httpRequest) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        ArticleDto article = articleService.getArticle(tenantId, id);

        String betreff = body.getOrDefault("betreff", "Artikel ueberpruefen: " + article.getTitle());
        String inhalt = body.getOrDefault("inhalt", "Bitte ueberpruefen Sie den Artikel: " + article.getTitle());
        String assigneeUserId = body.get("assigneeUserId");

        String jwtToken = extractToken(httpRequest);
        String taskId = taskService.createArticleTask(jwtToken, tenantId, id,
                article.getTitle(), betreff, inhalt, assigneeUserId);

        if (taskId != null) {
            // Link task to article
            articleService.linkTask(tenantId, id, taskId);
            return ResponseEntity.ok(Map.of("taskId", taskId));
        }
        return ResponseEntity.internalServerError().body(Map.of("error", "Aufgabe konnte nicht erstellt werden"));
    }

    @PostMapping("/generate-summary")
    public ResponseEntity<Map<String, String>> generateSummary(@RequestBody Map<String, String> body) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        String jwtToken = securityHelper.getCurrentToken();
        String content = body.get("content");
        String title = body.getOrDefault("title", "");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Inhalt ist erforderlich"));
        }

        String prompt = "Erstelle eine ausfuehrliche Zusammenfassung (3-5 Saetze) des folgenden Artikels. " +
                "Die Zusammenfassung soll die wichtigsten Punkte und Kernaussagen enthalten. " +
                "Antworte nur mit der Zusammenfassung, ohne Einleitung oder Erklaerung.\n\n" +
                "Titel: " + title + "\n\nInhalt:\n" + content;

        List<LlmIntegrationService.ChatTurn> turns = List.of(
                new LlmIntegrationService.ChatTurn("user", prompt));
        LlmIntegrationService.LlmResponse response = llmService.chat(tenantId, jwtToken,
                "Du bist ein hilfreicher Assistent, der Zusammenfassungen fuer Wissensartikel erstellt.", turns);

        return ResponseEntity.ok(Map.of("summary", response.content()));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
