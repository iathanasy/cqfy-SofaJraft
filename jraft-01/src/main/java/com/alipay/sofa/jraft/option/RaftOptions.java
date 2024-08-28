package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.util.Copiable;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:该类封装着raft协议中需要用到的一些配置参数，这些配置参数在发送网络信息，以及数据落盘
 * 将数据同步给跟随者时会用到，但在第一版本这些数据还用不到，所以就先不讲解了，这里面封装的信息有很多都和性能调优有关
 * 等后面用到哪个成员变量了，再单独给这个成员变量添加注释，然后进行讲解
 */
public class RaftOptions implements Copiable<RaftOptions> {


    private int maxByteCountPerRpc = 128 * 1024;

    private boolean fileCheckHole = false;

    private int maxEntriesSize = 1024;

    private int maxBodySize = 512 * 1024;

    private int maxAppendBufferSize = 256 * 1024;
    //进行选举时的最大延迟时间
    private int maxElectionDelayMs = 1000;

    private int electionHeartbeatFactor = 10;

    private int applyBatch = 32;

    private boolean sync = true;

    private boolean syncMeta = false;

    private boolean openStatistics = true;

    private boolean replicatorPipeline = true;

    private int maxReplicatorInflightMsgs = 256;

    private int disruptorBufferSize = 16384;

    private int disruptorPublishEventWaitTimeoutSecs = 10;

    private boolean enableLogEntryChecksum = false;


    private int maxReadIndexLag = -1;

    //选举超时之后，当前的候选者节点是否退位，这里默认退位，或者是下台
    private boolean stepDownWhenVoteTimedout = true;


    private boolean startupOldStorage = false;


    //下面都是一些get/set方法
    public boolean isStepDownWhenVoteTimedout() {
        return this.stepDownWhenVoteTimedout;
    }

    public void setStepDownWhenVoteTimedout(final boolean stepDownWhenVoteTimeout) {
        this.stepDownWhenVoteTimedout = stepDownWhenVoteTimeout;
    }

    public int getDisruptorPublishEventWaitTimeoutSecs() {
        return this.disruptorPublishEventWaitTimeoutSecs;
    }

    public void setDisruptorPublishEventWaitTimeoutSecs(final int disruptorPublishEventWaitTimeoutSecs) {
        this.disruptorPublishEventWaitTimeoutSecs = disruptorPublishEventWaitTimeoutSecs;
    }

    public boolean isEnableLogEntryChecksum() {
        return this.enableLogEntryChecksum;
    }

    public void setEnableLogEntryChecksum(final boolean enableLogEntryChecksumValidation) {
        this.enableLogEntryChecksum = enableLogEntryChecksumValidation;
    }

    public int getMaxReadIndexLag() {
        return maxReadIndexLag;
    }

    public void setMaxReadIndexLag(int maxReadIndexLag) {
        this.maxReadIndexLag = maxReadIndexLag;
    }

    public boolean isReplicatorPipeline() {
        return this.replicatorPipeline && RpcFactoryHelper.rpcFactory().isReplicatorPipelineEnabled();
    }

    public void setReplicatorPipeline(final boolean replicatorPipeline) {
        this.replicatorPipeline = replicatorPipeline;
    }

    public int getMaxReplicatorInflightMsgs() {
        return this.maxReplicatorInflightMsgs;
    }

    public void setMaxReplicatorInflightMsgs(final int maxReplicatorPiplelinePendingResponses) {
        this.maxReplicatorInflightMsgs = maxReplicatorPiplelinePendingResponses;
    }

    public int getDisruptorBufferSize() {
        return this.disruptorBufferSize;
    }

    public void setDisruptorBufferSize(final int disruptorBufferSize) {
        this.disruptorBufferSize = disruptorBufferSize;
    }

    public int getMaxByteCountPerRpc() {
        return this.maxByteCountPerRpc;
    }

    public void setMaxByteCountPerRpc(final int maxByteCountPerRpc) {
        this.maxByteCountPerRpc = maxByteCountPerRpc;
    }

    public boolean isFileCheckHole() {
        return this.fileCheckHole;
    }

    public void setFileCheckHole(final boolean fileCheckHole) {
        this.fileCheckHole = fileCheckHole;
    }

    public int getMaxEntriesSize() {
        return this.maxEntriesSize;
    }

    public void setMaxEntriesSize(final int maxEntriesSize) {
        this.maxEntriesSize = maxEntriesSize;
    }

    public int getMaxBodySize() {
        return this.maxBodySize;
    }

