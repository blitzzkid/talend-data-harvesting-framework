package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to the audit table written to by the Talend ETL jobs.
 *
 * The schema columns are stored as JSON inside TEXT so the ETL job can target
 * either PostgreSQL or MSSQL without changing column definitions. Parsing is
 * therefore done in Java (via {@link AuditPayloadParser}), not in SQL.
 *
 * No schema is hard-coded in @Table so the connection's default schema applies
 * — public on PostgreSQL, dbo on MSSQL.
 */
@Entity
@Table(name = "audit_table")
@Getter
@Setter
public class AuditRecord {

    @Id
    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "file_mapping_id")
    private Integer fileMappingId;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "status")
    private String status;

    /**
     * Backticks let Hibernate quote the identifier per dialect — "timestamp"
     * on PostgreSQL, [timestamp] on MSSQL — since `timestamp` is reserved.
     */
    @Column(name = "`timestamp`")
    private LocalDateTime eventTimestamp;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "column_mapping_id")
    private Integer columnMappingId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "source_table_name")
    private String sourceTableName;

    @Column(name = "source_table_schema")
    private String sourceTableSchemaJson;

    @Column(name = "target_table_name")
    private String targetTableName;

    @Column(name = "target_table_schema")
    private String targetTableSchemaJson;
}
