package com.alipay.sofa.jraft.storage.snapshot;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.google.protobuf.Message;

import java.io.Closeable;
import java.io.IOException;


public abstract class SnapshotWriter extends Snapshot implements Closeable, Lifecycle<Void> {

    private RaftOutter.SnapshotMeta currentMeta;


    public RaftOutter.SnapshotMeta getCurrentMeta() {
        return currentMeta;
    }

    void setCurrentMeta(RaftOutter.SnapshotMeta currentMeta) {
        this.currentMeta = currentMeta;
    }


    public abstract boolean saveMeta(final RaftOutter.SnapshotMeta meta);


    public boolean addFile(final String fileName) {
        return addFile(fileName, null);
    }


    public abstract boolean addFile(final String fileName, final Message fileMeta);


    public abstract boolean removeFile(final String fileName);


    public abstract void close(final boolean keepDataOnError) throws IOException;
}
