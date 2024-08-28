package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.*;
import com.alipay.sofa.jraft.closure.ClosureQueue;
import com.alipay.sofa.jraft.closure.ClosureQueueImpl;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.conf.ConfigurationEntry;
import com.alipay.sofa.jraft.conf.ConfigurationManager;
import com.alipay.sofa.jraft.entity.*;
import com.alipay.sofa.jraft.error.OverloadException;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.*;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.storage.LogManager;
import com.alipay.sofa.jraft.storage.LogStorage;
import com.alipay.sofa.jraft.storage.RaftMetaStorage;
import com.alipay.sofa.jraft.storage.SnapshotExecutor;
import com.alipay.sofa.jraft.storage.impl.LogManagerImpl;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotExecutorImpl;
import com.alipay.sofa.jraft.util.*;
import com.alipay.sofa.jraft.util.concurrent.LongHeldDetectingReadWriteLock;
import com.alipay.sofa.jraft.util.timer.RaftTimerFactory;
import com.google.protobuf.Message;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/21
 * @Description:整个框架中最核心的类，关于节点的一切功能几乎都定义在这个类中了，不是在该类中直接实现
 * 就是由该类的某个方法做了一层代理，总之，都会通过这个类的对象去执行操作，这个类的名字也很直接，就是节点实现的意思
 * 在分布式集群中，每一个服务器都会被抽象为一个节点，而节点的功能就是当前类提供的，在第一版本我只为大家引入了领导选举的功能
 * 实际上，该类有近4000行代码，在第一版本我为大家删去了近3000行，删去的内容后面会逐渐补全
 */
public class NodeImpl implements Node, RaftServerService {

    private static final Logger LOG = LoggerFactory.getLogger(NodeImpl.class);

    //得到定时器工厂，框架中的各种定时任务几乎都是这个定时器工厂提供的
    public final static RaftTimerFactory TIMER_FACTORY = JRaftUtils.raftTimerFactory();
    //这个成员变量是用来计算当前服务器上启动了几个raft节点，每启动一个，就自增1
    public static final AtomicInteger GLOBAL_NUM_NODES = new AtomicInteger(0);
    //提供读写锁功能的成员变量，这个读写锁虽然是jraft框架自己定义的，但本质是继承了jdk中的ReentrantReadWriteLock类
    private final ReadWriteLock readWriteLock = new NodeReadWriteLock(this);
    //写锁
    protected final Lock writeLock = this.readWriteLock.writeLock();
    //读锁
    protected final Lock readLock = this.readWriteLock.readLock();
    //当前节点的状态
    private volatile State state;
    //节点的当前任期
    private long currTerm;
    //上一次收到领导者的时间
    private volatile long lastLeaderTimestamp;
    //当前节点的PeerId
    private PeerId leaderId = new PeerId();
    //当前节点投过票的节点的PeerId，这个成员变量可以记录下来，当前节点为哪个节点投过票
    private PeerId votedId;
    //计算正式选票结果的计数器
    private final Ballot voteCtx = new Ballot();
    //计算预投票结果的计数器
    private final Ballot prevVoteCtx = new Ballot();
    //当前节点的全部配置信息，包括旧的配置信息
    private ConfigurationEntry conf;
    //当前节点所在集群的Id
    private final String groupId;
    //节点需要的配置参数
    private NodeOptions options;
    //整个jraft框架需要的一些配置参数
    private RaftOptions raftOptions;
    //当前节点自己的ID
    private final PeerId serverId;
    //处理配置变更的成员变量，在第一版本根本没发挥什么作用
    private final ConfigurationCtx confCtx;
    //日志存储组件，为了测试把这个成员变量改为public了
    public LogStorage logStorage;
    //为测试添加的方法，源码中根本就没有这个方法
    public LogStorage getLogStorage() {
        return logStorage;
    }
    //这个是日志组件成员变量的源码，是private修饰的
    //private LogStorage logStorage;
    //元数据存储器
    private RaftMetaStorage metaStorage;
    //配置信息管理组件
    private ConfigurationManager configManager;
    //日志管理组件
    private LogManager logManager;
    //状态机组件
    private FSMCaller fsmCaller;
    //状态机的回调队列
    private ClosureQueue closureQueue;
    //复制器组
    private ReplicatorGroup replicatorGroup;
    //该成员变量会为jraft框架内部提供客户端服务，用来发送心跳消息，日志传输等等
    private RaftClientService rpcService;
    //全局定时任务调度器
    private Scheduler timerManager;
    //选举超时定时器
    private RepeatedTimer electionTimer;
    //检测节点性能的成员变量，在第一版本没什么用
    private NodeMetrics metrics;
    //当前节点的Id
    private NodeId nodeId;
    //为jraft框架提供各种服务的工厂，在第一版本中，这个工厂只提供了元数据存储器服务
    private JRaftServiceFactory serviceFactory;
    //终于见到这个Disruptor，接下来会在日志管理组件中看到这样的成员变量
    //这个Disruptor就是用来将日志落盘的入口
    private Disruptor<LogEntryAndClosure> applyDisruptor;
    //Disruptor框架要使用的任务队列，也就是环形数组
    private RingBuffer<LogEntryAndClosure> applyQueue;
    //投票箱终于引入了
    private BallotBox ballotBox;
    //快照执行器
    private SnapshotExecutor snapshotExecutor;
    //快照生成定时器
    private RepeatedTimer snapshotTimer;

    @Override
    public void describe(Printer out) {

    }

    //读写锁的实现类，这个类中其实就是重写了LongHeldDetectingReadWriteLock类中的report方法
    //并且这个类中有一个静态成员变量，用来获得获取锁时的最大阻塞时间，这个时间变量会在父类中用到
    private static class NodeReadWriteLock extends LongHeldDetectingReadWriteLock {

        static final long  MAX_BLOCKING_MS_TO_REPORT = SystemPropertyUtil.getLong(
                "jraft.node.detecting.lock.max_blocking_ms_to_report", -1);

        private final Node node;

        public NodeReadWriteLock(final Node node) {
            super(MAX_BLOCKING_MS_TO_REPORT, TimeUnit.MILLISECONDS);
            this.node = node;
        }

