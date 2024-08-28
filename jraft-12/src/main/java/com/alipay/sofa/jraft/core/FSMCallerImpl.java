package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.FSMCaller;
import com.alipay.sofa.jraft.StateMachine;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ClosureQueue;
import com.alipay.sofa.jraft.closure.LoadSnapshotClosure;
import com.alipay.sofa.jraft.closure.SaveSnapshotClosure;
import com.alipay.sofa.jraft.closure.TaskClosure;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.conf.ConfigurationEntry;
import com.alipay.sofa.jraft.entity.*;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.FSMCallerOptions;
import com.alipay.sofa.jraft.storage.LogManager;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/17
 * @方法描述：状态机组件的实现类
 */
public class FSMCallerImpl implements FSMCaller {

    private static final Logger LOG = LoggerFactory.getLogger(FSMCallerImpl.class);

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：该枚举类中的对象表示的是提交到状态机组件的disruptor中事件的类型，在第七版本主要只使用COMMITTED这个枚举类对象，其他的后面版本再讲解
     */
    private enum TaskType {
        //空闲状态
        IDLE,
        //日志已提交
        COMMITTED,
        //快照保存
        SNAPSHOT_SAVE,
        //快照加载
        SNAPSHOT_LOAD,
        //领导者结束,下面这两个枚举对象实在转移领导权时使用的
        LEADER_STOP,
        //开始成为领导者
        LEADER_START,
        //开始成为跟随者
        START_FOLLOWING,
        //跟随者结束
        STOP_FOLLOWING,
        //停机
        SHUTDOWN,
        //刷新
        FLUSH,
        //运行错误
        ERROR;

        private String metricName;

