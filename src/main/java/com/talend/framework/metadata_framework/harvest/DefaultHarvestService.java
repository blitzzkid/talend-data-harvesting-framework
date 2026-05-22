package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.audit.AuditPayloadParser;
import com.talend.framework.metadata_framework.audit.AuditRecord;
import com.talend.framework.metadata_framework.audit.StepInfo;
import com.talend.framework.metadata_framework.audit.AuditRecordRepository;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.ParsedAuditRecord;
import com.talend.framework.metadata_framework.tdc.DataMappingScriptWriter;
import com.talend.framework.metadata_framework.tdc.PublishResult;
import com.talend.framework.metadata_framework.tdc.ScriptPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DefaultHarvestService implements HarvestService {

    private static final Logger log = LoggerFactory.getLogger(DefaultHarvestService.class);

    private final AuditRecordRepository repo;
    private final AuditPayloadParser parser;
    private final LineageBuilder lineageBuilder;
    private final DataMappingScriptWriter scriptWriter;
    private final ScriptPublisher scriptPublisher;

    public DefaultHarvestService(AuditRecordRepository repo,
                                 AuditPayloadParser parser,
                                 LineageBuilder lineageBuilder,
                                 DataMappingScriptWriter scriptWriter,
                                 ScriptPublisher scriptPublisher) {
        this.repo = repo;
        this.parser = parser;
        this.lineageBuilder = lineageBuilder;
        this.scriptWriter = scriptWriter;
        this.scriptPublisher = scriptPublisher;
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
    @Transactional(readOnly = true)
    public JobLineageGraph buildJobLineage(String jobName) {
        List<ParsedAuditRecord> records = parseRecordsForJob(jobName);
        return lineageBuilder.build(jobName, records);
    }

    @Override
    public HarvestResult harvestJob(String jobName) {
        JobLineageGraph graph = buildJobLineage(jobName);
        int datasets = graph.datasets().size();
        int edges = graph.edges().size();

        if (datasets == 0 && edges == 0) {
            return new HarvestResult(jobName, 0, 0, 0, null, List.of(),
                    "No lineage-bearing audit rows for job " + jobName);
        }

        try {
            String script = scriptWriter.write(graph);
            PublishResult delivered = scriptPublisher.publish(jobName, script);
            return new HarvestResult(jobName, datasets, datasets, edges,
                    delivered.destination(), List.of(),
                    "OK — delivered via " + delivered.mechanism()
                            + "; import the model in TDC to publish lineage");
        } catch (Exception ex) {
            log.warn("Data Mapping Script delivery failed for job {} — {}", jobName, ex.getMessage());
            return new HarvestResult(jobName, datasets, 0, 0, null,
                    List.of(ex.getMessage()), "Delivery failed: " + ex.getMessage());
        }
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
