package com.alipay.sofa.jraft.util.internal;


public class Updaters {

    public static <U, W> ReferenceFieldUpdater<U, W> newReferenceFieldUpdater(final Class<? super U> tClass,
                                                                              final String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeReferenceFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionReferenceFieldUpdater<>(tClass, fieldName);
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
