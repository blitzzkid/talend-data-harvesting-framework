package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_run", schema = "audit")
@Getter
@Setter
public class JobRun {

    @Id
    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "job_version")
    private String jobVersion;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "env", nullable = false)
    private String env;

    @Column(name = "context_params", nullable = false, columnDefinition = "jsonb")
    private String contextParamsJson;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "error_message")
    private String errorMessage;
}
