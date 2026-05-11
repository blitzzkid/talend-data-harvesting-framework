package com.talend.framework.metadata_framework.model;

import java.util.List;

/**
 * A node in the lineage graph — either a source/sink file or a database table.
 * The {@code id} is deterministic so repeated harvest pushes are idempotent.
 */
public record Dataset(
        String id,
        Kind kind,
        String connectionName,
        String schemaName,
        String name,
        List<ColumnDef> columns
) {
    public enum Kind { FILE, TABLE }
}
