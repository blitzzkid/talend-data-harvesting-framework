package com.talend.framework.metadata_framework.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate tdcRestTemplate(RestTemplateBuilder builder, TdcProperties props) {
        return builder
                .connectTimeout(Duration.ofMillis(props.getRequest().getConnectTimeoutMs()))
                .readTimeout(Duration.ofMillis(props.getRequest().getReadTimeoutMs()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, buildAuthHeader(props.getAuth()))
                .build();
    }

    private String buildAuthHeader(TdcProperties.Auth auth) {
        if ("basic".equalsIgnoreCase(auth.getType())) {
            String creds = auth.getUsername() + ":" + auth.getPassword();
            return "Basic " + Base64.getEncoder()
                    .encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        }
        return "Bearer " + (auth.getToken() == null ? "" : auth.getToken());
    }
}
