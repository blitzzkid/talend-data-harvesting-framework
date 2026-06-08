package com.talend.framework.metadata_framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * The shared TDC HTTP client. Base URL is {@code base-url + api-path}
     * (e.g. {@code http://localhost:11480/MM/rest/v1}), so endpoint paths
     * in callers are short — {@code /auth/login}, {@code /...}.
     *
     * <p>A {@link CookieManager} is attached so the JDK HttpClient automatically
     * stores and replays all cookies set by the server (including {@code JSESSIONID}
     * from the TDC home page and {@code x-auth-token} set via Set-Cookie).
     * This is required because the internal {@code /MM/api/} endpoints need a live
     * HTTP session established before they accept the auth token.
     */
    @Bean
    public RestClient tdcHttpClient(TdcProperties props) {
        String baseUrl = (props.getBaseUrl() == null ? "" : props.getBaseUrl())
                + (props.getApiPath() == null ? "" : props.getApiPath());

        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getRequest().getConnectTimeoutMs()))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(jdkClient))
                .build();
    }
}
