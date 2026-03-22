package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.entity.PromptCategory;
import de.wissensmanagement.repository.PromptCategoryRepository;
import de.wissensmanagement.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompt-kategorien")
public class PromptCategoryController {

    private final PromptCategoryRepository repo;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public PromptCategoryController(PromptCategoryRepository repo,
                                     SecurityHelper securityHelper,
                                     PermissionService permissionService) {
        this.repo = repo;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<CategoryDto> list() {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        return repo.findByTenantIdOrderBySortOrderAscNameAsc(tenantId).stream()
                .map(this::toDto).toList();
    }

    @PostMapping
    public CategoryDto create(@RequestBody CategoryRequest req) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        PromptCategory cat = PromptCategory.builder()
                .tenantId(tenantId)
                .name(req.name())
                .description(req.description())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build();
        return toDto(repo.save(cat));
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable String id, @RequestBody CategoryRequest req) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        PromptCategory cat = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt-Kategorie nicht gefunden"));
        cat.setName(req.name());
        cat.setDescription(req.description());
        if (req.sortOrder() != null) cat.setSortOrder(req.sortOrder());
        return toDto(repo.save(cat));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        permissionService.requireSchreiben(securityHelper.getCurrentToken());
        String tenantId = securityHelper.getCurrentTenantId();
        PromptCategory cat = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt-Kategorie nicht gefunden"));
        repo.delete(cat);
        return ResponseEntity.noContent().build();
    }

    private CategoryDto toDto(PromptCategory cat) {
        return new CategoryDto(cat.getId(), cat.getName(), cat.getDescription(), cat.getSortOrder());
    }

    record CategoryRequest(String name, String description, Integer sortOrder) {}
    record CategoryDto(String id, String name, String description, int sortOrder) {}
}
