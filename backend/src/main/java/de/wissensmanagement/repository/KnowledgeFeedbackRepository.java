package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeFeedbackRepository extends JpaRepository<KnowledgeFeedback, String> {

    Optional<KnowledgeFeedback> findByArticleIdAndUserId(String articleId, String userId);

    List<KnowledgeFeedback> findByArticleId(String articleId);
}
