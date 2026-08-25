package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Location;
import com.passwordmanager.backend.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datacenters/{datacenterId}/locations")
@CrossOrigin(origins = "http://localhost:5173")
public class LocationController {

    private final LocationService locationService;

    public LocationController(
            LocationService locationService
    ) {
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<List<Location>> getLocations(
            @PathVariable Long datacenterId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                locationService.getLocations(
                        datacenterId,
                        userId
                )
        );
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<Location> getLocation(
            @PathVariable Long datacenterId,
            @PathVariable Long locationId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                locationService.getLocation(
                        locationId,
                        userId
                )
        );
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(
            @PathVariable Long datacenterId,
            @RequestParam Long userId,
            @RequestBody CreateLocationRequest request
    ) {
        Location location =
                locationService.createLocation(
                        datacenterId,
                        userId,
                        request.getName()
                );

        return ResponseEntity.ok(location);
    }

    public static class CreateLocationRequest {

        private String name;

        public CreateLocationRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}