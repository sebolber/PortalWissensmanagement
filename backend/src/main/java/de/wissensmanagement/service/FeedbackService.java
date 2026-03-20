package de.wissensmanagement.service;

import de.wissensmanagement.dto.FeedbackRequest;
import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeFeedback;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import de.wissensmanagement.repository.KnowledgeFeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final KnowledgeFeedbackRepository feedbackRepo;
    private final KnowledgeArticleRepository articleRepo;

    public FeedbackService(KnowledgeFeedbackRepository feedbackRepo,
                            KnowledgeArticleRepository articleRepo) {
        this.feedbackRepo = feedbackRepo;
        this.articleRepo = articleRepo;
    }

    @Transactional
    public void submitFeedback(String tenantId, String userId, String articleId, FeedbackRequest req) {
        KnowledgeArticle article = articleRepo.findByIdAndTenantId(articleId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Artikel nicht gefunden: " + articleId));

        var existing = feedbackRepo.findByArticleIdAndUserId(articleId, userId);
        if (existing.isPresent()) {
            KnowledgeFeedback fb = existing.get();
            article.setRatingSum(article.getRatingSum() - fb.getRating() + req.getRating());
            fb.setRating(req.getRating());
            fb.setComment(req.getComment());
            feedbackRepo.save(fb);
        } else {
            KnowledgeFeedback fb = KnowledgeFeedback.builder()
                    .articleId(articleId)
                    .userId(userId)
                    .tenantId(tenantId)
                    .rating(req.getRating())
                    .comment(req.getComment())
                    .build();
            feedbackRepo.save(fb);
            article.setRatingSum(article.getRatingSum() + req.getRating());
            article.setRatingCount(article.getRatingCount() + 1);
        }
        articleRepo.save(article);
    }
}
