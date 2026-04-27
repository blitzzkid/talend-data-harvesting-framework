package com.talend.framework.metadata_framework.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineageEdge {
    String sourceSchema;
    String sourceTable;
    String sourceColumn;
    String targetSchema;
    String targetTable;
    String targetColumn;
    String transformationExpr;
}
