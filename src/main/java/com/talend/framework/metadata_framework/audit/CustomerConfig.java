package com.talend.framework.metadata_framework.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_config", schema = "audit")
@Getter
@Setter
public class CustomerConfig {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "tdc_folder_path", nullable = false)
    private String tdcFolderPath;

    @Column(name = "tdc_model_id")
    private String tdcModelId;

    @Column(name = "custom_attributes", nullable = false, columnDefinition = "jsonb")
    private String customAttributesJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
