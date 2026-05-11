package com.talend.framework.metadata_framework.model;

import com.talend.framework.metadata_framework.audit.Stage;

/**
 * One directed edge in the lineage graph — {@code sourceDatasetId -> targetDatasetId}.
 * {@code stage} and {@code component} are kept as edge attributes so TDC can
 * render which Talend pipeline step produced the edge.
 */
public record LineageEdge(
        String sourceDatasetId,
        String targetDatasetId,
        Stage stage,
        String component
) {
}
