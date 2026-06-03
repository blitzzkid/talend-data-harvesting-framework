package com.talend.framework.metadata_framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * The shared TDC HTTP client. Base URL is {@code base-url + api-path}
     * (e.g. {@code http://localhost:11480/MM/rest/v1}), so endpoint paths
     * in callers are short — {@code /auth/login}, {@code /...}.
     *
     * <p>Authentication is API-key based: {@link com.talend.framework.metadata_framework.tdc.TdcSession}
     * POSTs to {@code /auth/login} once to obtain a token, and
     * {@link com.talend.framework.metadata_framework.tdc.TdcRestClient} sends
     * it on every call as the {@code api-key} request header. No cookies or
     * CSRF nonce are involved, so no redirect special-casing is needed.
     */
    @Bean
    public RestClient tdcHttpClient(TdcProperties props) {
        String baseUrl = (props.getBaseUrl() == null ? "" : props.getBaseUrl())
                + (props.getApiPath() == null ? "" : props.getApiPath());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
