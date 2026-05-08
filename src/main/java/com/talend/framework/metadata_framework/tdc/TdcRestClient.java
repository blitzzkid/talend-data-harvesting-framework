package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TdcRestClient implements TdcClient {

    private final RestClient restClient;

    public TdcRestClient(@Qualifier("tdcHttpClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String upsertDataset(String folderPath, String modelId, Dataset dataset) {
        // TODO: POST /models/{modelId}/objects with dataset payload.
        throw new UnsupportedOperationException("upsertDataset not yet implemented");
    }

    @Override
    public void upsertLineage(String modelId, List<LineageEdge> edges) {
        // TODO: POST /models/{modelId}/lineage
        throw new UnsupportedOperationException("upsertLineage not yet implemented");
    }

    @Override
    public void setCustomAttributes(String objectId, Map<String, String> attributes) {
        // TODO: PATCH /objects/{objectId}/attributes
        throw new UnsupportedOperationException("setCustomAttributes not yet implemented");
    }

    @Override
    public boolean ping() {
        // TODO: GET /health
        return false;
    }
}
