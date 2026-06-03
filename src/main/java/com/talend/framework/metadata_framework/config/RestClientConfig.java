package com.talend.framework.metadata_framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * The shared TDC HTTP client. Base URL is {@code base-url + api-path}
     * (e.g. {@code http://localhost:11480/MM/rest/v1}), so endpoint paths
     * in callers are short — {@code /auth/login}, {@code /...}.
     *
     * <p>Uses the Spring Boot auto-configured {@link RestClient.Builder} so that
     * all default message converters (Jackson, String, etc.) are pre-registered.
     * Using the static {@code RestClient.builder()} factory instead can leave
     * Jackson converters missing, causing the request body to be sent empty.
     */
    @Bean
    public RestClient tdcHttpClient(TdcProperties props) {
        String baseUrl = (props.getBaseUrl() == null ? "" : props.getBaseUrl())
                + (props.getApiPath() == null ? "" : props.getApiPath());

        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getRequest().getConnectTimeoutMs()))
                .build();

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(jdkClient))
                .build();
    }
}
