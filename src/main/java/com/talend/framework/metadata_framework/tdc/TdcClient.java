package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;

import java.util.List;
import java.util.Map;

public interface TdcClient {

    /** Upsert a dataset (table) under the given TDC folder, returning its TDC object id. */
    String upsertDataset(String folderPath, String modelId, Dataset dataset);

    /** Push lineage edges between datasets already known to TDC. */
    void upsertLineage(String modelId, List<LineageEdge> edges);

    /** Set custom attributes on a TDC object (used to attach runtime metrics, customer tags, etc.). */
    void setCustomAttributes(String objectId, Map<String, String> attributes);

    /** Lightweight health probe — used by /status. */
    boolean ping();
}
