package com.talend.framework.metadata_framework.model;

import java.util.List;

/**
 * Future TDC payload carrier. Not used for objective 1 — kept as scaffolding
 * for the TDC push step (objective 2).
 */
public record Dataset(
        String connectionName,
        String schemaName,
        String tableName,
        Long rowCount,
        List<ColumnDef> columns
) {
}
