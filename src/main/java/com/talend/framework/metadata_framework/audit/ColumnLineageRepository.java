package com.talend.framework.metadata_framework.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ColumnLineageRepository extends JpaRepository<ColumnLineage, Long> {
    List<ColumnLineage> findByRunId(UUID runId);
}
