package com.passwordmanager.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passwordmanager.backend.entity.Datacenter;
import com.passwordmanager.backend.entity.Location;
import com.passwordmanager.backend.repository.LocationRepository;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final DatacenterService datacenterService;

    public LocationService(
            LocationRepository locationRepository,
            DatacenterService datacenterService
    ) {
        this.locationRepository = locationRepository;
        this.datacenterService = datacenterService;
    }

    // =========================================================
    // GET ALL LOCATIONS
    // =========================================================

    public List<Location> getLocations(
            Long datacenterId,
            Long userId
    ) {

        ensureAccess(
                datacenterId,
                userId
        );

        return locationRepository.findByDatacenterId(
                datacenterId
        );
    }

    // =========================================================
    // GET SINGLE LOCATION
    // =========================================================

    public Location getLocation(
            Long locationId,
            Long userId
    ) {

        Location location =
                locationRepository.findById(locationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Location not found"
                                )
                        );

        ensureAccess(
                location.getDatacenter().getId(),
                userId
        );

        return location;
    }

    // =========================================================
    // CREATE LOCATION
    // =========================================================

    @Transactional
    public Location createLocation(
            Long datacenterId,
            Long userId,
            String name
    ) {

        // User must have WRITE or ADMIN permission
        ensureWriteAccess(
                datacenterId,
                userId
        );

        // Validate location name
        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Location name is required"
            );
        }

        String trimmedName = name.trim();

        // Prevent duplicate location names
        // inside the same datacenter
        if (locationRepository
                .existsByNameAndDatacenterId(
                        trimmedName,
                        datacenterId
                )) {

            throw new IllegalArgumentException(
                    "Location already exists in this datacenter"
            );
        }

        // Get parent datacenter
        Datacenter datacenter =
                datacenterService.getDatacenter(
                        datacenterId
                );

        // Create location
        Location location =
                new Location(
                        trimmedName,
                        datacenter
                );

        return locationRepository.save(
                location
        );
    }

    // =========================================================
    // DELETE LOCATION
    // =========================================================

    @Transactional
    public void deleteLocation(
            Long locationId,
            Long userId
    ) {

        Location location =
                locationRepository.findById(locationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Location not found"
                                )
                        );

        Long datacenterId =
                location.getDatacenter().getId();

        // User must have WRITE or ADMIN permission
        ensureWriteAccess(
                datacenterId,
                userId
        );

        locationRepository.delete(
                location
        );
    }

    // =========================================================
    // CHECK READ ACCESS
    // =========================================================

    private void ensureAccess(
            Long datacenterId,
            Long userId
    ) {

        if (!datacenterService.hasAccess(
                datacenterId,
                userId
        )) {

            throw new SecurityException(
                    "Permission denied for this datacenter"
            );
        }
    }

    // =========================================================
    // CHECK WRITE ACCESS
    // =========================================================

    private void ensureWriteAccess(
            Long datacenterId,
            Long userId
    ) {

        if (!datacenterService.hasWriteAccess(
                datacenterId,
                userId
        )) {

            throw new SecurityException(
                    "Write permission required"
            );
        }
    }

    // =========================================================
    // DATACENTER SERVICE ACCESS
    // Used by InfrastructureFolderService
    // =========================================================

    public DatacenterService getDatacenterService() {
        return datacenterService;
    }
}