package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeUsageRepository extends JpaRepository<KnowledgeUsage, String> {

    @Query(value = "SELECT u.* FROM wm_usage u WHERE u.tenant_id = :tenantId AND u.used_by = :userId " +
           "ORDER BY u.used_at DESC LIMIT :limit", nativeQuery = true)
    List<KnowledgeUsage> findRecentByTenantAndUser(String tenantId, String userId, int limit);
}
