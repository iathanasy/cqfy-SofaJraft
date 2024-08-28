package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;


public interface StateMachine {


    void onApply(final Iterator iter);

    void onShutdown();


    void onSnapshotSave(final SnapshotWriter writer, final Closure done);
//
//
//    boolean onSnapshotLoad(final SnapshotReader reader);


    void onLeaderStart(final long term);


    void onLeaderStop(final Status status);


    void onError(final RaftException e);


    void onConfigurationCommitted(final Configuration conf);


    void onStopFollowing(final LeaderChangeContext ctx);


    void onStartFollowing(final LeaderChangeContext ctx);
}
