package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.dto.TagDto;
import de.wissensmanagement.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;
    private final SecurityHelper securityHelper;

    public TagController(TagService tagService, SecurityHelper securityHelper) {
        this.tagService = tagService;
        this.securityHelper = securityHelper;
    }

    @GetMapping
    public List<TagDto> list() {
        return tagService.listTags(securityHelper.getCurrentTenantId());
    }

    @PostMapping
    public TagDto create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag-Name ist erforderlich");
        }
        return tagService.createTag(securityHelper.getCurrentTenantId(), name.trim());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tagService.deleteTag(securityHelper.getCurrentTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
