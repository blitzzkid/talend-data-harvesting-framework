package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.audit.AuditPayloadParser;
import com.talend.framework.metadata_framework.audit.AuditRecord;
import com.talend.framework.metadata_framework.audit.AuditRecordRepository;
import com.talend.framework.metadata_framework.audit.StepInfo;
import com.talend.framework.metadata_framework.model.ParsedAuditRecord;
import com.talend.framework.metadata_framework.tdc.TdcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DefaultHarvestService implements HarvestService {

    private final AuditRecordRepository repo;
    private final AuditPayloadParser parser;
    private final TdcClient tdcClient;

    public DefaultHarvestService(AuditRecordRepository repo,
                                 AuditPayloadParser parser,
                                 TdcClient tdcClient) {
        this.repo = repo;
        this.parser = parser;
        this.tdcClient = tdcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public ParsedAuditRecord parseRecord(Integer jobId) {
        AuditRecord rec = repo.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("No audit record for job_id=" + jobId));
        return toParsed(rec);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParsedAuditRecord> parseRecordsForJob(String jobName) {
        return repo.findByJobNameOrderByEventTimestampAsc(jobName).stream()
                .map(this::toParsed)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParsedAuditRecord> parseRecordsSince(LocalDateTime since) {
        return repo.findByEventTimestampGreaterThanOrderByEventTimestampAsc(since).stream()
                .map(this::toParsed)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParsedAuditRecord> parseAllRecords() {
        return repo.findAllByOrderByEventTimestampAsc().stream()
                .map(this::toParsed)
                .toList();
    }

    @Override
    public HarvestResult harvestRecord(Integer jobId) {
        // Objective 2: build a TDC payload from parseRecord(jobId) and POST it.
        // Pending decision on which TDC ingestion path to use (REST API vs.
        // MIMB CSV bridge). See response.
        throw new UnsupportedOperationException(
                "TDC push pending — see Talend Data Catalog API integration design.");
    }

    private ParsedAuditRecord toParsed(AuditRecord r) {
        StepInfo step = StepInfo.parse(r.getStepName());
        return new ParsedAuditRecord(
                r.getJobId(),
                r.getJobName(),
                r.getFileMappingId(),
                r.getColumnMappingId(),
                step.stage(),
                step.component(),
                r.getStatus(),
                r.getEventTimestamp(),
                r.getErrorMessage(),
                r.getSourceTableName(),
                parser.parseSchema(r.getSourceTableSchemaJson()),
                r.getTargetTableName(),
                parser.parseSchema(r.getTargetTableSchemaJson())
        );
    }
}
