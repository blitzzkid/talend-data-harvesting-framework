package com.talend.framework.metadata_framework.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Column {
    String name;
    String dataType;
    boolean nullable;
}
