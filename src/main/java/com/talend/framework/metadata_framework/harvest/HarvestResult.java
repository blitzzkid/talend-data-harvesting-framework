package com.talend.framework.metadata_framework.harvest;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class HarvestResult {
    int runsProcessed;
    int datasetsUpserted;
    int lineageEdgesUpserted;
    List<UUID> failedRunIds;
    String message;
}
