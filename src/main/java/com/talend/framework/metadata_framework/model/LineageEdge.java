package com.talend.framework.metadata_framework.model;

/**
 * Future TDC payload carrier. Not used for objective 1 — kept as scaffolding
 * for the TDC push step (objective 2).
 */
public record LineageEdge(
        String sourceSchema,
        String sourceTable,
        String sourceColumn,
        String targetSchema,
        String targetTable,
        String targetColumn,
        String transformationExpr
) {
}
