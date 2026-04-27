package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "harvest_watermark", schema = "audit")
@Getter
@Setter
public class HarvestWatermark {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "last_run_started")
    private OffsetDateTime lastRunStarted;

    @Column(name = "last_run_id")
    private UUID lastRunId;

    @Column(name = "last_harvested_at", nullable = false)
    private OffsetDateTime lastHarvestedAt;
}
