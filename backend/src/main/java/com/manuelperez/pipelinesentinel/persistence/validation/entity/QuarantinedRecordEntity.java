package com.manuelperez.pipelinesentinel.persistence.validation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quarantined_records")
public class QuarantinedRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_run_id", nullable = false)
    private ValidationRunEntity validationRun;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_record", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> rawRecord;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuarantinedRecordEntity() {
    }

    public QuarantinedRecordEntity(
            ValidationRunEntity validationRun,
            int rowNumber,
            Map<String, String> rawRecord,
            Instant createdAt
    ) {
        this.validationRun = validationRun;
        this.rowNumber = rowNumber;
        this.rawRecord = rawRecord;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public ValidationRunEntity getValidationRun() {
        return validationRun;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public Map<String, String> getRawRecord() {
        return rawRecord;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
