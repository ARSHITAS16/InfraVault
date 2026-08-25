package com.passwordmanager.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "infrastructure_hosts",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"hostname", "folder_id"}
        )
    }
)
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hostname;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private InfrastructureFolder folder;

    public Host() {
    }

    public Host(
            String hostname,
            InfrastructureFolder folder
    ) {
        this.hostname = hostname;
        this.folder = folder;
    }

    public Long getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public InfrastructureFolder getFolder() {
        return folder;
    }

    public void setFolder(
            InfrastructureFolder folder
    ) {
        this.folder = folder;
    }
}