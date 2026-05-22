package com.talend.framework.metadata_framework.api;

import com.talend.framework.metadata_framework.config.TdcImportProperties;
import com.talend.framework.metadata_framework.harvest.HarvestResult;
import com.talend.framework.metadata_framework.harvest.HarvestService;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.ParsedAuditRecord;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/harvest")
public class HarvestController {

    private final HarvestService service;
    private final TdcImportProperties importProperties;

    public HarvestController(HarvestService service, TdcImportProperties importProperties) {
        this.service = service;
        this.importProperties = importProperties;
    }

    @GetMapping("/record/{jobId}")
    public ParsedAuditRecord previewRecord(@PathVariable Integer jobId) {
        return service.parseRecord(jobId);
    }

    @GetMapping("/job/{jobName}")
    public List<ParsedAuditRecord> previewByJobName(@PathVariable String jobName) {
        return service.parseRecordsForJob(jobName);
    }

    @GetMapping("/job/{jobName}/lineage")
    public JobLineageGraph previewJobLineage(@PathVariable String jobName) {
        return service.buildJobLineage(jobName);
    }

    @GetMapping("/since")
    public List<ParsedAuditRecord> previewSince(
            @RequestParam("since")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return service.parseRecordsSince(since);
    }

    @GetMapping("/all")
    public List<ParsedAuditRecord> previewAll() {
        return service.parseAllRecords();
    }

    @PostMapping("/job/{jobName}")
    public HarvestResult harvestJob(@PathVariable String jobName) {
        return service.harvestJob(jobName);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        TdcImportProperties.Delivery delivery = importProperties.getDelivery();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("delivery", delivery);
        if (delivery == TdcImportProperties.Delivery.SFTP) {
            TdcImportProperties.Sftp s = importProperties.getSftp();
            status.put("target", s.getHost() + ":" + s.getPort() + s.getRemoteDir());
        } else {
            status.put("target", importProperties.getOutputDir());
        }
        return status;
    }
}
