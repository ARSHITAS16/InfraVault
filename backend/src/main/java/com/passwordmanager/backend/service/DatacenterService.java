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
import com.passwordmanager.backend.repository.FolderRepository;
import com.passwordmanager.backend.repository.UserRepository;

@Service
public class DatacenterService {

    private final DatacenterRepository datacenterRepository;
    private final DatacenterUserRepository datacenterUserRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DatacenterService(
            DatacenterRepository datacenterRepository,
            DatacenterUserRepository datacenterUserRepository,
            FolderRepository folderRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.datacenterRepository = datacenterRepository;
        this.datacenterUserRepository = datacenterUserRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<Datacenter> getDatacentersForUser(String username) {
        User user = getUser(username);

        if (isSuperAdmin(user)) {
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
        User user = getUser(username);
        if (!isSuperAdmin(user) && !"DC_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Permission denied: Only SUPER_ADMIN or DC_ADMIN can create datacenters");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Datacenter name is required");
        }

        String trimmedName = name.trim();
        if (datacenterRepository.existsByName(trimmedName)) {
            throw new IllegalArgumentException("Datacenter with name '" + trimmedName + "' already exists");
        }

        Datacenter datacenter = new Datacenter(trimmedName, description);
        Datacenter savedDatacenter = datacenterRepository.save(datacenter);

        DatacenterUser datacenterUser = new DatacenterUser(savedDatacenter, user, PermissionLevel.ADMIN);
        datacenterUserRepository.save(datacenterUser);

        auditService.log(user.getId(), user.getUsername(), "CREATE_DATACENTER", "DATACENTER",
                savedDatacenter.getId(), savedDatacenter.getId(), "Created datacenter: " + savedDatacenter.getName());

        return savedDatacenter;
    }

    @Transactional
    public void deleteDatacenter(Long datacenterId, String username) {
        Datacenter datacenter = getDatacenter(datacenterId);
        User user = getUser(username);

        if (!hasAdminAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied: Admin access is required to delete a datacenter.");
        }

        long folderCount = folderRepository.countByDatacenterId(datacenterId);
        if (folderCount > 0) {
            throw new IllegalStateException("Datacenter '" + datacenter.getName() + "' is not empty. It contains "
                    + folderCount + " folders. Delete all folders inside before deleting the datacenter.");
        }

        List<DatacenterUser> dcUsers = datacenterUserRepository.findByDatacenterId(datacenterId);
        datacenterUserRepository.deleteAll(dcUsers);

        datacenterRepository.delete(datacenter);

        auditService.log(user.getId(), user.getUsername(), "DELETE_DATACENTER", "DATACENTER",
                datacenterId, datacenterId, "Deleted datacenter: " + datacenter.getName());
    }

    public List<DatacenterUser> getUsers(Long datacenterId, String actorUsername) {
        getDatacenter(datacenterId);
        User actor = getUser(actorUsername);
        if (!hasAdminAccess(datacenterId, actor.getId()) && !hasAccess(datacenterId, actor.getId())) {
            throw new SecurityException("Permission denied to view users for this datacenter");
        }
        return datacenterUserRepository.findByDatacenterId(datacenterId);
    }

    @Transactional
    public DatacenterUser addUser(Long datacenterId, Long userId, PermissionLevel permissionLevel, String actorUsername) {
        User actor = getUser(actorUsername);
        if (!hasAdminAccess(datacenterId, actor.getId())) {
            throw new SecurityException("Permission denied: Admin access required to manage datacenter users");
        }

        DatacenterUser saved = upsertDatacenterUser(datacenterId, userId, permissionLevel);

        auditService.log(actor.getId(), actor.getUsername(), "ASSIGN_DC_USER", "DATACENTER",
                datacenterId, datacenterId,
                "Assigned user '" + saved.getUser().getUsername() + "' with " + permissionLevel + " permission");

        return saved;
    }

    private DatacenterUser upsertDatacenterUser(Long datacenterId, Long userId, PermissionLevel permissionLevel) {
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
    public void removeUser(Long datacenterId, Long userId, String actorUsername) {
        User actor = getUser(actorUsername);
        if (!hasAdminAccess(datacenterId, actor.getId())) {
            throw new SecurityException("Permission denied: Admin access required to remove datacenter users");
        }

        DatacenterUser datacenterUser = datacenterUserRepository
                .findByDatacenterIdAndUserId(datacenterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not have access to this datacenter"));

        String targetUsername = datacenterUser.getUser().getUsername();
        datacenterUserRepository.delete(datacenterUser);

        auditService.log(actor.getId(), actor.getUsername(), "REVOKE_DC_USER", "DATACENTER",
                datacenterId, datacenterId, "Revoked datacenter access for user '" + targetUsername + "'");
    }

    public PermissionLevel getUserPermission(Long datacenterId, Long userId) {
        return datacenterUserRepository
                .findByDatacenterIdAndUserId(datacenterId, userId)
                .map(DatacenterUser::getPermissionLevel)
                .orElse(null);
    }

    public boolean hasAccess(Long datacenterId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return false;
        }
        if (isSuperAdmin(user)) {
            return true;
        }
        return datacenterUserRepository.existsByDatacenterIdAndUserId(datacenterId, userId);
    }

    public boolean hasWriteAccess(Long datacenterId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return false;
        }
        if ("VIEWER".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        if (isSuperAdmin(user)) {
            return true;
        }

        PermissionLevel permission = getUserPermission(datacenterId, userId);
        return permission == PermissionLevel.WRITE || permission == PermissionLevel.ADMIN;
    }

    public boolean hasAdminAccess(Long datacenterId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return false;
        }
        if ("VIEWER".equalsIgnoreCase(user.getRole()) || "OPERATOR".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        if (isSuperAdmin(user)) {
            return true;
        }

        return getUserPermission(datacenterId, userId) == PermissionLevel.ADMIN;
    }

    @Transactional
    public void copyPermissions(Long sourceDatacenterId, Long targetDatacenterId, String actorUsername) {
        if (sourceDatacenterId.equals(targetDatacenterId)) {
            throw new IllegalArgumentException("Source and target datacenter cannot be the same");
        }

        User actor = getUser(actorUsername);
        if (!hasAdminAccess(targetDatacenterId, actor.getId())) {
            throw new SecurityException("Permission denied: Admin access required on target datacenter");
        }
        if (!hasAccess(sourceDatacenterId, actor.getId())) {
            throw new SecurityException("Permission denied: Access required on source datacenter");
        }

        getDatacenter(sourceDatacenterId);
        getDatacenter(targetDatacenterId);

        List<DatacenterUser> sourceUsers = datacenterUserRepository.findByDatacenterId(sourceDatacenterId);

        for (DatacenterUser sourceUser : sourceUsers) {
            upsertDatacenterUser(targetDatacenterId, sourceUser.getUser().getId(), sourceUser.getPermissionLevel());
        }

        auditService.log(actor.getId(), actor.getUsername(), "COPY_PERMISSIONS", "DATACENTER",
                targetDatacenterId, targetDatacenterId,
                "Copied " + sourceUsers.size() + " user permission(s) from datacenter ID " + sourceDatacenterId);
    }

    private boolean isSuperAdmin(User user) {
        return user != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
