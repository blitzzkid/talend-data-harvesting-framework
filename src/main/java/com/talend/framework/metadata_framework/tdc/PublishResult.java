package com.talend.framework.metadata_framework.tdc;

/**
 * Outcome of delivering a Data Mapping Script to TDC.
 *
 * @param mechanism   how it was delivered ({@code "local"} or {@code "sftp"})
 * @param destination where it landed (absolute local path, or {@code host:port/remote/path})
 */
public record PublishResult(String mechanism, String destination) {
}
