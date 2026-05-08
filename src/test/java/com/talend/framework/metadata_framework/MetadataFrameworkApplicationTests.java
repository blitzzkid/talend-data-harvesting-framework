package com.talend.framework.metadata_framework;

import com.talend.framework.metadata_framework.audit.AuditRecordRepository;
import com.talend.framework.metadata_framework.tdc.TdcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration," +
        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class MetadataFrameworkApplicationTests {

    @MockitoBean
    AuditRecordRepository auditRecordRepository;

    @MockitoBean
    TdcClient tdcClient;

    @Test
    void contextLoads() {
    }

}
