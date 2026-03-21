package de.wissensmanagement.service;

import de.wissensmanagement.dto.GroupingDto;
import de.wissensmanagement.entity.KnowledgeGrouping;
import de.wissensmanagement.repository.KnowledgeGroupingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GroupingService {

    private final KnowledgeGroupingRepository repo;

    public GroupingService(KnowledgeGroupingRepository repo) {
        this.repo = repo;
    }

    public List<GroupingDto> listGroupings(String tenantId) {
        return repo.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(g -> GroupingDto.builder().id(g.getId()).name(g.getName()).description(g.getDescription()).build())
                .toList();
    }

    @Transactional
    public GroupingDto createGrouping(String tenantId, String name, String description) {
        if (repo.findByTenantIdAndName(tenantId, name).isPresent()) {
            throw new IllegalArgumentException("Gruppierung existiert bereits: " + name);
        }
        KnowledgeGrouping g = KnowledgeGrouping.builder()
                .tenantId(tenantId).name(name).description(description).build();
        g = repo.save(g);
        return GroupingDto.builder().id(g.getId()).name(g.getName()).description(g.getDescription()).build();
    }

    @Transactional
    public void deleteGrouping(String tenantId, String id) {
        KnowledgeGrouping g = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Gruppierung nicht gefunden: " + id));
        repo.delete(g);
    }
}
