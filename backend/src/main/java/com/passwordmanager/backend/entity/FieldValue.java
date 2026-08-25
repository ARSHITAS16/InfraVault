package com.passwordmanager.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "field_values",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"host_id", "field_definition_id"}
        )
    }
)
public class FieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private FieldDefinition fieldDefinition;

    public FieldValue() {
    }

    public FieldValue(
            String value,
            Host host,
            FieldDefinition fieldDefinition
    ) {
        this.value = value;
        this.host = host;
        this.fieldDefinition = fieldDefinition;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public void setFieldDefinition(
            FieldDefinition fieldDefinition
    ) {
        this.fieldDefinition = fieldDefinition;
    }
}