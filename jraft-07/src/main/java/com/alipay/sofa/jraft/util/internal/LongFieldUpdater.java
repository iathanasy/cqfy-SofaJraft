package com.alipay.sofa.jraft.util.internal;


public interface LongFieldUpdater<U> {

    void set(final U obj, final long newValue);

    long get(final U obj);
}