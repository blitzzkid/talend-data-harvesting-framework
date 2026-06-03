package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the TDC REST API key obtained from {@code POST /auth/login}.
 *
 * <p>Per the official Metadata Management REST API
 * (<a href="https://metaintegration.net/Products/MIMM/REST-API/">spec</a>):
 * <ul>
 *   <li>Base path: {@code /MM/rest/v1}</li>
 *   <li>Login: {@code POST /auth/login} with JSON {@code {username, password}}
 *       → 200 with {@code {token}}; 401 on bad credentials.</li>
 *   <li>Subsequent calls send the token as the {@code api-key} request header.</li>
 * </ul>
 *
 * <p>Login is lazy: {@link #ensureAuthenticated()} is a no-op when a token is
 * already held. {@link #invalidate()} clears it so the next call re-logs in
 * (used by the 401/403 retry path in {@link TdcRestClient}).
 */
@Component
public class TdcSession {

    private static final Logger log = LoggerFactory.getLogger(TdcSession.class);

    private static final String LOGIN_PATH = "/auth/login";

    private final RestClient    http;
    private final TdcProperties props;

    private volatile String  apiKey;
    private volatile boolean authenticated = false;

    public TdcSession(@Qualifier("tdcHttpClient") RestClient http, TdcProperties props) {
        this.http  = http;
        this.props = props;
    }

    public synchronized void ensureAuthenticated() {
        if (!authenticated) {
            login();
        }
    }

    public synchronized void invalidate() {
        authenticated = false;
        apiKey        = null;
    }

    /** The token to send as the {@code api-key} header on each authenticated call. */
    public String getApiKey() {
        return apiKey;
    }

    private void login() {
        Map<String, String> body = Map.of(
                "username", props.getAuth().getUsername() == null ? "" : props.getAuth().getUsername(),
                "password", props.getAuth().getPassword() == null ? "" : props.getAuth().getPassword()
        );

        log.info("Authenticating with TDC at {}{} (user {})",
                props.getBaseUrl(), LOGIN_PATH, props.getAuth().getUsername());

        Map<String, Object> response;
        try {
            response = http.post()
                    .uri(LOGIN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            throw new TdcApiException("TDC /auth/login failed: " + ex.getMessage(), ex);
        }

        Object token = response == null ? null : response.get("token");
        if (!(token instanceof String tokenStr) || tokenStr.isBlank()) {
            // Surface the response so the user can compare against the docs if a
            // field name differs on their build.
            throw new TdcApiException("TDC /auth/login returned no token; body=" + safeBody(response), null);
        }

        apiKey        = tokenStr;
        authenticated = true;
        log.info("TDC login successful (token length {})", tokenStr.length());
    }

    private static Map<String, Object> safeBody(Map<String, Object> body) {
        if (body == null) {
            return Map.of();
        }
        // Don't leak the token in error messages (defensive — we land here only when token is missing).
        Map<String, Object> safe = new LinkedHashMap<>(body);
        safe.replaceAll((k, v) -> "token".equalsIgnoreCase(k) ? "<redacted>" : v);
        return safe;
    }
}
