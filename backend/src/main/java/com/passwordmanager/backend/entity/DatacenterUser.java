
package com.passwordmanager.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
    name = "datacenter_users",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"datacenter_id", "user_id"}
        )
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DatacenterUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private Datacenter datacenter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionLevel permissionLevel;

    public DatacenterUser() {
    }

    public DatacenterUser(
            Datacenter datacenter,
            User user,
            PermissionLevel permissionLevel
    ) {
        this.datacenter = datacenter;
        this.user = user;
        this.permissionLevel = permissionLevel;
    }

    public Long getId() {
        return id;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(
            PermissionLevel permissionLevel
    ) {
        this.permissionLevel = permissionLevel;
    }
}