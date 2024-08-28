package com.alipay.sofa.jraft.error;


public class OverloadException extends JRaftException {


    private static final long serialVersionUID = -5505054326197103575L;

    public OverloadException() {
        super();
    }

    public OverloadException(final String message, final Throwable cause, final boolean enableSuppression,
                             final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public OverloadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public OverloadException(final String message) {
        super(message);
    }

    public OverloadException(final Throwable cause) {
        super(cause);
    }

}