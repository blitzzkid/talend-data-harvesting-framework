package com.talend.framework.metadata_framework.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class RunContext {
    UUID runId;
    String jobName;
    String jobVersion;
    String customerId;
    String env;
    String status;
    OffsetDateTime startedAt;
    OffsetDateTime endedAt;
    Map<String, String> contextParams;
}
