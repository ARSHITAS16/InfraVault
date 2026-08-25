package com.passwordmanager.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "devices")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hostname;

    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    private String capacity;

    @Column(name = "service_tag")
    private String serviceTag;

    @Column(name = "support_end_date")
    private LocalDate supportEndDate;

    @Column(name = "console_port")
    private String consolePort;

    @Column(name = "idrac_configured")
    private Boolean idracConfigured = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    public Device() {
    }

    public Device(String hostname, Folder folder) {
        this.hostname = hostname;
        this.folder = folder;
    }

    public Device(String hostname, String model, String serialNumber, String capacity, String serviceTag, LocalDate supportEndDate, Folder folder) {
        this.hostname = hostname;
        this.model = model;
        this.serialNumber = serialNumber;
        this.capacity = capacity;
        this.serviceTag = serviceTag;
        this.supportEndDate = supportEndDate;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getServiceTag() {
        return serviceTag;
    }

    public void setServiceTag(String serviceTag) {
        this.serviceTag = serviceTag;
    }

    public LocalDate getSupportEndDate() {
        return supportEndDate;
    }

    public void setSupportEndDate(LocalDate supportEndDate) {
        this.supportEndDate = supportEndDate;
    }

    public String getConsolePort() {
        return consolePort;
    }

    public void setConsolePort(String consolePort) {
        this.consolePort = consolePort;
    }

    public Boolean getIdracConfigured() {
        return idracConfigured;
    }

    public void setIdracConfigured(Boolean idracConfigured) {
        this.idracConfigured = idracConfigured;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}