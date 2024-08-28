package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.closure.ReadIndexClosure;
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



}
