package com.talend.framework.metadata_framework.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Integer> {

    List<AuditRecord> findByJobNameOrderByEventTimestampAsc(String jobName);

    List<AuditRecord> findByEventTimestampGreaterThanOrderByEventTimestampAsc(LocalDateTime since);

    List<AuditRecord> findAllByOrderByEventTimestampAsc();
}
