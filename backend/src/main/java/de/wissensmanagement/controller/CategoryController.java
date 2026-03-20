package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.dto.CategoryDto;
import de.wissensmanagement.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kategorien")
public class CategoryController {

    private final CategoryService categoryService;
    private final SecurityHelper securityHelper;

    public CategoryController(CategoryService categoryService, SecurityHelper securityHelper) {
        this.categoryService = categoryService;
        this.securityHelper = securityHelper;
    }

    @GetMapping
    public List<CategoryDto> list() {
        return categoryService.listCategories(securityHelper.getCurrentTenantId());
    }

    @GetMapping("/{id}")
    public CategoryDto getById(@PathVariable String id) {
        return categoryService.getCategory(securityHelper.getCurrentTenantId(), id);
    }

    @PostMapping
    public CategoryDto create(@Valid @RequestBody CategoryDto req) {
        return categoryService.createCategory(securityHelper.getCurrentTenantId(), req);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable String id, @Valid @RequestBody CategoryDto req) {
        return categoryService.updateCategory(securityHelper.getCurrentTenantId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        categoryService.deleteCategory(securityHelper.getCurrentTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
