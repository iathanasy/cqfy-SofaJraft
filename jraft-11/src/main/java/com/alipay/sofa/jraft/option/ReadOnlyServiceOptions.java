package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.FSMCaller;
import com.alipay.sofa.jraft.core.NodeImpl;

public class ReadOnlyServiceOptions {

    private RaftOptions raftOptions;
    private NodeImpl node;
    private FSMCaller fsmCaller;

    public NodeImpl getNode() {
        return node;
    }

    public void setNode(NodeImpl node) {
        this.node = node;
    }

    public RaftOptions getRaftOptions() {
        return raftOptions;
    }

    public void setRaftOptions(RaftOptions raftOptions) {
        this.raftOptions = raftOptions;
    }

    public FSMCaller getFsmCaller() {
        return fsmCaller;
    }

    public void setFsmCaller(FSMCaller fsm) {
        this.fsmCaller = fsm;
    }
}
