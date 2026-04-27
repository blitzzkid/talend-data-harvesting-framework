package com.talend.framework.metadata_framework.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HarvestWatermarkRepository extends JpaRepository<HarvestWatermark, String> {
}
