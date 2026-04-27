package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "run_metric", schema = "audit")
@Getter
@Setter
public class RunMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "metric_value")
    private BigDecimal metricValue;

    @Column(name = "metric_text")
    private String metricText;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;
}
