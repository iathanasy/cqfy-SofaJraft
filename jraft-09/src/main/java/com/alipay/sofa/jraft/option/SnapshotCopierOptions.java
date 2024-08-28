package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.core.Scheduler;
import com.alipay.sofa.jraft.rpc.RaftClientService;


public class SnapshotCopierOptions {

    private String groupId;
    private RaftClientService raftClientService;
    private Scheduler timerManager;
    private RaftOptions raftOptions;
    private NodeOptions nodeOptions;

    public SnapshotCopierOptions() {
        super();
    }

    public SnapshotCopierOptions(String groupId, RaftClientService raftClientService, Scheduler timerManager,
                                 RaftOptions raftOptions, NodeOptions nodeOptions) {
        super();
        this.groupId = groupId;
        this.raftClientService = raftClientService;
        this.timerManager = timerManager;
        this.raftOptions = raftOptions;
        this.nodeOptions = nodeOptions;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public NodeOptions getNodeOptions() {
        return this.nodeOptions;
    }

    public void setNodeOptions(NodeOptions nodeOptions) {
        this.nodeOptions = nodeOptions;
    }

    public RaftClientService getRaftClientService() {
        return this.raftClientService;
    }

    public void setRaftClientService(RaftClientService raftClientService) {
        this.raftClientService = raftClientService;
    }

    public Scheduler getTimerManager() {
        return this.timerManager;
    }

    public void setTimerManager(Scheduler timerManager) {
        this.timerManager = timerManager;
    }

    public RaftOptions getRaftOptions() {
        return this.raftOptions;
    }

    public void setRaftOptions(RaftOptions raftOptions) {
        this.raftOptions = raftOptions;
    }
}
