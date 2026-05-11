package com.talend.framework.metadata_framework.tdc;

/** Wraps any failure from the TDC REST client so callers see a consistent type. */
public class TdcApiException extends RuntimeException {

    public TdcApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
