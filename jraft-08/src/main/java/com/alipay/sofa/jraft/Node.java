package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.core.NodeMetrics;
import com.alipay.sofa.jraft.core.State;
import com.alipay.sofa.jraft.entity.NodeId;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.LogStorage;
import com.alipay.sofa.jraft.util.Describer;


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


}
