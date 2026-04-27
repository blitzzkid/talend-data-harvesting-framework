package com.talend.framework.metadata_framework.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRunRepository extends JpaRepository<JobRun, UUID> {

    List<JobRun> findByCustomerIdOrderByStartedAtDesc(String customerId);

    @Query("""
            select r from JobRun r
            where r.customerId = :customerId
              and (:since is null or r.startedAt > :since)
            order by r.startedAt asc
            """)
    List<JobRun> findNewRuns(@Param("customerId") String customerId,
                             @Param("since") OffsetDateTime since);
}
