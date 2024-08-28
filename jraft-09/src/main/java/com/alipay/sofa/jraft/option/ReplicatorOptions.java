package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.core.BallotBox;
import com.alipay.sofa.jraft.core.NodeImpl;
import com.alipay.sofa.jraft.core.ReplicatorType;
import com.alipay.sofa.jraft.core.Scheduler;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.RaftClientService;
import com.alipay.sofa.jraft.storage.LogManager;
import com.alipay.sofa.jraft.storage.SnapshotStorage;
import com.alipay.sofa.jraft.util.Copiable;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:复制器对象需要的配置参数，这个类中的成员变量应该都很熟悉了，就不再添加详细注释了，看看这个类的对象是在
 * 哪里被创建的，结合上下文一看就全明白了
 */
public class ReplicatorOptions implements Copiable<ReplicatorOptions> {

    private int dynamicHeartBeatTimeoutMs;
    private int electionTimeoutMs;
    private String groupId;
    private PeerId serverId;
    private PeerId peerId;
    private LogManager logManager;
    //投票箱
    private BallotBox ballotBox;
    private NodeImpl node;
    private long term;
    private SnapshotStorage snapshotStorage;
    private RaftClientService raftRpcService;
    private Scheduler timerManager;
    private ReplicatorType replicatorType;

    public ReplicatorOptions() {
        super();
    }

    public ReplicatorOptions(final ReplicatorType replicatorType, final int dynamicHeartBeatTimeoutMs,
                             final int electionTimeoutMs, final String groupId, final PeerId serverId,
                             final PeerId peerId,final LogManager logManager, final BallotBox ballotBox,
                             final NodeImpl node, final long term, final RaftClientService raftRpcService) {
        super();
        this.replicatorType = replicatorType;
        this.dynamicHeartBeatTimeoutMs = dynamicHeartBeatTimeoutMs;
        this.electionTimeoutMs = electionTimeoutMs;
        this.groupId = groupId;
        this.serverId = serverId;
        if (peerId != null) {
            this.peerId = peerId.copy();
        } else {
            this.peerId = null;
        }
        this.logManager = logManager;
        this.ballotBox = ballotBox;
        this.node = node;
        this.term = term;
        this.raftRpcService = raftRpcService;
        this.timerManager = timerManager;
    }

    public final ReplicatorType getReplicatorType() {
        return this.replicatorType;
    }

    public void setReplicatorType(final ReplicatorType replicatorType) {
        this.replicatorType = replicatorType;
    }

    public RaftClientService getRaftRpcService() {
        return this.raftRpcService;
    }

    public void setRaftRpcService(final RaftClientService raftRpcService) {
        this.raftRpcService = raftRpcService;
    }

    @Override
    public ReplicatorOptions copy() {
        final ReplicatorOptions replicatorOptions = new ReplicatorOptions();
        replicatorOptions.setDynamicHeartBeatTimeoutMs(this.dynamicHeartBeatTimeoutMs);
        replicatorOptions.setReplicatorType(this.replicatorType);
        replicatorOptions.setElectionTimeoutMs(this.electionTimeoutMs);
        replicatorOptions.setGroupId(this.groupId);
        replicatorOptions.setServerId(this.serverId);
        replicatorOptions.setPeerId(this.peerId);
        replicatorOptions.setLogManager(this.logManager);
        replicatorOptions.setBallotBox(this.ballotBox);
        replicatorOptions.setNode(this.node);
        replicatorOptions.setTerm(this.term);
        replicatorOptions.setSnapshotStorage(this.snapshotStorage);
        replicatorOptions.setRaftRpcService(this.raftRpcService);
        replicatorOptions.setTimerManager(this.timerManager);
        return replicatorOptions;
    }

    public Scheduler getTimerManager() {
        return this.timerManager;
    }

    public void setTimerManager(final Scheduler timerManager) {
        this.timerManager = timerManager;
    }

    public PeerId getPeerId() {
        return this.peerId;
    }

    public void setPeerId(final PeerId peerId) {
        if (peerId != null) {
            this.peerId = peerId.copy();
        } else {
            this.peerId = null;
        }
    }

    public SnapshotStorage getSnapshotStorage() {
        return this.snapshotStorage;
    }

    public void setSnapshotStorage(final SnapshotStorage snapshotStorage) {
        this.snapshotStorage = snapshotStorage;
    }
    public int getDynamicHeartBeatTimeoutMs() {
        return this.dynamicHeartBeatTimeoutMs;
    }

    public void setDynamicHeartBeatTimeoutMs(final int dynamicHeartBeatTimeoutMs) {
        this.dynamicHeartBeatTimeoutMs = dynamicHeartBeatTimeoutMs;
    }

    public int getElectionTimeoutMs() {
        return this.electionTimeoutMs;
    }

    public void setElectionTimeoutMs(final int electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public PeerId getServerId() {
        return this.serverId;
    }

    public void setServerId(final PeerId serverId) {
        this.serverId = serverId;
    }

    public LogManager getLogManager() {
        return this.logManager;
    }

    public void setLogManager(final LogManager logManager) {
        this.logManager = logManager;
    }

    public BallotBox getBallotBox() {
        return this.ballotBox;
    }

    public void setBallotBox(final BallotBox ballotBox) {
        this.ballotBox = ballotBox;
    }

    public NodeImpl getNode() {
        return this.node;
    }

    public void setNode(final NodeImpl node) {
        this.node = node;
    }

    public long getTerm() {
        return this.term;
    }

    public void setTerm(final long term) {
        this.term = term;
    }


    @Override
    public String toString() {
        return "ReplicatorOptions{" + "replicatorType=" + this.replicatorType + "dynamicHeartBeatTimeoutMs="
                + this.dynamicHeartBeatTimeoutMs + ", electionTimeoutMs=" + this.electionTimeoutMs + ", groupId='"
                + this.groupId + '\'' + ", serverId=" + this.serverId + ", peerId=" + this.peerId + "," +
                " node=" + this.node + ", term=" + this.term
                + ",  raftRpcService=" + this.raftRpcService
                + ", timerManager=" + this.timerManager + '}';
    }
}
