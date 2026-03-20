package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeUsageRepository extends JpaRepository<KnowledgeUsage, String> {
}
