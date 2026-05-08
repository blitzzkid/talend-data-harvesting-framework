package com.talend.framework.metadata_framework.audit;

/**
 * Parsed form of {@code step_name} values like {@code "BRONZE_TO_SILVER - tJavaRow_4"}.
 */
public record StepInfo(Stage stage, String component) {

    private static final String SEPARATOR = " - ";

    public static StepInfo parse(String stepName) {
        if (stepName == null || stepName.isBlank()) {
            return new StepInfo(Stage.UNKNOWN, null);
        }
        String[] parts = stepName.split(SEPARATOR, 2);
        Stage stage = Stage.fromPrefix(parts[0]);
        String component = parts.length > 1 ? parts[1].trim() : null;
        return new StepInfo(stage, component);
    }
}