        @Override
        public void report(final AcquireMode acquireMode, final Thread heldThread,
                           final Collection<Thread> queuedThreads, final long blockedNanos) {
            final long blockedMs = TimeUnit.NANOSECONDS.toMillis(blockedNanos);
            LOG.warn(
                    "Raft-Node-Lock report: currentThread={}, acquireMode={}, heldThread={}, queuedThreads={}, blockedMs={}.",
                    Thread.currentThread(), acquireMode, heldThread, queuedThreads, blockedMs);
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:Disruptor框架中的生产者生产的数据，在Disruptor中，会创建一个事件工厂，然后专门用来生产生产者生产的数据
     * 这个LogEntryAndClosure对象会被放到环形数组中，当作一个个要被消费者消费的数据，当然，真正要消费的数据还是这个类的对象中
     * 持有的LogEntry对象，那怎么把真正的要消费的数据放到这个类的对象中呢？这就要创建一个数据传输器对象了，很快我们就会见到了
     */
    private static class LogEntryAndClosure {
        LogEntry entry;
        Closure        done;
        long           expectedTerm;
        CountDownLatch shutdownLatch;

        public void reset() {
            this.entry = null;
            this.done = null;
            this.expectedTerm = 0;
            this.shutdownLatch = null;
        }
    }

    //事件工厂，需要在Disruptor框架中使用
    private static class LogEntryAndClosureFactory implements EventFactory<LogEntryAndClosure> {

        @Override
        public LogEntryAndClosure newInstance() {
            return new LogEntryAndClosure();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:Disruptor狂简要使用的事件处理器，这个事件处理器会被Disruptor框架的批处理器持有
     * 然后就开始批量处理任务了
     */
    private class LogEntryAndClosureHandler implements EventHandler<LogEntryAndClosure> {
        //创建一个用于批量处理日志的集合，这个集合的容量默认是32
        private final List<LogEntryAndClosure> tasks = new ArrayList<>(NodeImpl.this.raftOptions.getApplyBatch());

        //该方法其实会在disruptor的BatchEventProcessor任务批处理器中被循环调用
        //该方法就是Disruptor的批处理器要调用的真正消费数据的方法，这个方法就是将日志落盘的入口方法
        @Override
        public void onEvent(final LogEntryAndClosure event, final long sequence, final boolean endOfBatch)
                throws Exception {
            //这里是判断当前节点是否要下线了，如果要下线，并且批处理日志的队列中还有日志数据
            //就要尽快把这些日志都刷新到硬盘上
            if (event.shutdownLatch != null) {
                if (!this.tasks.isEmpty()) {
                    //在这个方法中开始处理业务日志
                    executeApplyingTasks(this.tasks);
                    reset();
                }
                final int num = GLOBAL_NUM_NODES.decrementAndGet();
                LOG.info("The number of active nodes decrement to {}.", num);
                event.shutdownLatch.countDown();
                return;
            }
            //一直向这个集合中添加事件，每一个事件中都包含一条要存储到本地的日志
            this.tasks.add(event);
            //直到满足32条日志后，就把32条日志成批处理，endOfBatch表示是否最后一个，当集合中的数据等于32条的时候
            //就要开始进行批量写入本地了
            if (this.tasks.size() >= NodeImpl.this.raftOptions.getApplyBatch() || endOfBatch) {
                //在这个方法中开始处理业务日志
                executeApplyingTasks(this.tasks);
                //清空tasks集合，并且重置每一个event对象
                reset();
            }
        }

        //重置可循环利用对象
        private void reset() {
            for (final LogEntryAndClosure task : this.tasks) {
                //这里重置event对象是为了循环利用这个对象
                task.reset();
            }//这里清空集合，是为了下一次处理32个日志做准备
            this.tasks.clear();
        }
    }



    //配置类上下文，这个内部类中有很多方法，但都被我删除了，比如addNewPeers方法，向集群中添加新节点，添加新节点就意味着配置变更
    //第一个版本大家还不用关注这个类
    private static class ConfigurationCtx {
        //这个枚举类中的几个枚举对象代表的是配置阶段
        enum Stage {
            //配置没有变更的阶段
            STAGE_NONE,
            //处于这个阶段，说明当前节点正在追赶数据进度
            STAGE_CATCHING_UP,
            //新旧配置的过度阶段
            STAGE_JOINT,
            //配置变更完毕
            STAGE_STABLE
        }

        //当前节点
        final NodeImpl node;
        //当前配置阶段
        Stage stage;
        //配置变更过多少次了
        int nchanges;
        //配置版本号
        long version;
        List<PeerId> newPeers = new ArrayList<>();
        List<PeerId> oldPeers = new ArrayList<>();
        List<PeerId> addingPeers = new ArrayList<>();
        List<PeerId> newLearners = new ArrayList<>();
        List<PeerId> oldLearners = new ArrayList<>();
        Closure done;

        public ConfigurationCtx(final NodeImpl node) {
            super();
            this.node = node;
            this.stage = Stage.STAGE_NONE;
            this.version = 0;
            this.done = null;
        }

        void flush(final Configuration conf, final Configuration oldConf) {
            Requires.requireTrue(!isBusy(), "Flush when busy");
            this.newPeers = conf.listPeers();
            this.newLearners = conf.listLearners();
            if (oldConf == null || oldConf.isEmpty()) {
                this.stage = Stage.STAGE_STABLE;
                this.oldPeers = this.newPeers;
                this.oldLearners = this.newLearners;
            } else {
                this.stage = Stage.STAGE_JOINT;
                this.oldPeers = oldConf.listPeers();
                this.oldLearners = oldConf.listLearners();
            }
            this.node.unsafeApplyConfiguration(conf, oldConf == null || oldConf.isEmpty() ? null : oldConf, true);
        }

        boolean isBusy() {
            return this.stage != Stage.STAGE_NONE;
        }

    }


    private class ConfigurationChangeDone implements Closure {
        private final long    term;
        private final boolean leaderStart;

        public ConfigurationChangeDone(final long term, final boolean leaderStart) {
            super();
            this.term = term;
            this.leaderStart = leaderStart;
        }

        @Override
        public void run(final Status status) {
            //这里暂时不做实现
//            if (status.isOk()) {
//                onConfigurationChangeDone(this.term);
//                if (this.leaderStart) {
//                    getOptions().getFsm().onLeaderStart(this.term);
//                }
//            } else {
//                LOG.error("Fail to run ConfigurationChangeDone, status: {}.", status);
//            }
        }
    }


    private void unsafeApplyConfiguration(final Configuration newConf, final Configuration oldConf,
                                          final boolean leaderStart) {
        Requires.requireTrue(this.confCtx.isBusy(), "ConfigurationContext is not busy");
        final LogEntry entry = new LogEntry(EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION);
        entry.setId(new LogId(0, this.currTerm));
        entry.setPeers(newConf.listPeers());
        entry.setLearners(newConf.listLearners());
        if (oldConf != null) {
            entry.setOldPeers(oldConf.listPeers());
            entry.setOldLearners(oldConf.listLearners());
        }
        final ConfigurationChangeDone configurationChangeDone = new ConfigurationChangeDone(this.currTerm, leaderStart);
        //给配置变更日志创建投票计数器
        if (!this.ballotBox.appendPendingTask(newConf, oldConf, configurationChangeDone)) {
            ThreadPoolsFactory.runClosureInThread(this.groupId, configurationChangeDone, new Status(
                    RaftError.EINTERNAL, "Fail to append task."));
            return;
        }
        final List<LogEntry> entries = new ArrayList<>();
        entries.add(entry);
        //把配置变更日志写入日志管理系统
        this.logManager.appendEntries(entries, new LeaderStableClosure(entries));
        //checkAndSetConfiguration(false);
    }

    //构造方法
    public NodeImpl() {
        this(null, null);
    }

    //构造方法，在测试类中，我使用的就是这个构造方法
    public NodeImpl(final String groupId, final PeerId serverId) {
        //父类就是Object了，这行代码没什么实质作用，我感觉什么用也没有，顶多算是一种代码规范吧
        super();
        //对集群Id判空
        if (groupId != null) {
            Utils.verifyGroupId(groupId);
        }
        this.groupId = groupId;
        this.serverId = serverId != null ? serverId.copy() : null;
        this.state = State.STATE_UNINITIALIZED;
        this.currTerm = 0;
        //初始化lastLeaderTimestamp的值
        updateLastLeaderTimestamp(Utils.monotonicMs());
        //创建配置信息上下文对象
        this.confCtx = new ConfigurationCtx(this);
        final int num = GLOBAL_NUM_NODES.incrementAndGet();
        LOG.info("The number of active nodes increment to {}.", num);
    }


    //判断是否触发选举超时了，如果触发了就可以让选举定时器进行工作了，在选举定时器中就可以
    //预投票，然后正式投票选举出新的领导者了
    private boolean isCurrentLeaderValid() {
        //下面这行代码的意思就是用当前时间减去当前节点最后一次收到领导者信息的时间
        //如果这个时间差超过了用户设置的超时选举的时间，那就可以在超时选举定时器中进行投票选举领导者的活动了
        return Utils.monotonicMs() - this.lastLeaderTimestamp < this.options.getElectionTimeoutMs();
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/19
     * @方法描述：初始化快照组建的方法
     */
    private boolean initSnapshotStorage() {
        if (StringUtils.isEmpty(this.options.getSnapshotUri())) {
            LOG.warn("Do not set snapshot uri, ignore initSnapshotStorage.");
            return true;
        }
        this.snapshotExecutor = new SnapshotExecutorImpl();
        final SnapshotExecutorOptions opts = new SnapshotExecutorOptions();
        opts.setUri(this.options.getSnapshotUri());
        opts.setFsmCaller(this.fsmCaller);
        opts.setNode(this);
        opts.setLogManager(this.logManager);
        opts.setAddr(this.serverId != null ? this.serverId.getEndpoint() : null);
        opts.setInitTerm(this.currTerm);
        opts.setFilterBeforeCopyRemote(this.options.isFilterBeforeCopyRemote());
        opts.setSnapshotThrottle(this.options.getSnapshotThrottle());
        return this.snapshotExecutor.init(opts);
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/30
     * @Description:初始化日志组件的方法
     */
    private boolean initLogStorage() {
        //创建日志存储器
        this.logStorage = this.serviceFactory.createLogStorage(this.options.getLogUri(), this.raftOptions);
        //创建日志管理器，日志管理器内部持有者日志存储器
        this.logManager = new LogManagerImpl();
        //创建封装日志管理器需要参数的对象
        final LogManagerOptions opts = new LogManagerOptions();
        //下面都是设置一些信息的操作了
        opts.setGroupId(this.groupId);
        //设置日志编解码工厂
        opts.setLogEntryCodecFactory(this.serviceFactory.createLogEntryCodecFactory());
        opts.setLogStorage(this.logStorage);
        //设置集群配置管理器
        opts.setConfigurationManager(this.configManager);
        opts.setNodeMetrics(this.metrics);
        opts.setRaftOptions(this.raftOptions);
        //在这里初始化日志管理器
        return this.logManager.init(opts);
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/17
     * @方法描述：初始化状态机组件的方法
     */
    private boolean initFSMCaller(final LogId bootstrapId) {
        if (this.fsmCaller == null) {
            LOG.error("Fail to init fsm caller, null instance, bootstrapId={}.", bootstrapId);
            return false;
        }//创建状态机需要的回调队列，这个队列中封装着需要和业务层面沟通的回调方法
        this.closureQueue = new ClosureQueueImpl(this.groupId);
        //创建状态机初始化需要的参数配置对象
        final FSMCallerOptions opts = new FSMCallerOptions();
        //这行代码暂时注释掉，等12版本再讲解这行代码
        //opts.setAfterShutdown(status -> afterShutdown());
        opts.setLogManager(this.logManager);
        //这一步很关键，就是把用户定义的真正的状态机设置到参数对象中了
        //注意，这里的状态机是从options中获得的，因为在启动程序的时候，用户需要把状态机设置到
        //options对象中
        opts.setFsm(this.options.getFsm());
        opts.setClosureQueue(this.closureQueue);
        opts.setNode(this);
        opts.setBootstrapId(bootstrapId);
        //设置状态机的Disruptor环形数组的容量大小
        opts.setDisruptorBufferSize(this.raftOptions.getDisruptorBufferSize());
        //初始化状态机组件，这里我还是再多解释一句，虽然我把fsmCaller直接称为状态机组件
        //其实，是这个组件对用户定义的真正的状态机做了一层包装，确切地说，是fsmCaller内部
        //持有了用户自己定义的状态机，当有日志要被应用到状态机时，会通过fsmCaller调用方法
        //然后进一步把要应用的日志应用到用户定义的状态机中
        return this.fsmCaller.init(opts);
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:初始化元数据存储器的方法，这个方法很重要
     */
    private boolean initMetaStorage() {
        //通过服务工厂创建元数据存储器，这里把用户配置的元数据文件的本地路径传递给元数据存储器了
        this.metaStorage = this.serviceFactory.createRaftMetaStorage(this.options.getRaftMetaUri(), this.raftOptions);
        //创建RaftMetaStorageOptions对象，这个对象是专门给元数据存储器使用的，从名字上就可以看出来
        //封装了元数据存储器需要的配置参数
        RaftMetaStorageOptions opts = new RaftMetaStorageOptions();
        //把当前节点设置到opts中
        opts.setNode(this);
        //在这里真正初始化了元数据存储器，初始化的过程中，会把元数据本地文件中的任期和为谁投票这些数据加载到内存中
        //就赋值给元数据存储器对象中的两个对应成员变量，初始化失败则记录日志
        if (!this.metaStorage.init(opts)) {
            LOG.error("Node {} init meta storage failed, uri={}.", this.serverId, this.options.getRaftMetaUri());
            return false;
        }
        //给当前节点的任期赋值
        this.currTerm = this.metaStorage.getTerm();
        //得到当前节点的投票记录，这里的copy在功能上就相当于一个深拷贝方法
        this.votedId = this.metaStorage.getVotedFor().copy();
        return true;
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/19
     * @方法描述：快照生成定时器要执行的方法
     */
    private void handleSnapshotTimeout() {
        this.writeLock.lock();
        try {
            if (!this.state.isActive()) {
                return;
            }
        } finally {
            this.writeLock.unlock();
        }//在另一个线程中执行生成快照的操作，这么做是为了避免快照定时器的线程阻塞
        //快照定时器使用的可是时间轮线程，这个线程可不能阻塞，否则其他定时任务就不能及时执行了
        //这里的回调方法为null
        ThreadPoolsFactory.runInThread(this.groupId, () -> doSnapshot(null, false));
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/19
     * @方法描述：执行快照生成的入口方法
     */
    private void doSnapshot(final Closure done, boolean sync) {
        if (this.snapshotExecutor != null) {
            //判断是同步生成还是异步生成
            if (sync) {//同步生成
                this.snapshotExecutor.doSnapshotSync(done);
            } else {//这里就是异步生成
                this.snapshotExecutor.doSnapshot(done);
            }
        } else {
            if (done != null) {
                final Status status = new Status(RaftError.EINVAL, "Snapshot is not supported");
                ThreadPoolsFactory.runClosureInThread(this.groupId, done, status);
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:超时选举处理处理方法，这个方法就是超时选举定时器要调用的方法
     */
    private void handleElectionTimeout() {
        //设计一个是否释放锁的标志
        boolean doUnlock = true;
        //获得写锁，这里获得写锁是因为下面进行的一系列操作中对某些属性进行了状态或者值的变更
        //比如节点状态，也就是state，还有领导者节点是谁，也就是this.leaderId，这些成员变量的值都会在投票完成后进行变更，还有任期的值也会改变
        //但是在整个框架中，不止有超时选举定时器的线程会对这两个成员变量进行操作，还有另外的线程会对这些属性进行读取和变更
        //比如当前节点刚刚成为候选者，但是在整个集群中有另外的节点已经成为候选者，并且发送过来索要投票的消息了
        //如果这时候当前节点正在把票投给自己，因为当前节点也正在发起选举，肯定会先投自己一票，但是另外的节点发送索要投票的消息过来了
        //另外的线程在处理索要投票的消息时，肯定会判断当前节点是否已经投过票了，这时候一个线程正在修改投票记录，一个线程正在读取投票记录
        //很容易发生并发问题，所以需要使用写锁来保证并发安全，谁获得到了写锁，谁才能访问对应的属性
        //这里我还要再解释一下，jraft内部的通信也是依靠Netty来实现的，集群内部各个节点都是使用Netty的功能进行远程通信的
        //只不过是在Netty之上封装了一层，变成了一个bolt框架，关于这个框架的知识大家可以自己去查一查，简单看一看，说实话，我之前也没接触过这个框架
        //但是看懂代码还是不成问题的，既然底层使用的是Netty，IO逻辑使用的是Netty自己的单线程执行器，那么业务逻辑就要使用另外定义的线程池了
        //就是因为有这个业务线程池，所以才会出现上面我为大家分析的情况
        //从我刚才的分析中也能看到，为什么要把这个读锁在这里就加上
        this.writeLock.lock();
        try {
            //判断当前节点的状态，如果当前节点不是跟随着的状态，就不能进行投票选举
            //因为候选者是从跟随着转换过去的，连跟随者都不是，怎么转换成候选者呢？
            if (this.state != State.STATE_FOLLOWER) {
                return;
            }
            //这里的这个判断非常重要，超时选举定时器启动之后，只要节点状态是跟随着，就会一直工作
            //每过一段时间就会触发一次，但是触发了并不意味着就要真的去进行选举，会在这里进行判断
            //看看距离当前节点最后一次收到领导者信息过去的多少时间，如果这个时间超过了超时选举时间，才会进入预投票阶段
            //否则直接退出，这里返回true就代表还没超时呢
            if (isCurrentLeaderValid()) {
                return;
            }
            //既然是要超时选举了，说明领导者已经没有了，在这里对当前节点存放的领导者信息进行充值
            resetLeaderId(PeerId.emptyPeer(), new Status(RaftError.ERAFTTIMEDOUT, "Lost connection from leader %s.",
                    this.leaderId));
            //设置释放锁标识为false，这里设置成false是为了不在finally块中释放写锁
            //因为写锁一旦走到这里，就意味着要进行预投票了，会进入下面的preVote方法
            //写锁会在下面这个方法内释放
            doUnlock = false;
            //开始预投票，注意，只要进行预投票，就意味着这时候还持有者写锁
            //这一点要记清楚
            preVote();
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }

    //返回一个随机超时时间的方法，超时选举定时器会调用该方法。这里大家应该能想到，在raft协议中，为了避免多个节点同时成为候选者
    //所以会让超时选举时间都错开一些，也就是每个节点的超时选举时间都不相同，就是通过下面的方法实现的
    private int randomTimeout(final int timeoutMs) {
        return ThreadLocalRandom.current().nextInt(timeoutMs, timeoutMs + this.raftOptions.getMaxElectionDelayMs());
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:这个方法是用来返回心跳超时时间的，所谓心跳超时时间，其实就是领导者要给跟随者发送心跳消息的时间间隔
     * 当一个节点成功当选领导者之后，经过这个超时时间之后时间领导者就向跟随着发送心跳消息，这个心跳超时时间一点要比选举超时时间小
     * 否则不就触发选举了吗，这个方法的具体被调用的时机就在复制器对象中，也就是Replicator类的start方法中，具体逻辑可以去start方法中查看
     */
    private int heartbeatTimeout(final int electionTimeout) {
        return Math.max(electionTimeout / this.raftOptions.getElectionHeartbeatFactor(), 10);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:本类最最核心的方法，初始化节点的方法，jraft框架中的所有组件都是在这个方法中初始化的，但是我们这是第一版本代码
     * 所以初始化的组件非常少，只有集群内部使用的客户端服务初始化了，还有元数据存储器初始化了
     */
    @Override
    public boolean init(final NodeOptions opts) {
        //对Node节点需要的数据进行非空校验
        Requires.requireNonNull(opts, "Null node options");
        Requires.requireNonNull(opts.getRaftOptions(), "Null raft options");
        Requires.requireNonNull(opts.getServiceFactory(), "Null jraft service factory");
        //得到了DefaultJRaftServiceFactory对象，这个就是提供组件服务的工厂，在第一版本中只提供了元数据存储器组件服务
        //日志和快照服务都没有提供
        this.serviceFactory = opts.getServiceFactory();
        this.options = opts;
        this.raftOptions = opts.getRaftOptions();
        //默认不启用节点性能监控功能
        this.metrics = new NodeMetrics(opts.isEnableMetrics());
        //设置当前节点选举优先级
        this.serverId.setPriority(opts.getElectionPriority());
        //校验一下IP地址不为空
        if (this.serverId.getIp().equals(Utils.IP_ANY)) {
            LOG.error("Node can't started from IP_ANY.");
            return false;
        }//在这里校验当前节点的IP地址和端口号是否已经添加到节点管理器中了
        //如果没有添加进去则记录错误日志，这里大家可以去看一下测试类，当测试类启动时
        //节点的网络地址已经在RaftGroupService对象的start方法中被添加到节点管理器中了
        //所以程序执行到这里时，肯定会返回true，也就不会进入下面的分支了
        if (!NodeManager.getInstance().serverExists(this.serverId.getEndpoint())) {
            LOG.error("No RPC server attached to, did you forget to call addService?");
            return false;
        }//下面这个if分支是用来得到专门用于处理心跳消息，日志复制的RPC请求的回调方法的执行器组
        //恕我直言，这个执行器组中每一个执行器的工作很复杂，对于学习第一版本的你们，水有点深，你们可能把握不住
        //所以，为大家引入日志复制之后，再详细讲解这个执行器组的功能吧
        if (this.options.getAppendEntriesExecutors() == null) {
            //如果上面得到的执行器组为空，就从工具类中获得一个，这个执行器组会交给集群内部节点通信的客户端服务对象使用
            //下面的代码就会展现具体逻辑
            this.options.setAppendEntriesExecutors(Utils.getDefaultAppendEntriesExecutor());
        }
        //创建一个全局的定时任务管理器，在第一版本，这个定时任务管理器只负责启动领导者向跟随者发送心跳消息的任务
        this.timerManager = TIMER_FACTORY.getRaftScheduler(this.options.isSharedTimerPool(),
                this.options.getTimerPoolSize(), "JRaft-Node-ScheduleThreadPool");
        //缓存NodeId对象的toString方法结果
        final String suffix = getNodeId().toString();
        //下面我删减了很多代码，大家简单看看就行，在源码中快照服务，状态机，日志组件都会初始化，但在第一版本中我全删除了
        String name = "JRaft-VoteTimer-" + suffix;
        name = "JRaft-ElectionTimer-" + suffix;
        //在这里创建了一个超时选举定时器，this.options.getElectionTimeoutMs()得到的就是超时选举时间
        this.electionTimer = new RepeatedTimer(name, this.options.getElectionTimeoutMs(),
                TIMER_FACTORY.getElectionTimer(this.options.isSharedElectionTimer(), name)) {
            //这个方法实现的是RepeatedTimer类中的方法
            @Override
            protected void onTrigger() {
                //这是超时选举定时器中最核心的方法，就是在该方法中，开始进行超时选举任务了
                handleElectionTimeout();
            }

            @Override
            protected int adjustTimeout(final int timeoutMs) {
                //在一定范围内返回一个随机的时间，这就意味着每个节点的超时选举时间是不同的
                //否则多个节点同时成为候选者，很可能选举失败
                return randomTimeout(timeoutMs);
            }
        };
        name = "JRaft-SnapshotTimer-" + suffix;
        //创建快照生成定时器
        this.snapshotTimer = new RepeatedTimer(name, this.options.getSnapshotIntervalSecs() * 1000,
                TIMER_FACTORY.getSnapshotTimer(this.options.isSharedSnapshotTimer(), name)) {

            private volatile boolean firstSchedule = true;

            @Override
            protected void onTrigger() {
                handleSnapshotTimeout();
            }

            @Override
            protected int adjustTimeout(final int timeoutMs) {
                if (!this.firstSchedule) {
                    return timeoutMs;
                }
                this.firstSchedule = false;
                if (timeoutMs > 0) {
                    int half = timeoutMs / 2;
                    return half + ThreadLocalRandom.current().nextInt(half);
                } else {
                    return timeoutMs;
                }
            }
        };
        //创建配置管理器
        this.configManager = new ConfigurationManager();
        //初始化一个disruptor队列，这里采用的是多生产者模式
        this.applyDisruptor = DisruptorBuilder.<LogEntryAndClosure> newInstance()
                //设置disruptor大小，默认16384
                .setRingBufferSize(this.raftOptions.getDisruptorBufferSize())
                //设置事件工厂
                .setEventFactory(new LogEntryAndClosureFactory())
                //设置线程工厂
                .setThreadFactory(new NamedThreadFactory("JRaft-NodeImpl-Disruptor-", true))
                //采用多生产者模式，也就是并发向环形队列中填充数据
                .setProducerType(ProducerType.MULTI)
                //阻塞策略
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        //设置消费者处理器，使用的还是批处理器，因为这里只创建了一个LogEntryAndClosureHandler消费处理器
        //这个消费者处理器会被disruptor框架内不得BatchEventProcessor批处理器持有，然后开始处理任务
        //如果还有朋友不熟悉Disruptor框架，我强烈建议先去把Disruptor框架学习一下，否则你不明白日志处理的回调逻辑
        this.applyDisruptor.handleEventsWith(new LogEntryAndClosureHandler());
        //设置异常处理器
        this.applyDisruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(getClass().getSimpleName()));
        //启动disruptor框架的消费者，启动之后消费者会阻塞，因为现在生产者还没有向环形数组中生产数据呢
        this.applyQueue = this.applyDisruptor.start();
        //创建状态机管理器
        this.fsmCaller = new FSMCallerImpl();
        //初始化日志存储组件
        if (!initLogStorage()) {
            LOG.error("Node {} initLogStorage failed.", getNodeId());
            return false;
        }
        //初始化元数据存储器组件
        if (!initMetaStorage()) {
            LOG.error("Node {} initMetaStorage failed.", getNodeId());
            return false;
        }//初始化状态机组件
        if (!initFSMCaller(new LogId(0, 0))) {
            LOG.error("Node {} initFSMCaller failed.", getNodeId());
            return false;
        }
        //初始化投票箱
        this.ballotBox = new BallotBox();
        //设置投票箱需要的配置参数对象
        final BallotBoxOptions ballotBoxOpts = new BallotBoxOptions();
        //把状态机和状态机回调队列设置到投票箱的配置参数对象中
        ballotBoxOpts.setWaiter(this.fsmCaller);
        //这里把状态机的回调队列也交给投票箱使用了
        ballotBoxOpts.setClosureQueue(this.closureQueue);
        //初始化ballotBox的属性
        if (!this.ballotBox.init(ballotBoxOpts)) {
            LOG.error("Node {} init ballotBox failed.", getNodeId());
            return false;
        }//初始化快照组件
        if (!initSnapshotStorage()) {
            LOG.error("Node {} initSnapshotStorage failed.", getNodeId());
            return false;
        }
        //检验日志索引一致性，这里说的索引一致性先不用关注，等引入了日志快照之后，我会为大家详细讲解这个校验日志索引的方法
        //在第二版本，大家只需要知道，如果一个raft集群中的一个节点还没有生成日志快照时，默认第一条日志的索引是1
        //这里就是判断当前节点的日志索引是不是1
        final Status st = this.logManager.checkConsistency();
        if (!st.isOk()) {
            LOG.error("Node {} is initialized with inconsistent log, status={}.", getNodeId(), st);
            return false;
        }
        //创建一个配置对象，该对象会封装一个集群的配置信息
        this.conf = new ConfigurationEntry();
        this.conf.setId(new LogId());
        this.conf.setConf(this.options.getInitialConf());
        if (!this.conf.isEmpty()) {
            Requires.requireTrue(this.conf.isValid(), "Invalid conf: %s", this.conf);
        } else {
            LOG.info("Init node {} with empty conf.", this.serverId);
        }
        //下面就到了初始化集群内部RPC组件的时候了
        //先创建一个复制器组对象
        this.replicatorGroup = new ReplicatorGroupImpl();
        //初始化RPC客户端服务，服务端在Test类中就创建了，当前节点的客户端就在这里初始化
        //创建提供客户端服务的对象，并且把上面创建的执行器组交给客户端使用
        this.rpcService = new DefaultRaftClientService(this.replicatorGroup, this.options.getAppendEntriesExecutors());
        //创建一个复制器组需要的配置参数对象
        final ReplicatorGroupOptions rgOpts = new ReplicatorGroupOptions();
        //设置心跳超时时间，也就是当一个节点成为领导者之后，经过这个时间之后，就要立刻向它的跟随者发送心跳消息了
        rgOpts.setHeartbeatTimeoutMs(heartbeatTimeout(this.options.getElectionTimeoutMs()));
        //设置超时选举时间
        rgOpts.setElectionTimeoutMs(this.options.getElectionTimeoutMs());
        //设置日志管理器
        rgOpts.setLogManager(this.logManager);
        //设置投票箱
        rgOpts.setBallotBox(this.ballotBox);
        //下面这几个配置就很简单了，大家看看是哪些对象被设置进去就行
        rgOpts.setNode(this);
        rgOpts.setRaftRpcClientService(this.rpcService);
        //设置快照存储器
        rgOpts.setSnapshotStorage(this.snapshotExecutor != null ? this.snapshotExecutor.getSnapshotStorage() : null);
        rgOpts.setRaftOptions(this.raftOptions);
        //设置全局的定时任务管理器
        rgOpts.setTimerManager(this.timerManager);
        //设置性能监控对象
        this.options.setMetricRegistry(this.metrics.getMetricRegistry());
        //在这里初始化客户端服务组件，初始化的过程中，创建了一个业务线程池来处理业务逻辑
        //毕竟Netty的单线程执行器只负责处理IO逻辑
        if (!this.rpcService.init(this.options)) {
            LOG.error("Fail to init rpc service.");
            return false;
        }//初始化复制器组，这个初始化就没做什么特别的工作了，就是调用了一些set方法，为其内部的成员变量赋值了
        this.replicatorGroup.init(new NodeId(this.groupId, this.serverId), rgOpts);
        //把当前节点的状态设置为跟随者，节点刚启动的时候就是跟随者的状态
        this.state = State.STATE_FOLLOWER;
        if (LOG.isInfoEnabled()) {
            LOG.info("Node {} init, term={}, lastLogId={}, conf={}, oldConf={}.", getNodeId(), this.currTerm,
                    this.logManager.getLastLogId(false), this.conf.getConf(), this.conf.getOldConf());
        }
        //在这里启动了快照生成定时器
        if (this.snapshotExecutor != null && this.options.getSnapshotIntervalSecs() > 0) {
            LOG.debug("Node {} start snapshot timer, term={}.", getNodeId(), this.currTerm);
            this.snapshotTimer.start();
        }
        //这里会判断一下配置信息是否为空，现在当然不可能为空
        //因为上面已经初始化好了，false取反就会进入下面的分之，最重要的一个操作就来了
        if (!this.conf.isEmpty()) {
            //新启动的节点需要进行选举
            //就是在这里，当前节点开始进行选举任务，具体逻辑就在下面这个方法中
            //在下面这个方法中，会启动刚才创建好的超时选举定时器，在定时器任务中执行选举操作
            stepDown(this.currTerm, false, new Status());
        }
        //把当前节点添加到节点管理器中
        if (!NodeManager.getInstance().add(this)) {
            LOG.error("NodeManager add {} failed.", getNodeId());
            return false;
        }
        //获取写锁，因为下面可能要直接进行选举任务
        this.writeLock.lock();
        //走到这里会判断一下当前配置信息对象中是否只有一个节点，如果只有一个节点并且就是当前节点，那就要直接让当前节点成为领导者，所以下面就开始直接进行正式选举了
        if (this.conf.isStable() && this.conf.getConf().size() == 1 && this.conf.getConf().contains(this.serverId)) {
            //该方法就是正式进入选举的方法，注意，这里并没有经过预投票机制
            electSelf();
        } else {
            this.writeLock.unlock();
        }
        return true;
    }



    @Override
    public NodeMetrics getNodeMetrics() {
        return this.metrics;
    }

    //该方法暂时不做实现
    @Override
    public void shutdown() {

    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:正式发起选取的方法，在正式发起选举的方法中，就有些限制了，最关键的就是集群中的每一个raft节点只允许投一票
     * 该限制的具体实现逻辑就在本方法中，并且该方法的大部分逻辑和预投票的逻辑差不多
     */
    private void electSelf() {
        //定义一个局部变量，用来得到旧的任期
        long oldTerm;
        try {
            //记录开始正式投票的日志
            LOG.info("Node {} start vote and grant vote self, term={}.", getNodeId(), this.currTerm);
            //根据当前节点的ID去配置信息中查看当前节点是为集群中的节点，如果不是集群中的节点就直接退出
            if (!this.conf.contains(this.serverId)) {
                LOG.warn("Node {} can't do electSelf as it is not in {}.", getNodeId(), this.conf);
                return;
            }
            //如果当前节点目前是跟随者，就停止超时选举定时器工作，超时选举定时器是用来进行选举的，但是当前的节点正在进行选举工作
            //就没必要让这个定时器工作了，等选举工作结束之后再启动，如果选举迟迟没有结果，会有另一个定时器处理的，这个定时器在第一版本中并没有引入
            //后面再引入吧
            if (this.state == State.STATE_FOLLOWER) {
                LOG.debug("Node {} stop election timer, term={}.", getNodeId(), this.currTerm);
                //在这里停止超时选举定时器工作
                this.electionTimer.stop();
            }
            //既然正在选举，肯定是没有领导者了，所以在进行选举时把当前节点的领导者节点设置为空
            //这里的PeerId.emptyPeer()就是一个无效的节点ID，会被赋值给当前节点的领导者ID
            resetLeaderId(PeerId.emptyPeer(), new Status(RaftError.ERAFTTIMEDOUT,
                    "A follower's leader_id is reset to NULL as it begins to request_vote."));
            //将当前节点状态更改为候选者
            this.state = State.STATE_CANDIDATE;
            //递增当前节点的任期
            this.currTerm++;
            //当前节点要为自己投票，所以把为谁投票的ID设置为自己的服务器ID
            this.votedId = this.serverId.copy();
            //这里还会记录一条日志，表示要启动投票超时计时器了
            LOG.debug("Node {} start vote timer, term={} .", getNodeId(), this.currTerm);
            //初始化投票计数器，这里初始化的是正式投票的计数器
            this.voteCtx.init(this.conf.getConf(), this.conf.isStable() ? null : this.conf.getOldConf());
            //得到旧的任期
            oldTerm = this.currTerm;
        } finally {
            //释放写锁，这里的finally其实也是为了上面的那个if分之兜底，如果进入上面的if分之后，就直接退出该方法了，所以要释放写锁
            this.writeLock.unlock();
        }
        //这里重构了，引入了日志组件，所以就从日志管理器中获得当前节点最后一个日志的ID
        final LogId lastLogId = this.logManager.getLastLogId(true);
        //再次获得写锁
        this.writeLock.lock();
        try {
            //再次对任期进行判断，这个原因已经在预投票方法中讲解过了
            if (oldTerm != this.currTerm) {
                LOG.warn("Node {} raise term {} when getLastLogId.", getNodeId(), this.currTerm);
                return;
            }
            //遍历配置中的节点
            for (final PeerId peer : this.conf.listPeers()) {
                //如果发现这个节点就是当前节点，就跳过这次玄幻
                if (peer.equals(this.serverId)) {
                    continue;
                }
                //连接遍历到的这个节点，如果不能成功连接，就记录日志
                if (!this.rpcService.connect(peer.getEndpoint())) {
                    LOG.warn("Node {} channel init failed, address={}.", getNodeId(), peer.getEndpoint());
                    continue;
                }
                //创建一个回调对象，这个对象中实现了一个回调函数，该回调函数会在接收到正式投票请求的响应后被毁掉
                final OnRequestVoteRpcDone done = new OnRequestVoteRpcDone(peer, this.currTerm, this);
                //创建正式投票请求
                done.request = RpcRequests.RequestVoteRequest.newBuilder()
                        //这里设置成false，意味着不是预投票请求
                        .setPreVote(false)
                        .setGroupId(this.groupId)
                        .setServerId(this.serverId.toString())
                        .setPeerId(peer.toString())
                        .setTerm(this.currTerm)
                        .setLastLogIndex(lastLogId.getIndex())
                        .setLastLogTerm(lastLogId.getTerm())
                        .build();
                //发送请求
                this.rpcService.requestVote(peer.getEndpoint(), done.request, done);
            }
            //因为当前节点正在申请成为领导者，任期和投票记录有变更了，所以需要持久化一下
            //这里投票记录就是当前节点自己
            this.metaStorage.setTermAndVotedFor(this.currTerm, this.serverId);
            //在这里给当前节点投票
            this.voteCtx.grant(this.serverId);
            //如果获取的投票已经超过半数，则让当前节点成为领导者
            if (this.voteCtx.isGranted()) {
                //这个就是让当前节点成为领导者的方法
                becomeLeader();
            }
        } finally {
            //释放写锁
            this.writeLock.unlock();
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:这个方法会将当前节点记录的领导者信息重置
     */
    private void resetLeaderId(final PeerId newLeaderId, final Status status) {
        //判断当前传进来的节点是否为空
        if (newLeaderId.isEmpty()) {
            //如果为空，可能意味着正要进行选举操作，进行选举操作则意味着
            //集群失去了领导者，所以把当前节点记录的领导者信息设置为空即可
            this.leaderId = PeerId.emptyPeer();
        } else {
            //如果走到这里说明传进该方法中的节点并不为空，这可能是集群已经选举出了新的领导者
            //所以把当前节点记录的领导者信息设置为新的节点
            this.leaderId = newLeaderId.copy();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:根据传进该方法中的参数，对当前节点的身份进行变更，说的直接一点就是身份降级为跟随者并且在第一版本中，该方法只会在handleAppendEntriesRequest方法中被调用
     * 而handleAppendEntriesRequest方法是处理来自领导者的信息的，不管来自领导者信息是日志复制还是心跳，总是这条信息肯定是来自领导者
     * 这也就意味着集群中有了新的领导者
     */
    private void checkStepDown(final long requestTerm, final PeerId serverId) {
        //该对象用来封装当前节点身份变更的重要信息
        final Status status = new Status();
        if (requestTerm > this.currTerm) {
            //如果传进来的任期大于当前节点的任期，那么当前节点就要变成跟随者
            status.setError(RaftError.ENEWLEADER, "Raft node receives message from new leader with higher term.");
            //就在下面的方法中，将当前节点的状态设置为跟随者
            stepDown(requestTerm, false, status);
            //如果走到下面的两个分支，不管是哪一个，都意味着领导者的任期和当前节点相等
        } else if (this.state != State.STATE_FOLLOWER) {
            //如果走到这里，意味着当前节点是候选者节点，并且收到了和自己任期相同的领导者的信息
            status.setError(RaftError.ENEWLEADER, "Candidate receives message from new leader with the same term.");
            //让自己降级为跟随者，集群中有多个候选者时可能会出现这种情况
            stepDown(requestTerm, false, status);
        } else if (this.leaderId.isEmpty()) {
            //走到这里说明当前节点是跟随者，并且自己记录的领导者信息为空，但现在收到到了领导者发送过来的信息
            //说明领导者是刚刚选举出来，这时候就仍然让自己保持跟随者状态即可
            status.setError(RaftError.ENEWLEADER, "Follower receives message from new leader with the same term.");
            stepDown(requestTerm, false, status);
        }//走到这里就把当前节点记录的领导者信息复制为新领导者的信息
        if (this.leaderId == null || this.leaderId.isEmpty()) {
            resetLeaderId(serverId, status);
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:当前节点成功获得超过集群半数节点的投票后，就会调用该方法，让自己成为领导者
     */
    private void becomeLeader() {
        //判断当前节点状态是否为候选者，领导者的节点状态只能从候选者变更过来
        Requires.requireTrue(this.state == State.STATE_CANDIDATE, "Illegal state: " + this.state);
        //当前节点正在成为领导者，记录这条日志
        LOG.info("Node {} become leader of group, term={}, conf={}, oldConf={}.", getNodeId(), this.currTerm,
                this.conf.getConf(), this.conf.getOldConf());
        //将当前节点状态设置为领导者
        this.state = State.STATE_LEADER;
        //设置领导者ID为当前服务器ID，也就是说领导者就是自己
        this.leaderId = this.serverId.copy();
        //重置raft复制组的term，这行代码的意义在第一版本还体现不出来，后面日志复制的时候就体现出来了
        //领导者一旦确定了，其他节点的信息都会封装到下面这个ReplicatorGroup复制器组对象中
        //领导者的日志都会通过复制组发送给每一个跟随着节点，跟随者节点就会封装复制组器中的一个个复制器对象中
        //Replicator类的对象就是复制器对象，这个后面版本代码会详细讲解的
        this.replicatorGroup.resetTerm(this.currTerm);
        //遍历配置信息中的节点
        for (final PeerId peer : this.conf.listPeers()) {
            //如果遍历到的节点就是当前节点，就直接跳过这次循环
            if (peer.equals(this.serverId)) {
                continue;
            }
            //接下来要向复制器组中添加复制器对象了，记录一条日志
            LOG.debug("Node {} add a replicator, term={}, peer={}.", getNodeId(), this.currTerm, peer);
            //如果添加复制器失败，记录错误日志
            //这里有一点非常重要，在下面这个addReplicator方法中，跟随着节点就会被包装成一个个复制器对象
            //然后把每一个复制器对象添加到这个复制组器对象中，并且在下面这个方法内部，还会启动心跳定时器，以及发送探针请求
            //这时候，领导者就会向集群中的所有跟随者发送信息了
            if (!this.replicatorGroup.addReplicator(peer)) {
                LOG.error("Fail to add a replicator, peer={}.", peer);
            }
        }
        //下面是和学习者有关的操作，暂时不用关注
        for (final PeerId peer : this.conf.listLearners()) {
            LOG.debug("Node {} add a learner replicator, term={}, peer={}.", getNodeId(), this.currTerm, peer);
            //这里的ReplicatorType.Learner枚举类对象表示了当前包装为复制器的节点身份为学习者
            if (!this.replicatorGroup.addReplicator(peer, ReplicatorType.Learner)) {
                LOG.error("Fail to add a learner replicator, peer={}.", peer);
            }
        }//这里的这行代码非常重要，设置投票箱要提交的下一条日志的索引
        this.ballotBox.resetPendingIndex(this.logManager.getLastLogIndex() + 1);
        if (this.confCtx.isBusy()) {
            throw new IllegalStateException();
        }
        //刷新配置
        this.confCtx.flush(this.conf.getConf(), this.conf.getOldConf());
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:检测当前节点是否需要进行身份降级的操作，学习这个方法时候，不要只看这个方法，而要看看
     * 这个方法会在哪些方法中被调用，结合外层方法一起学习该方法的功能和逻辑，否则你虽然看懂了这个方法的每一行代码
     * 但根本不明白每一行代码背后的具体作用，当然这个方法我也做了大量删减，导致方法参数中的status几乎没什么作用，这个参数需要和状态机打交道
     * 但现在还没有引入状态机
     */
    private void stepDown(final long term, final boolean wakeupCandidate, final Status status) {
        LOG.debug("Node {} stepDown, term={}, newTerm={}, wakeupCandidate={}.", getNodeId(), this.currTerm, term,
                wakeupCandidate);
        //检查当前节点的状态是否活跃。其实就是看看是否正在关闭或者有没有异常
        if (!this.state.isActive()) {
            return;
        }
        //这里我删减了很多代码
        //重置当前节点的领导者信息
        resetLeaderId(PeerId.emptyPeer(), status);
        //将当前节点的状态变更为跟随者
        this.state = State.STATE_FOLLOWER;
        //跟新最后一次收到领导者信息的时间，这里更新这个时间主要是因为stepDown方法会在
        //多个和投票选举有关的方法中被调用，既然正在投票选举，肯定就不能让超时选举定时器工作
        //所以每次都要更新这个时间，这样超时选举定时器就不会真正开始进行选主工作了
        updateLastLeaderTimestamp(Utils.monotonicMs());
        //如果方法参数中的任期必当前节点的任期大，这时候就意味着当前节点正在进行选举工作
        //这个stepDown方法可能正在被handleRequestVoteRequest方法调用
        if (term > this.currTerm) {
            //修改当前节点任期
            this.currTerm = term;
            //这里先把当前节点的投票记录设置为空，因为到了一个新的任期了，一个任期只能投一票
            //但是这个新的任期中还没有投呢，所以先设置为空，在外层的handleRequestVoteRequest方法中会投票
            //然后给当前节点的投票记录复制
            this.votedId = PeerId.emptyPeer();
            //持久化元数据信息
            this.metaStorage.setTermAndVotedFor(term, this.votedId);
        }
        //如果当前节点不是学习者，那就启动当前节点的超时选举定时器
        if (!isLearner()) {
            //启动超时选举定时器，走到这里，就意味着当前的stepDown方法，也可能是在NodeImpl类的init方法中被调用的
            //大家可以仔细研究研究，一定要理清楚是哪个外层方法在调用当前的stepDown方法
            //当然即便不是init方法中调用的，也需要重新启动超时选举定时器，因为该节点马上就要投完票了，这意味着新的领导者即将产生
            //如果没产生，那么就根据超时时间继续进行新一轮选举，这正是超时选举定时器的工作
            this.electionTimer.restart();
        } else {
            LOG.info("Node {} is a learner, election timer is not started.", this.nodeId);
        }
    }

    //判断当前节点是不是一个学习者
    private boolean isLearner() {
        return this.conf.listLearners().contains(this.serverId);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:该方法就是专门用来处理预投票请求的，当前节点接收到另一个节点预投票的请求时，就会通过这个方法来处理
     * 这个方法的参数就是集群中的候选者节点发送过来的RPC预投票请求
     */
    @Override
    public Message handlePreVoteRequest(final RpcRequests.RequestVoteRequest request) {
        //定义一个是否释放写锁的标志
        boolean doUnlock = true;
        //获取写锁
        this.writeLock.lock();
        try {
            //检查当前节点是否处于活跃状态
            if (!this.state.isActive()) {
                LOG.warn("Node {} is not in active state, currTerm={}.", getNodeId(), this.currTerm);
                return RpcFactoryHelper.responseFactory()
                        .newResponse(RpcRequests.RequestVoteResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Node %s is not in active state, state %s.", getNodeId(), this.state.name());
            }
            //创建一个PeerId对象，这个对象是用来解析候选者节点信息的
            final PeerId candidateId = new PeerId();
            //先从请求中获取候选者节点信息，也就是发送预投票请求的节点信息
            if (!candidateId.parse(request.getServerId())) {
                LOG.warn("Node {} received PreVoteRequest from {} serverId bad format.", getNodeId(),
                        request.getServerId());
                //解析失败则发送失败响应
                return RpcFactoryHelper.responseFactory()
                        .newResponse(RpcRequests.RequestVoteResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Parse candidateId failed: %s.", request.getServerId());
            }
            //定义一个投票变量，false表示当前节点还没有给候选者投票
            boolean granted = false;
            do {
                //现在就进入循环了，但是这里我想强调一下，这个循环是个伪循环，因为while中的条件为false，意味着这个循环只循环一次
                //这里定义这个循环只是为了能使用berak，以此来处理不同的情况
                //首先先判断一下候选者节点是否在当前的集群配置中
                if (!this.conf.contains(candidateId)) {
                    LOG.warn("Node {} ignore PreVoteRequest from {} as it is not in conf <{}>.", getNodeId(),
                            request.getServerId(), this.conf);
                    //不在则退出循环，不必往下进行了
                    break;
                }
                //这里又进行了一步判断，先判断当前节点记录的领导者信息是否为空，如果不为空说明在当前节点看来领导者还在呢，然后
                //又判断了一下最后一次接收领导者信息是否超过选举超时时间了，如果没超过，当前节点记录的领导者显然还是有效领导者，这种情况也忽略候选者节点的预投票请求
                //出现这种情况，可能是因为网络延迟，有的跟随者没有及时接收到领导者的心跳信息，因此就转变成候选者开始进行预投票活动了
                if (this.leaderId != null && !this.leaderId.isEmpty() && isCurrentLeaderValid()) {
                    LOG.info(
                            "Node {} ignore PreVoteRequest from {}, term={}, currTerm={}, because the leader {}'s lease is still valid.",
                            getNodeId(), request.getServerId(), request.getTerm(), this.currTerm, this.leaderId);
                    break;
                }
                //判断候选者节点的任期是否小于当前节点任期，小于的话也忽略预投票请求
                if (request.getTerm() < this.currTerm) {
                    LOG.info("Node {} ignore PreVoteRequest from {}, term={}, currTerm={}.", getNodeId(),
                            request.getServerId(), request.getTerm(), this.currTerm);
                    //这个方法在第一版本不用关注，因为我根本就没做实现
                    checkReplicator(candidateId);
                    break;
                }
                //可以看到，这里又进行了一次复制器检查，其实在这个方法内部会判断当前接收到预投票请求的节点是不是领导者
                //如果是领导者，并且领导者并没有出故障，所以就根据候选者节点信息去复制器组中检查一下，如果复制器组中没有与候选者节点
                //对应的复制器对象，就为这个候选者节点创建一个复制器节点，创建完毕后，也会给候选者发送心跳消息，让候选者重新成为跟随者
                checkReplicator(candidateId);
                //释放写锁，这里忽然释放写锁了，我感觉没必要释放写锁
                //但是在this.logManager.getLastLogId(true)这个方法中可能会产生阻塞
                //所以把写锁释放，让其他线程可以获得写锁
                doUnlock = false;
                this.writeLock.unlock();
                //获取当前节点的最后一条日志的ID
                final LogId lastLogId = this.logManager.getLastLogId(true);
                //重新获得写锁
                doUnlock = true;
                this.writeLock.lock();
                //从请求中获得候选者节点的最后一条日志的ID
                final LogId requestLastLogId = new LogId(request.getLastLogIndex(), request.getLastLogTerm());
                //如果候选者节点的最后一条日志的ID和任期大于当前节点，当前节点就给候选者投票
                //预投票的步骤中并没有对当前节点做了只能投一次票的限制
                granted = requestLastLogId.compareTo(lastLogId) >= 0;
                LOG.info("Node {} received PreVoteRequest from {}, term={}, currTerm={}, granted={}, requestLastLogId={},lastLogId={}.",
                        getNodeId(), request.getServerId(), request.getTerm(), this.currTerm, granted, requestLastLogId,lastLogId);
                //直接退出循环
            } while (false);
            //构建给候选者节点返回的响应
            return RpcRequests.RequestVoteResponse.newBuilder()
                    .setTerm(this.currTerm)
                    //这里参数为true，意味着当前节点给候选者投票了
                    .setGranted(granted)
                    .build();
        } finally {
            //根据释放锁标志判断是否释放写锁
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:当接收到领导者发送过来的信息时，就调用这个方法，更新当前节点最后一次接收到领导者信息的时间
     * 当然，在stepDown方法中也会调用该方法，原因也为大家解释过了
     */
    private void updateLastLeaderTimestamp(final long lastLeaderTimestamp) {
        this.lastLeaderTimestamp = lastLeaderTimestamp;
    }

    //根据给订的PeerId去复制器组中检查有没有对应的复制器对象
    private void checkReplicator(final PeerId candidateId) {
        if (this.state == State.STATE_LEADER) {
            this.replicatorGroup.checkReplicator(candidateId, false);
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:当接收到候选者节点发送过来的正式所要投票的请求时，就会调用这个方法处理，正式投票的请求
     * 就是该方法中的参数对象
     */
    @Override
    public Message handleRequestVoteRequest(final RpcRequests.RequestVoteRequest request) {
        //定义一个是否释放写锁的标志
        boolean doUnlock = true;
        //获取写锁
        this.writeLock.lock();
        try {
            //检查当前节点是否处于活跃状态
            if (!this.state.isActive()) {
                LOG.warn("Node {} is not in active state, currTerm={}.", getNodeId(), this.currTerm);
                return RpcFactoryHelper.responseFactory()
                        .newResponse(RpcRequests.RequestVoteResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Node %s is not in active state, state %s.", getNodeId(), this.state.name());
            }
            //创建一个PeerId对象，这个对象是用来解析候选者节点信息的
            final PeerId candidateId = new PeerId();
            //解析候选者节点信息
            if (!candidateId.parse(request.getServerId())) {
                LOG.warn("Node {} received RequestVoteRequest from {} serverId bad format.", getNodeId(),
                        request.getServerId());
                return RpcFactoryHelper.responseFactory()
                        .newResponse(RpcRequests.RequestVoteResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Parse candidateId failed: %s.", request.getServerId());
            }
            do {//进入伪循环
                //首先判断候选者的任期是否大于等于当前节点的任期
                if (request.getTerm() >= this.currTerm) {
                    //记录日志
                    LOG.info("Node {} received RequestVoteRequest from {}, term={}, currTerm={}.", getNodeId(),
                            request.getServerId(), request.getTerm(), this.currTerm);
                    //如果候选者节点任期较大，责让当前节点身份降级，成为跟随者
                    //这里也能进一步想到，如果候选者节点任期和当前节点相等，那就不会让当前节点身份降级
                    //很可能一个集群中同时有两个候选者呀，任期也一样，谁也不必改变谁的，只等着领导者选出来
                    //让领导者给候选者发送信息，让候选者身份降级就行
                    //当然，当前节点假如是候选者的话，就不会给发送请求过来的候选者投票了
                    //因为只能投一票，这一票已经投给自己了
                    if (request.getTerm() > this.currTerm) {
                        //这里调用了stepDown方法，stepDown方法的逻辑已经讲解过了，就不再细说了
                        stepDown(request.getTerm(), false, new Status(RaftError.EHIGHERTERMRESPONSE,
                                "Raft node receives higher term RequestVoteRequest."));
                    }
                } else {
                    //走到这里意味着候选者任期小于当前节点，直接忽略该请求即可
                    LOG.info("Node {} ignore RequestVoteRequest from {}, term={}, currTerm={}.", getNodeId(),
                            request.getServerId(), request.getTerm(), this.currTerm);
                    break;
                }
                doUnlock = false;
                //释放写锁
                this.writeLock.unlock();
                //获取当前节点最后一条日志
                final LogId lastLogId = this.logManager.getLastLogId(true);
                doUnlock = true;
                //获取写锁
                this.writeLock.lock();
                //判断当前节点的任期是否和之前相等，防止在释放锁的期间，已经有领导者产生了，并且修改了当前节点的任期
                if (request.getTerm() != this.currTerm) {
                    //不一致则直接退出循环
                    LOG.warn("Node {} raise term {} when get lastLogId.", getNodeId(), this.currTerm);
                    break;
                }
                //校验候选者节点的日志是否比当前节点最后一条日志新
                final boolean logIsOk = new LogId(request.getLastLogIndex(), request.getLastLogTerm())
                        .compareTo(lastLogId) >= 0;
                //这里的true也是写死的，本来是上面的哪个logIsOk变量，如果为true，并且当前节点还没有投过票
                //那就给候选者投票。这里写死的代码后面版本都会重构
                if (logIsOk && (this.votedId == null || this.votedId.isEmpty())) {
                    //在这里把当前节点的状态改为跟随者
                    stepDown(request.getTerm(), false, new Status(RaftError.EVOTEFORCANDIDATE,
                            "Raft node votes for some candidate, step down to restart election_timer."));
                    //记录当前节点的投票信息
                    this.votedId = candidateId.copy();
                    //元数据信息持久化
                    this.metaStorage.setVotedFor(candidateId);
                }
            } while (false);
            //这里构建响应对象的时候，会调用setGranted方法，这个方法的参数是一个bool，也就是是否投票的意思
            //如果经过上面的一系列操作，发现候选者节点的任期和当前节点任期相等了，并且候选者节点的信息和当前节点的投票记录
            //也相等了，给setGranted方法传递的参数就为true，也就代表着投票成功
            return RpcRequests.RequestVoteResponse.newBuilder()
                    .setTerm(this.currTerm)
                    .setGranted(request.getTerm() == this.currTerm && candidateId.equals(this.votedId))
                    .build();
        } finally {
            //释放写锁的操作
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }






    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/12
     * @Description:该类对象封装的回调方法非常重要，用于跟随者节点向领导者回复日志投票的响应，以及将被提交的日志应用到状态机的操作
     * 都在该对象封装的回调方法中实现
     */
    private static class FollowerStableClosure extends LogManager.StableClosure {
        //已经提交了的日志的索引吗
        final long committedIndex;
        //用来构建响应
        final RpcRequests.AppendEntriesResponse.Builder responseBuilder;
        //当前跟随者节点本身
        final NodeImpl node;
        //封装的回调方法，注意，这个回调方法是从AppendEntriesRequestProcessor的processRequest0方法中一路传递过来的
        //回调方法的逻辑就是给领导者回复响应
        final RpcRequestClosure done;
        //跟随者当前的任期
        final long term;

        //构造方法
        public FollowerStableClosure(final RpcRequests.AppendEntriesRequest request,
                                     final RpcRequests.AppendEntriesResponse.Builder responseBuilder, final NodeImpl node,
                                     final RpcRequestClosure done, final long term) {
            super(null);
            //这行代码也许会给一些朋友造成困惑，为什么要让领导者已经提交的最新的日志索引和领导者发送过来的前一条日志索引加上本批次日志条目数量做对比
            //在正常情况下，比如说，领导者上一次发送了日志索引为1-10的一批日志，发送完了之后收到了跟随者的过半投票
            //于是领导者就把1-10的日志提交了，并且把已提交的日志索引设置为10了，发送的前一条日志索引也为10，因为下一条日志是从11开始的呀
            //现在，11-20的这批日志发送给跟随者了，在发送日志的请求中，设置了已提交的最新的日志索引为10
            //这时候在跟随者这边，在下面这行代码中，request.getCommittedIndex()肯定是小于request.getPrevLogIndex() + request.getEntriesCount()
            //这是正常情况下的逻辑，但是，假如当前的跟随者和领导者发送过网络分区呢？大家一定要理清楚，领导者的CommittedIndex是相对自己的来说的，不管有几个跟随者
            //只要领导者的每一条日志收到过半投票，CommittedIndex都会按照顺序增加，可是领导者要发送给每一个跟随者的nextIndex和prevLogIndex可能是不同的
            //或按照每个跟随者各自的进度来赋值，如果当前跟随者发生过网络分区，故障恢复后，当前跟随者希望接收的下一条日志索引是1，领导者就会从索引为1的日志开始向
            //当前的跟随者发送日志，假如说就发过来索引为1-5的这几条日志，但是现在领导者已经提交的日志最新索引为10了
            //这时候，就要对比一下这两个值，然后当前跟随者发现自己直接收到5条日志，能提交的日志的索引最多也只是5，所以就会把committedIndex设置为后者的值
            this.committedIndex = Math.min(request.getCommittedIndex(), request.getPrevLogIndex() + request.getEntriesCount());
            this.responseBuilder = responseBuilder;
            this.node = node;
            this.done = done;
            //把日志落盘之前的任期赋值了
            this.term = term;
        }

        //这个就是当日志落盘成功后要被回调的方法
        @Override
        public void run(final Status status) {
            //对状态码进行判断
            if (!status.isOk()) {
                this.done.run(status);
                return;
            }
            this.node.readLock.lock();
            try {//这里判断了一些当前跟随者的任期和日志落盘之前的任期
                //这个this.term 就是跟随者日志落盘之前的任期
                if (this.term != this.node.currTerm) {
                    //如果任期不一致了，说明在跟随者进行日志落盘的过程中，集群内发生过领导者变更，肯定重新选举，提高了任期，选出了新的领导者
                    //所以当前跟随者的任期才会发生变化，如果是这样的话，就不应该回复旧的领导者日志落盘成功的响应了
                    //这里向回复给旧领导者中响应中设置了新的任期，这个任期肯定大于旧领导者信息了
                    //这样旧的领导者就可以进行身份降级了
                    this.responseBuilder.setSuccess(false).setTerm(this.node.currTerm);
                    //回复响应
                    this.done.sendResponse(this.responseBuilder.build());
                    return;
                }
            } finally {
                this.node.readLock.unlock();
            }
            //设置响应成功，设置任期
            this.responseBuilder.setSuccess(true).setTerm(this.term);
            //给跟随者节点设置可以提交的最新的日志索引，在投票箱中就会把日志应用到状态机上了
            //但在第五版本还没引入状态机，所以在投票箱中，我把状态机的代码注释了
            //这里是跟随者向自己的投票箱提交了从领导者发送过来的已提交的最大索引的日志
            //这就意味着在跟随者节点上可以把这个索引日志和之前的日志都应用到自己的状态机上了
            this.node.ballotBox.setLastCommittedIndex(this.committedIndex);
            //在这里发送响应，同时打印一条日志在跟随者节点落盘成功
            System.out.println("日志在跟随者节点接收成功！！！！！！！！");
            this.done.sendResponse(this.responseBuilder.build());
        }
    }





    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:当前节点接收到领导者发送过来的消息时，不管消息是日志复制还是心跳消息，还是探针消息，都会调用该方法进行处理
     * 换句话说，其实领导者要发送的日志复制消息，以及心跳消息探针消息，使用的都是同一个RPC请求，也就是该方法中的第一个参数对象
     */
    @Override
    public Message handleAppendEntriesRequest(final RpcRequests.AppendEntriesRequest request, final RpcRequestClosure done) {
        //这些重复逻辑就不添加注释了
        boolean doUnlock = true;
        this.writeLock.lock();
        //得到操作开始时间
        final long startMs = Utils.monotonicMs();
        //从请求中获得领导者传送过来的日志的条数
        final int entriesCount = request.getEntriesCount();
        //定义一个变量，标志日志处理是否成功
        boolean success = false;
        try {//判断当前节点是否活跃
            if (!this.state.isActive()) {
                LOG.warn("Node {} is not in active state, currTerm={}.", getNodeId(), this.currTerm);
                return RpcFactoryHelper
                        .responseFactory()
                        .newResponse(RpcRequests.AppendEntriesResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Node %s is not in active state, state %s.", getNodeId(), this.state.name());
            }//解析领导者节点的信息，这个serverId封装的就是领导者的信息
            final PeerId serverId = new PeerId();
            if (!serverId.parse(request.getServerId())) {
                LOG.warn("Node {} received AppendEntriesRequest from {} serverId bad format.", getNodeId(),
                        request.getServerId());
                return RpcFactoryHelper
                        .responseFactory()
                        .newResponse(RpcRequests.AppendEntriesResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Parse serverId failed: %s.", request.getServerId());
            }
            //如果领导者的任期小于当前节点的任期，则直接忽略这条请求
            //这种情况也不是没可能出现，如果出现网络分区了，分区故障解决之后，另一个分区的领导者任期很大
            //把当前节点的任期修改了，修改的比当前节点原来的领导者的任期还要大，就会出现这种情况
            if (request.getTerm() < this.currTerm) {
                LOG.warn("Node {} ignore stale AppendEntriesRequest from {}, term={}, currTerm={}.", getNodeId(),
                        request.getServerId(), request.getTerm(), this.currTerm);
                return RpcRequests.AppendEntriesResponse.newBuilder()
                        .setSuccess(false)
                        .setTerm(this.currTerm)
                        .build();
            }
            //更具领导者的任期来判断当前节点是否需要进行身份降级，也就是变更身份为跟随者
            //在下面这个方法中很可能会把当前节点的领导者id设置为请求中传递过来的节点的id
            checkStepDown(request.getTerm(), serverId);
            //这里判断了一个情况，就是当前节点有自己的领导者记录，并且领导者记录和
            //当前发送消息过来的领导者节点并不相同
            //很可能就是发生了网络分区，分区故障恢复后，另一个分区的领导者发送消息过来了
            if (!serverId.equals(this.leaderId)) {
                LOG.error("Another peer {} declares that it is the leader at term {} which was occupied by leader {}.",
                        serverId, this.currTerm, this.leaderId);
                //这里给请求中的任期加1，是为了提高当前节点的任期，然后进行身份降级
                //ELEADERCONFLICT为领导者冲突错误
                stepDown(request.getTerm() + 1, false, new Status(RaftError.ELEADERCONFLICT,
                        "More than one leader in the same term."));
                //在返回的响应中也给任期加1，这样发送心跳过来的领导者就会在接收到响应后对自己的身份降级
                //如果是另一个领导者发送消息过来，当前节点的任期已经增加1了，那么另一个领导者在接收到响应后也会进行身份降级
                //集群中没有领导者之后，很快就会触发超时选举了，选举出新的领导者，这就是解决脑裂问题的大概逻辑
                return RpcRequests.AppendEntriesResponse.newBuilder()
                        .setSuccess(false)
                        .setTerm(request.getTerm() + 1)
                        .build();
            }
            //更新最后一次接收到领导者消息的时间
            updateLastLeaderTimestamp(Utils.monotonicMs());
            //获得请求中的前一条日志索引，注意，这里的请求肯定是来自领导者
            //领导者请求中的前一条日志，其实就是领导者在发送新的日志前的最后一条日志
            //如果这条日志的索引和当前跟随者节点的最后一条日志的索引相同的话
            //这就意味着可以继续向下判断，如果任期也相同的话，这就意味着领导者和跟随者的日志是同步完整的
            final long prevLogIndex = request.getPrevLogIndex();
            //获得领导者节点的前一条日志的任期
            final long prevLogTerm = request.getPrevLogTerm();
            //获得当前跟随者节点的最后一条日志的任期，这里是根据领导者请求中的前一条日志索引获得的
            //这里其实就是对这个索引进行判断了
            final long localPrevLogTerm = this.logManager.getTerm(prevLogIndex);
            //接下来就是具体判断了，先判断当前节点的最后一条日志的任期是否和领导者请求中的任期不想等
            //不相等就说明日志不匹配，可能跟随者的日志落后领导者，领导者发送过来的前一条日志，跟随者根本没有
            //也可能是跟随者的本地日志有一些是旧的领导者的，当前发送消息过来的是新的领导者
            //新的领导者的日志可能最新的日志还没有当前跟随者节点多，但是最新日志的任期比当前跟随者节点的日志的大
            //这也是一种日志不匹配的情况，这样一来，显然就需要领导者那边递减自己要发送给当前跟随者的下一条日志的索引
            //直到日志对应的任期匹配了，这样才能开始传输日志
            if (localPrevLogTerm != prevLogTerm) {
                final long lastLogIndex = this.logManager.getLastLogIndex();
                LOG.warn(
                        "Node {} reject term_unmatched AppendEntriesRequest from {}, term={}, prevLogIndex={}, prevLogTerm={}, localPrevLogTerm={}, lastLogIndex={}, entriesSize={}.",
                        getNodeId(), request.getServerId(), request.getTerm(), prevLogIndex, prevLogTerm, localPrevLogTerm,
                        lastLogIndex, entriesCount);
                return RpcRequests.AppendEntriesResponse.newBuilder()
                        .setSuccess(false)
                        .setTerm(this.currTerm)
                        .setLastLogIndex(lastLogIndex)
                        .build();
            }
            //这是我自己添加的代码
            LOG.info("FOLLOWER接收到了心跳消息！");
            //如果请求中的日志条目为0，说明当前的请求是心跳或者是探针消息
            if (entriesCount == 0) {
                //在处理探针消息的逻辑中也删除了一些和日志有干的代码，后面会加上
                //创建响应对象
                final RpcRequests.AppendEntriesResponse.Builder respBuilder = RpcRequests.AppendEntriesResponse.newBuilder()
                        .setSuccess(true)
                        .setTerm(this.currTerm)
                        .setLastLogIndex(this.logManager.getLastLogIndex());
                doUnlock = false;
                this.writeLock.unlock();
                //在这里把跟随者节点要提交的最后一条日志索引赋值
                this.ballotBox.setLastCommittedIndex(Math.min(request.getCommittedIndex(), prevLogIndex));
                return respBuilder.build();
            }//下面把处理日志操作的逻辑全部加行了
            //快速检查日志组件是否超负荷了，所谓超负荷就是判断日志管理器中的环形数组是否还有足够的空间放下这一批要存放的日志
            //方法参数是1，意味着只要环形数组还有一个位置能让生产者发布数据，就不算超负荷
            if (!this.logManager.hasAvailableCapacityToAppendEntries(1)) {
                //日志组件超负荷则记录日志
                LOG.warn("Node {} received AppendEntriesRequest but log manager is busy.", getNodeId());
                //返回响应，响应的错误状态码为忙碌
                return RpcFactoryHelper
                        .responseFactory()
                        .newResponse(RpcRequests.AppendEntriesResponse.getDefaultInstance(), RaftError.EBUSY,
                                "Node %s:%s log manager is busy.", this.groupId, this.serverId);
            }
            //走到这里意味着日志组件可以正常工作，就下来就应该把从领导者传输过来的日志在跟随者进行落盘了
            //得到从领导者传过来的前一条日志的索引
            long index = prevLogIndex;
            //创建存放日志条目对象的集合，这个集合会交给日志管理器组件，日志管理器组件会将这个集合中的日志
            //进行落盘
            final List<LogEntry> entries = new ArrayList<>(entriesCount);
            //这里的逻辑和跟随者发送日志时，对日志做的包装的逻辑类戏，只不过这里是将日志从包装中取出来
            ByteBuffer allData = null;
            //把日志从请求中取出来，然后全部交给allData对象
            if (request.hasData()) {
                allData = request.getData().asReadOnlyByteBuffer();
            }
            //获取这一次发送过来的这批日志的元数据的集合，这个应该还有印象吧
            final List<RaftOutter.EntryMeta> entriesList = request.getEntriesList();
            //下面就是具体从请求中解析出每一条日志的逻辑
            for (int i = 0; i < entriesCount; i++) {
                //这里就得到了要解析的日志的每一条索引，index本来是领导者传递过来的前一条日志的索引
                //自增之后，就可以当作传输过来的第一条日志的索引了
                index++;
                //从元数据集合中获取当前正在遍历的这个日志条目的元数据
                final RaftOutter.EntryMeta entry = entriesList.get(i);
                //这里就是根据元数据和上面的allData对象，将索引为index的日志条目对象完整解析出来了
                final LogEntry logEntry = logEntryFromMeta(index, allData, entry);
                //判断刚才解析出来的日志条目对象是否为空
                if (logEntry != null) {
                    //如果不为空，则在这里根据校验和判断一下日志是否有损坏
                    if (this.raftOptions.isEnableLogEntryChecksum() && logEntry.isCorrupted()) {
                        //如果损坏了，就计算一下现在的校验和
                        long realChecksum = logEntry.checksum();
                        //记录错误日志
                        LOG.error(
                                "Corrupted log entry received from leader, index={}, term={}, expectedChecksum={}, realChecksum={}",
                                logEntry.getId().getIndex(), logEntry.getId().getTerm(), logEntry.getChecksum(),
                                realChecksum);
                        //给领导者返回影响，响应的错误状态码为无效参数
                        return RpcFactoryHelper
                                .responseFactory()
                                .newResponse(RpcRequests.AppendEntriesResponse.getDefaultInstance(), RaftError.EINVAL,
                                        "The log entry is corrupted, index=%d, term=%d, expectedChecksum=%d, realChecksum=%d",
                                        logEntry.getId().getIndex(), logEntry.getId().getTerm(), logEntry.getChecksum(),
                                        realChecksum);
                    }
                    //把解析出来的日志条目放到日志条目集合中
                    entries.add(logEntry);
                }
            }
            //接下来就是将日志落盘的具体逻辑了，但是，这里请大家注意一下，现在是跟随者节点要把日志落盘，日志落盘成功后，肯定要向领导者回复落盘成功的消息
            //所谓落盘成功其实就是返回一个true，也就代表着跟随者节点给这条日志投票了，这个逻辑就在下面创建的封装回调方法的对象中实现了
            //当然，还有一个最重要的逻辑，那就是跟随者根据领导者传递过来的committedIndex，也就是已经可以应用到状态机的日志的索引，将对应日志应用到状态机
            //这个逻辑也在FollowerStableClosure封装的回调方法中实现了
            final FollowerStableClosure closure = new FollowerStableClosure(request, RpcRequests.AppendEntriesResponse.newBuilder()
                    .setTerm(this.currTerm), this, done, this.currTerm);
            //在这里把存放了日志条目的集合交给日志管理器去落盘了
            this.logManager.appendEntries(entries, closure);
            //这行代码暂且注释掉，没有配置变更
            //checkAndSetConfiguration(true);
            //操作成功
            success = true;
            //这里返回null，就和AppendEntriesRequestProcessor处理器的processRequest0方法接应上了
            //因为日志落盘是一个复杂的操作，肯定不能同步等待落盘成功的响应，所以这里直接返回null，让日志落盘异步执行
            //然后异步返回响应，返回响应的逻辑就在上面创建的FollowerStableClosure对象封装的回调方法中
            return null;
        } finally {
            //下面都是一些监控性能的操作了，简单看看就行
            if (doUnlock) {
                this.writeLock.unlock();
            }
            //得到操作耗时
            final long processLatency = Utils.monotonicMs() - startMs;
            //日志数量为0，意味着处理的请求是心跳或者探针请求，记录处理请求的耗时
            if (entriesCount == 0) {
                this.metrics.recordLatency("handle-heartbeat-requests", processLatency);
            } else {
                //这里就是处理日志请求的耗时
                this.metrics.recordLatency("handle-append-entries", processLatency);
            }
            if (success) {
                //操作成功，就记录本批次处理的日志数量，这里也只是针对日志请求
                this.metrics.recordSize("handle-append-entries-count", entriesCount);
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/12
     * @Description:解析出一个完整日志条目的方法
     */
    private LogEntry logEntryFromMeta(final long index, final ByteBuffer allData, final RaftOutter.EntryMeta entry) {
        //从存放了日志元信息的对象中判断日志条目类型，如果类型不是未知的，就继续向下执行
        if (entry.getType() != EnumOutter.EntryType.ENTRY_TYPE_UNKNOWN) {
            //创建一个日志条目对象，用于存放下面解析出来的信息
            final LogEntry logEntry = new LogEntry();
            //创建日志ID，日志的索引和日志任期都已经可以获得了
            //然后把日志ID设置到刚刚创建出来的logEntry对象中
            logEntry.setId(new LogId(index, entry.getTerm()));
            //设置日志类型
            logEntry.setType(entry.getType());
            //设置日志校验和
            if (entry.hasChecksum()) {
                logEntry.setChecksum(entry.getChecksum());
            }//从元数据对象中获取日志有效信息的字节长度
            final long dataLen = entry.getDataLen();
            //如果字节长度大于0
            if (dataLen > 0) {
                //从allData中获得真正的日志有效信息
                final byte[] bs = new byte[(int) dataLen];
                assert allData != null;
                allData.get(bs, 0, bs.length);
                //把日志有效信息，也就是真正的业务执行设置到日志条目对象中
                logEntry.setData(ByteBuffer.wrap(bs));
            }//接下来就是判断有没有集群节点信息了
            if (entry.getPeersCount() > 0) {
                //如果有集群节点信息，那就判断日志类型是不是配置变更类型
                //如果不是则报错，由此可见，只有配置变更类型的日志才会有集群节点信息
                if (entry.getType() != EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                    throw new IllegalStateException(
                            "Invalid log entry that contains peers but is not ENTRY_TYPE_CONFIGURATION type: "
                                    + entry.getType());
                }//如果日志类型就是集群配置变更类型，就在下面这个方法中把集群节点信息填充到日志条目对象中
                fillLogEntryPeers(entry, logEntry);
            } else if (entry.getType() == EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                //走到这里意味着当前日志条目类型为配置变更类型，但是该日志中并没有集群节点信息，所以也会直接报错
                throw new IllegalStateException(
                        "Invalid log entry that contains zero peers but is ENTRY_TYPE_CONFIGURATION type");
            }
            return logEntry;
        }
        return null;
    }


    //这个方法就是把集群节点信息，从元数据对象中解析出来，然后设置到日志条目对象中
    //逻辑非常简单，就不再添加注释了
    private void fillLogEntryPeers(final RaftOutter.EntryMeta entry, final LogEntry logEntry) {
        // TODO refactor
        if (entry.getPeersCount() > 0) {
            final List<PeerId> peers = new ArrayList<>(entry.getPeersCount());
            for (final String peerStr : entry.getPeersList()) {
                final PeerId peer = new PeerId();
                peer.parse(peerStr);
                peers.add(peer);
            }
            logEntry.setPeers(peers);
        }
        if (entry.getOldPeersCount() > 0) {
            final List<PeerId> oldPeers = new ArrayList<>(entry.getOldPeersCount());
            for (final String peerStr : entry.getOldPeersList()) {
                final PeerId peer = new PeerId();
                peer.parse(peerStr);
                oldPeers.add(peer);
            }
            logEntry.setOldPeers(oldPeers);
        }
        if (entry.getLearnersCount() > 0) {
            final List<PeerId> peers = new ArrayList<>(entry.getLearnersCount());
            for (final String peerStr : entry.getLearnersList()) {
                final PeerId peer = new PeerId();
                peer.parse(peerStr);
                peers.add(peer);
            }
            logEntry.setLearners(peers);
        }
        if (entry.getOldLearnersCount() > 0) {
            final List<PeerId> peers = new ArrayList<>(entry.getOldLearnersCount());
            for (final String peerStr : entry.getOldLearnersList()) {
                final PeerId peer = new PeerId();
                peer.parse(peerStr);
                peers.add(peer);
            }
            logEntry.setOldLearners(peers);
        }
    }






    //下面这几个方法逻辑比较简单，就不添加注释了
    @Override
    public PeerId getLeaderId() {
        this.readLock.lock();
        try {
            return this.leaderId.isEmpty() ? null : this.leaderId;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public String getGroupId() {
        return this.groupId;
    }

    public PeerId getServerId() {
        return this.serverId;
    }

    @Override
    public NodeId getNodeId() {
        if (this.nodeId == null) {
            this.nodeId = new NodeId(this.groupId, this.serverId);
        }
        return this.nodeId;
    }

    public RaftClientService getRpcService() {
        return this.rpcService;
    }

    public void onError(final RaftException error) {
        LOG.warn("Node {} got error: {}.", getNodeId(), error);
        this.writeLock.lock();
        try {
            if (this.state.compareTo(State.STATE_FOLLOWER) <= 0) {
                stepDown(this.currTerm, this.state == State.STATE_LEADER, new Status(RaftError.EBADNODE,
                        "Raft node(leader or candidate) is in error."));
            }
            if (this.state.compareTo(State.STATE_ERROR) < 0) {
                this.state = State.STATE_ERROR;
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:如果当前节点的身份是候选者，发送了索要投票的请求给其他节点后，收到其他节点的响应后就会调用这个方法
     * 处理响应，这个方法中term参数代表的是当前候选者节点发送索要投票请求之前的任期，这个逻辑可以从OnRequestVoteRpcDone类的run方法中查看
     */
    public void handleRequestVoteResponse(final PeerId peerId, final long term, final RpcRequests.RequestVoteResponse response) {
        this.writeLock.lock();
        try {
            //当前候选者节点接收到响应之后，发现自己的身份不是候选者了，就直接退出该方法
            //也许在接收到投票响应之前已经收到了集群中选举出来的领导者的信息，这样就会把自己的身份降级为跟随者了
            if (this.state != State.STATE_CANDIDATE) {
                LOG.warn("Node {} received invalid RequestVoteResponse from {}, state not in STATE_CANDIDATE but {}.",
                        getNodeId(), peerId, this.state);
                return;
            }
            //判断当前候选者节点的任期是否发生了变化，这里的这个term代表的是当前候选者节点发送索要投票请求之前的任期
            //而在发送索要投票请求之后，收到响应之前，很可能任期就被新的领导者改变了，如果前后任期不一致，也直接退出该方法
            if (term != this.currTerm) {
                LOG.warn("Node {} received stale RequestVoteResponse from {}, term={}, currTerm={}.", getNodeId(),
                        peerId, term, this.currTerm);
                return;
            }
            //如果接收到响应后发现请求中的任期，也就是回复响应的节点的任期比自己大
            //也直接退出该方法，并且让自己成为跟随者
            if (response.getTerm() > this.currTerm) {
                LOG.warn("Node {} received invalid RequestVoteResponse from {}, term={}, expect={}.", getNodeId(),
                        peerId, response.getTerm(), this.currTerm);
                //身份降级
                stepDown(response.getTerm(), false, new Status(RaftError.EHIGHERTERMRESPONSE,
                        "Raft node receives higher term request_vote_response."));
                return;
            }
            //从请求中判断是否收到了投票
            if (response.getGranted()) {
                //如果收到了投票就把来自peerId节点的投票收集到计票器中
                this.voteCtx.grant(peerId);
                //使用投票计算器计算当前节点收到的投票是否超过了集群中半数节点
                if (this.voteCtx.isGranted()) {
                    //超过了就让当前节点成为领导者
                    becomeLeader();
                }
            }
        } finally {
            //释放写锁
            this.writeLock.unlock();
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:这个类的对象中封装着一个回调方法，该方法会在候选者节点接收到正式投票的响应之后被回调
     */
    private class OnRequestVoteRpcDone extends RpcResponseClosureAdapter<RpcRequests.RequestVoteResponse> {

        final long         startMs;
        final PeerId       peer;
        final long         term;
        final NodeImpl     node;
        RpcRequests.RequestVoteRequest request;

        public OnRequestVoteRpcDone(final PeerId peer, final long term, final NodeImpl node) {
            super();
            this.startMs = Utils.monotonicMs();
            this.peer = peer;
            this.term = term;
            this.node = node;
        }

        @Override
        public void run(final Status status) {
            NodeImpl.this.metrics.recordLatency("request-vote", Utils.monotonicMs() - this.startMs);
            if (!status.isOk()) {
                LOG.warn("Node {} RequestVote to {} error: {}.", this.node.getNodeId(), this.peer, status);
            } else {
                this.node.handleRequestVoteResponse(this.peer, this.term, getResponse());
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:如果当前节点向其他节点发送了索要预投票的请求，收到其他节点的响应后就会调用这个方法
     * 处理响应，这个方法中term参数代表的是当前候选者节点发送索要投票请求之前的任期，这个逻辑可以从OnPreVoteRpcDone类的run方法中查看
     */
    public void handlePreVoteResponse(final PeerId peerId, final long term, final RpcRequests.RequestVoteResponse response) {
        boolean doUnlock = true;
        this.writeLock.lock();
        try {
            //如果当前节点的身份不再是跟随者，就直接退出该方法
            //这行代码会在什么情况下出现呢？发起预投票的节点在收到预投票之后不是跟随者了？
            //我想不到，这个节点不可能成为候选者，只有进行正式投票时才会将自己身份转换为候选者
            //但是进行正式投票的入口方法在该方法中呀，还没有执行到呢，这个节点更不可能成为领导者了
            //除非是有网络延迟，这个节点已经成为领导者或者候选者了，它之前发送给某个节点的预投票请求才刚刚收到响应
            if (this.state != State.STATE_FOLLOWER) {
                LOG.warn("Node {} received invalid PreVoteResponse from {}, state not in STATE_FOLLOWER but {}.",
                        getNodeId(), peerId, this.state);
                return;
            }
            //判断当前节点的前后任期是否一致，不一致则直接退出该方法
            if (term != this.currTerm) {
                LOG.warn("Node {} received invalid PreVoteResponse from {}, term={}, currTerm={}.", getNodeId(),
                        peerId, term, this.currTerm);
                return;
            }
            //如果接收到响应后发现请求中的任期，也就是回复响应的节点的任期比自己大
            //也直接退出该方法，并且让自己成为跟随者，处理预请求响应，正常情况下，没有发生分区故障的话，响应的节点应该当前发起预请求的任期一样
            if (response.getTerm() > this.currTerm) {
                LOG.warn("Node {} received invalid PreVoteResponse from {}, term {}, expect={}.", getNodeId(), peerId,
                        response.getTerm(), this.currTerm);
                stepDown(response.getTerm(), false, new Status(RaftError.EHIGHERTERMRESPONSE,
                        "Raft node receives higher term pre_vote_response."));
                return;
            }
            LOG.info("Node {} received PreVoteResponse from {}, term={}, granted={}.", getNodeId(), peerId,
                    response.getTerm(), response.getGranted());
            //从响应中判断回复响应的节点是否给当前节点投票了
            if (response.getGranted()) {
                //如果投票了，就在这里收集票数到计票器中
                this.prevVoteCtx.grant(peerId);
                //如果获得票数超过集群节点一半，就进入正式选举阶段
                if (this.prevVoteCtx.isGranted()) {
                    //这里更改一下释放锁标志，不在该方法的finally块中释放写锁了
                    //因为下面的electSelf方法内部还需要写锁
                    doUnlock = false;
                    //开始正式投票
                    electSelf();
                }
            }
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:这个类的对象中封装着一个回调方法，该方法会在候选者节点接收到预投票的响应之后被回调
     */
    private class OnPreVoteRpcDone extends RpcResponseClosureAdapter<RpcRequests.RequestVoteResponse> {

        final long         startMs;
        final PeerId       peer;
        final long         term;
        RpcRequests.RequestVoteRequest request;

        public OnPreVoteRpcDone(final PeerId peer, final long term) {
            super();
            this.startMs = Utils.monotonicMs();
            this.peer = peer;
            this.term = term;
        }

        @Override
        public void run(final Status status) {
            NodeImpl.this.metrics.recordLatency("pre-vote", Utils.monotonicMs() - this.startMs);
            if (!status.isOk()) {
                LOG.warn("Node {} PreVote to {} error: {}.", getNodeId(), this.peer, status);
            } else {
                handlePreVoteResponse(this.peer, this.term, getResponse());
            }
        }
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:预投票的方法，raft协议中好像触发了超时选举任务时，就会直接进入投票选举阶段了
     * 但是在sofajraft框架中多引入了一个步骤，那就是预投票机制，所谓预投票机制就是在集群中的节点正式投票选主之前，先进行一次预投票，这个预投票和正式投票的逻辑没什么区别
     * 也是将当前节点的任期自增1，获取当前节点的最后一条日志的信息，然后根据这两条信息去和其他节点的信息做对比，只有任期比其他节点大，日志比其他节点新，其他节点才会给当前节点投票
     * 唯一的区别就是在预投票过程中没有限制一个节点只能投一票，最后只有当这个节点预投票通过了，也就是获得了整个集群中超过一半节点的投票
     * 这个节点才能正式开始向其他节点发起所要投票的请求。这么做是考虑到在一个集群中出现网络分区后，有些节点的任期无限制增大，等分区故障处理之后
     * 影响整个集群的工作。比如一个只有5个节点的集群，出现网络分区了，1个节点不能和其他四个节点通信了，而领导者在那4个节点之中，这样，其他4个节点还可以正常工作
     * 但是单独的1个节点因为无法接收到领导者的心跳，很快就会触发超时选举任务，如果没有预投票机制，那这个节点就会直接给自己的任期自增1，然后发起投票，但是它根本没办法和其他节点通信
     * 所以仍然会触发超时选举，于是任期就会无限自增，直到故障恢复，但是很可能故障恢复之后，这个节点的任期比集群中领导者的节点大的多，恢复通信后，根据任期大小显然就会让领导者下台了
     * 但是这个节点根本就没有日志数据，为了防止这种情况出现，所以搞了一个预投票机制，假如这个故障节点预投票失败了，就不会再进入正式选举，这里我要再强调一下，在进行预投票的时候
     * 会讲当前节点的任期自增1，这个其实并没有真的给当前节点的任期赋值，只是在传输消息中使用了自增的任期，这样就保证了，如果预投票失败，当前节点的任期并不会有任何变化
     * 只有正式投票，才会改变当前节点的任期
     */
    private void preVote() {
        //定义一个变量记录当前节点任期
        long oldTerm;
        try {
            //记录开始预投票的日志
            LOG.info("Node {} term {} start preVote.", getNodeId(), this.currTerm);
            //根据当前节点的ID去配置信息中查看当前节点是为集群中的节点，如果不是集群中的节点就直接退出
            if (!this.conf.contains(this.serverId)) {
                LOG.warn("Node {} can't do preVote as it is not in conf <{}>.", getNodeId(), this.conf);
                return;
            }
            //这里记录当前节点的任期
            oldTerm = this.currTerm;
        } finally {
            //这里有一个finally块主要是因为上面大代码中有一个if判断，如果判断成立就直接退出方法了，退出之前需要把写锁释放了，因为在上层方法的最后并没有释放写锁
            //注意，如果上面的if分之没有被执行，走到这里仍然会执行释放写锁的方法，这就意味着下面我们好要重新获得写锁
            //但是在释放写锁的这段时间中会发生什么事呢？假如集群中已经选举出了新的节点，并且已经把当前节点的任期值改变了怎么办呢？
            this.writeLock.unlock();
        }
        //获取最后一条日志ID
        final LogId lastLogId = this.logManager.getLastLogId(true);
        //设置释放写锁标志
        boolean doUnlock = true;
        //重新获取写锁
        this.writeLock.lock();
        try {
            //这里在获得写锁之后，重新对当前节点的任期进行了一次判断，看看当前节点的任期是否还和之前相等，因为很可能在释放写锁的间隙中，集群已经选举出新的节点了
            //当前节点的任期已经被改变了，这样一来，就不必让当前节点再进行预投票了，所以就直接退出了
            if (oldTerm != this.currTerm) {
                LOG.warn("Node {} raise term {} when get lastLogId.", getNodeId(), this.currTerm);
                return;
            }
            //初始化预投票计算器，当前节点收到的投票结果都会被prevVoteCtx这个成员变量收集，在该成员变量中计算
            //如果收到的预投票的票数大于一半，就会进入正式投票状态
            //这里初始化预投票计算器其实就是把配置类中的节点信息交给prevVoteCtx对象
            //这样这个prevVoteCtx预投票计算器就知道集群中有多少节点了，也知道得到的预投票的票数超过多少就可以进入正式投票状态了
            this.prevVoteCtx.init(this.conf.getConf(), this.conf.isStable() ? null : this.conf.getOldConf());
            //遍历配置中的节点
            for (final PeerId peer : this.conf.listPeers()) {
                //如果发现这个节点就是当前节点，就跳过这次玄幻
                if (peer.equals(this.serverId)) {
                    continue;
                }
                //连接遍历到的这个节点，如果不能成功连接，就记录日志
                if (!this.rpcService.connect(peer.getEndpoint())) {
                    LOG.warn("Node {} channel init failed, address={}.", getNodeId(), peer.getEndpoint());
                    continue;
                }
                //创建一个回调对象，这个对象中实现了一个回调函数，该回调函数会在接收到预投票请求的响应后被毁掉
                //构造方法中的peer代表要把消息送到的目标节点，第二个则是当前节点发送请求消息前的任期
                final OnPreVoteRpcDone done = new OnPreVoteRpcDone(peer, this.currTerm);
                //向被遍历到的这个节点发送一个预投票请求
                //下面就是构建预投票请求的内容
                done.request = RpcRequests.RequestVoteRequest.newBuilder()
                        //设置为true意味着是预投票请求
                        .setPreVote(true)
                        .setGroupId(this.groupId)
                        .setServerId(this.serverId.toString())
                        .setPeerId(peer.toString())
                        //在发送请求时发送的任期值为当前节点的任期值加1
                        .setTerm(this.currTerm + 1)
                        //当前节点最后一条日志的索引
                        .setLastLogIndex(lastLogId.getIndex())
                        //最后一条日志的任期
                        .setLastLogTerm(lastLogId.getTerm())
                        .build();
                //发送预投票请求
                this.rpcService.preVote(peer.getEndpoint(), done.request, done);
            }
            //给自己投一票
            this.prevVoteCtx.grant(this.serverId);
            //这里判断了一下，看是否得到了超过集群中半数节点的投票
            if (this.prevVoteCtx.isGranted()) {
                //超过了则不在本方法中释放写锁，而是直接开始进行正式投票
                doUnlock = false;
                //开始正式进行投票
                electSelf();
            }
        } finally {
            //这里根据是否释放写锁标志来判断是不是要释放写锁
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:向jraft框架中提交日志的方法，这些日志就是从业务从传递到核心算法层的，日志以Task的形式在
     * jraft内部传输，每一个Tast对象中都包装了一条日志，当然，这个日志已经被包装成ByteBuffer了
     */
    @Override
    public void apply(final Task task) {
        Requires.requireNonNull(task, "Null task");
        //创建日志对象
        final LogEntry entry = new LogEntry();
        //把业务层传来的指令包装成一个日志条目，到这里一个业务指令就变成一条日志了
        //只不过现在的日志还没有索引，任期等等
        entry.setData(task.getData());
        //创建一个Disruptor框架的生产者需要使用的数据传输器，这个数据传输器的作用就是把日志条目对象
        //存放到LogEntryAndClosure对象中，而LogEntryAndClosure对象就会放到环形数组中
        final EventTranslator<LogEntryAndClosure> translator = (event, sequence) -> {
            event.reset();
            event.done = task.getDone();
            event.entry = entry;
            event.expectedTerm = task.getExpectedTerm();
        };
        //默认提交日志到Disruptor框架中是采用非阻塞模式
        switch(this.options.getApplyTaskMode()) {
            case Blocking:
                //这里阻塞的意思是，消费速度跟不上生产者速度时，当前发布生产者数据的线程就直接阻塞了
                //知道发不成功才结束阻塞
                this.applyQueue.publishEvent(translator);
                break;
            case NonBlocking:
            default:
                //这里非阻塞的意思是，当消费速度跟不上生产速度时，尝试向队列中发布生产者数据
                //如果发布不成功，就直接回复给客户端当前节点非常忙碌，并没有让线程阻塞
                if (!this.applyQueue.tryPublishEvent(translator)) {
                    String errorMsg = "Node is busy, has too many tasks, queue is full and bufferSize="+ this.applyQueue.getBufferSize();
                    ThreadPoolsFactory.runClosureInThread(this.groupId, task.getDone(),
                            new Status(RaftError.EBUSY, errorMsg));
                    LOG.warn("Node {} applyQueue is overload.", getNodeId());
                    this.metrics.recordTimes("apply-task-overload-times", 1);
                    if(task.getDone() == null) {
                        throw new OverloadException(errorMsg);
                    }
                }
                break;
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:批处理日志的方法，其实在这个方法中也只不过是做了一点点校验，然后就设置了一下日志的任期和类型
     */
    private void executeApplyingTasks(final List<LogEntryAndClosure> tasks) {
        //虽然该方法是在批处理器中执行的，批处理器就是一个线程，不会有并发问题
        //但是在整个集群中，除了当前的线程，可能还会有日志快照线程，日志传输线程
        //同时访问日志组件，对日志组件中的某些属性进行修改，所以，这里要直接获得写锁
        this.writeLock.lock();
        try {//这个tasks集合中存放着32个LogEntryAndClosure对象
            //每个LogEntryAndClosure对象中存放着一个logEntry对象
            final int size = tasks.size();
            //判断当前节点是不是领导者，如果不是领导者不能处理日志
            if (this.state != State.STATE_LEADER) {
                final Status st = new Status();
                if (this.state != State.STATE_TRANSFERRING) {
                    st.setError(RaftError.EPERM, "Is not leader.");
                } else {
                    st.setError(RaftError.EBUSY, "Is transferring leadership.");
                }
                LOG.debug("Node {} can't apply, status={}.", getNodeId(), st);
                //这个就是获得每一个LogEntryAndClosure对象中包装每一个Task中的回调方法
                //这个回调方法是用来通知业务层日志处理是否成功的，这个回调方法会在日志被顺利提交之后才会被回调
                //具体逻辑会在第5版本实现
                final List<Closure> dones = tasks.stream().map(ele -> ele.done)
                        .filter(Objects::nonNull).collect(Collectors.toList());
                //回调每一个方法，通知业务层日志处理失败，其实也就是指令操作失败，因为访问的节点已经不是领导者了
                ThreadPoolsFactory.runInThread(this.groupId, () -> {
                    for (final Closure done : dones) {
                        done.run(st);
                    }
                });
                return;
            }//在这里创建的这个集合专门存放每一条日志
            final List<LogEntry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                //获得每一个包装日志的对象
                final LogEntryAndClosure task = tasks.get(i);
                //当领导者在处理日志的时候，这个时候刚初始化好的日志对象中的任期是-1，这个是在Task创建的时候初始化好的
                //这时候expectedTerm肯定等于-1，并且不等于当前节点的任期
                //expectedTerm的值不等于-1，就报错
                if (task.expectedTerm != -1 && task.expectedTerm != this.currTerm) {
                    LOG.debug("Node {} can't apply task whose expectedTerm={} doesn't match currTerm={}.", getNodeId(),
                            task.expectedTerm, this.currTerm);
                    if (task.done != null) {
                        //执行回调方法，通知业务层操作失败了
                        final Status st = new Status(RaftError.EPERM, "expected_term=%d doesn't match current_term=%d",
                                task.expectedTerm, this.currTerm);
                        //在这里执行回调方法
                        ThreadPoolsFactory.runClosureInThread(this.groupId, task.done, st);
                        task.reset();
                    }
                    continue;
                } //下面这段代码是为每一个日志条目对象创建了Ballot对象，并且把每一个日志条目对应的回调方法放到了
                //投票箱的回调队列中，投票箱使用的回调队列，实际上就是状态机组件持有的回调队列，这个要搞清楚
                if (!this.ballotBox.appendPendingTask(this.conf.getConf(),
                        this.conf.isStable() ? null : this.conf.getOldConf(), task.done)) {
                    //如果没有创建成功成功，则直接通知业务层操作失败
                    ThreadPoolsFactory.runClosureInThread(this.groupId, task.done, new Status(RaftError.EINTERNAL, "Fail to append task."));
                    task.reset();
                    continue;
                }
                //为每一个日志条目设置任期
                task.entry.getId().setTerm(this.currTerm);
                //设置日志条目类型，从这个方法中设置设置的日志条目对象都是业务日志
                task.entry.setType(EnumOutter.EntryType.ENTRY_TYPE_DATA);
                //得到了每一条日志，并且把日志添加到集合中
                entries.add(task.entry);
                //这里也是为了循环利用Event
                task.reset();
            }//将设置好的日志交给日志管理器处理，在日志管理器中，这些日志将会被设置任期，然后进行落盘的操作
            //这里的回调是当日志落盘成功后，直接判断是否可以在领导者提交了，这里判断提交的条件是看看每一条日志是否
            //得到了集群中过半节点的投票，第三版本还不用关心这个回调函数
            //因为我们还没有引入状态机，也没有引入日志传输的功能
            this.logManager.appendEntries(entries, new LeaderStableClosure(entries));
            //更新配置信息，暂时先注释掉
            //checkAndSetConfiguration(true);
        } finally {
            this.writeLock.unlock();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:该回调方法中的逻辑是用来判断当前日志是否可以被提交，然后应用到状态机上了
     */
    class LeaderStableClosure extends LogManager.StableClosure {

        public LeaderStableClosure(final List<LogEntry> entries) {
            super(entries);
        }

        @Override
        public void run(final Status status) {
            if (status.isOk()) {
                //在这里把日志应用到状态机上，注意，这里是领导者在调用这个方法，领导者的日志要应用到状态机上了，这里其实真正的作用就是自己给自己投了一票而已
                //真正应用的入口方法在Replicator类中
                NodeImpl.this.ballotBox.commitAt(this.firstLogIndex, this.firstLogIndex + this.nEntries - 1,
                        NodeImpl.this.serverId);
                System.out.println("领导者自己把业务日志写入数据库成功！！！！！！！！！！！！！");
            } else {
                LOG.error("Node {} append [{}, {}] failed, status={}.", getNodeId(), this.firstLogIndex,
                        this.firstLogIndex + this.nEntries - 1, status);
            }
        }
    }


    @Override
    public NodeOptions getOptions() {
        return this.options;
    }

    public JRaftServiceFactory getServiceFactory() {
        return this.serviceFactory;
    }

    @Override
    public RaftOptions getRaftOptions() {
        return this.raftOptions;
    }

    @Override
    public State getNodeState() {
        return this.state;
    }

    //执行身份降级的方法
    void increaseTermTo(final long newTerm, final Status status) {
        this.writeLock.lock();
        try {
            if (newTerm < this.currTerm) {
                return;
            }
            stepDown(newTerm, false, status);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public long getLastLogIndex() {
        this.readLock.lock();
        try {
            if (this.state.isActive()) {
                return this.logManager.getLastLogIndex();
            }
            throw new IllegalStateException("The node is not active, current state: " + this.state);
        } finally {
            this.readLock.unlock();
        }
    }

}