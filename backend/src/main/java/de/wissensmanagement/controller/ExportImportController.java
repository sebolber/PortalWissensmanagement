package de.wissensmanagement.controller;

import de.wissensmanagement.config.SecurityHelper;
import de.wissensmanagement.service.ExportImportService;
import de.wissensmanagement.service.ExportImportService.ExportData;
import de.wissensmanagement.service.ExportImportService.ImportResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/konfiguration")
public class ExportImportController {

    private final ExportImportService exportImportService;
    private final SecurityHelper securityHelper;

    public ExportImportController(ExportImportService exportImportService, SecurityHelper securityHelper) {
        this.exportImportService = exportImportService;
        this.securityHelper = securityHelper;
    }

    @GetMapping("/export")
    public ResponseEntity<ExportData> exportAll() {
        String tenantId = securityHelper.getCurrentTenantId();
        ExportData data = exportImportService.exportAll(tenantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"wissensmanagement-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResult> importAll(@RequestBody ExportData data) {
        String tenantId = securityHelper.getCurrentTenantId();
        ImportResult result = exportImportService.importAll(tenantId, data);
        return ResponseEntity.ok(result);
    }
}
