package com.manuelperez.pipelinesentinel.persistence.validation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "data_sources")
public class DataSourceEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "schema_version", nullable = false)
    private String schemaVersion;

    @Column(nullable = false)
    private boolean active;

    protected DataSourceEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public boolean isActive() {
        return active;
    }
}
