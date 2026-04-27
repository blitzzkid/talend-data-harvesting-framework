package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "column_lineage", schema = "audit")
@Getter
@Setter
public class ColumnLineage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "source_schema", nullable = false)
    private String sourceSchema;

    @Column(name = "source_table", nullable = false)
    private String sourceTable;

    @Column(name = "source_column", nullable = false)
    private String sourceColumn;

    @Column(name = "target_schema", nullable = false)
    private String targetSchema;

    @Column(name = "target_table", nullable = false)
    private String targetTable;

    @Column(name = "target_column", nullable = false)
    private String targetColumn;

    @Column(name = "transformation_expr")
    private String transformationExpr;
}
