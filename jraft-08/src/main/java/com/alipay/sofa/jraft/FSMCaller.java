package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.closure.SaveSnapshotClosure;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.FSMCallerOptions;
import com.alipay.sofa.jraft.util.Describer;


public interface FSMCaller extends Lifecycle<FSMCallerOptions>, Describer {


    interface LastAppliedLogIndexListener {


        void onApplied(final long lastAppliedLogIndex);
    }


    boolean isRunningOnFSMThread();


    void addLastAppliedLogIndexListener(final LastAppliedLogIndexListener listener);


    boolean onCommitted(final long committedIndex);


    //boolean onSnapshotLoad(final LoadSnapshotClosure done);


    public void onSnapshotSaveSync(SaveSnapshotClosure done);


    boolean onSnapshotSave(final SaveSnapshotClosure done);


    boolean onLeaderStop(final Status status);


    boolean onLeaderStart(final long term);

    boolean onStartFollowing(final LeaderChangeContext ctx);


    boolean onStopFollowing(final LeaderChangeContext ctx);


    boolean onError(final RaftException error);


    long getLastAppliedIndex();


    long getLastCommittedIndex();

    void join() throws InterruptedException;
}
