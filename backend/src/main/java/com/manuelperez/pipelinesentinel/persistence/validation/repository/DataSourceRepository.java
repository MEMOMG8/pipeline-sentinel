package com.manuelperez.pipelinesentinel.persistence.validation.repository;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, UUID> {

    Optional<DataSourceEntity> findByCodeAndActiveTrue(String code);
}
