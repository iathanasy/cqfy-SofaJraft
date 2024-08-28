package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.NodeMetrics;
import com.alipay.sofa.jraft.core.State;
import com.alipay.sofa.jraft.entity.NodeId;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.ReadOnlyOption;
import com.alipay.sofa.jraft.storage.LogStorage;
import com.alipay.sofa.jraft.util.Describer;

import java.util.List;


public interface Node extends Lifecycle<NodeOptions>, Describer {


    PeerId getLeaderId();


    NodeId getNodeId();

    String getGroupId();

    RaftOptions getRaftOptions();

    NodeOptions getOptions();

    void apply(final Task task);

    State getNodeState();

    NodeMetrics getNodeMetrics();

    long getLastLogIndex();

    LogStorage getLogStorage();

    void readIndex(final byte[] requestContext, final ReadIndexClosure done);

    void readIndex(final ReadOnlyOption readOnlyOptions, final byte[] requestContext, final ReadIndexClosure done);

    List<PeerId> listPeers();

    void addPeer(final PeerId peer, final Closure done);

    boolean isLeader();

    boolean isLeader(final boolean blocking);

    long getLastCommittedIndex();

    long getLastAppliedLogIndex();

    void shutdown(final Closure done);

    void join() throws InterruptedException;

    List<PeerId> listAlivePeers();

    List<PeerId> listLearners();

    List<PeerId> listAliveLearners();

    void removePeer(final PeerId peer, final Closure done);

    void changePeers(final Configuration newPeers, final Closure done);

    Status resetPeers(final Configuration newPeers);

    void addLearners(final List<PeerId> learners, final Closure done);

    void removeLearners(final List<PeerId> learners, final Closure done);

    void resetLearners(final List<PeerId> learners, final Closure done);

    void snapshot(final Closure done);

    Status transferLeadershipTo(PeerId peer);
}