        public String metricName() {
            if (this.metricName == null) {
                this.metricName = "fsm-" + name().toLowerCase().replaceAll("_", "-");
            }
            return this.metricName;
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：要提交给disruptor框架的异步任务
     */
    private static class ApplyTask {
        //任务类型
        TaskType type;
        //提交的日志ID
        long committedIndex;
        //任期
        long term;
        Status status;
        LeaderChangeContext leaderChangeCtx;
        //要回调的方法
        Closure done;
        CountDownLatch shutdownLatch;

        public void reset() {
            this.type = null;
            this.committedIndex = 0;
            this.term = 0;
            this.status = null;
            this.leaderChangeCtx = null;
            this.done = null;
            this.shutdownLatch = null;
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：事件工厂，该工厂会创建ApplyTask，然后将ApplyTask放到disruptor框架的环形数组中
     */
    private static class ApplyTaskFactory implements EventFactory<ApplyTask> {

        @Override
        public ApplyTask newInstance() {
            return new ApplyTask();
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：事件处理器，也就是disruptor框架要用的消费者处理器
     */
    private class ApplyTaskHandler implements EventHandler<ApplyTask> {
        boolean firstRun = true;
        //这个是最大的可以应用的日志的索引，初始化为-1，随着日志不断被应用，这个值也会被不断更新
        //注意，这个是消费者处理器中的成员变量，消费者处理器会处理所有要被应用的日志，所以这个成员变量是被所有日志共享的
        //当然，这个共享也是有限制了，就是被同一批处理的日志共享，在每一批新的日志应用的时候，这个值会被重置
        private long maxCommittedIndex = -1;

        @Override
        public void onEvent(final ApplyTask event, final long sequence, final boolean endOfBatch) throws Exception {
            setFsmThread();
            //在这里执行每一个异步任务
            this.maxCommittedIndex = runApplyTask(event, this.maxCommittedIndex, endOfBatch);
        }

        private void setFsmThread() {
            if (firstRun) {
                //在这里给状态机的线程赋值了，其实就是disruptor中的线程
                fsmThread = Thread.currentThread();
                firstRun = false;
            }
        }
    }
    //状态机执行任务的线程
    private volatile Thread fsmThread;
    //日志管理器组件
    private LogManager logManager;
    //真正的用户定义的状态机
    private StateMachine fsm;
    //存放了用户定义的回调方法的队列，这些回调方法是用来通知业务层状态机执行结果的
    private ClosureQueue closureQueue;
    //最新的被应用的日志的ID，初始化为0
    private final AtomicLong lastAppliedIndex;
    //最新的被提交的日志的ID，初始化为0
    private final AtomicLong lastCommittedIndex;
    //最新的被应用的日志的任期
    private long lastAppliedTerm;
    //这个也是回调方法，在停机时要被调用，现在还用不到这个
    private Closure afterShutdown;
    //当前节点本身
    private NodeImpl node;
    //状态机组件目前的状态，在构造方法中被初始化，一开始是空闲状态
    private volatile TaskType currTask;
    //正在被应用的日志索引，初始化为0
    private final AtomicLong applyingIndex;
    //执行异常
    private volatile RaftException error;
    //disruptor启动器
    private Disruptor<ApplyTask> disruptor;
    //disruptor的环形数组
    private RingBuffer<ApplyTask> taskQueue;
    //优雅停机时会用到的成员变量
    private volatile CountDownLatch shutdownLatch;
    //性能检测工具
    private NodeMetrics nodeMetrics;
    //这个成员变量在第七版本也用不到，在第十版本会用到，和线性一致读有关的成员变量
    private final CopyOnWriteArrayList<LastAppliedLogIndexListener> lastAppliedLogIndexListeners = new CopyOnWriteArrayList<>();

    //构造方法
    public FSMCallerImpl() {
        super();
        this.currTask = TaskType.IDLE;
        this.lastAppliedIndex = new AtomicLong(0);
        this.applyingIndex = new AtomicLong(0);
        this.lastCommittedIndex = new AtomicLong(0);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：初始化状态机组件的方法，这个方法就不添加注释了，因为目前为止已经见过很多这样的方法了，日志组件初始化的逻辑也是这一套，状态机组件初始化的逻辑也是如此
     * 都是设置一些最初的配置参数，然后创建disruptor框架，启动disruptor框架
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean init(final FSMCallerOptions opts) {
        this.logManager = opts.getLogManager();
        this.fsm = opts.getFsm();
        this.closureQueue = opts.getClosureQueue();
        this.afterShutdown = opts.getAfterShutdown();
        this.node = opts.getNode();
        this.nodeMetrics = this.node.getNodeMetrics();
        this.lastCommittedIndex.set(opts.getBootstrapId().getIndex());
        this.lastAppliedIndex.set(opts.getBootstrapId().getIndex());
        notifyLastAppliedIndexUpdated(this.lastAppliedIndex.get());
        this.lastAppliedTerm = opts.getBootstrapId().getTerm();
        this.disruptor = DisruptorBuilder.<ApplyTask> newInstance()
                .setEventFactory(new ApplyTaskFactory())
                .setRingBufferSize(opts.getDisruptorBufferSize())
                .setThreadFactory(new NamedThreadFactory("JRaft-FSMCaller-Disruptor-", true))
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.disruptor.handleEventsWith(new ApplyTaskHandler());
        this.disruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(getClass().getSimpleName()));
        this.taskQueue = this.disruptor.start();
        if (this.nodeMetrics.getMetricRegistry() != null) {
            this.nodeMetrics.getMetricRegistry().register("jraft-fsm-caller-disruptor",
                    new DisruptorMetricSet(this.taskQueue));
        }
        this.error = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_NONE);
        LOG.info("Starts FSMCaller successfully.");
        return true;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：停止组件工作的方法
     */
    @Override
    public synchronized void shutdown() {
        if (this.shutdownLatch != null) {
            return;
        }
        LOG.info("Shutting down FSMCaller...");
        if (this.taskQueue != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            this.shutdownLatch = latch;
            ThreadPoolsFactory.runInThread(getNode().getGroupId(), () -> this.taskQueue.publishEvent((task, sequence) -> {
                task.reset();
                task.type = TaskType.SHUTDOWN;
                task.shutdownLatch = latch;
            }));
        }
        doShutdown();
    }

    @Override
    public void addLastAppliedLogIndexListener(final LastAppliedLogIndexListener listener) {
        this.lastAppliedLogIndexListeners.add(listener);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：该方法会把生产者数据存放到disruptor的环形队列中
     */
    private boolean enqueueTask(final EventTranslator<ApplyTask> tpl) {
        if (this.shutdownLatch != null) {
            LOG.warn("FSMCaller is stopped, can not apply new task.");
            return false;
        }
        this.taskQueue.publishEvent(tpl);
        return true;
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：该方法就是把已经提交成功的日志，应用到状态机中的入口方法
     */
    @Override
    public boolean onCommitted(final long committedIndex) {
        return enqueueTask((task, sequence) -> {
            //设置为COMMITTED类型
            task.type = TaskType.COMMITTED;
            //committedIndex是从BallotBox类的commitAt方法中传递过来的参数
            //就是最新被提交的日志的索引
            task.committedIndex = committedIndex;
        });
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：下面这些方法在第七版本还用不到，所以就先不讲解了
     */
    void flush() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        enqueueTask((task, sequence) -> {
            task.type = TaskType.FLUSH;
            task.shutdownLatch = latch;
        });
        latch.await();
    }

    @Override
    public boolean onSnapshotLoad(final LoadSnapshotClosure done) {
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.SNAPSHOT_LOAD;
            task.done = done;
        });
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：安装快照的入口方法
     */
    @Override
    public boolean onSnapshotSave(final SaveSnapshotClosure done) {
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.SNAPSHOT_SAVE;
            task.done = done;
        });
    }

    @Override
    public boolean onLeaderStop(final Status status) {
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.LEADER_STOP;
            task.status = new Status(status);
        });
    }

    @Override
    public boolean onLeaderStart(final long term) {
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.LEADER_START;
            task.term = term;
        });
    }

