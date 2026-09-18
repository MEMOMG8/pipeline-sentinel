package com.manuelperez.pipelinesentinel.persistence.validation.repository;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationIssueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValidationIssueRepository extends JpaRepository<ValidationIssueEntity, UUID> {

    Page<ValidationIssueEntity> findByValidationRun_IdOrderByRowNumberAscCreatedAtAsc(UUID validationRunId, Pageable pageable);

    List<ValidationIssueEntity> findByValidationRun_IdAndQuarantinedRecord_IdIn(UUID validationRunId, List<UUID> quarantinedRecordIds);
}
