package com.passwordmanager.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "infrastructure_folders",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"name", "location_id"}
        )
    }
)
public class InfrastructureFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    public InfrastructureFolder() {
    }

    public InfrastructureFolder(
            String name,
            Location location
    ) {
        this.name = name;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}