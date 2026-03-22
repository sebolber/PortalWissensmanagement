package de.wissensmanagement.repository;

import de.wissensmanagement.entity.PromptConfig;
import de.wissensmanagement.enums.PromptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptConfigRepository extends JpaRepository<PromptConfig, String> {

    List<PromptConfig> findByTenantIdOrderByNameAsc(String tenantId);

    List<PromptConfig> findByTenantIdAndPromptTypeOrderByNameAsc(String tenantId, PromptType promptType);

    Optional<PromptConfig> findByIdAndTenantId(String id, String tenantId);

    List<PromptConfig> findByTenantIdAndActiveOrderBySortOrderAscNameAsc(String tenantId, boolean active);

    List<PromptConfig> findByTenantIdAndPromptTypeAndActiveOrderBySortOrderAscNameAsc(
            String tenantId, PromptType promptType, boolean active);
}
