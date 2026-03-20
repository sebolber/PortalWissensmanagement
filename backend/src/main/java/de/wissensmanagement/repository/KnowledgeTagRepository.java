package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeTagRepository extends JpaRepository<KnowledgeTag, String> {

    List<KnowledgeTag> findByTenantIdOrderByNameAsc(String tenantId);

    Optional<KnowledgeTag> findByTenantIdAndName(String tenantId, String name);

    Optional<KnowledgeTag> findByIdAndTenantId(String id, String tenantId);
}
