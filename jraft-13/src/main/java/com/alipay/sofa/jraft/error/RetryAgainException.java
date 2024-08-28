package com.alipay.sofa.jraft.error;

public class RetryAgainException extends Exception {

    private static final long serialVersionUID = 8690835003361525337L;

    public RetryAgainException() {
        super();
    }

    public RetryAgainException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RetryAgainException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryAgainException(String message) {
        super(message);
    }

    public RetryAgainException(Throwable cause) {
        super(cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
