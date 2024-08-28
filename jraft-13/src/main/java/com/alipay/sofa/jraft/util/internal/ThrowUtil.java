package com.alipay.sofa.jraft.util.internal;


public final class ThrowUtil {

    private static final ReferenceFieldUpdater<Throwable, Throwable> causeUpdater = Updaters.newReferenceFieldUpdater(
            Throwable.class, "cause");

    /**
     * Raises an exception bypassing compiler checks for checked exceptions.
     */
    public static void throwException(final Throwable t) {
        if (UnsafeUtil.hasUnsafe()) {
            UnsafeUtil.throwException(t);
        } else {
            ThrowUtil.throwException0(t);
        }
    }

    /**
     * private static <E extends java/lang/Throwable> void throwException0(java.lang.Throwable) throws E;
     *      flags: ACC_PRIVATE, ACC_STATIC
     *      Code:
     *      stack=1, locals=1, args_size=1
     *          0: aload_0
     *          1: athrow
     *      ...
     *  Exceptions:
     *      throws java.lang.Throwable
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException0(final Throwable t) throws E {
        throw (E) t;
    }

    public static <T extends Throwable> T cutCause(final T cause) {
        Throwable rootCause = cause;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (rootCause != cause) {
            cause.setStackTrace(rootCause.getStackTrace());
            causeUpdater.set(cause, cause);
        }
        return cause;
    }

    private ThrowUtil() {
    }
}
