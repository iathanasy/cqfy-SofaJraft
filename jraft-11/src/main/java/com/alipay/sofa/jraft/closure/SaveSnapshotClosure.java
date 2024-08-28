package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;


public interface SaveSnapshotClosure extends Closure {


    SnapshotWriter start(final RaftOutter.SnapshotMeta meta);
}
