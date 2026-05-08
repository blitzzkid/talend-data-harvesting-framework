package com.talend.framework.metadata_framework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One column entry as it appears inside the audit table's schema JSON.
 * {@code type} is intentionally a free-form String because Talend writes both
 * Java types ({@code "BigDecimal"}) and SQL types ({@code "character varying"})
 * depending on the step.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ColumnDef(String name, String type, Boolean nullable, Integer position) {
}
