package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.dto.GroupingDto;
import de.wissensmanagement.service.GroupingService;
import de.wissensmanagement.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gruppierungen")
public class GroupingController {

    private final GroupingService groupingService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public GroupingController(GroupingService groupingService, SecurityHelper securityHelper,
                              PermissionService permissionService) {
        this.groupingService = groupingService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<GroupingDto> list() {
        permissionService.requireLesen(securityHelper.getCurrentToken());
        return groupingService.listGroupings(securityHelper.getCurrentTenantId());
    }

    @PostMapping
    public GroupingDto create(@RequestBody Map<String, String> body) {
        permissionService.requireAdmin(securityHelper.getCurrentToken());
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name ist erforderlich");
        }
        return groupingService.createGrouping(securityHelper.getCurrentTenantId(),
                name.trim(), body.get("description"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        permissionService.requireAdmin(securityHelper.getCurrentToken());
        groupingService.deleteGrouping(securityHelper.getCurrentTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
