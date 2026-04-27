package com.talend.framework.metadata_framework.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tdc")
public class TdcProperties {

    private String baseUrl;
    private String apiPath;
    private String defaultModelId;
    private Auth auth = new Auth();
    private Request request = new Request();

    @Getter
    @Setter
    public static class Auth {
        private String type = "bearer";
        private String token;
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class Request {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
    }
}
