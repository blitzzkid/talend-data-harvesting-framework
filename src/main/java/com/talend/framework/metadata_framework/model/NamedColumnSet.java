package com.talend.framework.metadata_framework.model;

import java.util.List;

/**
 * A list of columns optionally tagged with a label. Single-source schemas have
 * a {@code null} label; multi-source (e.g. BRONZE_TO_SILVER) carry the source
 * key from the JSON object — typically {@code "source1"}, {@code "source2"}.
 */
public record NamedColumnSet(String label, List<ColumnDef> columns) {

    public static NamedColumnSet unnamed(List<ColumnDef> columns) {
        return new NamedColumnSet(null, columns);
    }
}
