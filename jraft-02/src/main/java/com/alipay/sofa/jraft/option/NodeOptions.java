package com.alipay.sofa.jraft.option;

import com.alipay.remoting.util.StringUtils;
import com.alipay.sofa.jraft.JRaftServiceFactory;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.ElectionPriority;
import com.alipay.sofa.jraft.util.Copiable;
import com.alipay.sofa.jraft.util.JRaftServiceLoader;
import com.alipay.sofa.jraft.util.Utils;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:这个类封装的是Node节点对象需要用到的配置参数
 */
public class NodeOptions extends RpcOptions implements Copiable<NodeOptions> {

    //该类加载的时候就会执行这行代码，然后就会通过SPI机制加载DefaultJRaftServiceFactory类到内存中，DefaultJRaftServiceFactory类是JRaftServiceFactory接口的实现类
    //DefaultJRaftServiceFactory的作用我写在该类的注释中了，去该类中看一看
    public static final JRaftServiceFactory defaultServiceFactory = JRaftServiceLoader.load(JRaftServiceFactory.class).first();

    //超时选举时间，默认1000ms
    private int electionTimeoutMs = 1000;

    //节点参与选举优先级，默认为-1，禁用优先级功能
    private int electionPriority = ElectionPriority.Disabled;

    //衰减优先级的辅助变量，在NodeImpl类中会见到该成员变量的用法
    private int decayPriorityGap = 10;

    //领导者租约时间，租约也是一个比较重要的概念
    private int leaderLeaseTimeRatio = 90;

    //集群配置信息对象在这里初始化，刚初始化时配置信息是空的
    private Configuration initialConf = new Configuration();

    //用户配置的存储日志文件的路径
    private String logUri;

    //元数据文件存储路径
    private String raftMetaUri;

    //默认不禁用集群客户端，通过集群客户端可以直接和整个集群进行交互
    private boolean disableCli = false;
    //该参数的作用是决定是否要共享定时任务管理器，如果定时任务管理器器共享的话
    //那么集群中的同类定时任务可能都要交给同一种定时任务管理器管理，使用其中的定时任务调度线程池来调度
    private boolean sharedTimerPool = false;

    //全局定时任务管理器中线程池核心线程的数量，默认是CPU核数的3倍，但是不超过20，
    private int timerPoolSize = Utils.cpus() * 3 > 20 ? 20 : Utils.cpus() * 3;

    //集群客户端的线程池大小
    private int cliRpcThreadPoolSize = Utils.cpus();

    //jraft内部节点的服务端线程池的大小，默认为CPU核数乘6
    private int raftRpcThreadPoolSize = Utils.cpus() * 6;
    //是否启用节点性能监控功能，默认为不启用
    private boolean enableMetrics = false;

    //下面这四个成员变量意味着超时选举定时器、投票定时器、检测降级或下台定时器和快照生成定时器都不共享
    private boolean sharedElectionTimer = false;

    private boolean sharedVoteTimer = false;

    private boolean sharedStepDownTimer = false;

    private boolean sharedSnapshotTimer = false;

    //得到了提供集群内部组件服务的工厂，第一版本中该工厂只提供元数据存储器服务
    private JRaftServiceFactory serviceFactory = defaultServiceFactory;

