package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.audit.*;
import com.talend.framework.metadata_framework.config.HarvestProperties;
import com.talend.framework.metadata_framework.model.HarvestPayload;
import com.talend.framework.metadata_framework.tdc.TdcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultHarvestService implements HarvestService {

    private final JobRunRepository jobRunRepo;
    private final DatasetIoRepository datasetIoRepo;
    private final ColumnLineageRepository lineageRepo;
    private final RunMetricRepository metricRepo;
    private final CustomerConfigRepository customerConfigRepo;
    private final HarvestWatermarkRepository watermarkRepo;
    private final TdcClient tdcClient;
    private final HarvestProperties props;

    public DefaultHarvestService(JobRunRepository jobRunRepo,
                                 DatasetIoRepository datasetIoRepo,
                                 ColumnLineageRepository lineageRepo,
                                 RunMetricRepository metricRepo,
                                 CustomerConfigRepository customerConfigRepo,
                                 HarvestWatermarkRepository watermarkRepo,
                                 TdcClient tdcClient,
                                 HarvestProperties props) {
        this.jobRunRepo = jobRunRepo;
        this.datasetIoRepo = datasetIoRepo;
        this.lineageRepo = lineageRepo;
        this.metricRepo = metricRepo;
        this.customerConfigRepo = customerConfigRepo;
        this.watermarkRepo = watermarkRepo;
        this.tdcClient = tdcClient;
        this.props = props;
    }

    @Override
    @Transactional(readOnly = true)
    public HarvestPayload buildPayload(UUID runId) {
        // TODO: load job_run + dataset_io + column_lineage + run_metric, map to HarvestPayload.
        throw new UnsupportedOperationException("buildPayload not yet implemented");
    }

    @Override
    public HarvestResult harvestRun(UUID runId) {
        // TODO: buildPayload -> tdcClient.upsertDataset / upsertLineage / setCustomAttributes.
        throw new UnsupportedOperationException("harvestRun not yet implemented");
    }

    @Override
    public HarvestResult harvestCustomer(String customerId) {
        // TODO: read watermark -> findNewRuns -> harvestRun per run -> advance watermark.
        throw new UnsupportedOperationException("harvestCustomer not yet implemented");
    }
}
