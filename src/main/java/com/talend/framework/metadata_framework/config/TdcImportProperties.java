package com.talend.framework.metadata_framework.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * How harvested lineage is delivered to Talend Data Catalog.
 *
 * The framework does not write to TDC over REST (TDC has no create-lineage
 * endpoint). Instead it serializes the lineage graph into a Data Mapping
 * Script ({@code .sql}) and drops it where the TDC "Data Mapping Script"
 * import bridge can read it. Two delivery modes:
 *
 * <ul>
 *   <li>{@code LOCAL} — write the script to {@link #outputDir} on this host.
 *       Used when the harvester runs on the TDC VM, or for manual copy.</li>
 *   <li>{@code SFTP} — upload the script to {@link Sftp#remoteDir} on the TDC
 *       VM. Used when the harvester runs on a dev laptop and reaches the VM's
 *       SSH port through an SSH tunnel (e.g. {@code localhost:2222}).</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tdc.import")
public class TdcImportProperties {

    public enum Delivery { LOCAL, SFTP }

    private Delivery delivery = Delivery.LOCAL;

    /** Directory the script is written to when {@code delivery=LOCAL}. */
    private String outputDir = "build/tdc-scripts";

    @NestedConfigurationProperty
    private Sftp sftp = new Sftp();

    @Getter
    @Setter
    public static class Sftp {
        /** Tunnel endpoint, not the VM IP — the laptop reaches the VM's SSH via a local forward. */
        private String host = "localhost";
        private int port = 2222;
        private String username;
        private String password;
        /** Directory on the TDC VM that the Data Mapping Script bridge scans. */
        private String remoteDir = "/home/work-admin/SQL/";
        /** Off by default: the tunnel endpoint key is the VM's and changes per environment. */
        private boolean strictHostKeyChecking = false;
        private int connectTimeoutMs = 10000;
    }
}
