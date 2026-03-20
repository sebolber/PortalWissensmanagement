package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, String> {

    List<KnowledgeCategory> findByTenantIdOrderByOrderIndexAsc(String tenantId);

    Optional<KnowledgeCategory> findByIdAndTenantId(String id, String tenantId);

    Optional<KnowledgeCategory> findByTenantIdAndName(String tenantId, String name);

    List<KnowledgeCategory> findByTenantIdAndParentIdIsNullOrderByOrderIndexAsc(String tenantId);

    List<KnowledgeCategory> findByTenantIdAndParentIdOrderByOrderIndexAsc(String tenantId, String parentId);

    long countByTenantId(String tenantId);
}
