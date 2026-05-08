package com.talend.framework.metadata_framework.model;

import com.talend.framework.metadata_framework.audit.Stage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit-table row in normalized form: step_name split into {@link Stage} +
 * component, and the source/target schema JSON parsed into structured columns.
 */
public record ParsedAuditRecord(
        Integer jobId,
        String jobName,
        Integer fileMappingId,
        Integer columnMappingId,
        Stage stage,
        String component,
        String status,
        LocalDateTime eventTimestamp,
        String errorMessage,
        String sourceTableName,
        List<NamedColumnSet> sourceSchemas,
        String targetTableName,
        List<NamedColumnSet> targetSchemas
) {
}
