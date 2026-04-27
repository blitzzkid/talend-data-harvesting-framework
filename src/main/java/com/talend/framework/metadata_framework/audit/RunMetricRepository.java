package com.talend.framework.metadata_framework.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RunMetricRepository extends JpaRepository<RunMetric, Long> {
    List<RunMetric> findByRunId(UUID runId);
}
