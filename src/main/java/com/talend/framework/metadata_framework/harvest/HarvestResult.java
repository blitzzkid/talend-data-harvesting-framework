package com.talend.framework.metadata_framework.harvest;

import java.util.List;

/**
 * Outcome of a job-level harvest: building the lineage graph and delivering it
 * to TDC as a Data Mapping Script.
 *
 * @param jobName              the ETL job this result is for
 * @param recordsProcessed     audit rows actually considered (after filters)
 * @param datasetsWritten      datasets emitted into the script
 * @param lineageEdgesWritten  lineage edges emitted into the script
 * @param output               where the script was delivered (path or sftp location); null on failure
 * @param failures             one human-readable line per failure
 * @param message              overall summary
 */
public record HarvestResult(
        String jobName,
        int recordsProcessed,
        int datasetsWritten,
        int lineageEdgesWritten,
        String output,
        List<String> failures,
        String message
) {
}
