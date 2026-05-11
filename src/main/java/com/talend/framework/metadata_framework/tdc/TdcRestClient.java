package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.model.ColumnDef;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST client for Talend Data Catalog. Endpoint paths and payload shapes are
 * placeholders based on the documented TDC (Metadata Manager) REST API
 * pattern; they need to be verified against the actual TDC instance once
 * credentials are available.
 *
 * Verified API references to confirm:
 * <ul>
 *   <li>Object create/upsert: {@code POST /models/{modelId}/objects}</li>
 *   <li>Lineage upsert:        {@code POST /models/{modelId}/lineage}</li>
 *   <li>Custom attributes:     {@code PATCH /objects/{id}/attributes}</li>
 *   <li>Health:                {@code GET /health}</li>
 * </ul>
 * The base URL and {@code /MM/api/v1} prefix are applied by the configured
 * {@link RestClient} bean in {@code RestClientConfig}.
 */
@Component
public class TdcRestClient implements TdcClient {

    private static final Logger log = LoggerFactory.getLogger(TdcRestClient.class);

    private final RestClient http;

    public TdcRestClient(@Qualifier("tdcHttpClient") RestClient http) {
        this.http = http;
    }

    @Override
    public String upsertDataset(String folderPath, String modelId, Dataset dataset) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", dataset.id());
        body.put("type", dataset.kind() == Dataset.Kind.FILE ? "File" : "Table");
        body.put("name", dataset.name());
        body.put("folder", folderPath);

        Map<String, Object> attrs = new LinkedHashMap<>();
        if (dataset.connectionName() != null) attrs.put("connectionName", dataset.connectionName());
        if (dataset.schemaName() != null) attrs.put("schemaName", dataset.schemaName());
        body.put("attributes", attrs);

        body.put("columns", toColumnPayload(dataset.columns()));

        log.debug("TDC upsertDataset model={} folder={} id={}", modelId, folderPath, dataset.id());
        try {
            // TODO: verify endpoint path against TDC instance.
            return http.post()
                    .uri("/models/{modelId}/objects", modelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new TdcApiException("upsertDataset failed for " + dataset.id(), ex);
        }
    }

    @Override
    public void upsertLineage(String modelId, List<LineageEdge> edges) {
        List<Map<String, Object>> payload = new ArrayList<>(edges.size());
        for (LineageEdge e : edges) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", e.sourceDatasetId());
            entry.put("target", e.targetDatasetId());
            if (e.stage() != null) entry.put("stage", e.stage().name());
            if (e.component() != null) entry.put("component", e.component());
            payload.add(entry);
        }

        log.debug("TDC upsertLineage model={} edges={}", modelId, edges.size());
        try {
            // TODO: verify endpoint path against TDC instance.
            http.post()
                    .uri("/models/{modelId}/lineage", modelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new TdcApiException("upsertLineage failed for model " + modelId, ex);
        }
    }

    @Override
    public void setCustomAttributes(String objectId, Map<String, String> attributes) {
        log.debug("TDC setCustomAttributes object={} attrs={}", objectId, attributes.keySet());
        try {
            // TODO: verify endpoint path against TDC instance.
            http.patch()
                    .uri("/objects/{id}/attributes", objectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(attributes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new TdcApiException("setCustomAttributes failed for " + objectId, ex);
        }
    }

    @Override
    public boolean ping() {
        try {
            http.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.debug("TDC ping failed: {}", ex.getMessage());
            return false;
        }
    }

    private List<Map<String, Object>> toColumnPayload(List<ColumnDef> columns) {
        List<Map<String, Object>> out = new ArrayList<>(columns.size());
        for (ColumnDef c : columns) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.name());
            m.put("type", c.type());
            if (c.nullable() != null) m.put("nullable", c.nullable());
            if (c.position() != null) m.put("position", c.position());
            out.add(m);
        }
        return out;
    }
}
