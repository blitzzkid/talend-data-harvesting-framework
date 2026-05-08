package com.talend.framework.metadata_framework.harvest;

import java.util.List;

public record HarvestResult(
        int recordsProcessed,
        int datasetsUpserted,
        int lineageEdgesUpserted,
        List<Integer> failedJobIds,
        String message
) {
}
