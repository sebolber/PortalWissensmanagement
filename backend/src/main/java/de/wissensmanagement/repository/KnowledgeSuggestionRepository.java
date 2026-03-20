package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeSuggestionRepository extends JpaRepository<KnowledgeSuggestion, String> {

    List<KnowledgeSuggestion> findByTenantIdAndContextTypeAndContextRefIdOrderByConfidenceDesc(
            String tenantId, String contextType, String contextRefId);
}
