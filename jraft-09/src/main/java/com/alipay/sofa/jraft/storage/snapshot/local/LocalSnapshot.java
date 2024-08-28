package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.snapshot.Snapshot;
import com.google.protobuf.Message;

import java.util.Set;


public class LocalSnapshot extends Snapshot {

    private final LocalSnapshotMetaTable metaTable;

    public LocalSnapshot(RaftOptions raftOptions) {
        this.metaTable = new LocalSnapshotMetaTable(raftOptions);
    }

    public LocalSnapshotMetaTable getMetaTable() {
        return this.metaTable;
    }

    @Override
    public String getPath() {
        throw new UnsupportedOperationException();
    }

    /**
     * List all the existing files in the Snapshot currently
     *
     * @return the existing file list
     */
    @Override
    public Set<String> listFiles() {
        return this.metaTable.listFiles();
    }

    @Override
    public Message getFileMeta(final String fileName) {
        return this.metaTable.getFileMeta(fileName);
    }
}

