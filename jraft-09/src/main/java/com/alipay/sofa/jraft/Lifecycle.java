package com.alipay.sofa.jraft;


//生命周期接口
public interface Lifecycle<T> {

    boolean init(final T opts);

    void shutdown();
}