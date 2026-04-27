package com.talend.framework.metadata_framework.scheduler;

import com.talend.framework.metadata_framework.audit.CustomerConfig;
import com.talend.framework.metadata_framework.audit.CustomerConfigRepository;
import com.talend.framework.metadata_framework.harvest.HarvestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "harvest.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HarvestScheduler {

    private static final Logger log = LoggerFactory.getLogger(HarvestScheduler.class);

    private final CustomerConfigRepository customerConfigRepo;
    private final HarvestService harvestService;

    public HarvestScheduler(CustomerConfigRepository customerConfigRepo, HarvestService harvestService) {
        this.customerConfigRepo = customerConfigRepo;
        this.harvestService = harvestService;
    }

    @Scheduled(fixedDelayString = "${harvest.scheduler.fixed-delay-ms:60000}")
    public void pollAllCustomers() {
        for (CustomerConfig cfg : customerConfigRepo.findByEnabledTrue()) {
            try {
                harvestService.harvestCustomer(cfg.getCustomerId());
            } catch (RuntimeException ex) {
                log.warn("Harvest failed for customer {}: {}", cfg.getCustomerId(), ex.getMessage());
            }
        }
    }
}
