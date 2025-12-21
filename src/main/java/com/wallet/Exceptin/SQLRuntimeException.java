package com.wallet.Exceptin;

public class SQLRuntimeException extends RuntimeException {
    public SQLRuntimeException(String message) {
        super(message);
    }
    public SQLRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
    public SQLRuntimeException(Throwable cause) {
        super(cause);
    }
    public SQLRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
