package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.audit.Stage;
import com.talend.framework.metadata_framework.model.ColumnDef;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.LineageEdge;
import com.talend.framework.metadata_framework.model.NamedColumnSet;
import com.talend.framework.metadata_framework.model.ParsedAuditRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link JobLineageGraph} from a job run's parsed audit rows.
 *
 * Stage handling:
 * <ul>
 *   <li>{@code COLUMN_MAPPING_LOADED} / {@code UNKNOWN} — skipped via
 *       {@link Stage#contributesToLineage()}.</li>
 *   <li>{@code PROCESSING_FILE} / {@code FILE_INGESTED} — the source is a
 *       FILE, the target is a bronze TABLE. Both stages produce identical
 *       edges and are deduplicated.</li>
 *   <li>{@code BRONZE_TO_SILVER} — multi-source: the source schema JSON is
 *       an object whose keys are the bronze table names. One edge per
 *       bronze source is emitted to the silver target.</li>
 *   <li>{@code SILVER_TO_GOLD} — single source TABLE -> single target TABLE.</li>
 * </ul>
 *
 * Connection names ({@code FileSystem}, {@code PostgreSQL}) are hardcoded
 * for the pre-sales demo. Move to {@code TdcProperties} when we onboard a
 * second connection.
 */
@Component
public class LineageBuilder {

    private static final String FILE_CONNECTION = "FileSystem";
    private static final String TABLE_CONNECTION = "PostgreSQL";

    public JobLineageGraph build(String jobName, List<ParsedAuditRecord> records) {
        Map<String, Dataset> datasets = new LinkedHashMap<>();
        List<LineageEdge> edges = new ArrayList<>();
        LocalDateTime latest = null;

        for (ParsedAuditRecord r : records) {
            if (r.stage() == null || !r.stage().contributesToLineage()) {
                continue;
            }
            if (r.eventTimestamp() != null && (latest == null || r.eventTimestamp().isAfter(latest))) {
                latest = r.eventTimestamp();
            }

            switch (r.stage()) {
                case PROCESSING_FILE, FILE_INGESTED -> addFileToTable(datasets, edges, r);
                case BRONZE_TO_SILVER -> addMultiSourceToTable(datasets, edges, r);
                case SILVER_TO_GOLD -> addTableToTable(datasets, edges, r);
                default -> { /* unreachable: filtered by contributesToLineage */ }
            }
        }

        return new JobLineageGraph(
                jobName,
                latest,
                new ArrayList<>(datasets.values()),
                dedupeEdges(edges));
    }

    private void addFileToTable(Map<String, Dataset> datasets, List<LineageEdge> edges,
                                ParsedAuditRecord r) {
        Dataset source = fileDataset(r.sourceTableName(), firstColumns(r.sourceSchemas()));
        Dataset target = tableDataset(r.targetTableName(), firstColumns(r.targetSchemas()));
        if (source == null || target == null) {
            return;
        }
        putDataset(datasets, source);
        putDataset(datasets, target);
        edges.add(new LineageEdge(source.id(), target.id(), r.stage(), r.component()));
    }

    private void addMultiSourceToTable(Map<String, Dataset> datasets, List<LineageEdge> edges,
                                       ParsedAuditRecord r) {
        Dataset target = tableDataset(r.targetTableName(), firstColumns(r.targetSchemas()));
        if (target == null || r.sourceSchemas() == null || r.sourceSchemas().isEmpty()) {
            return;
        }
        putDataset(datasets, target);

        // source_table_name encodes all bronze table names as "table1 + table2 + ...".
        // The source schema JSON uses generic keys (source1, source2) that do not match
        // the actual table names in the JDBC model — split the combined name string and
        // map positionally so TDC can stitch the lineage to the harvested tables.
        List<String> tableNames = splitSourceTableNames(r.sourceTableName());
        List<NamedColumnSet> schemas = r.sourceSchemas();
        for (int i = 0; i < schemas.size(); i++) {
            NamedColumnSet src = schemas.get(i);
            String name = (i < tableNames.size() && !tableNames.get(i).isBlank())
                    ? tableNames.get(i)
                    : r.sourceTableName();
            Dataset source = tableDataset(name, src.columns());
            if (source == null) {
                continue;
            }
            putDataset(datasets, source);
            edges.add(new LineageEdge(source.id(), target.id(), r.stage(), r.component()));
        }
    }

    private List<String> splitSourceTableNames(String combined) {
        if (combined == null || combined.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(combined.split("\\+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private void addTableToTable(Map<String, Dataset> datasets, List<LineageEdge> edges,
                                 ParsedAuditRecord r) {
        Dataset source = tableDataset(r.sourceTableName(), firstColumns(r.sourceSchemas()));
        Dataset target = tableDataset(r.targetTableName(), firstColumns(r.targetSchemas()));
        if (source == null || target == null) {
            return;
        }
        putDataset(datasets, source);
        putDataset(datasets, target);
        edges.add(new LineageEdge(source.id(), target.id(), r.stage(), r.component()));
    }

    private Dataset fileDataset(String name, List<ColumnDef> columns) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return new Dataset(
                "file:" + sanitize(name),
                Dataset.Kind.FILE,
                FILE_CONNECTION,
                null,
                name,
                columns == null ? List.of() : columns);
    }

    private Dataset tableDataset(String name, List<ColumnDef> columns) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String schema = inferSchema(name);
        String table = stripSchema(name);
        return new Dataset(
                "table:" + sanitize(schema) + "." + sanitize(table),
                Dataset.Kind.TABLE,
                TABLE_CONNECTION,
                schema,
                table,
                columns == null ? List.of() : columns);
    }

    private void putDataset(Map<String, Dataset> datasets, Dataset d) {
        // Prefer richer column lists if an earlier entry was empty.
        Dataset existing = datasets.get(d.id());
        if (existing == null || (existing.columns().isEmpty() && !d.columns().isEmpty())) {
            datasets.put(d.id(), d);
        }
    }

    private List<LineageEdge> dedupeEdges(List<LineageEdge> edges) {
        Map<String, LineageEdge> byKey = new LinkedHashMap<>();
        for (LineageEdge e : edges) {
            byKey.putIfAbsent(e.sourceDatasetId() + "->" + e.targetDatasetId(), e);
        }
        return new ArrayList<>(byKey.values());
    }

    private List<ColumnDef> firstColumns(List<NamedColumnSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return List.of();
        }
        return sets.get(0).columns();
    }

    private String inferSchema(String qualifiedName) {
        int dot = qualifiedName.indexOf('.');
        return dot > 0 ? qualifiedName.substring(0, dot) : "public";
    }

    private String stripSchema(String qualifiedName) {
        int dot = qualifiedName.indexOf('.');
        return dot > 0 ? qualifiedName.substring(dot + 1) : qualifiedName;
    }

    private String sanitize(String s) {
        return s == null ? "_" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