    public void setMaxBodySize(final int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public int getMaxAppendBufferSize() {
        return this.maxAppendBufferSize;
    }

    public void setMaxAppendBufferSize(final int maxAppendBufferSize) {
        this.maxAppendBufferSize = maxAppendBufferSize;
    }

    public int getMaxElectionDelayMs() {
        return this.maxElectionDelayMs;
    }

    public void setMaxElectionDelayMs(final int maxElectionDelayMs) {
        this.maxElectionDelayMs = maxElectionDelayMs;
    }

    public int getElectionHeartbeatFactor() {
        return this.electionHeartbeatFactor;
    }

    public void setElectionHeartbeatFactor(final int electionHeartbeatFactor) {
        this.electionHeartbeatFactor = electionHeartbeatFactor;
    }

    public int getApplyBatch() {
        return this.applyBatch;
    }

    public void setApplyBatch(final int applyBatch) {
        this.applyBatch = applyBatch;
    }

    public boolean isSync() {
        return this.sync;
    }

    public void setSync(final boolean sync) {
        this.sync = sync;
    }

    public boolean isSyncMeta() {
        return this.sync || this.syncMeta;
    }

    public void setSyncMeta(final boolean syncMeta) {
        this.syncMeta = syncMeta;
    }

    public boolean isOpenStatistics() {
        return this.openStatistics;
    }

    public void setOpenStatistics(final boolean openStatistics) {
        this.openStatistics = openStatistics;
    }

    public boolean isStartupOldStorage() {
        return startupOldStorage;
    }

    public void setStartupOldStorage(final boolean startupOldStorage) {
        this.startupOldStorage = startupOldStorage;
    }

    //复制方法，其实就是在内部创建了一个RaftOptions，把成员变量都设置给这个RaftOptions方法
    //然后将创建的这个RaftOptions方法回去，说白了就是一个深拷贝，
    @Override
    public RaftOptions copy() {
        final RaftOptions raftOptions = new RaftOptions();
        raftOptions.setMaxByteCountPerRpc(this.maxByteCountPerRpc);
        raftOptions.setFileCheckHole(this.fileCheckHole);
        raftOptions.setMaxEntriesSize(this.maxEntriesSize);
        raftOptions.setMaxBodySize(this.maxBodySize);
        raftOptions.setMaxAppendBufferSize(this.maxAppendBufferSize);
        raftOptions.setMaxElectionDelayMs(this.maxElectionDelayMs);
        raftOptions.setElectionHeartbeatFactor(this.electionHeartbeatFactor);
        raftOptions.setApplyBatch(this.applyBatch);
        raftOptions.setSync(this.sync);
        raftOptions.setSyncMeta(this.syncMeta);
        raftOptions.setOpenStatistics(this.openStatistics);
        raftOptions.setReplicatorPipeline(this.replicatorPipeline);
        raftOptions.setMaxReplicatorInflightMsgs(this.maxReplicatorInflightMsgs);
        raftOptions.setDisruptorBufferSize(this.disruptorBufferSize);
        raftOptions.setDisruptorPublishEventWaitTimeoutSecs(this.disruptorPublishEventWaitTimeoutSecs);
        raftOptions.setEnableLogEntryChecksum(this.enableLogEntryChecksum);
        raftOptions.setStartupOldStorage(this.startupOldStorage);
        return raftOptions;
    }

    @Override
    public String toString() {
        return "RaftOptions{" + "maxByteCountPerRpc=" + maxByteCountPerRpc + ", fileCheckHole=" + fileCheckHole
                + ", maxEntriesSize=" + maxEntriesSize + ", maxBodySize=" + maxBodySize + ", maxAppendBufferSize="
                + maxAppendBufferSize + ", maxElectionDelayMs=" + maxElectionDelayMs + ", electionHeartbeatFactor="
                + electionHeartbeatFactor + ", applyBatch=" + applyBatch + ", sync=" + sync + ", syncMeta=" + syncMeta
                + ", openStatistics=" + openStatistics + ", replicatorPipeline=" + replicatorPipeline
                + ", maxReplicatorInflightMsgs=" + maxReplicatorInflightMsgs + ", disruptorBufferSize="
                + disruptorBufferSize + ", disruptorPublishEventWaitTimeoutSecs=" + disruptorPublishEventWaitTimeoutSecs
                + ", enableLogEntryChecksum=" + enableLogEntryChecksum + "," +
                " maxReadIndexLag=" + maxReadIndexLag + ", stepDownWhenVoteTimedout=" + stepDownWhenVoteTimedout
                + ", startUpOldStorage=" + startupOldStorage + '}';
    }
}
