package com.talend.framework.metadata_framework.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Dataset {
    String connectionName;
    String schemaName;
    String tableName;
    Long rowCount;
    List<Column> columns;
}
