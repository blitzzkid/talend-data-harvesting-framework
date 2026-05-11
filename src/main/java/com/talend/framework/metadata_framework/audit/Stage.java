package com.talend.framework.metadata_framework.audit;

/**
 * The pipeline stages encoded in the prefix of {@code step_name}. Order
 * follows the data flow: raw file -> bronze -> silver -> gold.
 */
public enum Stage {
    COLUMN_MAPPING_LOADED,
    PROCESSING_FILE,
    FILE_INGESTED,
    BRONZE_TO_SILVER,
    SILVER_TO_GOLD,
    UNKNOWN;

    public static Stage fromPrefix(String prefix) {
        if (prefix == null) {
            return UNKNOWN;
        }
        for (Stage s : values()) {
            if (s.name().equals(prefix.trim())) {
                return s;
            }
        }
        return UNKNOWN;
    }

    /**
     * True for stages that represent actual data movement and should produce
     * lineage edges. {@code COLUMN_MAPPING_LOADED} is a config-load event
     * (not a dataflow event) and is excluded from the lineage push.
     */
    public boolean contributesToLineage() {
        return this != COLUMN_MAPPING_LOADED && this != UNKNOWN;
    }
}
