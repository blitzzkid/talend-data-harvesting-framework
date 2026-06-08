package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * TDC client using the official Metadata Management REST API ({@code /MM/rest/v1}).
 *
 * <h3>Auth</h3>
 * Every call sends the token returned by {@link TdcSession} as the
 * {@code api-key} request header (the scheme documented in the MITI Swagger spec).
 *
 * <h3>Endpoints used</h3>
 * <ul>
 *   <li>{@code POST /operations/startImport/{contentId}} — triggers a new JDBC harvest
 *       (the "dots" model).</li>
 *   <li>{@code POST /dataMapping/importScript} — uploads the generated lineage SQL and
 *       imports it into the Data Mapping Script model (no SSH required).</li>
 * </ul>
 */
@Component
public class TdcRestClient implements TdcClient {

    private static final Logger log = LoggerFactory.getLogger(TdcRestClient.class);

    /** Official REST API path for triggering a model import. */
    private static final String START_IMPORT_PATH = "/operations/startImport/";

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
            session.invalidate();
            session.ensureAuthenticated();
            return session.getApiKey() != null;
        } catch (RuntimeException ex) {
            log.warn("TDC ping failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Triggers an import of the JDBC-harvested database model via
     * {@code POST /operations/startImport/{contentId}}.
     * Returns immediately with the operation ID; TDC runs the import asynchronously.
     */
    @Override
    public void refreshModel(String modelId) {
        String contentId = require(props.getHarvestedModelContentId(), "tdc.harvested-model-content-id");
        log.info("Triggering TDC JDBC harvest via startImport (contentId={})", contentId);
        session.ensureAuthenticated();
        try {
            Map<?, ?> response = http.post()
                    .uri(START_IMPORT_PATH + contentId)
                    .header("api-key", session.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(Map.class);
            log.info("startImport response: {}", response);
        } catch (RestClientException ex) {
            throw new TdcApiException("startImport failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Triggers an import of the Data Mapping Script model via
     * {@code POST /operations/startImport/{contentId}}.
     * The SQL file must already be at its configured path on the TDC VM
     * (delivered via {@link com.talend.framework.metadata_framework.tdc.TdcSshClient})
     * before this is called. TDC reads it from there when it processes the import.
     */
    @Override
    public void pushLineage(String modelId, JobLineageGraph graph) {
        String contentId = require(props.getLineageModelContentId(), "tdc.lineage-model-content-id");
        log.info("Triggering TDC lineage import via startImport (contentId={}) for job={} edges={}",
                contentId, graph.jobName(), graph.edges().size());
        session.ensureAuthenticated();
        try {
            Map<?, ?> response = http.post()
                    .uri(START_IMPORT_PATH + contentId)
                    .header("api-key", session.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(Map.class);
            log.info("startImport (lineage) response: {}", response);
        } catch (RestClientException ex) {
            throw new TdcApiException("startImport (lineage) failed: " + ex.getMessage(), ex);
        }
    }

    private String require(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new TdcApiException("TDC config missing: set '" + key + "' in application-local.yml", null);
        }
        return value;
    }
}
