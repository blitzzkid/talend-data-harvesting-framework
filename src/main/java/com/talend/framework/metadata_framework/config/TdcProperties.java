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
    /**
     * The content ID (e.g. {@code -1_56}) of the PostgreSQL (JDBC) Imported Model.
     * Found in the TDC browser URL when the model is selected: {@code objectId=-1_XX}.
     */
    private String harvestedModelContentId;
    /**
     * The content ID (e.g. {@code -1_39}) of the Data Mapping Script model.
     * Found in the TDC browser URL when the model is selected: {@code objectId=-1_XX}.
     */
    private String lineageModelContentId;
    /**
     * The configuration ID (e.g. {@code -1_4}) that both models belong to.
     * Found in the TDC browser URL: {@code configId=-1_XX}.
     */
    private String configId;
    private Auth auth = new Auth();
    private Request request = new Request();
    private Ssh ssh = new Ssh();

    @Getter
    @Setter
    public static class Auth {
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class Request {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 60000;
    }

    /**
     * SSH credentials for delivering the generated lineage SQL file to the TDC VM.
     * The file is written to {@code remoteSqlPath} via SCP, then TDC reads it
     * from there when {@code ImportHarvestableContent} is called.
     *
     * <p>Supports either private-key auth ({@code privateKeyPath}) or password auth.
     * Private key is preferred — set {@code privateKeyPath} and leave {@code password} blank.
     */
    @Getter
    @Setter
    public static class Ssh {
        private String host;
        private int port = 22;
        private String username;
        /** Optional: password for the private key (leave blank if key has no passphrase). */
        private String passphrase;
        /** Optional: plain password auth (use only if private key is not available). */
        private String password;
        /**
         * Path to the SSH private key file. Accepts both Linux paths
         * (e.g. {@code /home/melvtan/.ssh/id_rsa}) and Windows UNC paths to WSL
         * (e.g. {@code \\\\wsl.localhost\\Ubuntu-24.04\\home\\melvtan\\.ssh\\id_rsa}).
         */
        private String privateKeyPath;
        /** Absolute path on the TDC VM where the lineage SQL file is written. */
        private String remoteSqlPath;
    }
}
