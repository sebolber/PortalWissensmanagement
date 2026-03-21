package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeGrouping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeGroupingRepository extends JpaRepository<KnowledgeGrouping, String> {

    List<KnowledgeGrouping> findByTenantIdOrderByNameAsc(String tenantId);

    Optional<KnowledgeGrouping> findByIdAndTenantId(String id, String tenantId);

    Optional<KnowledgeGrouping> findByTenantIdAndName(String tenantId, String name);
}
