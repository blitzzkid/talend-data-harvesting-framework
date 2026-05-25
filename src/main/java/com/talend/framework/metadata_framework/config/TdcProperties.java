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
    /** The PostgreSQL (JDBC) Imported Model — the harvested tables/columns ("the dots") to refresh. */
    private String harvestedModelId;
    /** The Data Mapping model the audit lineage is pushed into ("the connections"), stitched to the harvested model. */
    private String lineageModelId;
    private Auth auth = new Auth();
    private Request request = new Request();
    private Api api = new Api();

    /**
     * Write endpoints for the internal {@code /MM/api/} RPC API the TDC web UI
     * uses (the documented {@code /MM/api/v1} REST API is not the one in play
     * here). These are not publicly documented: capture the exact operation
     * path and JSON payload from Chrome DevTools &gt; Network while performing
     * the refresh/import in the TDC UI, then set them here. Left blank until
     * confirmed, so a misconfigured call fails loudly instead of guessing.
     */
    @Getter
    @Setter
    public static class Api {
        private String refreshPath;
        private String lineagePath;
    }

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
