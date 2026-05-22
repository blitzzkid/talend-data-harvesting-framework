package com.talend.framework.metadata_framework.tdc;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.talend.framework.metadata_framework.config.TdcImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Delivers a Data Mapping Script to where the TDC import bridge can read it,
 * per {@link TdcImportProperties#getDelivery()}: a local file, or an SFTP
 * upload to the TDC VM (typically reached through an SSH tunnel).
 */
@Component
public class ScriptPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScriptPublisher.class);
    private static final Pattern UNSAFE_FILENAME = Pattern.compile("[^A-Za-z0-9._-]");

    private final TdcImportProperties props;

    public ScriptPublisher(TdcImportProperties props) {
        this.props = props;
    }

    public PublishResult publish(String jobName, String script) {
        String fileName = UNSAFE_FILENAME.matcher(jobName == null ? "harvest" : jobName)
                .replaceAll("_") + ".sql";
        return switch (props.getDelivery()) {
            case LOCAL -> writeLocal(fileName, script);
            case SFTP -> uploadSftp(fileName, script);
        };
    }

    private PublishResult writeLocal(String fileName, String script) {
        try {
            Path dir = Path.of(props.getOutputDir());
            Files.createDirectories(dir);
            Path out = dir.resolve(fileName);
            Files.writeString(out, script, StandardCharsets.UTF_8);
            String dest = out.toAbsolutePath().toString();
            log.info("Wrote Data Mapping Script to {}", dest);
            return new PublishResult("local", dest);
        } catch (Exception ex) {
            throw new ScriptDeliveryException("local write failed for " + fileName, ex);
        }
    }

    private PublishResult uploadSftp(String fileName, String script) {
        TdcImportProperties.Sftp s = props.getSftp();
        Session session = null;
        ChannelSftp channel = null;
        try {
            JSch jsch = new JSch();
            if (s.usesKeyAuth()) {
                // Same key you use for `ssh work-admin@<vm>`. mwiede/jsch supports
                // OpenSSH-format keys including ed25519.
                if (s.getPassphrase() != null && !s.getPassphrase().isBlank()) {
                    jsch.addIdentity(s.getPrivateKeyPath(), s.getPassphrase());
                } else {
                    jsch.addIdentity(s.getPrivateKeyPath());
                }
            }

            session = jsch.getSession(s.getUsername(), s.getHost(), s.getPort());
            if (!s.usesKeyAuth() && s.getPassword() != null && !s.getPassword().isBlank()) {
                session.setPassword(s.getPassword());
            }

            Properties cfg = new Properties();
            cfg.put("StrictHostKeyChecking", s.isStrictHostKeyChecking() ? "yes" : "no");
            session.setConfig(cfg);
            session.connect(s.getConnectTimeoutMs());

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(s.getConnectTimeoutMs());

            ensureRemoteDir(channel, s.getRemoteDir());
            String remotePath = joinRemote(s.getRemoteDir(), fileName);
            try (InputStream in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8))) {
                channel.put(in, remotePath);
            }
            String dest = s.getHost() + ":" + s.getPort() + remotePath;
            log.info("Uploaded Data Mapping Script to {}", dest);
            return new PublishResult("sftp", dest);
        } catch (Exception ex) {
            throw new ScriptDeliveryException(
                    "SFTP upload failed to " + s.getHost() + ":" + s.getPort() + s.getRemoteDir(), ex);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /** Create the remote directory (and any missing parents) if it does not exist. */
    private void ensureRemoteDir(ChannelSftp channel, String dir) throws SftpException {
        if (dir == null || dir.isBlank()) {
            return;
        }
        try {
            channel.stat(dir);
            return;
        } catch (SftpException notThere) {
            // fall through and create
        }
        StringBuilder path = new StringBuilder(dir.startsWith("/") ? "/" : "");
        for (String segment : dir.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            path.append(segment).append('/');
            String current = path.toString();
            try {
                channel.stat(current);
            } catch (SftpException missing) {
                channel.mkdir(current);
            }
        }
    }

    private String joinRemote(String dir, String fileName) {
        if (dir == null || dir.isBlank()) {
            return fileName;
        }
        return dir.endsWith("/") ? dir + fileName : dir + "/" + fileName;
    }
}
