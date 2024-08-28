package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;


public interface SaveSnapshotClosure extends Closure {

    /**
     * Starts to save snapshot, returns the writer.
     *
     * @param meta metadata of snapshot.
     * @return returns snapshot writer.
     */
    SnapshotWriter start(final RaftOutter.SnapshotMeta meta);
}
