package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Location;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import com.passwordmanager.backend.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datacenters/{datacenterId}/locations")
public class LocationController {

    private final LocationService locationService;
    private final UserRepository userRepository;

    public LocationController(LocationService locationService, UserRepository userRepository) {
        this.locationService = locationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Location>> getLocations(@PathVariable Long datacenterId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(locationService.getLocations(datacenterId, userId));
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<Location> getLocation(@PathVariable Long datacenterId,
                                                @PathVariable Long locationId,
                                                Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(locationService.getLocation(locationId, userId));
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(@PathVariable Long datacenterId,
                                                   Authentication authentication,
                                                   @RequestBody CreateLocationRequest request) {
        Long userId = resolveUserId(authentication);
        Location location = locationService.createLocation(datacenterId, userId, request.getName());
        return ResponseEntity.ok(location);
    }

    private Long resolveUserId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        return user.getId();
    }

    public static class CreateLocationRequest {
        private String name;
        public CreateLocationRequest() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
