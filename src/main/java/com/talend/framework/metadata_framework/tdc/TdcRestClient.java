package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class TdcRestClient implements TdcClient {

    private final RestTemplate restTemplate;
    private final TdcProperties props;

    public TdcRestClient(RestTemplate tdcRestTemplate, TdcProperties props) {
        this.restTemplate = tdcRestTemplate;
        this.props = props;
    }

    @Override
    public String upsertDataset(String folderPath, String modelId, Dataset dataset) {
        // TODO: POST {baseUrl}{apiPath}/models/{modelId}/objects with dataset payload.
        throw new UnsupportedOperationException("upsertDataset not yet implemented");
    }

    @Override
    public void upsertLineage(String modelId, List<LineageEdge> edges) {
        // TODO: POST {baseUrl}{apiPath}/models/{modelId}/lineage
        throw new UnsupportedOperationException("upsertLineage not yet implemented");
    }

    @Override
    public void setCustomAttributes(String objectId, Map<String, String> attributes) {
        // TODO: PATCH {baseUrl}{apiPath}/objects/{objectId}/attributes
        throw new UnsupportedOperationException("setCustomAttributes not yet implemented");
    }

    @Override
    public boolean ping() {
        // TODO: GET {baseUrl}{apiPath}/health
        return false;
    }
}
