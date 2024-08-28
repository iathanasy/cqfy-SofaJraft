package com.alipay.sofa.jraft.storage.snapshot;

import com.alipay.sofa.jraft.Status;

import java.io.Closeable;


public abstract class SnapshotCopier extends Status implements Closeable {


    public abstract void cancel();


    public abstract void join() throws InterruptedException;


    public abstract void start();


    public abstract SnapshotReader getReader();
}