    @Override
    public boolean onStartFollowing(final LeaderChangeContext ctx) {
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.START_FOLLOWING;
            task.leaderChangeCtx = new LeaderChangeContext(ctx.getLeaderId(), ctx.getTerm(), ctx.getStatus());
        });
    }

    @Override
    public boolean onStopFollowing(final LeaderChangeContext ctx) {
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.STOP_FOLLOWING;
            task.leaderChangeCtx = new LeaderChangeContext(ctx.getLeaderId(), ctx.getTerm(), ctx.getStatus());
        });
    }


    public class OnErrorClosure implements Closure {
        private RaftException error;

        public OnErrorClosure(final RaftException error) {
            super();
            this.error = error;
        }

        public RaftException getError() {
            return this.error;
        }

        public void setError(final RaftException error) {
            this.error = error;
        }

        @Override
        public void run(final Status st) {
        }
    }

    @Override
    public boolean onError(final RaftException error) {
        if (!this.error.getStatus().isOk()) {
            LOG.warn("FSMCaller already in error status, ignore new error.", error);
            return false;
        }
        final OnErrorClosure c = new OnErrorClosure(error);
        return enqueueTask((task, sequence) -> {
            task.type = TaskType.ERROR;
            task.done = c;
        });
    }

    @Override
    public long getLastCommittedIndex() {
        return lastCommittedIndex.get();
    }

    @Override
    public long getLastAppliedIndex() {
        return this.lastAppliedIndex.get();
    }

    public NodeImpl getNode() {
        return this.node;
    }

    @Override
    public synchronized void join() throws InterruptedException {
        if (this.shutdownLatch != null) {
            this.shutdownLatch.await();
            this.disruptor.shutdown();
            if (this.afterShutdown != null) {
                //在这里执行了从nodeimp中传递过来的回调方法
                //这个回调方法的执行意味着jraft框架中所有功能组件都已经关闭了
                //当然，rpc客户端服务组件不在考虑范围之内，rpc关闭是bolt框架负责的
                this.afterShutdown.run(Status.OK());
                this.afterShutdown = null;
            }
            this.shutdownLatch = null;
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：本类中的核心方法，也是disruptor框架的消费处理器中要调用的方法，在第七版本只会触发COMMITTED事件，所以我们就只看这个
     */
    @SuppressWarnings("ConstantConditions")
    private long runApplyTask(final ApplyTask task, long maxCommittedIndex, final boolean endOfBatch) {
        CountDownLatch shutdown = null;
        //在这里判断task是否为COMMITTED，如果是的话，就给maxCommittedIndex赋值
        if (task.type == TaskType.COMMITTED) {
            if (task.committedIndex > maxCommittedIndex) {
                //这时候maxCommittedIndex就不是-1了
                maxCommittedIndex = task.committedIndex;
            }
            task.reset();
        } else {//执行到这里，意味着当前提交的异步任务并不是COMMITTED类型的
            //但是要判断一下当前的批处理中的maxCommittedIndex是否大于0，如果大于0说明现在有日志要被应用到状态机上
            //所以尽管要处理的是其他类型的任务，也要先把日志应用到状态机上
            if (maxCommittedIndex >= 0) {
                this.currTask = TaskType.COMMITTED;
                //在这里执行真正的提交日志的方法
                doCommitted(maxCommittedIndex);
                //重置maxCommittedIndex的值
                maxCommittedIndex = -1L;
            }
            final long startMs = Utils.monotonicMs();
            try {
                switch (task.type) {
                    //在这个switch中，只关注这一个分支即可，这里如果提交的任务是COMMITTED类型的
                    case COMMITTED:
                        //会在下面的方法中报错，因为COMMITTED类型的任务已经在最开始的if分支中处理了，所以不应该进入下面这个else分支中
                        //如果真的执行到这里了就报错
                        Requires.requireTrue(false, "Impossible");
                        break;
                    case SNAPSHOT_SAVE:
                        //生成快照的操作
                        onSnapshotSaveSync(task);
                        break;
                    case SNAPSHOT_LOAD:
                        this.currTask = TaskType.SNAPSHOT_LOAD;
                        if (passByStatus(task.done)) {
                            //加载快照的操作
                            doSnapshotLoad((LoadSnapshotClosure) task.done);
                        }
                        break;
                    case LEADER_STOP:
                        this.currTask = TaskType.LEADER_STOP;
                        doLeaderStop(task.status);
                        break;
                    case LEADER_START:
                        this.currTask = TaskType.LEADER_START;
                        doLeaderStart(task.term);
                        break;
                    case START_FOLLOWING:
                        this.currTask = TaskType.START_FOLLOWING;
                        doStartFollowing(task.leaderChangeCtx);
                        break;
                    case STOP_FOLLOWING:
                        this.currTask = TaskType.STOP_FOLLOWING;
                        doStopFollowing(task.leaderChangeCtx);
                        break;
                    case ERROR:
                        this.currTask = TaskType.ERROR;
                        doOnError((OnErrorClosure) task.done);
                        break;
                    case IDLE:
                        Requires.requireTrue(false, "Can't reach here");
                        break;
                    case SHUTDOWN:
                        this.currTask = TaskType.SHUTDOWN;
                        shutdown = task.shutdownLatch;
                        break;
                    case FLUSH:
                        this.currTask = TaskType.FLUSH;
                        shutdown = task.shutdownLatch;
                        break;
                }
            } finally {
                this.nodeMetrics.recordLatency(task.type.metricName(), Utils.monotonicMs() - startMs);
                task.reset();
            }
        }
        try {//在这里会有一个总的判断，判断当前要消费的数据是不是本批数据中的最后一个，这是disruptor框架中的知识
            //如果是最后一个，并且maxCommittedIndex大于0，说明肯定有一些日志要被应用到状态机上
            //那就直接应用这批日志即可，如果不是本批数据中的最后一个，那就什么也不做
            //除非有其他非COMMITTED类型的任务被提交了，这时候就会在上面的else分支中把还未应用的日志都应用到状态机上
            if (endOfBatch && maxCommittedIndex >= 0) {
                this.currTask = TaskType.COMMITTED;
                //应用日志到状态机上
                doCommitted(maxCommittedIndex);
                //重置maxCommittedIndex为-1
                maxCommittedIndex = -1L;
            }
            this.currTask = TaskType.IDLE;
            return maxCommittedIndex;
        } finally {
            if (shutdown != null) {
                shutdown.countDown();
            }
        }
    }


    Thread getFsmThread() {
        return this.fsmThread;
    }

    @Override
    public boolean isRunningOnFSMThread() {
        return Thread.currentThread() == fsmThread;
    }

    @Override
    public void onSnapshotSaveSync(SaveSnapshotClosure done) {
        ApplyTask task = new ApplyTask();
        task.type = TaskType.SNAPSHOT_SAVE;
        task.done = done;
        this.onSnapshotSaveSync(task);
    }

    private void onSnapshotSaveSync(final ApplyTask task) {
        this.currTask = TaskType.SNAPSHOT_SAVE;
        //在这里先判断状态机组件运行过程中是否出过错
        if (passByStatus(task.done)) {
            //执行生成快照的操作
            doSnapshotSave((SaveSnapshotClosure) task.done);
        }
    }

    private void doShutdown() {
        if (this.node != null) {
            this.node = null;
        }
        if (this.fsm != null) {
            this.fsm.onShutdown();
        }
    }

    private void notifyLastAppliedIndexUpdated(final long lastAppliedIndex) {
        for (final LastAppliedLogIndexListener listener : this.lastAppliedLogIndexListeners) {
            listener.onApplied(lastAppliedIndex);
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：根据日志索引，将对应的日志应用到状态机中的方法，committedInde是已提交的最新的日志的索引
     */
    private void doCommitted(final long committedIndex) {
        //校验错误状态码，如果状态机在工作中已经出现错误，就直接推出该方法，不再应用日志
        if (!this.error.getStatus().isOk()) {
            return;
        }
        //得到最后被应用的日志的索引
        final long lastAppliedIndex = this.lastAppliedIndex.get();
        //这里判断一下，最后被应用的日志的索引大于现在正要被应用的日志的最大索引，就说明日志已经被应用过了，那就直接退出该方法
        if (lastAppliedIndex >= committedIndex) {
            return;
        }
        //给最新提交的日志的索引赋值
        this.lastCommittedIndex.set(committedIndex);
        //获得执行应用日志操作的开始时间
        final long startMs = Utils.monotonicMs();
        try {
            //定义一个集合存放用户在业务层定义的回调方法
            final List<Closure> closures = new ArrayList<>();
            //定义一个集合存放用户在业务层定义的回调方法，这个集合是要给onTaskCommitted方法使用的
            final List<TaskClosure> taskClosures = new ArrayList<>();
            //在这个方法中，就把回调队列中的对象全都放到closures和taskClosures队列中了，并且也返回了要回调的第一个方法的索引
            final long firstClosureIndex = this.closureQueue.popClosureUntil(committedIndex, closures, taskClosures);
            //在这里回调taskClosures队列中的方法，这个TaskClosure接口中的方法为onCommitted，这个回调方法会在日志还未应用到状态机之前被调用
            //而我在第七版本提供的测试类中的ExpectClosure对象，其内部提供的回调方法会在日志成功应用到状态机之后被回调
            onTaskCommitted(taskClosures);
            //校验firstClosureIndex合法性
            Requires.requireTrue(firstClosureIndex >= 0, "Invalid firstClosureIndex");
            //创建日志迭代器，这个日志迭代器会根据lastAppliedIndex和committedIndex这两个值，把这个范围的日志都当作要被应用的日志
            //然后一条一条地应用到状态机上
            final IteratorImpl iterImpl = new IteratorImpl(this, this.logManager, closures, firstClosureIndex,
                    lastAppliedIndex, committedIndex, this.applyingIndex);
            //如果迭代器还有日志条目，就继续在循环中将这个日志条目应用到状态机上
            while (iterImpl.isGood()) {
                //得到要应用的日志条目
                final LogEntry logEntry = iterImpl.entry();
                //如果这一条日志的类型不是业务日志类型，那就不必应用到状态机上
                if (logEntry.getType() != EnumOutter.EntryType.ENTRY_TYPE_DATA) {
                    //判断是不是配置变更日志
                    if (logEntry.getType() == EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                        //如果是配置变更日志，并且就配置不为空
                        if (logEntry.getOldPeers() != null && !logEntry.getOldPeers().isEmpty()) {
                            //可以通过这个方法通知业务层，用户可以在这个方法中定义一些扩展操作
                            this.fsm.onConfigurationCommitted(new Configuration(iterImpl.entry().getPeers()));
                        }
                    }
                    //在这里回调配置变更日志的回调方法，这里是领导者才会有的操作
                    if (iterImpl.done() != null) {
                        iterImpl.done().run(Status.OK());
                    }
                    //得到下一个日志条目
                    iterImpl.next();
                    //进入下一次循环
                    continue;
                }
                //走到这里意味着是业务日志，那就在下面这个方法中将日志应用到状态机上
                doApplyTasks(iterImpl);
            }
            //日志应用中出现错误，就设置错误状态给状态机组件
            //并且执行回调队列中剩余的回调方法
            if (iterImpl.hasError()) {
                setError(iterImpl.getError());
                iterImpl.runTheRestClosureWithError();
            }
            //得到最后应用的日志的索引
            long lastIndex = iterImpl.getIndex() - 1;
            //得到对应日志的任期
            final long lastTerm = this.logManager.getTerm(lastIndex);
            //更新状态机的成员变量
            setLastApplied(lastIndex, lastTerm);
        } finally {
            //记录操作耗时
            this.nodeMetrics.recordLatency("fsm-commit", Utils.monotonicMs() - startMs);
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：日志被应用到状态机后，更新对应的成员变量的值，这个就不再添加注释了，很简单
     */
    void setLastApplied(long lastIndex, final long lastTerm) {
        final LogId lastAppliedId = new LogId(lastIndex, lastTerm);
        this.lastAppliedIndex.set(lastIndex);
        this.lastAppliedTerm = lastTerm;
        this.logManager.setAppliedId(lastAppliedId);
        //更新了最新被应用的日志的索引后，通知状态机监听器执行方法
        notifyLastAppliedIndexUpdated(lastIndex);
    }

    private void onTaskCommitted(final List<TaskClosure> closures) {
        for (int i = 0, size = closures.size(); i < size; i++) {
            final TaskClosure done = closures.get(i);
            done.onCommitted();
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：将业务日志应用到状态机上的方法
     */
    private void doApplyTasks(final IteratorImpl iterImpl) {
        //这里对日志迭代器做了点简单的包装，其实就是可以获得真正的有效日志了，也就是data部分
        //这部分解码之后们就是用户的操作执行
        final IteratorWrapper iter = new IteratorWrapper(iterImpl);
        final long startApplyMs = Utils.monotonicMs();
        final long startIndex = iter.getIndex();
        try {//在这里将业务日志应用到状态机上，同时也会把日志对应的回调方法在状态机中回调
            //这部分逻辑是用户自己实现的
            this.fsm.onApply(iter);
        } finally {
            this.nodeMetrics.recordLatency("fsm-apply-tasks", Utils.monotonicMs() - startApplyMs);
            this.nodeMetrics.recordSize("fsm-apply-tasks-count", iter.getIndex() - startIndex);
        }
        if (iter.hasNext()) {
            LOG.error("Iterator is still valid, did you return before iterator reached the end?");
        }
        //得到下一个日志条目
        iter.next();
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/20
     * @方法描述：该方法就是用来生成快照的
     */
    private void doSnapshotSave(final SaveSnapshotClosure done) {
        Requires.requireNonNull(done, "SaveSnapshotClosure is null");
        final long lastAppliedIndex = this.lastAppliedIndex.get();
        final RaftOutter.SnapshotMeta.Builder metaBuilder = RaftOutter.SnapshotMeta.newBuilder()
                //设置本次生成快照的元数据信息，本次快照要记录的最后一条日志索引其实就是当前状态机最新应用的日志的索引
                .setLastIncludedIndex(lastAppliedIndex)
                .setLastIncludedTerm(this.lastAppliedTerm);
        //下面就是把集群节点信息设置到元数据中了，这里就是从最新应用的日志条目中获得配置变更日志，这里我要多解释一下，实际上在ConfigurationManager类中根据索引获得小于该索引的最新配置条目
        //然后再从配置变更日志中获得集群配置信息
        final ConfigurationEntry confEntry = this.logManager.getConfiguration(lastAppliedIndex);
        if (confEntry == null || confEntry.isEmpty()) {
            LOG.error("Empty conf entry for lastAppliedIndex={}", lastAppliedIndex);
            ThreadPoolsFactory.runClosureInThread(getNode().getGroupId(), done, new Status(RaftError.EINVAL,
                    "Empty conf entry for lastAppliedIndex=%s", lastAppliedIndex));
            return;
        }
        for (final PeerId peer : confEntry.getConf()) {
            metaBuilder.addPeers(peer.toString());
        }
        for (final PeerId peer : confEntry.getConf().getLearners()) {
            metaBuilder.addLearners(peer.toString());
        }
        if (confEntry.getOldConf() != null) {
            for (final PeerId peer : confEntry.getOldConf()) {
                metaBuilder.addOldPeers(peer.toString());
            }
            for (final PeerId peer : confEntry.getOldConf().getLearners()) {
                metaBuilder.addOldLearners(peer.toString());
            }
        }
        final SnapshotWriter writer = done.start(metaBuilder.build());
        if (writer == null) {
            done.run(new Status(RaftError.EINVAL, "snapshot_storage create SnapshotWriter failed"));
            return;
        }//这里可以看到，快照生成的操作其实是在用户自己定义的状态集中实现的
        this.fsm.onSnapshotSave(writer, done);
    }




    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/3/14
     * @方法描述：该方法就是把快照中的数据应用到状态机上
     */
    private void doSnapshotLoad(final LoadSnapshotClosure done) {
        //校验回调对象不为null
        Requires.requireNonNull(done, "LoadSnapshotClosure is null");
        //得到快照读取器
        final SnapshotReader reader = done.start();
        //对快照读取器做判空操作
        if (reader == null) {
            done.run(new Status(RaftError.EINVAL, "open SnapshotReader failed"));
            return;
        }
        //使用快照读取器加载快照元数据
        final RaftOutter.SnapshotMeta meta = reader.load();
        //对快照元数据做判空操作
        if (meta == null) {
            done.run(new Status(RaftError.EINVAL, "SnapshotReader load meta failed"));
            if (reader.getRaftError() == RaftError.EIO) {
                final RaftException err = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_SNAPSHOT, RaftError.EIO,
                        "Fail to load snapshot meta");
                setError(err);
            }
            return;
        }
        //现在得到的是目前状态机应用的最后一条日志的Id，这个日志Id对象包含应用的最后一条日志的索引和任期
        final LogId lastAppliedId = new LogId(this.lastAppliedIndex.get(), this.lastAppliedTerm);
        //根据从领导者下载的快照元数据创建一个最新的要被应用的日志Id对象
        final LogId snapshotId = new LogId(meta.getLastIncludedIndex(), meta.getLastIncludedTerm());
        //将上面两个日志Id对象做一下判断，如果当前状态及的日志Id大于领导者快照应用的最新日志Id
        //说明从领导者传递过来的是一个过期快照，不能应用快照中的数据
        if (lastAppliedId.compareTo(snapshotId) > 0) {
            done.run(new Status(
                    RaftError.ESTALE,
                    "Loading a stale snapshot last_applied_index=%d last_applied_term=%d snapshot_index=%d snapshot_term=%d",
                    lastAppliedId.getIndex(), lastAppliedId.getTerm(), snapshotId.getIndex(), snapshotId.getTerm()));
            return;
        }
        //在这里应用快照文件中的所有数据到用户定义的状态机上
        if (!this.fsm.onSnapshotLoad(reader)) {
            done.run(new Status(-1, "StateMachine onSnapshotLoad failed"));
            final RaftException e = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_STATE_MACHINE,
                    RaftError.ESTATEMACHINE, "StateMachine onSnapshotLoad failed");
            setError(e);
            return;
        }
        //从快照元数据中获得领导者配置信息
        if (meta.getOldPeersCount() == 0) {
            final Configuration conf = new Configuration();
            //构建配置信息对象
            for (int i = 0, size = meta.getPeersCount(); i < size; i++) {
                final PeerId peer = new PeerId();
                Requires.requireTrue(peer.parse(meta.getPeers(i)), "Parse peer failed");
                conf.addPeer(peer);
            }
            //状态机更新配置信息
            this.fsm.onConfigurationCommitted(conf);
        }
        //更新提交的最新的日志索引
        this.lastCommittedIndex.set(meta.getLastIncludedIndex());
        //更新应用的最新日志索引
        this.lastAppliedIndex.set(meta.getLastIncludedIndex());
        //更新最新应用的日志人气
        this.lastAppliedTerm = meta.getLastIncludedTerm();
        //执行回调方法
        done.run(Status.OK());
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StateMachine [");
        switch (this.currTask) {
            case IDLE:
                sb.append("Idle");
                break;
            case COMMITTED:
                sb.append("Applying logIndex=").append(this.applyingIndex);
                break;
            case SNAPSHOT_SAVE:
                sb.append("Saving snapshot");
                break;
            case SNAPSHOT_LOAD:
                sb.append("Loading snapshot");
                break;
            case ERROR:
                sb.append("Notifying error");
                break;
            case LEADER_STOP:
                sb.append("Notifying leader stop");
                break;
            case LEADER_START:
                sb.append("Notifying leader start");
                break;
            case START_FOLLOWING:
                sb.append("Notifying start following");
                break;
            case STOP_FOLLOWING:
                sb.append("Notifying stop following");
                break;
            case SHUTDOWN:
                sb.append("Shutting down");
                break;
            default:
                break;
        }
        return sb.append(']').toString();
    }



    private void doOnError(final OnErrorClosure done) {
        setError(done.getError());
    }

    private void doLeaderStop(final Status status) {
        this.fsm.onLeaderStop(status);
    }

    private void doLeaderStart(final long term) {
        this.fsm.onLeaderStart(term);
    }

    private void doStartFollowing(final LeaderChangeContext ctx) {
        this.fsm.onStartFollowing(ctx);
    }

    private void doStopFollowing(final LeaderChangeContext ctx) {
        this.fsm.onStopFollowing(ctx);
    }

    private void setError(final RaftException e) {
        if (this.error.getType() != EnumOutter.ErrorType.ERROR_TYPE_NONE) {
            return;
        }
        this.error = e;
        if (this.fsm != null) {
            this.fsm.onError(e);
        }
        if (this.node != null) {
            this.node.onError(e);
        }
    }


    RaftException getError() {
        return this.error;
    }

    private boolean passByStatus(final Closure done) {
        final Status status = this.error.getStatus();
        if (!status.isOk()) {
            if (done != null) {
                done.run(new Status(RaftError.EINVAL, "FSMCaller is in bad status=`%s`", status));
                return false;
            }
        }
        return true;
    }

    @Override
    public void describe(final Printer out) {
        out.print("  ") //
                .println(toString());
    }
}
