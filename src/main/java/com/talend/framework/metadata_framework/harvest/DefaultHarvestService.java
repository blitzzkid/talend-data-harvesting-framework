package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.audit.AuditPayloadParser;
import com.talend.framework.metadata_framework.audit.AuditRecord;
import com.talend.framework.metadata_framework.audit.StepInfo;
import com.talend.framework.metadata_framework.audit.AuditRecordRepository;
import com.talend.framework.metadata_framework.config.TdcProperties;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.ParsedAuditRecord;
import com.talend.framework.metadata_framework.tdc.TdcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DefaultHarvestService implements HarvestService {

    private static final Logger log = LoggerFactory.getLogger(DefaultHarvestService.class);

    private final AuditRecordRepository repo;
    private final AuditPayloadParser parser;
    private final LineageBuilder lineageBuilder;
    private final TdcClient tdcClient;
    private final TdcProperties tdcProperties;

    public DefaultHarvestService(AuditRecordRepository repo,
                                 AuditPayloadParser parser,
                                 LineageBuilder lineageBuilder,
                                 TdcClient tdcClient,
                                 TdcProperties tdcProperties) {
        this.repo = repo;
        this.parser = parser;
        this.lineageBuilder = lineageBuilder;
        this.tdcClient = tdcClient;
        this.tdcProperties = tdcProperties;
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
        int considered = graph.datasets().size();

        if (graph.datasets().isEmpty() && graph.edges().isEmpty()) {
            return new HarvestResult(jobName, 0, 0, 0, List.of(),
                    "No lineage-bearing audit rows for job " + jobName);
        }

        String modelId = tdcProperties.getDefaultModelId();
        String folderPath = "/Talend/Jobs/" + jobName;
        List<String> failures = new ArrayList<>();
        int datasetsUpserted = 0;

        for (Dataset d : graph.datasets()) {
            try {
                tdcClient.upsertDataset(folderPath, modelId, d);
                datasetsUpserted++;
            } catch (Exception ex) {
                String msg = "dataset " + d.id() + ": " + ex.getMessage();
                log.warn("TDC dataset upsert failed — {}", msg);
                failures.add(msg);
            }
        }

        int edgesUpserted = 0;
        if (!graph.edges().isEmpty()) {
            try {
                tdcClient.upsertLineage(modelId, graph.edges());
                edgesUpserted = graph.edges().size();
            } catch (Exception ex) {
                String msg = "lineage upsert: " + ex.getMessage();
                log.warn("TDC lineage upsert failed — {}", msg);
                failures.add(msg);
            }
        }

        String message = failures.isEmpty()
                ? "OK"
                : failures.size() + " upsert(s) failed; see failures";
        return new HarvestResult(jobName, considered, datasetsUpserted, edgesUpserted,
                failures, message);
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
