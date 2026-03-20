package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.dto.*;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.service.ArticleService;
import de.wissensmanagement.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artikel")
public class ArticleController {

    private final ArticleService articleService;
    private final FeedbackService feedbackService;
    private final SecurityHelper securityHelper;

    public ArticleController(ArticleService articleService,
                              FeedbackService feedbackService,
                              SecurityHelper securityHelper) {
        this.articleService = articleService;
        this.feedbackService = feedbackService;
        this.securityHelper = securityHelper;
    }

    @GetMapping
    public Page<ArticleDto> list(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        String tenantId = securityHelper.getCurrentTenantId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        return articleService.listArticles(tenantId, status, q, categoryId, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public ArticleDto getById(@PathVariable String id,
                               @RequestParam(defaultValue = "true") boolean trackView) {
        String tenantId = securityHelper.getCurrentTenantId();
        if (trackView) {
            return articleService.getArticleAndTrackView(tenantId, id);
        }
        return articleService.getArticle(tenantId, id);
    }

    @PostMapping
    public ArticleDto create(@Valid @RequestBody ArticleCreateRequest req) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return articleService.createArticle(tenantId, userId, req);
    }

    @PutMapping("/{id}")
    public ArticleDto update(@PathVariable String id,
                              @Valid @RequestBody ArticleUpdateRequest req) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return articleService.updateArticle(tenantId, userId, id, req);
    }

    @PutMapping("/{id}/publish")
    public ArticleDto publish(@PathVariable String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.publishArticle(tenantId, id);
    }

    @PutMapping("/{id}/archive")
    public ArticleDto archive(@PathVariable String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.archiveArticle(tenantId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        articleService.deleteArticle(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versionen")
    public List<ArticleVersionDto> getVersions(@PathVariable String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getVersionHistory(tenantId, id);
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> submitFeedback(@PathVariable String id,
                                                @Valid @RequestBody FeedbackRequest req) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        feedbackService.submitFeedback(tenantId, userId, id, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistik")
    public StatistikDto statistik() {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getStatistik(tenantId);
    }

    @GetMapping("/neueste")
    public List<ArticleDto> newest(@RequestParam(defaultValue = "5") int limit) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getNewestArticles(tenantId, limit);
    }

    @GetMapping("/beliebt")
    public List<ArticleDto> popular(@RequestParam(defaultValue = "5") int limit) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getMostViewedArticles(tenantId, limit);
    }

    @GetMapping("/zuletzt-genutzt")
    public List<ArticleDto> recentlyUsed(@RequestParam(defaultValue = "5") int limit) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getRecentlyUsedArticles(tenantId, limit);
    }

    @GetMapping("/aufgabe/{taskId}")
    public List<ArticleDto> forTask(@PathVariable String taskId) {
        String tenantId = securityHelper.getCurrentTenantId();
        return articleService.getArticlesForTask(tenantId, taskId);
    }
}
