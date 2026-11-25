package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Datacenter;
import com.passwordmanager.backend.entity.DatacenterUser;
import com.passwordmanager.backend.entity.Folder;
import com.passwordmanager.backend.entity.PermissionLevel;
import com.passwordmanager.backend.service.DatacenterService;
import com.passwordmanager.backend.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datacenters")
public class DatacenterController {

    private final DatacenterService datacenterService;
    private final FolderService folderService;

    public DatacenterController(DatacenterService datacenterService, FolderService folderService) {
        this.datacenterService = datacenterService;
        this.folderService = folderService;
    }

    @GetMapping
    public ResponseEntity<List<Datacenter>> getDatacenters(Authentication authentication) {
        return ResponseEntity.ok(datacenterService.getDatacentersForUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Datacenter> getDatacenter(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(datacenterService.getDatacenterForUser(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<Datacenter> createDatacenter(Authentication authentication,
                                                       @RequestBody CreateDatacenterRequest request) {
        Datacenter datacenter = datacenterService.createDatacenter(
                request.getName(),
                request.getDescription(),
                authentication.getName()
        );
        return ResponseEntity.ok(datacenter);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatacenter(@PathVariable Long id, Authentication authentication) {
        datacenterService.deleteDatacenter(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getUsers(@PathVariable Long id, Authentication authentication) {
        List<DatacenterUser> users = datacenterService.getUsers(id, authentication.getName());
        List<Map<String, Object>> result = users.stream().map(this::toDatacenterUserMap).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<?> addUser(@PathVariable Long id,
                                     @RequestBody AddUserRequest request,
                                     Authentication authentication) {
        if (request.getUserId() == null || request.getPermissionLevel() == null) {
            throw new IllegalArgumentException("userId and permissionLevel are required");
        }
        PermissionLevel level = PermissionLevel.valueOf(request.getPermissionLevel().trim().toUpperCase());
        DatacenterUser du = datacenterService.addUser(id, request.getUserId(), level, authentication.getName());
        return ResponseEntity.ok(toDatacenterUserMap(du));
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<Void> removeUser(@PathVariable Long id,
                                           @PathVariable Long userId,
                                           Authentication authentication) {
        datacenterService.removeUser(id, userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/copy-permissions")
    public ResponseEntity<Map<String, String>> copyPermissions(@PathVariable Long id,
                                                               @RequestParam Long sourceId,
                                                               Authentication authentication) {
        datacenterService.copyPermissions(sourceId, id, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Permissions copied successfully"));
    }

    @GetMapping("/{id}/folders")
    public ResponseEntity<List<Folder>> getFolders(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(folderService.getFoldersForDatacenter(id, authentication.getName()));
    }

    @PostMapping("/{id}/folders")
    public ResponseEntity<Folder> createFolder(@PathVariable Long id,
                                               Authentication authentication,
                                               @RequestBody Map<String, String> body) {
        Folder folder = folderService.createFolder(id, body.get("name"), authentication.getName());
        return ResponseEntity.ok(folder);
    }

    private Map<String, Object> toDatacenterUserMap(DatacenterUser du) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", du.getId());
        map.put("permissionLevel", du.getPermissionLevel() != null ? du.getPermissionLevel().name() : null);
        if (du.getUser() != null) {
            Map<String, Object> userMap = new java.util.HashMap<>();
            userMap.put("id", du.getUser().getId());
            userMap.put("username", du.getUser().getUsername());
            userMap.put("email", du.getUser().getEmail());
            userMap.put("role", du.getUser().getRole());
            map.put("user", userMap);
        }
        return map;
    }

    public static class CreateDatacenterRequest {
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class AddUserRequest {
        private Long userId;
        private String permissionLevel;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getPermissionLevel() { return permissionLevel; }
        public void setPermissionLevel(String permissionLevel) { this.permissionLevel = permissionLevel; }
    }
}
