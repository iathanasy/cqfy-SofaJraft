package com.alipay.sofa.jraft.storage.snapshot;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.entity.RaftOutter;

import java.io.Closeable;




public abstract class SnapshotReader extends Snapshot implements Closeable, Lifecycle<Void> {


    public abstract RaftOutter.SnapshotMeta load();


    public abstract String generateURIForCopy();
}