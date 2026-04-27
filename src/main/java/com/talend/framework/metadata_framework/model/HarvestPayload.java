package com.talend.framework.metadata_framework.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class HarvestPayload {
    RunContext runContext;
    List<Dataset> sources;
    List<Dataset> targets;
    List<LineageEdge> lineage;
    Map<String, BigDecimal> numericMetrics;
    Map<String, String> textMetrics;
}
