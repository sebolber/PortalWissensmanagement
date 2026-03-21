package de.wissensmanagement.service;

import de.wissensmanagement.entity.PromptConfig;
import de.wissensmanagement.enums.PromptType;
import de.wissensmanagement.repository.PromptConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PromptConfigService {

    private final PromptConfigRepository promptConfigRepo;

    public PromptConfigService(PromptConfigRepository promptConfigRepo) {
        this.promptConfigRepo = promptConfigRepo;
    }

    public List<PromptConfig> listAll(String tenantId) {
        return promptConfigRepo.findByTenantIdOrderByNameAsc(tenantId);
    }

    public List<PromptConfig> listByType(String tenantId, PromptType type) {
        return promptConfigRepo.findByTenantIdAndPromptTypeOrderByNameAsc(tenantId, type);
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
    public void delete(String tenantId, String id) {
        PromptConfig config = getById(tenantId, id);
        promptConfigRepo.delete(config);
    }
}
