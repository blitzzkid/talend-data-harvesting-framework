package com.talend.framework.metadata_framework.tdc;

/** Wraps any failure while writing or uploading a Data Mapping Script. */
public class ScriptDeliveryException extends RuntimeException {

    public ScriptDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
