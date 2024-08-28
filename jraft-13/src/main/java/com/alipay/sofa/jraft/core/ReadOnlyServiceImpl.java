package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.FSMCaller;
import com.alipay.sofa.jraft.ReadOnlyService;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.entity.ReadIndexState;
import com.alipay.sofa.jraft.entity.ReadIndexStatus;
import com.alipay.sofa.jraft.error.OverloadException;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.ReadOnlyOption;
import com.alipay.sofa.jraft.option.ReadOnlyServiceOptions;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.rpc.RpcResponseClosureAdapter;
import com.alipay.sofa.jraft.util.*;
import com.google.protobuf.ZeroByteStringHelper;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;



//提供只读服务的类
public class ReadOnlyServiceImpl implements ReadOnlyService, FSMCaller.LastAppliedLogIndexListener {


    //又看见了disruptor框架，这说明只读请求肯定也是放在环形队列中，使用批处理器一次处理很多读请求
    private Disruptor<ReadIndexEvent> readIndexDisruptor;
    private RingBuffer<ReadIndexEvent> readIndexQueue;
    private RaftOptions raftOptions;
    private NodeImpl node;
    private final Lock lock = new ReentrantLock();
    private FSMCaller fsmCaller;
    private volatile CountDownLatch shutdownLatch;
    //单线程定时任务执行器
    private ScheduledExecutorService scheduledExecutorService;
    private NodeMetrics nodeMetrics;
    private volatile RaftException error;
    private final TreeMap<Long, List<ReadIndexStatus>> pendingNotifyStatus = new TreeMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyServiceImpl.class);


    //disruptor的环形数组中存放的要消费的对象
    private static class ReadIndexEvent {
        ReadOnlyOption readOnlyOptions;
        Bytes requestContext;
        ReadIndexClosure done;
        CountDownLatch shutdownLatch;
        long startTime;

        private void reset() {
            this.readOnlyOptions = null;
            this.requestContext = null;
            this.done = null;
            this.shutdownLatch = null;
            this.startTime = 0L;
        }
    }

    private static class ReadIndexEventFactory implements EventFactory<ReadIndexEvent> {

        @Override
        public ReadIndexEvent newInstance() {
            return new ReadIndexEvent();
        }
    }


    //disruptor的消费处理器
    private class ReadIndexEventHandler implements EventHandler<ReadIndexEvent> {


        //定义一个集合，集合中存放从环形数组中获得的要小费的数据，等到集合中的数据达到了限制，就批量处理集合中的所有消费数据
        private final List<ReadIndexEvent> events = new ArrayList<>(ReadOnlyServiceImpl.this.raftOptions.getApplyBatch());

        @Override
        public void onEvent(final ReadIndexEvent newEvent, final long sequence, final boolean endOfBatch)
                throws Exception {
            if (newEvent.shutdownLatch != null) {
                executeReadIndexEvents(this.events);
                reset();
                newEvent.shutdownLatch.countDown();
                return;
            }

            this.events.add(newEvent);
            if (this.events.size() >= ReadOnlyServiceImpl.this.raftOptions.getApplyBatch() || endOfBatch) {
                executeReadIndexEvents(this.events);
                reset();
            }
        }

        private void reset() {
            for (final ReadIndexEvent event : this.events) {
                event.reset();
            }
            this.events.clear();
        }
    }



    //该方法会在当前节点接收到ReadIndexResponse响应后被回调
    class ReadIndexResponseClosure extends RpcResponseClosureAdapter<RpcRequests.ReadIndexResponse> {

        //这个ReadIndexState对象中封装了每一个请求的一些相关数据
        final List<ReadIndexState> states;
        //读请求本身
        final RpcRequests.ReadIndexRequest request;

        //构造方法
        public ReadIndexResponseClosure(final List<ReadIndexState> states, final RpcRequests.ReadIndexRequest request) {
            super();
            this.states = states;
            this.request = request;
        }



        @Override
        public void run(final Status status) {
            //判断读请求的响应是否成功，响应状态是失败的，就直接通知业务层读操作失败了
            if (!status.isOk()) {
                //通知业务层读操作失败
                notifyFail(status);
                return;
            }
            //得到只读请求的响应对象
            final RpcRequests.ReadIndexResponse readIndexResponse = getResponse();
            //判断响应本身是否成功
            if (!readIndexResponse.getSuccess()) {
                //响应失败则通知业务层操作失败
                notifyFail(new Status(-1, "Fail to run ReadIndex task, maybe the leader stepped down."));
                return;
            }
            //走到这里意味着响应是成功的，创建一个ReadIndexStatus对象，并且把领导者回复的最新应用的日志索引设置到ReadIndexStatus对象中
            //很快就会用到
            final ReadIndexStatus readIndexStatus = new ReadIndexStatus(this.states, this.request,
                    readIndexResponse.getIndex());
            for (final ReadIndexState state : this.states) {
                //遍历states集合，这个集合中存放着从客户端发送过来的请求的信息
                state.setIndex(readIndexResponse.getIndex());
            }
            boolean doUnlock = true;
            ReadOnlyServiceImpl.this.lock.lock();
            try {
                //判断当前节点的状态机应用的日志索引是否大于等于领导者回复的日志索引
                //如果大于等于，说明当前节点已经应用了最新日志，就可以处理读请求了
                if (readIndexStatus.isApplied(ReadOnlyServiceImpl.this.fsmCaller.getLastAppliedIndex())) {
                    ReadOnlyServiceImpl.this.lock.unlock();
                    doUnlock = false;
                    //这里就可以通知业务层执行读操作了
                    notifySuccess(readIndexStatus);
                } else {
                    //如果当前节点状态及应用的日志索引比领导者小，说明当前节点的状态还不是最新的，并且这两个索引的差距非常大
                    //这时候就直接通知业务层读操作失败，因为等待当前节点应用到领导者日志索引可能要等很久，就直接回复失败响应了
                    if (readIndexStatus.isOverMaxReadIndexLag(ReadOnlyServiceImpl.this.fsmCaller.getLastAppliedIndex(), ReadOnlyServiceImpl.this.raftOptions.getMaxReadIndexLag())) {
                        ReadOnlyServiceImpl.this.lock.unlock();
                        doUnlock = false;
                        notifyFail(new Status(-1, "Fail to run ReadIndex task, the gap of current node's apply index between leader's commit index over maxReadIndexLag"));
                    } else  {
                        //走到这里就意味着当前节点状态及应用的日志索引比领导者小，但是有没有小很多
                        //这时候就可以把等待被执行的读操作的请求信息放到pendingNotifyStatus这个map中
                        //其中key就是读操作等待的索引，一旦当前节点应用到这个索引了，就可以通过注册在当前节点状态机上的监听方法通知业务层可以执行读操作了
                        //现在大家应该明白了本类的onApplied方法的作用了吧
                        //注意，这里添加的value是一个list，也就是说该日志索引对应的所有读操作都会被放到这个集合中，然后再放到map中
                        ReadOnlyServiceImpl.this.pendingNotifyStatus
                                .computeIfAbsent(readIndexStatus.getIndex(), k -> new ArrayList<>(10))
                                .add(readIndexStatus);
                    }
                }
            } finally {
                if (doUnlock) {
                    ReadOnlyServiceImpl.this.lock.unlock();
                }
            }
        }


        //通知业务层读操作失败的方法
        private void notifyFail(final Status status) {
            final long nowMs = Utils.monotonicMs();
            //遍历states集合，这个集合中封装着每一个客户端发送的读请求对应的信息
            for (final ReadIndexState state : this.states) {
                ReadOnlyServiceImpl.this.nodeMetrics.recordLatency("read-index", nowMs - state.getStartTimeMs());
                //获得回调方法，这个回调方法就是业务层在执行读操作时定义的
                final ReadIndexClosure done = state.getDone();
                if (done != null) {
                    final Bytes reqCtx = state.getRequestContext();
                    //执行回调方法，通知业务层操作失败
                    done.run(status, ReadIndexClosure.INVALID_LOG_INDEX, reqCtx != null ? reqCtx.get() : null);
                }
            }
        }
    }



    //处理客户端读操作的方法
    private void handleReadIndex(final ReadOnlyOption option, final List<ReadIndexEvent> events) {
        //在sofajraft框架中，只要是读操作，都要交给领导者处理一下，因为假如是跟随者节点，跟随者节点可不知道自己应用的日志是不是最新的
        //只有向领导者发送一个请求询问一下，领导者把自己状态机应用的最新日志索引告诉跟随者节点，跟随者将自己状态及应用的最新日志索引
        //和领导者的对比一下，这样一来跟随者就知道自己是否应用到最新日志了，如果应用到了，那么跟随者就可以直接处理读操作了
        //如果还没有应用到领导者状态机的日志索引，跟随者节点就要等一等，等到应用到了再通知业务层执行读操作
        //现在就要创建一个ReadIndexRequest请求构建器，这个ReadIndexRequest请求就是要发送给领导者询问领导者最新应用到状态机上的日志索引
        //当然，如果当前节点就是领导者也会象征性的发送一个请求，其实就是直接调用NodeImpl对象的handleReadIndexRequest处理这个请求即可
        final RpcRequests.ReadIndexRequest.Builder rb = RpcRequests.ReadIndexRequest.newBuilder()
                .setGroupId(this.node.getGroupId())
                .setServerId(this.node.getServerId().toString())
                .setReadOnlyOptions(ReadOnlyOption.convertMsgType(option));
        //在这里根据ReadOnlyOption中的处理读请求的策略过滤请求，把和option相同的请求策略的请求封装到一个ReadIndexState对象中
        //最后返回一个存放了很多ReadIndexState对象的集合，这个集合会被封装到ReadIndexResponseClosure回调对象中
        //再处理领导者回复的ReadIndexRequest请求的响应时会被用到
        final List<ReadIndexState> states = events.stream()
                .filter(it -> option.equals(it.readOnlyOptions))
                .map(it -> {
                    rb.addEntries(ZeroByteStringHelper.wrap(it.requestContext.get()));
                    return new ReadIndexState(it.requestContext, it.done, it.startTime);
                }).collect(Collectors.toList());

        //判断上面得到的集合非空
        if (states.isEmpty()) {
            return;
        }
        //在这里创建了ReadIndexRequest请求
        final RpcRequests.ReadIndexRequest request = rb.build();
        //在这里把请求交给NodeImpl对象的handleReadIndexRequest方法来处理了，并且定义了一个回调对象ReadIndexResponseClosure
        //该对象中的run方法会在接收到ReadIndexResponse响应后被回调
        //在NodeImpl对象的handleReadIndexRequest方法中会判断当前节点是否为领导者节点
        //如果是领导者那么直接处理请求即可，如果是跟随者，跟随者就会把这个请求发送给领导者，让领导者处理
        this.node.handleReadIndexRequest(request, new ReadIndexResponseClosure(states, request));
    }



    //该方法内调用了两次handleReadIndex方法，但是两次方法传递的参数不一致，一个传递的是只读策略，一个传递的是租约策略
    //这是考虑到发送过来的请求采用的擦略不同，所以处理两次，这样一来，不管是哪种策略的读请求都可以被处理到了
    //当然，这里调用两次方法并不意味着所有请求都会被处理两次，传递只读策略的handleReadIndex方法只会处理只读策略的请求
    //而传递租约策略的handleReadIndex方法只会处理租约策略的请求
    private void executeReadIndexEvents(final List<ReadIndexEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        handleReadIndex(ReadOnlyOption.ReadOnlySafe, events);
        handleReadIndex(ReadOnlyOption.ReadOnlyLeaseBased, events);
    }



    private void resetPendingStatusError(final Status st) {
        this.lock.lock();
        try {
            final Iterator<List<ReadIndexStatus>> it = this.pendingNotifyStatus.values().iterator();
            while (it.hasNext()) {
                final List<ReadIndexStatus> statuses = it.next();
                for (final ReadIndexStatus status : statuses) {
                    reportError(status, st);
                }
                it.remove();
            }
        } finally {
            this.lock.unlock();
        }
    }


    //初始化的方法，这个方法的逻辑非常简单，就不添加多么详细的注释了
    @Override
    public boolean init(final ReadOnlyServiceOptions opts) {
        this.node = opts.getNode();
        this.nodeMetrics = this.node.getNodeMetrics();
        this.fsmCaller = opts.getFsmCaller();
        this.raftOptions = opts.getRaftOptions();

        //创建定时任务调度器，这个定时任务调度器是单线程的
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ReadOnlyService-PendingNotify-Scanner", true));

        //创建disruptor，启动disruptor
        this.readIndexDisruptor = DisruptorBuilder.<ReadIndexEvent> newInstance() //
                .setEventFactory(new ReadIndexEventFactory()) //
                .setRingBufferSize(this.raftOptions.getDisruptorBufferSize()) //
                .setThreadFactory(new NamedThreadFactory("JRaft-ReadOnlyService-Disruptor-", true)) //
                .setWaitStrategy(new BlockingWaitStrategy()) //
                .setProducerType(ProducerType.MULTI) //
                .build();
        this.readIndexDisruptor.handleEventsWith(new ReadIndexEventHandler());
        this.readIndexDisruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(getClass().getSimpleName()));
        this.readIndexQueue = this.readIndexDisruptor.start();
        if (this.nodeMetrics.getMetricRegistry() != null) {
            this.nodeMetrics.getMetricRegistry() //
                    .register("jraft-read-only-service-disruptor", new DisruptorMetricSet(this.readIndexQueue));
        }


        //添加状态机监听器，当状态机中应用的最新日志索引发生了变化，就会通知监听器执行相应方法
        this.fsmCaller.addLastAppliedLogIndexListener(this);

        //这里的单线程定时任务调度器调度了一个定时任务，这个定时任务会定期执行本类的onApplied方法
        //onApplied方法的作用很简单，就是判断当前节点的状态机是否应用到领导者应用的那个最新的日志了
        //如果应用到了，那么当前节点就可以处理读请求了
        this.scheduledExecutorService.scheduleAtFixedRate(() -> onApplied(this.fsmCaller.getLastAppliedIndex()),
                this.raftOptions.getMaxElectionDelayMs(), this.raftOptions.getMaxElectionDelayMs(), TimeUnit.MILLISECONDS);
        return true;
    }


    @Override
    public synchronized void setError(final RaftException error) {
        if (this.error == null) {
            this.error = error;
        }
    }

    @Override
    public synchronized void shutdown() {
        if (this.shutdownLatch != null) {
            return;
        }
        this.shutdownLatch = new CountDownLatch(1);
        ThreadPoolsFactory.runInThread( this.node.getGroupId(),
                () -> this.readIndexQueue.publishEvent((event, sequence) -> event.shutdownLatch = this.shutdownLatch));
        this.scheduledExecutorService.shutdown();
    }

    @Override
    public void join() throws InterruptedException {
        if (this.shutdownLatch != null) {
            this.shutdownLatch.await();
        }
        this.readIndexDisruptor.shutdown();
        resetPendingStatusError(new Status(RaftError.ESTOP, "Node is quit."));
        this.scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public void addRequest(byte[] reqCtx, ReadIndexClosure closure) {
        addRequest(this.node.getRaftOptions().getReadOnlyOptions(), reqCtx, closure);
    }



    //该方法就是处理业务层要进行的读操作的方法
    @Override
    public void addRequest(final ReadOnlyOption readOnlyOptions, final byte[] reqCtx, final ReadIndexClosure closure) {
        if (this.shutdownLatch != null) {
            ThreadPoolsFactory.runClosureInThread(this.node.getGroupId(), closure, new Status(RaftError.EHOSTDOWN, "Was stopped"));
            throw new IllegalStateException("Service already shutdown.");
        }
        try {//定义一个disruptor框架要使用的数据传输器
            EventTranslator<ReadIndexEvent> translator = (event, sequence) -> {
                event.readOnlyOptions = readOnlyOptions;
                event.done = closure;
                event.requestContext = new Bytes(reqCtx);
                event.startTime = Utils.monotonicMs();
            };
            //下面就是把请求相关的信息提交到环形队列中的操作，这些代码在领导者提交日志给disruptor的时候已经见过了，就不再重复讲解了
            switch (this.node.getOptions().getApplyTaskMode()) {
                case Blocking:
                    this.readIndexQueue.publishEvent(translator);
                    break;
                case NonBlocking:
                default:
                    if (!this.readIndexQueue.tryPublishEvent(translator)) {
                        final String errorMsg = "Node is busy, has too many read-index requests, queue is full and bufferSize=" + this.readIndexQueue.getBufferSize();
                        ThreadPoolsFactory.runClosureInThread(this.node.getGroupId(), closure,
                                new Status(RaftError.EBUSY, errorMsg));
                        this.nodeMetrics.recordTimes("read-index-overload-times", 1);
                        LOG.warn("Node {} ReadOnlyServiceImpl readIndexQueue is overload.", this.node.getNodeId());
                        if (closure == null) {
                            throw new OverloadException(errorMsg);
                        }
                    }
                    break;
            }
        } catch (final Exception e) {
            ThreadPoolsFactory.runClosureInThread(this.node.getGroupId(), closure
                    , new Status(RaftError.EPERM, "Node is down."));
        }
    }




    //这个是一个监听方法，该类本身就是一个状态机监听器，当前节点的状态机应用了新的日志之后会更新状态机最新应用的日志索引
    //一旦更新了最新应用的日志索引，就会回调状态机监听器中的方法，这时候这个方法就会被回调了
    @Override
    public void onApplied(final long appliedIndex) {
        List<ReadIndexStatus> pendingStatuses = null;
        this.lock.lock();
        try {
            //先判断要被唤醒map中有没有等待被处理的读操作
            //如果没有就直接退出该方法即可
            if (this.pendingNotifyStatus.isEmpty()) {
                return;
            }
            //如果有的话，根据对应的map得到对应的集合，集合中就是等待处理的读操作
            final Map<Long, List<ReadIndexStatus>> statuses = this.pendingNotifyStatus.headMap(appliedIndex, true);
            if (statuses != null) {
                pendingStatuses = new ArrayList<>(statuses.size() << 1);
                final Iterator<Map.Entry<Long, List<ReadIndexStatus>>> it = statuses.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<Long, List<ReadIndexStatus>> entry = it.next();
                    //把所有待处理的读操作放到上面定义的这个pendingStatuses集合中
                    pendingStatuses.addAll(entry.getValue());
                    //这时候就把map中的数据清楚了
                    it.remove();
                }

            }
            //如果只读服务运行出错，就通知业务层操作失败
            if (this.error != null) {
                resetPendingStatusError(this.error.getStatus());
            }
        } finally {
            this.lock.unlock();
            //对pendingStatuses集合判空处理，有待处理的读操作才执行下面的方法
            if (pendingStatuses != null && !pendingStatuses.isEmpty()) {
                for (final ReadIndexStatus status : pendingStatuses) {
                    //在这里通知业务层执行所有读操作
                    notifySuccess(status);
                }
            }
        }
    }

    //专门为测试创建的方法
    void flush() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        this.readIndexQueue.publishEvent((task, sequence) -> task.shutdownLatch = latch);
        latch.await();
    }

    //专门为测试创建的方法
    TreeMap<Long, List<ReadIndexStatus>> getPendingNotifyStatus() {
        return this.pendingNotifyStatus;
    }

    //专门为测试创建的方法
    RaftOptions getRaftOptions() {
        return this.raftOptions;
    }

    private void reportError(final ReadIndexStatus status, final Status st) {
        final long nowMs = Utils.monotonicMs();
        final List<ReadIndexState> states = status.getStates();
        final int taskCount = states.size();
        for (int i = 0; i < taskCount; i++) {
            final ReadIndexState task = states.get(i);
            final ReadIndexClosure done = task.getDone();
            if (done != null) {
                this.nodeMetrics.recordLatency("read-index", nowMs - task.getStartTimeMs());
                done.run(st);
            }
        }
    }


    //通知业务层执行读操作的方法
    private void notifySuccess(final ReadIndexStatus status) {
        final long nowMs = Utils.monotonicMs();
        final List<ReadIndexState> states = status.getStates();
        final int taskCount = states.size();
        for (int i = 0; i < taskCount; i++) {
            final ReadIndexState task = states.get(i);
            final ReadIndexClosure done = task.getDone();
            if (done != null) {
                this.nodeMetrics.recordLatency("read-index", nowMs - task.getStartTimeMs());
                done.setResult(task.getIndex(), task.getRequestContext().get());
                //在这里执行了回调方法，回调方法就是业务层最初传进来的那个回调方法，在回调方法中定义了读操作的逻辑
                done.run(Status.OK());
            }
        }
    }
}
