package com.passwordmanager.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passwordmanager.backend.entity.Datacenter;
import com.passwordmanager.backend.entity.DatacenterUser;
import com.passwordmanager.backend.entity.PermissionLevel;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.DatacenterRepository;
import com.passwordmanager.backend.repository.DatacenterUserRepository;
import com.passwordmanager.backend.repository.UserRepository;

@Service
public class DatacenterService {

    private final DatacenterRepository datacenterRepository;
    private final DatacenterUserRepository datacenterUserRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DatacenterService(
            DatacenterRepository datacenterRepository,
            DatacenterUserRepository datacenterUserRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.datacenterRepository = datacenterRepository;
        this.datacenterUserRepository = datacenterUserRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<Datacenter> getDatacentersForUser(String username) {
        User user = getUser(username);

        if ("SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return datacenterRepository.findAll();
        }

        return datacenterUserRepository
                .findByUserId(user.getId())
                .stream()
                .map(DatacenterUser::getDatacenter)
                .toList();
    }

    public Datacenter getDatacenter(Long id) {
        return datacenterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datacenter not found"));
    }

    public Datacenter getDatacenterForUser(Long datacenterId, String username) {
        User user = getUser(username);

        if (!hasAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        return getDatacenter(datacenterId);
    }

    @Transactional
    public Datacenter createDatacenter(String name, String description, String username) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Datacenter name is required");
        }

        String trimmedName = name.trim();
        if (datacenterRepository.existsByName(trimmedName)) {
            throw new IllegalArgumentException("Datacenter with name '" + trimmedName + "' already exists");
        }

        User user = getUser(username);

        Datacenter datacenter = new Datacenter(trimmedName, description);
        Datacenter savedDatacenter = datacenterRepository.save(datacenter);

        // Automatically give creator ADMIN access to this datacenter
        DatacenterUser datacenterUser = new DatacenterUser(savedDatacenter, user, PermissionLevel.ADMIN);
        datacenterUserRepository.save(datacenterUser);

        auditService.log(user.getId(), user.getUsername(), "CREATE_DATACENTER", "DATACENTER", savedDatacenter.getId(), savedDatacenter.getId(), "Created datacenter: " + savedDatacenter.getName());

        return savedDatacenter;
    }

    public List<DatacenterUser> getUsers(Long datacenterId) {
        getDatacenter(datacenterId);
        return datacenterUserRepository.findByDatacenterId(datacenterId);
    }

    @Transactional
    public DatacenterUser addUser(Long datacenterId, Long userId, PermissionLevel permissionLevel) {
        Datacenter datacenter = getDatacenter(datacenterId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (permissionLevel == null) {
            throw new IllegalArgumentException("Permission level is required");
        }

        DatacenterUser existing = datacenterUserRepository
                .findByDatacenterIdAndUserId(datacenterId, userId)
                .orElse(null);

        if (existing != null) {
            existing.setPermissionLevel(permissionLevel);
            return datacenterUserRepository.save(existing);
        }

        DatacenterUser datacenterUser = new DatacenterUser(datacenter, user, permissionLevel);
        return datacenterUserRepository.save(datacenterUser);
    }

    @Transactional
    public void removeUser(Long datacenterId, Long userId) {
        DatacenterUser datacenterUser = datacenterUserRepository
                .findByDatacenterIdAndUserId(datacenterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not have access to this datacenter"));

        datacenterUserRepository.delete(datacenterUser);
    }

    public PermissionLevel getUserPermission(Long datacenterId, Long userId) {
        return datacenterUserRepository
                .findByDatacenterIdAndUserId(datacenterId, userId)
                .map(DatacenterUser::getPermissionLevel)
                .orElse(null);
    }

    public boolean hasAccess(Long datacenterId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        return datacenterUserRepository.existsByDatacenterIdAndUserId(datacenterId, userId);
    }

    public boolean hasWriteAccess(Long datacenterId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }

        PermissionLevel permission = getUserPermission(datacenterId, userId);
        return permission == PermissionLevel.WRITE || permission == PermissionLevel.ADMIN;
    }

    public boolean hasAdminAccess(Long datacenterId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }

        return getUserPermission(datacenterId, userId) == PermissionLevel.ADMIN;
    }

    @Transactional
    public void copyPermissions(Long sourceDatacenterId, Long targetDatacenterId) {
        if (sourceDatacenterId.equals(targetDatacenterId)) {
            throw new IllegalArgumentException("Source and target datacenter cannot be the same");
        }

        getDatacenter(sourceDatacenterId);
        getDatacenter(targetDatacenterId);

        List<DatacenterUser> sourceUsers = datacenterUserRepository.findByDatacenterId(sourceDatacenterId);

        for (DatacenterUser sourceUser : sourceUsers) {
            addUser(targetDatacenterId, sourceUser.getUser().getId(), sourceUser.getPermissionLevel());
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}