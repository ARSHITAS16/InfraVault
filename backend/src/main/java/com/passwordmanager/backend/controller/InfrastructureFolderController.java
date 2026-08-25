package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.InfrastructureFolder;
import com.passwordmanager.backend.service.InfrastructureFolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations/{locationId}/folders")
@CrossOrigin(origins = "http://localhost:5173")
public class InfrastructureFolderController {

    private final InfrastructureFolderService folderService;

    public InfrastructureFolderController(
            InfrastructureFolderService folderService
    ) {
        this.folderService = folderService;
    }

    @GetMapping
    public ResponseEntity<List<InfrastructureFolder>> getFolders(
            @PathVariable Long locationId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                folderService.getFolders(
                        locationId,
                        userId
                )
        );
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<InfrastructureFolder> getFolder(
            @PathVariable Long locationId,
            @PathVariable Long folderId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                folderService.getFolder(
                        folderId,
                        userId
                )
        );
    }

    @PostMapping
    public ResponseEntity<InfrastructureFolder> createFolder(
            @PathVariable Long locationId,
            @RequestParam Long userId,
            @RequestBody CreateFolderRequest request
    ) {
        return ResponseEntity.ok(
                folderService.createFolder(
                        locationId,
                        userId,
                        request.getName()
                )
        );
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long locationId,
            @PathVariable Long folderId,
            @RequestParam Long userId
    ) {
        folderService.deleteFolder(
                folderId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    public static class CreateFolderRequest {

        private String name;

        public CreateFolderRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}