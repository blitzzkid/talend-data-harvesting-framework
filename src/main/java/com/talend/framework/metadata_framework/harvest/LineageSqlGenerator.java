package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.model.ColumnDef;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a Data Mapping Script SQL file from a {@link JobLineageGraph}.
 *
 * <p>TDC's "Data Mapping Script" bridge reads this SQL DSL — a proprietary
 * Meta Integration dialect, not standard SQL — and parses it into lineage:
 * <pre>
 *   CREATE CONNECTION "FileSystem" TYPE "File System";
 *   CREATE CONNECTION "PostgreSQL"  TYPE "Relational Database";
 *
 *   SELECT col1 AS col1, col2 AS col2
 *   INTO   schema."target_table"@"ConnectionName"
 *   FROM   "source"@"ConnectionName";
 * </pre>
 *
 * <p>Connection names ({@code FileSystem}, {@code PostgreSQL}) must match the
 * names TDC uses internally when it stitches the lineage model to the harvested
 * JDBC model. The JDBC model uses {@code PostgreSQL} as the catalog label.
 */
@Component
public class LineageSqlGenerator {

    private static final String FILE_CONNECTION  = "FileSystem";
    private static final String DB_CONNECTION    = "PostgreSQL";
    private static final String DB_SCHEMA        = "public";

    public String generate(JobLineageGraph graph) {
        Map<String, Dataset> datasetsById = graph.datasets().stream()
                .collect(Collectors.toMap(Dataset::id, d -> d));

        StringBuilder sb = new StringBuilder();
        sb.append("-- Lineage for job: ").append(graph.jobName()).append("\n");
        sb.append("-- Generated from ETL audit table. Do not edit manually.\n");
        sb.append("CREATE CONNECTION \"").append(FILE_CONNECTION).append("\"  TYPE \"File System\";\n");
        sb.append("CREATE CONNECTION \"").append(DB_CONNECTION).append("\"  TYPE \"Relational Database\";\n");
        sb.append("\n");

        int queryIndex = 0;
        for (LineageEdge edge : graph.edges()) {
            Dataset src = datasetsById.get(edge.sourceDatasetId());
            Dataset tgt = datasetsById.get(edge.targetDatasetId());
            if (src == null || tgt == null) {
                continue;
            }

            // Determine column list — use source columns when available.
            List<ColumnDef> cols = src.columns() != null && !src.columns().isEmpty()
                    ? src.columns()
                    : (tgt.columns() != null ? tgt.columns() : List.of());

            if (cols.isEmpty()) {
                // No column metadata — emit a wildcard-style comment and skip SELECT columns.
                sb.append("-- ").append(edge.stage()).append(": ")
                  .append(src.name()).append(" -> ").append(tgt.name()).append("\n");
                sb.append("SELECT *\n");
            } else {
                sb.append("-- ").append(edge.stage()).append(": ")
                  .append(src.name()).append(" -> ").append(tgt.name()).append("\n");
                sb.append("SELECT ");
                for (int i = 0; i < cols.size(); i++) {
                    String col = quoteName(cols.get(i).name());
                    sb.append(col).append(" AS ").append(col);
                    if (i < cols.size() - 1) sb.append(",\n       ");
                }
                sb.append("\n");
            }

            sb.append("INTO   ").append(targetRef(tgt)).append("\n");
            sb.append("FROM   ").append(sourceRef(src)).append(";\n");
            sb.append("\n");
            queryIndex++;
        }

        return sb.toString();
    }

    private String sourceRef(Dataset d) {
        return switch (d.kind()) {
            case FILE  -> quoteName(d.name()) + "@\"" + FILE_CONNECTION + "\"";
            case TABLE -> schemaPrefix(d) + quoteName(d.name()) + "@\"" + DB_CONNECTION + "\"";
        };
    }

    private String targetRef(Dataset d) {
        return switch (d.kind()) {
            case FILE  -> quoteName(d.name()) + "@\"" + FILE_CONNECTION + "\"";
            case TABLE -> schemaPrefix(d) + quoteName(d.name()) + "@\"" + DB_CONNECTION + "\"";
        };
    }

    private String schemaPrefix(Dataset d) {
        String schema = d.schemaName() != null ? d.schemaName() : DB_SCHEMA;
        return schema + ".";
    }

    private String quoteName(String name) {
        return "\"" + name + "\"";
    }
}
