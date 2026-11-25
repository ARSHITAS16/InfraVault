package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Host;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import com.passwordmanager.backend.service.HostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure-folders/{folderId}/hosts")
public class HostController {

    private final HostService hostService;
    private final UserRepository userRepository;

    public HostController(HostService hostService, UserRepository userRepository) {
        this.hostService = hostService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Host>> getHosts(@PathVariable Long folderId, Authentication authentication) {
        return ResponseEntity.ok(hostService.getHosts(folderId, resolveUserId(authentication)));
    }

    @GetMapping("/{hostId}")
    public ResponseEntity<Host> getHost(@PathVariable Long folderId,
                                        @PathVariable Long hostId,
                                        Authentication authentication) {
        return ResponseEntity.ok(hostService.getHost(hostId, resolveUserId(authentication)));
    }

    @PostMapping
    public ResponseEntity<Host> createHost(@PathVariable Long folderId,
                                           Authentication authentication,
                                           @RequestBody CreateHostRequest request) {
        Host host = hostService.createHost(folderId, resolveUserId(authentication), request.getHostname());
        return ResponseEntity.ok(host);
    }

    @DeleteMapping("/{hostId}")
    public ResponseEntity<Void> deleteHost(@PathVariable Long folderId,
                                           @PathVariable Long hostId,
                                           Authentication authentication) {
        hostService.deleteHost(hostId, resolveUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        return user.getId();
    }

    public static class CreateHostRequest {
        private String hostname;
        public CreateHostRequest() {}
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
    }
}
