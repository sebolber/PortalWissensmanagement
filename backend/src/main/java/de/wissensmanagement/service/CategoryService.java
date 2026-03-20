package de.wissensmanagement.service;

import de.wissensmanagement.dto.CategoryDto;
import de.wissensmanagement.entity.KnowledgeCategory;
import de.wissensmanagement.repository.KnowledgeCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final KnowledgeCategoryRepository categoryRepo;

    public CategoryService(KnowledgeCategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    public List<CategoryDto> listCategories(String tenantId) {
        return categoryRepo.findByTenantIdOrderByOrderIndexAsc(tenantId).stream()
                .map(this::toDto).toList();
    }

    public CategoryDto getCategory(String tenantId, String id) {
        return toDto(findByTenant(tenantId, id));
    }

    @Transactional
    public CategoryDto createCategory(String tenantId, CategoryDto req) {
        KnowledgeCategory cat = KnowledgeCategory.builder()
                .tenantId(tenantId)
                .name(req.getName())
                .description(req.getDescription())
                .parentId(req.getParentId())
                .orderIndex(req.getOrderIndex())
                .build();
        return toDto(categoryRepo.save(cat));
    }

    @Transactional
    public CategoryDto updateCategory(String tenantId, String id, CategoryDto req) {
        KnowledgeCategory cat = findByTenant(tenantId, id);
        cat.setName(req.getName());
        cat.setDescription(req.getDescription());
        cat.setParentId(req.getParentId());
        cat.setOrderIndex(req.getOrderIndex());
        return toDto(categoryRepo.save(cat));
    }

    @Transactional
    public void deleteCategory(String tenantId, String id) {
        KnowledgeCategory cat = findByTenant(tenantId, id);
        categoryRepo.delete(cat);
    }

    private KnowledgeCategory findByTenant(String tenantId, String id) {
        return categoryRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Kategorie nicht gefunden: " + id));
    }

    private CategoryDto toDto(KnowledgeCategory c) {
        return CategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .parentId(c.getParentId())
                .orderIndex(c.getOrderIndex())
                .build();
    }
}
