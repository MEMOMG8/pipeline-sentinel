package com.manuelperez.pipelinesentinel.persistence.validation.entity;

import com.manuelperez.pipelinesentinel.domain.validation.IssueCategory;
import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "validation_issues")
public class ValidationIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_run_id", nullable = false)
    private ValidationRunEntity validationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantined_record_id")
    private QuarantinedRecordEntity quarantinedRecord;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueCategory category;

    @Column(nullable = false)
    private String code;

    @Column(name = "field_name")
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueSeverity severity;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ValidationIssueEntity() {
    }

    public ValidationIssueEntity(
            ValidationRunEntity validationRun,
            QuarantinedRecordEntity quarantinedRecord,
            Integer rowNumber,
            IssueCategory category,
            String code,
            String fieldName,
            IssueSeverity severity,
            String message,
            Instant createdAt
    ) {
        this.validationRun = validationRun;
        this.quarantinedRecord = quarantinedRecord;
        this.rowNumber = rowNumber;
        this.category = category;
        this.code = code;
        this.fieldName = fieldName;
        this.severity = severity;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public ValidationRunEntity getValidationRun() {
        return validationRun;
    }

    public QuarantinedRecordEntity getQuarantinedRecord() {
        return quarantinedRecord;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public IssueCategory getCategory() {
        return category;
    }

    public String getCode() {
        return code;
    }

    public String getFieldName() {
        return fieldName;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
