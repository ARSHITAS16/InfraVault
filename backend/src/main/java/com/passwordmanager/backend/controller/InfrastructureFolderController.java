package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.InfrastructureFolder;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import com.passwordmanager.backend.service.InfrastructureFolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations/{locationId}/folders")
public class InfrastructureFolderController {

    private final InfrastructureFolderService folderService;
    private final UserRepository userRepository;

    public InfrastructureFolderController(InfrastructureFolderService folderService, UserRepository userRepository) {
        this.folderService = folderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<InfrastructureFolder>> getFolders(@PathVariable Long locationId,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(folderService.getFolders(locationId, resolveUserId(authentication)));
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<InfrastructureFolder> getFolder(@PathVariable Long locationId,
                                                          @PathVariable Long folderId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(folderService.getFolder(folderId, resolveUserId(authentication)));
    }

    @PostMapping
    public ResponseEntity<InfrastructureFolder> createFolder(@PathVariable Long locationId,
                                                             Authentication authentication,
                                                             @RequestBody CreateFolderRequest request) {
        return ResponseEntity.ok(folderService.createFolder(locationId, resolveUserId(authentication), request.getName()));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long locationId,
                                             @PathVariable Long folderId,
                                             Authentication authentication) {
        folderService.deleteFolder(folderId, resolveUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        return user.getId();
    }

    public static class CreateFolderRequest {
        private String name;
        public CreateFolderRequest() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
