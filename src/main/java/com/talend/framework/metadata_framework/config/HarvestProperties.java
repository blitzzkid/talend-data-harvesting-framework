package com.talend.framework.metadata_framework.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "harvest")
public class HarvestProperties {

    private Scheduler scheduler = new Scheduler();
    private Idempotency idempotency = new Idempotency();

    @Getter
    @Setter
    public static class Scheduler {
        private boolean enabled = true;
        private long fixedDelayMs = 60_000L;
        private int batchSize = 100;
    }

    @Getter
    @Setter
    public static class Idempotency {
        private String naturalKeyTemplate = "{jobName}:{runId}";
    }
}
