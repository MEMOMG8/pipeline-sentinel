package com.manuelperez.pipelinesentinel.persistence.validation.entity;

import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationOutcome;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationRunStatus;
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
@Table(name = "validation_runs")
public class ValidationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSourceEntity dataSource;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_severity")
    private IssueSeverity maxSeverity;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "issue_count", nullable = false)
    private int issueCount;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected ValidationRunEntity() {
    }

    public ValidationRunEntity(
            DataSourceEntity dataSource,
            String originalFilename,
            ValidationRunStatus status,
            ValidationOutcome outcome,
            IssueSeverity maxSeverity,
            int totalRows,
            int validRows,
            int invalidRows,
            int issueCount,
            Instant submittedAt,
            Instant completedAt
    ) {
        this.dataSource = dataSource;
        this.originalFilename = originalFilename;
        this.status = status;
        this.outcome = outcome;
        this.maxSeverity = maxSeverity;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.issueCount = issueCount;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public DataSourceEntity getDataSource() {
        return dataSource;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public ValidationRunStatus getStatus() {
        return status;
    }

    public ValidationOutcome getOutcome() {
        return outcome;
    }

    public IssueSeverity getMaxSeverity() {
        return maxSeverity;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public int getIssueCount() {
        return issueCount;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
