package com.alipay.sofa.jraft.util.internal;


public interface IntegerFieldUpdater<U> {

    void set(final U obj, final int newValue);

    int get(final U obj);
}