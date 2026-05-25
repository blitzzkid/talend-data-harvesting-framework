package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
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
 *   <li>Each call is a POST carrying {@code Cookie}, {@code x-nonce} and
 *       {@code X-Requested-With: XMLHttpRequest}.</li>
 *   <li>On 401/403 the session is invalidated and the call retried once.</li>
 * </ol>
 *
 * <h3>Endpoints — not yet known</h3>
 * The operation paths are <b>not</b> hardcoded. Set {@code tdc.api.refresh-path}
 * and {@code tdc.api.lineage-path} to the values observed in Chrome DevTools &gt;
 * Network while doing the equivalent action in the TDC UI (re-import a model;
 * create/edit a data mapping). The payload builders below produce a reasonable
 * JSON shape; adjust them to match the captured body. Until a path is set, the
 * call fails with a clear message rather than guessing.
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

    @Override
    public void refreshModel(String modelId) {
        String path = requirePath(props.getApi().getRefreshPath(), "tdc.api.refresh-path");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("modelId", modelId);
        log.debug("TDC refresh model={}", modelId);
        postWithAuth(path, body);
    }

    @Override
    public void pushLineage(String modelId, JobLineageGraph graph) {
        String path = requirePath(props.getApi().getLineagePath(), "tdc.api.lineage-path");
        Map<String, Object> body = lineageBody(modelId, graph);
        log.debug("TDC push lineage model={} job={} edges={}",
                modelId, graph.jobName(), graph.edges().size());
        postWithAuth(path, body);
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
    // Payload builder — adjust field names to match the DevTools-captured body
    // -------------------------------------------------------------------------

    /**
     * One entry per edge, each side resolved to its real {@code connection /
     * schema / table} so TDC can stitch it to the harvested model — not the
     * internal dataset id.
     */
    private Map<String, Object> lineageBody(String modelId, JobLineageGraph graph) {
        Map<String, Dataset> byId = new LinkedHashMap<>();
        for (Dataset d : graph.datasets()) {
            byId.put(d.id(), d);
        }

        List<Map<String, Object>> edges = new ArrayList<>(graph.edges().size());
        for (LineageEdge e : graph.edges()) {
            Dataset source = byId.get(e.sourceDatasetId());
            Dataset target = byId.get(e.targetDatasetId());
            if (source == null || target == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", endpointRef(source));
            entry.put("target", endpointRef(target));
            if (e.stage() != null) entry.put("stage", e.stage().name());
            if (e.component() != null) entry.put("component", e.component());
            edges.add(entry);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("modelId", modelId);
        body.put("jobName", graph.jobName());
        body.put("edges", edges);
        return body;
    }

    private Map<String, Object> endpointRef(Dataset d) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("type", d.kind() == Dataset.Kind.FILE ? "File" : "Table");
        if (d.connectionName() != null) ref.put("connection", d.connectionName());
        if (d.schemaName() != null) ref.put("schema", d.schemaName());
        ref.put("name", d.name());
        return ref;
    }
}
