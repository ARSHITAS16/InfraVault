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
                                                                   @RequestParam("folderId") Long folderId,
                                                                   Authentication authentication) {
        List<Map<String, Object>> preview = importService.previewImport(file, folderId, authentication.getName());
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitImport(@RequestParam("folderId") Long folderId,
                                                            @RequestBody List<Map<String, Object>> rows,
                                                            Authentication authentication) {
        int count = importService.commitImport(folderId, rows, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Imported successfully", "count", count));
    }
}
