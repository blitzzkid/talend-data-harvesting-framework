package com.talend.framework.metadata_framework.tdc;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.talend.framework.metadata_framework.config.TdcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Delivers the generated lineage SQL file to the TDC VM via SFTP (SSH/JSch).
 * Supports private-key auth (preferred) or password auth.
 */
@Component
public class TdcSshClient {

    private static final Logger log = LoggerFactory.getLogger(TdcSshClient.class);

    private final TdcProperties props;

    public TdcSshClient(TdcProperties props) {
        this.props = props;
    }

    public void uploadSql(String content) {
        TdcProperties.Ssh ssh = props.getSsh();
        String host = requireSsh(ssh.getHost(),         "tdc.ssh.host");
        String user = requireSsh(ssh.getUsername(),     "tdc.ssh.username");
        String path = requireSsh(ssh.getRemoteSqlPath(),"tdc.ssh.remote-sql-path");

        log.info("Uploading lineage SQL to {}@{}:{}", user, host, path);

        Session session = null;
        ChannelSftp channel = null;
        try {
            JSch jsch = new JSch();
            boolean keyLoaded = false;
            boolean passwordConfigured = ssh.getPassword() != null && !ssh.getPassword().isBlank();

            if (ssh.getPrivateKeyPath() != null && !ssh.getPrivateKeyPath().isBlank()) {
                try {
                    String passphrase = ssh.getPassphrase();
                    if (passphrase != null && !passphrase.isBlank()) {
                        jsch.addIdentity(ssh.getPrivateKeyPath(), passphrase.getBytes(StandardCharsets.UTF_8));
                    } else {
                        jsch.addIdentity(ssh.getPrivateKeyPath());
                    }
                    keyLoaded = true;
                    log.debug("Using private key auth: {}", ssh.getPrivateKeyPath());
                } catch (JSchException keyEx) {
                    if (passwordConfigured) {
                        log.warn("Private key could not be loaded ({}). Falling back to SSH password auth.",
                                keyEx.getMessage());
                    } else {
                        throw new TdcApiException("SSH private key could not be loaded from "
                                + ssh.getPrivateKeyPath() + ": " + keyEx.getMessage(), keyEx);
                    }
                }
            }

            session = jsch.getSession(user, host, ssh.getPort());

            if (!keyLoaded && passwordConfigured) {
                session.setPassword(ssh.getPassword());
                log.debug("Using password auth for SSH");
            }

            if (!keyLoaded && !passwordConfigured) {
                throw new TdcApiException(
                        "SSH auth not configured: set tdc.ssh.private-key-path (recommended) or tdc.ssh.password",
                        null);
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(props.getRequest().getConnectTimeoutMs());

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            channel.put(new ByteArrayInputStream(bytes), path, ChannelSftp.OVERWRITE);
            log.info("Lineage SQL uploaded successfully ({} bytes)", bytes.length);

        } catch (JSchException | SftpException ex) {
            throw new TdcApiException("SSH/SFTP upload to " + host + ":" + path + " failed: " + ex.getMessage(), ex);
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private String requireSsh(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new TdcApiException("SSH config missing: set " + key + " in application-local.yml", null);
        }
        return value;
    }
}
