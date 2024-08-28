package com.alipay.sofa.jraft.storage;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.option.SnapshotCopierOptions;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotCopier;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;


public interface SnapshotStorage extends Lifecycle<Void>, Storage {


    boolean setFilterBeforeCopyRemote();

    SnapshotWriter create();


    SnapshotReader open();


    SnapshotReader copyFrom(final String uri, final SnapshotCopierOptions opts);


    SnapshotCopier startToCopyFrom(final String uri, final SnapshotCopierOptions opts);


    void setSnapshotThrottle(final SnapshotThrottle snapshotThrottle);
}
