package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.ParsedAuditRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface HarvestService {

    /** Read one audit row by its primary key and return the parsed form. */
    ParsedAuditRecord parseRecord(Integer jobId);

    /** Read all audit rows for a Talend job, oldest first. */
    List<ParsedAuditRecord> parseRecordsForJob(String jobName);

    /** Read every audit row newer than the given timestamp, oldest first. */
    List<ParsedAuditRecord> parseRecordsSince(LocalDateTime since);

    /** Read every audit row, oldest first. */
    List<ParsedAuditRecord> parseAllRecords();

    /**
     * Build the lineage graph for a job's run from its audit rows. Treats all
     * rows for the job name as one run — multi-run support (time-window
     * clustering or a run_id column) is deferred.
     */
    JobLineageGraph buildJobLineage(String jobName);

    /** Build {@link #buildJobLineage} and push the result to TDC. */
    HarvestResult harvestJob(String jobName);
}
