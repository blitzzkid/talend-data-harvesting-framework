package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.config.TdcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Manages a TDC session: form-based login via {@code /MM/j_spring_security_check},
 * cookie capture ({@code x-auth-token} + {@code clientId}), and nonce retrieval
 * via {@code /MM/api/GetSessionInfo}.
 *
 * <p>All write calls in {@link TdcRestClient} must include:
 * <ul>
 *   <li>{@code Cookie: <cookieHeader()>}</li>
 *   <li>{@code x-nonce: <getNonce()>}</li>
 *   <li>{@code X-Requested-With: XMLHttpRequest}</li>
 * </ul>
 *
 * <p>TDC sessions expire after ~60 minutes. Call {@link #invalidate()} when a
 * call returns 401/403/500 so the next {@link #ensureAuthenticated()} re-logins.
 */
@Component
public class TdcSession {

    private static final Logger log = LoggerFactory.getLogger(TdcSession.class);

    private static final String LOGIN_PATH        = "/MM/j_spring_security_check";
    private static final String SESSION_INFO_PATH = "/MM/api/GetSessionInfo";

    private final RestClient     http;
    private final TdcProperties  props;

    private volatile String  authToken;
    private volatile String  clientId;
    private volatile String  nonce;
    private volatile boolean authenticated = false;

    public TdcSession(@Qualifier("tdcHttpClient") RestClient http, TdcProperties props) {
        this.http  = http;
        this.props = props;
    }

    /** Ensures a valid session exists; performs login if not yet authenticated. */
    public synchronized void ensureAuthenticated() {
        if (!authenticated) {
            login();
        }
    }

    /** Marks the session as invalid so the next call triggers a fresh login. */
    public synchronized void invalidate() {
        authenticated = false;
        authToken     = null;
        clientId      = null;
        nonce         = null;
    }

    /**
     * Returns the {@code Cookie} header value to include on every API call:
     * {@code clientId=<id>; x-auth-token=<token>}
     */
    public String cookieHeader() {
        return "clientId=" + clientId + "; x-auth-token=" + authToken;
    }

    /** Returns the CSRF nonce to send as the {@code x-nonce} request header. */
    public String getNonce() {
        return nonce;
    }

    // -------------------------------------------------------------------------
    // Internal login flow
    // -------------------------------------------------------------------------

    private void login() {
        String formBody = "j_username=" + encode(props.getAuth().getUsername())
                        + "&j_password=" + encode(props.getAuth().getPassword());

        log.info("Authenticating with TDC at {}", props.getBaseUrl());

        // POST the login form.  The server responds with HTTP 302 and sets
        // x-auth-token + clientId cookies.  The underlying HttpClient is
        // configured with Redirect.NEVER so the 302 is visible here and we can
        // extract the Set-Cookie headers before any redirect is followed.
        http.post()
                .uri(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .exchange((req, resp) -> {
                    List<String> setCookies = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
                    if (setCookies != null) {
                        for (String cookie : setCookies) {
                            // Each Set-Cookie value is like: name=value; Path=/MM; ...
                            String nameValue = cookie.split(";")[0];
                            if (nameValue.startsWith("x-auth-token=")) {
                                authToken = nameValue.substring("x-auth-token=".length());
                            } else if (nameValue.startsWith("clientId=")) {
                                clientId = nameValue.substring("clientId=".length());
                            }
                        }
                    }
                    return null;
                });

        if (authToken == null) {
            throw new TdcApiException(
                    "TDC login failed: no x-auth-token cookie in response from " + LOGIN_PATH, null);
        }

        // Fetch session info to obtain the per-session CSRF nonce required on
        // every subsequent request as the x-nonce header.
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionInfo = http.post()
                .uri(SESSION_INFO_PATH)
                .header(HttpHeaders.COOKIE, cookieHeader())
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .body(Map.class);

        if (sessionInfo == null || sessionInfo.get("nonce") == null) {
            throw new TdcApiException("TDC GetSessionInfo returned no nonce", null);
        }

        nonce         = (String) sessionInfo.get("nonce");
        authenticated = true;
        log.info("TDC session established. User: {}, nonce: {}", sessionInfo.get("userName"), nonce);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
