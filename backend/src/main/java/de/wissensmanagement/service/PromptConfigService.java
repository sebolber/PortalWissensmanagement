package de.wissensmanagement.service;

import de.wissensmanagement.entity.PromptCategory;
import de.wissensmanagement.entity.PromptConfig;
import de.wissensmanagement.enums.PromptType;
import de.wissensmanagement.repository.PromptCategoryRepository;
import de.wissensmanagement.repository.PromptConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PromptConfigService {

    private final PromptConfigRepository promptConfigRepo;
    private final PromptCategoryRepository categoryRepo;

    public PromptConfigService(PromptConfigRepository promptConfigRepo,
                               PromptCategoryRepository categoryRepo) {
        this.promptConfigRepo = promptConfigRepo;
        this.categoryRepo = categoryRepo;
    }

    public List<PromptConfig> listAll(String tenantId) {
        return promptConfigRepo.findByTenantIdOrderByNameAsc(tenantId);
    }

    public List<PromptConfig> listByType(String tenantId, PromptType type) {
        return promptConfigRepo.findByTenantIdAndPromptTypeOrderByNameAsc(tenantId, type);
    }

    public List<PromptConfig> listActive(String tenantId) {
        return promptConfigRepo.findByTenantIdAndActiveOrderBySortOrderAscNameAsc(tenantId, true);
    }

    public List<PromptConfig> listActiveByType(String tenantId, PromptType type) {
        return promptConfigRepo.findByTenantIdAndPromptTypeAndActiveOrderBySortOrderAscNameAsc(tenantId, type, true);
    }

    public PromptConfig getById(String tenantId, String id) {
        return promptConfigRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt-Konfiguration nicht gefunden"));
    }

    @Transactional
    public PromptConfig create(String tenantId, String name, String description,
                                String promptText, PromptType promptType) {
        PromptConfig config = PromptConfig.builder()
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .promptText(promptText)
                .promptType(promptType)
                .build();
        return promptConfigRepo.save(config);
    }

    @Transactional
    public PromptConfig createFull(String tenantId, String name, String description,
                                    String promptText, PromptType promptType,
                                    String categoryId, boolean active, int sortOrder) {
        PromptConfig config = PromptConfig.builder()
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .promptText(promptText)
                .promptType(promptType)
                .active(active)
                .sortOrder(sortOrder)
                .build();
        if (categoryId != null && !categoryId.isBlank()) {
            categoryRepo.findByIdAndTenantId(categoryId, tenantId)
                    .ifPresent(config::setCategory);
        }
        return promptConfigRepo.save(config);
    }

    @Transactional
    public PromptConfig update(String tenantId, String id, String name, String description,
                                String promptText, PromptType promptType) {
        PromptConfig config = getById(tenantId, id);
        config.setName(name);
        config.setDescription(description);
        config.setPromptText(promptText);
        config.setPromptType(promptType);
        return promptConfigRepo.save(config);
    }

    @Transactional
    public PromptConfig updateFull(String tenantId, String id, String name, String description,
                                    String promptText, PromptType promptType,
                                    String categoryId, boolean active, int sortOrder) {
        PromptConfig config = getById(tenantId, id);
        config.setName(name);
        config.setDescription(description);
        config.setPromptText(promptText);
        config.setPromptType(promptType);
        config.setActive(active);
        config.setSortOrder(sortOrder);
        if (categoryId != null && !categoryId.isBlank()) {
            categoryRepo.findByIdAndTenantId(categoryId, tenantId)
                    .ifPresent(config::setCategory);
        } else {
            config.setCategory(null);
        }
        return promptConfigRepo.save(config);
    }

    @Transactional
    public void delete(String tenantId, String id) {
        PromptConfig config = getById(tenantId, id);
        promptConfigRepo.delete(config);
    }
}
