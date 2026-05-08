package com.talend.framework.metadata_framework.harvest;

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

    /** Push one audit row's lineage into TDC. Pending objective 2. */
    HarvestResult harvestRecord(Integer jobId);
}
