package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Host;
import com.passwordmanager.backend.service.HostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure-folders/{folderId}/hosts")
@CrossOrigin(origins = "http://localhost:5173")
public class HostController {

    private final HostService hostService;

    public HostController(
            HostService hostService
    ) {
        this.hostService = hostService;
    }

    // =========================================================
    // GET ALL HOSTS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<Host>> getHosts(
            @PathVariable Long folderId,
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                hostService.getHosts(
                        folderId,
                        userId
                )
        );
    }

    // =========================================================
    // GET SINGLE HOST
    // =========================================================

    @GetMapping("/{hostId}")
    public ResponseEntity<Host> getHost(
            @PathVariable Long folderId,
            @PathVariable Long hostId,
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                hostService.getHost(
                        hostId,
                        userId
                )
        );
    }

    // =========================================================
    // CREATE HOST
    // =========================================================

    @PostMapping
    public ResponseEntity<Host> createHost(
            @PathVariable Long folderId,
            @RequestParam Long userId,
            @RequestBody CreateHostRequest request
    ) {

        Host host =
                hostService.createHost(
                        folderId,
                        userId,
                        request.getHostname()
                );

        return ResponseEntity.ok(host);
    }

    // =========================================================
    // DELETE HOST
    // =========================================================

    @DeleteMapping("/{hostId}")
    public ResponseEntity<Void> deleteHost(
            @PathVariable Long folderId,
            @PathVariable Long hostId,
            @RequestParam Long userId
    ) {

        hostService.deleteHost(
                hostId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // REQUEST DTO
    // =========================================================

    public static class CreateHostRequest {

        private String hostname;

        public CreateHostRequest() {
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }
}