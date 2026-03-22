package de.wissensmanagement.repository;

import de.wissensmanagement.entity.PromptCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptCategoryRepository extends JpaRepository<PromptCategory, String> {

    List<PromptCategory> findByTenantIdOrderBySortOrderAscNameAsc(String tenantId);

    Optional<PromptCategory> findByIdAndTenantId(String id, String tenantId);

    Optional<PromptCategory> findByTenantIdAndName(String tenantId, String name);
}
