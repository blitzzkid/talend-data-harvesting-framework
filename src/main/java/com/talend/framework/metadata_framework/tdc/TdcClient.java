package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;

import java.util.List;
import java.util.Map;

/**
 * Stub for objective 2. Implementation is pending a decision on which TDC
 * ingestion mechanism we use (REST API vs. MIMB custom CSV bridge).
 */
public interface TdcClient {

    String upsertDataset(String folderPath, String modelId, Dataset dataset);

    void upsertLineage(String modelId, List<LineageEdge> edges);

    void setCustomAttributes(String objectId, Map<String, String> attributes);

    boolean ping();
}
