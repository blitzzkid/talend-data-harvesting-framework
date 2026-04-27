package com.talend.framework.metadata_framework.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerConfigRepository extends JpaRepository<CustomerConfig, String> {
    List<CustomerConfig> findByEnabledTrue();
}
