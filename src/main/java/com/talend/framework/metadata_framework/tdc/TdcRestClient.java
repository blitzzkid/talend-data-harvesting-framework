package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * TDC client using the internal {@code /MM/api/} RPC API.
 *
 * <h3>Authentication</h3>
 * Authentication is session-based.  {@link TdcSession} handles the login flow:
 * <ol>
 *   <li>POST {@code /MM/j_spring_security_check} → captures {@code x-auth-token}
 *       and {@code clientId} cookies from the 302 response.</li>
 *   <li>POST {@code /MM/api/GetSessionInfo} → extracts the CSRF {@code nonce}.</li>
 * </ol>
 * Every request must include:
 * <ul>
 *   <li>{@code Cookie: clientId=…; x-auth-token=…}</li>
 *   <li>{@code x-nonce: <nonce>}</li>
 *   <li>{@code X-Requested-With: XMLHttpRequest}</li>
 * </ul>
 *
 * <h3>Write endpoints</h3>
 * The TDC {@code /MM/api/v1/} documented REST API is not enabled on this
 * instance ({@code hasRestDoc: false} in GetSessionInfo).  The write endpoints
 * in the internal {@code /MM/api/} namespace are yet to be discovered by
 * inspecting Chrome DevTools Network while performing create/import operations
 * in the TDC UI.  Until then, {@link #upsertDataset}, {@link #upsertLineage},
 * and {@link #setCustomAttributes} throw {@link UnsupportedOperationException}.
 */
@Component
public class TdcRestClient implements TdcClient {

    private static final Logger log = LoggerFactory.getLogger(TdcRestClient.class);

    private static final String SESSION_INFO_PATH = "/MM/api/GetSessionInfo";

    private final RestClient http;
    private final TdcSession session;

    public TdcRestClient(@Qualifier("tdcHttpClient") RestClient http, TdcSession session) {
        this.http    = http;
        this.session = session;
    }

    // -------------------------------------------------------------------------
    // Connectivity check
    // -------------------------------------------------------------------------

    @Override
    public boolean ping() {
        try {
            session.ensureAuthenticated();
            http.post()
                    .uri(SESSION_INFO_PATH)
                    .header(HttpHeaders.COOKIE, session.cookieHeader())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("x-nonce", session.getNonce())
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
    // Write operations — endpoints TBD
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p><b>TODO</b>: Discover the correct {@code /MM/api/} endpoint by watching
     * Chrome DevTools &gt; Network while creating or importing an object in the
     * TDC UI, then implement the call here using the auth pattern below:
     * <pre>
     *   session.ensureAuthenticated();
     *   http.post()
     *       .uri("/MM/api/&lt;DiscoveredEndpoint&gt;")
     *       .header(HttpHeaders.COOKIE, session.cookieHeader())
     *       .header("x-nonce",          session.getNonce())
     *       .header("X-Requested-With", "XMLHttpRequest")
     *       .contentType(MediaType.APPLICATION_FORM_URLENCODED) // or APPLICATION_JSON
     *       .body(payload)
     *       .retrieve().body(String.class);
     * </pre>
     */
    @Override
    public String upsertDataset(String folderPath, String modelId, Dataset dataset) {
        throw new UnsupportedOperationException(
                "upsertDataset: TDC write endpoint not yet discovered. " +
                "Inspect Chrome DevTools > Network while creating an object in the TDC UI.");
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>TODO</b>: Discover the correct {@code /MM/api/} endpoint by watching
     * Chrome DevTools &gt; Network while viewing or editing lineage in the TDC UI.
     */
    @Override
    public void upsertLineage(String modelId, List<LineageEdge> edges) {
        throw new UnsupportedOperationException(
                "upsertLineage: TDC write endpoint not yet discovered. " +
                "Inspect Chrome DevTools > Network while editing lineage in the TDC UI.");
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>TODO</b>: Discover the correct {@code /MM/api/} endpoint by watching
     * Chrome DevTools &gt; Network while editing custom attributes on an object.
     */
    @Override
    public void setCustomAttributes(String objectId, Map<String, String> attributes) {
        throw new UnsupportedOperationException(
                "setCustomAttributes: TDC write endpoint not yet discovered. " +
                "Inspect Chrome DevTools > Network while editing object attributes in the TDC UI.");
    }
}
