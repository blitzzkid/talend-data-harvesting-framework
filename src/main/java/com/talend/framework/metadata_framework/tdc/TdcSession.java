package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the TDC REST API key obtained from {@code POST /auth/login}.
 *
 * <p>Per the official Metadata Management REST API
 * (<a href="https://metaintegration.net/Products/MIMM/REST-API/">spec</a>):
 * <ul>
 *   <li>Base path: {@code /MM/rest/v1}</li>
 *   <li>Login: {@code POST /auth/login} with JSON {@code {user, password}}
 *       → 200 with {@code {result: {token}, error}}; 401 on bad credentials.</li>
 *   <li>Subsequent calls send the token as the {@code api-key} request header.</li>
 *   <li>Logout: {@code POST /auth/logout} with {@code api-key} header.</li>
 * </ul>
 *
 * <p>MIMM enforces one active session per user. {@link #invalidate()} therefore
 * calls the server-side logout before clearing the local token, so the next
 * {@link #ensureAuthenticated()} can log in without hitting the
 * "User already logged in" 401.
 */
@Component
public class TdcSession {

    private static final Logger log = LoggerFactory.getLogger(TdcSession.class);

    // forceLogin=true tells TDC to invalidate any existing session for this user
    // before creating a new one, preventing "Invalid or Stale session handle" errors
    // when the browser or another client has logged in independently.
    private static final String LOGIN_PATH  = "/auth/login?forceLogin=true";
    private static final String LOGOUT_PATH = "/auth/logout";

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
            warmUpSession();
            login();
        }
    }

    /**
     * Logs out from the TDC server (if a token is held) then clears local state,
     * so the next {@link #ensureAuthenticated()} can obtain a fresh token.
     *
     * <p>Logout errors are silently swallowed — the server-side session may have
     * already expired, but we still need to clear local state and re-login.
     */
    public synchronized void invalidate() {
        serverLogout();          // tell TDC to drop the session before we forget the token
        authenticated = false;
        apiKey        = null;
    }

    /** Called automatically on application shutdown to release the TDC session. */
    @PreDestroy
    public synchronized void shutdown() {
        if (authenticated) {
            log.info("Application shutting down — logging out TDC session");
            invalidate();
        }
    }

    /** The token to send as the {@code api-key} header on each authenticated call. */
    public String getApiKey() {
        return apiKey;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * GETs the TDC home page so the server creates an HTTP session and sets a
     * {@code JSESSIONID} cookie. The {@link java.net.CookieManager} in the
     * underlying JDK HttpClient stores it automatically and replays it on all
     * subsequent requests — including the internal {@code /MM/api/} calls that
     * require an active browser session in addition to the auth token.
     */
    private void warmUpSession() {
        String homeUrl = (props.getBaseUrl() == null ? "" : props.getBaseUrl()) + "/MM/";
        try {
            http.get()
                    .uri(homeUrl)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("TDC session warm-up GET {} succeeded", homeUrl);
        } catch (Exception ex) {
            // Non-fatal: a redirect or 4xx from the home page is fine — we just
            // need the server to issue a JSESSIONID in its response headers.
            log.debug("TDC session warm-up GET {} (non-fatal): {}", homeUrl, ex.getMessage());
        }
    }

    private void login() {
        // Pre-serialize to JSON string so StringHttpMessageConverter writes the bytes verbatim.
        // Relying on Jackson message-converter resolution to serialize a Map has proven unreliable
        // across Spring Boot versions (the converter may not be selected, leaving the body empty).
        String user     = Objects.toString(props.getAuth().getUsername(), "");
        String password = Objects.toString(props.getAuth().getPassword(), "");
        String jsonBody = "{\"user\":\"" + jsonEscape(user) + "\",\"password\":\"" + jsonEscape(password) + "\"}";

        String effectiveLoginUrl = (props.getBaseUrl() == null ? "" : props.getBaseUrl())
                + (props.getApiPath() == null ? "" : props.getApiPath())
                + LOGIN_PATH;
        log.info("Authenticating with TDC at {} (user {})",
                effectiveLoginUrl, props.getAuth().getUsername());

        Map<String, Object> response;
        try {
            response = http.post()
                    .uri(LOGIN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            throw new TdcApiException("TDC /auth/login failed: " + ex.getMessage(), ex);
        }

        // The MIMM REST API wraps the token: { "result": { "token": "..." }, "error": ... }
        Object resultObj = response == null ? null : response.get("result");
        Object token = (resultObj instanceof Map<?, ?> resultMap) ? resultMap.get("token") : null;
        if (!(token instanceof String tokenStr) || tokenStr.isBlank()) {
            // Surface the response so the user can compare against the docs if the
            // field names differ on their build.
            throw new TdcApiException("TDC /auth/login returned no token; body=" + safeBody(response), null);
        }

        apiKey        = tokenStr;
        authenticated = true;
        log.info("TDC login successful (token length {})", tokenStr.length());
    }

    /**
     * Calls {@code POST /auth/logout} with the current token. Any error is
     * logged at DEBUG and suppressed — a failed logout must not block re-login.
     */
    private void serverLogout() {
        if (apiKey == null) {
            return;   // nothing to logout
        }
        try {
            http.post()
                    .uri(LOGOUT_PATH)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("api-key", apiKey)
                    .body("{}")
                    .retrieve()
                    .toBodilessEntity();
            log.debug("TDC server-side logout succeeded");
        } catch (RestClientException ex) {
            // Session may have already expired — fine, just clear local state.
            log.debug("TDC server-side logout failed (session may have expired): {}", ex.getMessage());
        }
    }

    private static Map<String, Object> safeBody(Map<String, Object> body) {
        if (body == null) {
            return Map.of();
        }
        // Redact the token (defensive — we land here only when token is missing/wrong shape).
        Map<String, Object> safe = new LinkedHashMap<>(body);
        safe.replaceAll((k, v) -> {
            if ("token".equalsIgnoreCase(k)) return "<redacted>";
            if ("result".equalsIgnoreCase(k) && v instanceof Map<?, ?> nested) {
                Map<Object, Object> safeNested = new LinkedHashMap<>(nested);
                safeNested.replaceAll((nk, nv) -> "token".equalsIgnoreCase(String.valueOf(nk)) ? "<redacted>" : nv);
                return safeNested;
            }
            return v;
        });
        return safe;
    }

    /** Escapes backslashes and double-quotes so the value is safe inside a JSON string literal. */
    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
