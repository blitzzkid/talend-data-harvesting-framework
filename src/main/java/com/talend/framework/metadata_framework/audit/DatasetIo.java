package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_io", schema = "audit")
@Getter
@Setter
public class DatasetIo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "role", nullable = false)
    private String role; // 'source' | 'target'

    @Column(name = "connection_name", nullable = false)
    private String connectionName;

    @Column(name = "schema_name", nullable = false)
    private String schemaName;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;
}
