package com.alipay.sofa.jraft.storage;


public interface SnapshotThrottle {


    long throttledByThroughput(final long bytes);
}
