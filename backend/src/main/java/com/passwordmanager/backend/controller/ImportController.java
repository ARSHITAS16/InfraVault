package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.service.ExcelImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {

    private final ExcelImportService importService;

    public ImportController(ExcelImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/preview")
    public ResponseEntity<List<Map<String, Object>>> previewImport(@RequestParam("file") MultipartFile file,
                                                                   @RequestParam(value = "folderId", required = false) Long folderId,
                                                                   Authentication authentication) {
        Long targetFolderId = folderId != null ? folderId : 1L;
        List<Map<String, Object>> preview = importService.previewImport(file, targetFolderId, authentication.getName());
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitImport(@RequestParam(value = "folderId", required = false) Long folderId,
                                                            @RequestBody Object body,
                                                            Authentication authentication) {
        Long targetFolderId = folderId;
        List<Map<String, Object>> rows = null;

        if (body instanceof List) {
            rows = (List<Map<String, Object>>) body;
        } else if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            if (targetFolderId == null && map.containsKey("folderId") && map.get("folderId") != null) {
                try {
                    targetFolderId = Long.valueOf(map.get("folderId").toString());
                } catch (Exception ignored) {}
            }
            if (map.containsKey("previewRows") && map.get("previewRows") instanceof List) {
                rows = (List<Map<String, Object>>) map.get("previewRows");
            } else if (map.containsKey("rows") && map.get("rows") instanceof List) {
                rows = (List<Map<String, Object>>) map.get("rows");
            }
        }

        if (targetFolderId == null) {
            targetFolderId = 1L;
        }

        int count = importService.commitImport(targetFolderId, rows != null ? rows : List.of(), authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Imported successfully", "count", count));
    }
}
