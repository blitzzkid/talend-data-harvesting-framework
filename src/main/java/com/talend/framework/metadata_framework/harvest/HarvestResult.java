package com.talend.framework.metadata_framework.harvest;

import java.util.List;

/**
 * Outcome of a job-level TDC push.
 *
 * @param jobName              the Talend job this result is for
 * @param recordsProcessed     audit rows actually considered (after filters)
 * @param datasetsUpserted     dataset upsert calls that returned without error
 * @param lineageEdgesUpserted edges sent in the lineage upsert call(s)
 * @param failures             one human-readable line per failed upsert
 * @param message              overall summary
 */
public record HarvestResult(
        String jobName,
        int recordsProcessed,
        int datasetsUpserted,
        int lineageEdgesUpserted,
        List<String> failures,
        String message
) {
}
