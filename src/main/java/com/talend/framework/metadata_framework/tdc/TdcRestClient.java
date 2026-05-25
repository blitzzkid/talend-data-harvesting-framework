package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import com.talend.framework.metadata_framework.model.ColumnDef;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TDC client using the internal {@code /MM/api/} RPC API with session auth.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@link TdcSession} logs in (POST {@code /MM/j_spring_security_check}),
 *       capturing {@code x-auth-token} + {@code clientId} and the CSRF
 *       {@code nonce}.</li>
 *   <li>Every write is a POST carrying {@code Cookie}, {@code x-nonce} and
 *       {@code X-Requested-With: XMLHttpRequest}.</li>
 *   <li>On 401/403 the session is invalidated and the call retried once.</li>
 * </ol>
 *
 * <h3>Endpoints</h3>
 * The operation paths are <b>not</b> hardcoded — set {@code tdc.api.dataset-path}
 * and {@code tdc.api.lineage-path} to the values observed in Chrome DevTools &gt;
 * Network while performing the equivalent action in the TDC UI. The payload
 * builders below produce a reasonable JSON shape; adjust them to match the body
 * captured in DevTools. Until a path is set, the call fails with a clear message
 * rather than guessing.
 */
@Component
public class TdcRestClient implements TdcClient {

    private static final Logger log = LoggerFactory.getLogger(TdcRestClient.class);

    private static final String SESSION_INFO_PATH = "/MM/api/GetSessionInfo";

    private final RestClient http;
    private final TdcSession session;
    private final TdcProperties props;

    public TdcRestClient(@Qualifier("tdcHttpClient") RestClient http,
                         TdcSession session,
                         TdcProperties props) {
        this.http = http;
        this.session = session;
        this.props = props;
    }

    @Override
    public String upsertDataset(String folderPath, String modelId, Dataset dataset) {
        String path = requirePath(props.getApi().getDatasetPath(), "tdc.api.dataset-path");
        Map<String, Object> body = datasetBody(folderPath, modelId, dataset);
        log.debug("TDC POST dataset model={} folder={} id={}", modelId, folderPath, dataset.id());
        return postWithAuth(path, body);
    }

    @Override
    public void upsertLineage(String modelId, List<LineageEdge> edges) {
        String path = requirePath(props.getApi().getLineagePath(), "tdc.api.lineage-path");
        Map<String, Object> body = lineageBody(modelId, edges);
        log.debug("TDC POST lineage model={} edges={}", modelId, edges.size());
        postWithAuth(path, body);
    }

    @Override
    public void setCustomAttributes(String objectId, Map<String, String> attributes) {
        // Not used by the harvest flow; wire up with its own tdc.api.* path if needed.
        throw new UnsupportedOperationException("setCustomAttributes is not implemented");
    }

    @Override
    public boolean ping() {
        try {
            session.ensureAuthenticated();
            http.post()
                    .uri(SESSION_INFO_PATH)
                    .headers(this::applyAuthHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.debug("TDC ping failed: {}", ex.getMessage());
            session.invalidate();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Authenticated POST with one re-login retry on 401/403
    // -------------------------------------------------------------------------

    private String postWithAuth(String path, Object body) {
        session.ensureAuthenticated();
        try {
            return doPost(path, body);
        } catch (HttpStatusCodeException ex) {
            int code = ex.getStatusCode().value();
            if (code == 401 || code == 403) {
                log.debug("TDC POST {} got {} — re-authenticating and retrying once", path, code);
                session.invalidate();
                session.ensureAuthenticated();
                try {
                    return doPost(path, body);
                } catch (RestClientException retry) {
                    throw new TdcApiException("POST " + path + " failed after re-auth: "
                            + retry.getMessage(), retry);
                }
            }
            throw new TdcApiException("POST " + path + " failed: " + code + " "
                    + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new TdcApiException("POST " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    private String doPost(String path, Object body) {
        return http.post()
                .uri(path)
                .headers(this::applyAuthHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private void applyAuthHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.COOKIE, session.cookieHeader());
        headers.add("x-nonce", session.getNonce());
        headers.add("X-Requested-With", "XMLHttpRequest");
    }

    private String requirePath(String path, String key) {
        if (path == null || path.isBlank()) {
            throw new TdcApiException("TDC endpoint not configured: set '" + key
                    + "' to the /MM/api operation captured in Chrome DevTools.", null);
        }
        return path;
    }

    // -------------------------------------------------------------------------
    // Payload builders — adjust field names to match the DevTools-captured body
    // -------------------------------------------------------------------------

    private Map<String, Object> datasetBody(String folderPath, String modelId, Dataset dataset) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("modelId", modelId);
        body.put("folder", folderPath);
        body.put("id", dataset.id());
        body.put("type", dataset.kind() == Dataset.Kind.FILE ? "File" : "Table");
        body.put("name", dataset.name());
        if (dataset.connectionName() != null) body.put("connectionName", dataset.connectionName());
        if (dataset.schemaName() != null) body.put("schemaName", dataset.schemaName());
        body.put("columns", columnPayload(dataset.columns()));
        return body;
    }

    private Map<String, Object> lineageBody(String modelId, List<LineageEdge> edges) {
        List<Map<String, Object>> entries = new ArrayList<>(edges.size());
        for (LineageEdge e : edges) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", e.sourceDatasetId());
            entry.put("target", e.targetDatasetId());
            if (e.stage() != null) entry.put("stage", e.stage().name());
            if (e.component() != null) entry.put("component", e.component());
            entries.add(entry);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("modelId", modelId);
        body.put("edges", entries);
        return body;
    }

    private List<Map<String, Object>> columnPayload(List<ColumnDef> columns) {
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
