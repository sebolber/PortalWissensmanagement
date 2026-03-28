package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeUsage;
import de.wissensmanagement.repository.KnowledgeUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsageTrackingService {

    private final KnowledgeUsageRepository usageRepo;

    public UsageTrackingService(KnowledgeUsageRepository usageRepo) {
        this.usageRepo = usageRepo;
    }

    @Transactional
    public void trackUsage(String tenantId, String userId, String articleId, String contextType, String contextRefId) {
        KnowledgeUsage usage = KnowledgeUsage.builder()
                .tenantId(tenantId)
                .usedBy(userId)
                .articleId(articleId)
                .contextType(contextType)
                .contextReferenceId(contextRefId)
                .build();
        usageRepo.save(usage);
    }

    public List<KnowledgeUsage> getRecentUsage(String tenantId, String userId, int limit) {
        return usageRepo.findRecentByTenantAndUser(tenantId, userId, limit);
    }
}
