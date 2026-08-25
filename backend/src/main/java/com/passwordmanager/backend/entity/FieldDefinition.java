package com.passwordmanager.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "field_definitions",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"name", "folder_id"}
        )
    }
)
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Column(nullable = false)
    private boolean sensitive;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private InfrastructureFolder folder;

    public FieldDefinition() {
    }

    public FieldDefinition(
            String name,
            FieldType fieldType,
            boolean sensitive,
            InfrastructureFolder folder
    ) {
        this.name = name;
        this.fieldType = fieldType;
        this.sensitive = sensitive;
        this.folder = folder;
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

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
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