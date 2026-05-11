package com.talend.framework.metadata_framework.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The lineage graph for a single Talend job run, ready to be pushed to TDC.
 * Datasets and edges are deduplicated so the same dataset referenced by
 * multiple pipeline stages appears only once.
 */
public record JobLineageGraph(
        String jobName,
        LocalDateTime latestEventTimestamp,
        List<Dataset> datasets,
        List<LineageEdge> edges
) {
}
