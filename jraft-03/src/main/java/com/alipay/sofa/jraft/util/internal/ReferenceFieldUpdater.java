package com.alipay.sofa.jraft.util.internal;


public interface ReferenceFieldUpdater<U, W> {

    void set(final U obj, final W newValue);

    W get(final U obj);
}