package com.manuelperez.pipelinesentinel.persistence.validation.repository;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.QuarantinedRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuarantinedRecordRepository extends JpaRepository<QuarantinedRecordEntity, UUID> {

    Page<QuarantinedRecordEntity> findByValidationRun_IdOrderByRowNumberAsc(UUID validationRunId, Pageable pageable);

    List<QuarantinedRecordEntity> findByValidationRun_Id(UUID validationRunId);
}
