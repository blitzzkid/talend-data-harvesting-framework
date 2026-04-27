package com.talend.framework.metadata_framework.harvest;

import com.talend.framework.metadata_framework.model.HarvestPayload;

import java.util.UUID;

public interface HarvestService {

    /** Build the internal model for a single job run from audit tables. */
    HarvestPayload buildPayload(UUID runId);

    /** Push a single job run end-to-end into TDC. */
    HarvestResult harvestRun(UUID runId);

    /** Push every new run for a customer since the last watermark. */
    HarvestResult harvestCustomer(String customerId);
}