    public void setEnableMetrics(final boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    private RaftOptions raftOptions = new RaftOptions();

    public int getCliRpcThreadPoolSize() {
        return this.cliRpcThreadPoolSize;
    }

    public void setCliRpcThreadPoolSize(final int cliRpcThreadPoolSize) {
        this.cliRpcThreadPoolSize = cliRpcThreadPoolSize;
    }

    public boolean isEnableMetrics() {
        return this.enableMetrics;
    }

    public int getRaftRpcThreadPoolSize() {
        return this.raftRpcThreadPoolSize;
    }

    public void setRaftRpcThreadPoolSize(final int raftRpcThreadPoolSize) {
        this.raftRpcThreadPoolSize = raftRpcThreadPoolSize;
    }

    public boolean isSharedTimerPool() {
        return this.sharedTimerPool;
    }

    public void setSharedTimerPool(final boolean sharedTimerPool) {
        this.sharedTimerPool = sharedTimerPool;
    }

    public int getTimerPoolSize() {
        return this.timerPoolSize;
    }

    public void setTimerPoolSize(final int timerPoolSize) {
        this.timerPoolSize = timerPoolSize;
    }

    public RaftOptions getRaftOptions() {
        return this.raftOptions;
    }

    public void setRaftOptions(final RaftOptions raftOptions) {
        this.raftOptions = raftOptions;
    }

    public void validate() {
        if (StringUtils.isBlank(this.logUri)) {
            throw new IllegalArgumentException("Blank logUri");
        }
        if (StringUtils.isBlank(this.raftMetaUri)) {
            throw new IllegalArgumentException("Blank raftMetaUri");
        }
    }

    public JRaftServiceFactory getServiceFactory() {
        return this.serviceFactory;
    }

    public void setServiceFactory(final JRaftServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public int getElectionPriority() {
        return this.electionPriority;
    }

    public void setElectionPriority(final int electionPriority) {
        this.electionPriority = electionPriority;
    }

    public int getDecayPriorityGap() {
        return this.decayPriorityGap;
    }

    public void setDecayPriorityGap(final int decayPriorityGap) {
        this.decayPriorityGap = decayPriorityGap;
    }

    public int getElectionTimeoutMs() {
        return this.electionTimeoutMs;
    }

    public void setElectionTimeoutMs(final int electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
    }

    public int getLeaderLeaseTimeRatio() {
        return this.leaderLeaseTimeRatio;
    }

    public void setLeaderLeaseTimeRatio(final int leaderLeaseTimeRatio) {
        if (leaderLeaseTimeRatio <= 0 || leaderLeaseTimeRatio > 100) {
            throw new IllegalArgumentException("leaderLeaseTimeRatio: " + leaderLeaseTimeRatio
                    + " (expected: 0 < leaderLeaseTimeRatio <= 100)");
        }
        this.leaderLeaseTimeRatio = leaderLeaseTimeRatio;
    }

    public int getLeaderLeaseTimeoutMs() {
        return this.electionTimeoutMs * this.leaderLeaseTimeRatio / 100;
    }


    public Configuration getInitialConf() {
        return this.initialConf;
    }

    public void setInitialConf(final Configuration initialConf) {
        this.initialConf = initialConf;
    }


    public String getLogUri() {
        return this.logUri;
    }

    public void setLogUri(final String logUri) {
        this.logUri = logUri;
    }

    public String getRaftMetaUri() {
        return this.raftMetaUri;
    }

    public void setRaftMetaUri(final String raftMetaUri) {
        this.raftMetaUri = raftMetaUri;
    }


    public boolean isDisableCli() {
        return this.disableCli;
    }

    public void setDisableCli(final boolean disableCli) {
        this.disableCli = disableCli;
    }

    public boolean isSharedElectionTimer() {
        return this.sharedElectionTimer;
    }

    public void setSharedElectionTimer(final boolean sharedElectionTimer) {
        this.sharedElectionTimer = sharedElectionTimer;
    }

    public boolean isSharedVoteTimer() {
        return this.sharedVoteTimer;
    }

    public void setSharedVoteTimer(final boolean sharedVoteTimer) {
        this.sharedVoteTimer = sharedVoteTimer;
    }

    public boolean isSharedStepDownTimer() {
        return this.sharedStepDownTimer;
    }

    public void setSharedStepDownTimer(final boolean sharedStepDownTimer) {
        this.sharedStepDownTimer = sharedStepDownTimer;
    }

    public boolean isSharedSnapshotTimer() {
        return this.sharedSnapshotTimer;
    }

    public void setSharedSnapshotTimer(final boolean sharedSnapshotTimer) {
        this.sharedSnapshotTimer = sharedSnapshotTimer;
    }


    //又是一个深拷贝方法
    @Override
    public NodeOptions copy() {
        final NodeOptions nodeOptions = new NodeOptions();
        nodeOptions.setElectionTimeoutMs(this.electionTimeoutMs);
        nodeOptions.setElectionPriority(this.electionPriority);
        nodeOptions.setDecayPriorityGap(this.decayPriorityGap);
        nodeOptions.setDisableCli(this.disableCli);
        nodeOptions.setSharedTimerPool(this.sharedTimerPool);
        nodeOptions.setTimerPoolSize(this.timerPoolSize);
        nodeOptions.setCliRpcThreadPoolSize(this.cliRpcThreadPoolSize);
        nodeOptions.setRaftRpcThreadPoolSize(this.raftRpcThreadPoolSize);
        nodeOptions.setEnableMetrics(this.enableMetrics);
        nodeOptions.setRaftOptions(this.raftOptions == null ? new RaftOptions() : this.raftOptions.copy());
        nodeOptions.setSharedElectionTimer(this.sharedElectionTimer);
        nodeOptions.setSharedVoteTimer(this.sharedVoteTimer);
        nodeOptions.setSharedStepDownTimer(this.sharedStepDownTimer);
        nodeOptions.setSharedSnapshotTimer(this.sharedSnapshotTimer);
        nodeOptions.setRpcConnectTimeoutMs(super.getRpcConnectTimeoutMs());
        nodeOptions.setRpcDefaultTimeout(super.getRpcDefaultTimeout());
        nodeOptions.setRpcInstallSnapshotTimeout(super.getRpcInstallSnapshotTimeout());
        nodeOptions.setRpcProcessorThreadPoolSize(super.getRpcProcessorThreadPoolSize());
        nodeOptions.setEnableRpcChecksum(super.isEnableRpcChecksum());
        nodeOptions.setMetricRegistry(super.getMetricRegistry());

        return nodeOptions;
    }

    @Override
    public String toString() {
        return "NodeOptions{" + "electionTimeoutMs=" + this.electionTimeoutMs + ", electionPriority="
                + this.electionPriority + ", decayPriorityGap=" + this.decayPriorityGap + ", leaderLeaseTimeRatio="
                + this.leaderLeaseTimeRatio + ",  initialConf=" + this.initialConf + ", logUri='" + this.logUri + '\''
                + ", raftMetaUri='" + this.raftMetaUri + '\'' + ",disableCli=" + this.disableCli
                + ", sharedTimerPool=" + this.sharedTimerPool + ", timerPoolSize=" + this.timerPoolSize
                + ", cliRpcThreadPoolSize=" + this.cliRpcThreadPoolSize + ", raftRpcThreadPoolSize="
                + this.raftRpcThreadPoolSize + ", enableMetrics=" + this.enableMetrics + ", " +
                ", sharedElectionTimer=" + this.sharedElectionTimer + ", sharedVoteTimer="
                + this.sharedVoteTimer + ", sharedStepDownTimer=" + this.sharedStepDownTimer + ", sharedSnapshotTimer="
                + this.sharedSnapshotTimer + ", serviceFactory=" + this.serviceFactory + ", " +
                " raftOptions=" + this.raftOptions + "} " + super.toString();
    }
}
