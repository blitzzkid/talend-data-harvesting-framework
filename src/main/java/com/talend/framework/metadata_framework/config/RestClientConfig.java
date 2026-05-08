package com.talend.framework.metadata_framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient tdcHttpClient(TdcProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl() + props.getApiPath())
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
