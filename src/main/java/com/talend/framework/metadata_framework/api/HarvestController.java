package com.talend.framework.metadata_framework.api;

import com.talend.framework.metadata_framework.harvest.HarvestResult;
import com.talend.framework.metadata_framework.harvest.HarvestService;
import com.talend.framework.metadata_framework.model.HarvestPayload;
import com.talend.framework.metadata_framework.tdc.TdcClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/harvest")
public class HarvestController {

    private final HarvestService harvestService;
    private final TdcClient tdcClient;

    public HarvestController(HarvestService harvestService, TdcClient tdcClient) {
        this.harvestService = harvestService;
        this.tdcClient = tdcClient;
    }

    @PostMapping("/run/{runId}")
    public HarvestResult harvestRun(@PathVariable UUID runId) {
        return harvestService.harvestRun(runId);
    }

    @GetMapping("/run/{runId}/payload")
    public HarvestPayload previewPayload(@PathVariable UUID runId) {
        return harvestService.buildPayload(runId);
    }

    @PostMapping("/customer/{customerId}")
    public HarvestResult harvestCustomer(@PathVariable String customerId) {
        return harvestService.harvestCustomer(customerId);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("tdcReachable", tdcClient.ping());
    }
}
