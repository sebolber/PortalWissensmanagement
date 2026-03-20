package de.wissensmanagement.service;

import de.wissensmanagement.dto.TagDto;
import de.wissensmanagement.entity.KnowledgeTag;
import de.wissensmanagement.repository.KnowledgeTagRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final KnowledgeTagRepository tagRepo;

    public TagService(KnowledgeTagRepository tagRepo) {
        this.tagRepo = tagRepo;
    }

    public List<TagDto> listTags(String tenantId) {
        return tagRepo.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(t -> TagDto.builder().id(t.getId()).name(t.getName()).build())
                .toList();
    }

    @Transactional
    public TagDto createTag(String tenantId, String name) {
        if (tagRepo.findByTenantIdAndName(tenantId, name).isPresent()) {
            throw new IllegalArgumentException("Tag existiert bereits: " + name);
        }
        KnowledgeTag tag = KnowledgeTag.builder().tenantId(tenantId).name(name).build();
        tag = tagRepo.save(tag);
        return TagDto.builder().id(tag.getId()).name(tag.getName()).build();
    }

    @Transactional
    public void deleteTag(String tenantId, String id) {
        KnowledgeTag tag = tagRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tag nicht gefunden: " + id));
        tagRepo.delete(tag);
    }
}
