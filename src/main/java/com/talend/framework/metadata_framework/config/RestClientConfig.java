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
     * Builds the shared TDC HTTP client.
     *
     * <p>Key configuration: {@code Redirect.NEVER} — the TDC login endpoint
     * responds with HTTP 302 and sets the session cookies ({@code x-auth-token},
     * {@code clientId}) on that redirect response.  If the client followed the
     * redirect automatically those Set-Cookie headers would be lost.  Keeping
     * redirect following disabled lets {@link
     * com.talend.framework.metadata_framework.tdc.TdcSession} capture them via
     * the {@code exchange()} callback before the redirect is followed.
     */
    @Bean
    public RestClient tdcHttpClient(TdcProperties props) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(props.getRequest().getConnectTimeoutMs()))
                .build();

        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(jdkClient))
                .build();
    }
}
