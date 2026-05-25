package com.talend.framework.metadata_framework.harvest;

import java.util.List;

/**
 * Outcome of a job-level harvest: refresh the harvested model, then push the
 * audit-derived lineage into the Data Mapping model.
 *
 * @param jobName            the ETL job this result is for
 * @param datasetsConsidered distinct tables/files involved in the job's lineage ("dots")
 * @param lineageEdgesPushed source→target edges sent to TDC ("connections")
 * @param modelRefreshed     whether the harvested JDBC model refresh was triggered successfully
 * @param failures           one human-readable line per failure
 * @param message            overall summary
 */
public record HarvestResult(
        String jobName,
        int datasetsConsidered,
        int lineageEdgesPushed,
        boolean modelRefreshed,
        List<String> failures,
        String message
) {
}
