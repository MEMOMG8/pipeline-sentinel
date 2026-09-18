package com.manuelperez.pipelinesentinel.persistence.validation.repository;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ValidationRunRepository extends JpaRepository<ValidationRunEntity, UUID> {

    @EntityGraph(attributePaths = "dataSource")
    Page<ValidationRunEntity> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "dataSource")
    Optional<ValidationRunEntity> findById(UUID id);
}
