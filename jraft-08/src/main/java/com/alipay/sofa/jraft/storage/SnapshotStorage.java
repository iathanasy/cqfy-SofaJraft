package com.alipay.sofa.jraft.storage;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;


public interface SnapshotStorage extends Lifecycle<Void>, Storage {


    boolean setFilterBeforeCopyRemote();

    SnapshotWriter create();


    //SnapshotReader open();


    // copyFrom(final String uri, final SnapshotCopierOptions opts);


    //SnapshotCopier startToCopyFrom(final String uri, final SnapshotCopierOptions opts);


    void setSnapshotThrottle(final SnapshotThrottle snapshotThrottle);
}
