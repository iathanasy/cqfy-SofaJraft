package com.alipay.sofa.jraft.core;

import com.codahale.metrics.MetricRegistry;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.ReplicatorOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.util.*;
import com.alipay.sofa.jraft.util.internal.ThrowUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.ZeroByteStringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
 * 复制器对象，心跳检测，日志传输都是通过复制器对象进行的
 */
@ThreadSafe
public class Replicator implements ThreadId.OnError {


    private static final Logger LOG = LoggerFactory.getLogger(Replicator.class);

    //要发送到这个复制器对象代表的节点的下一条日志的索引
    private volatile long nextIndex;
    //RPC客户端，大家可以顺着构造方法，看看这个成员变量被赋值的逻辑
    //因为要在该类的对象中执行心跳传输日志复制等工作，肯定需要RPC客户端发送请求啊
    private final RaftClientService rpcService;
    //这个其实就可以看成一个句柄，复制器对象的引用被ThreadId对象持用
    //该对象会交给复制器组管理
    protected ThreadId id;
    //下面两个是配置参数对象
    private final ReplicatorOptions options;
    private final RaftOptions raftOptions;
    //全局定时任务管理器，这个定时任务管理器会提交发送心跳信息的定时任务
    private final Scheduler timerManager;
    //领导者最后一次发送信息的时间
    private volatile long lastRpcSendTimestamp;
    //表示复制器最新的一些统计信息，比如最近发送的一批日志的起始索引和结束索引
    //复制器当前的运行状态等等
    protected Stat statInfo = new Stat();
    //当前复制器的状态，这个状态
    private volatile State state;
    //探针消息发送的次数
    private volatile long probeCounter = 0;
    //心跳计数器，代表发送了多少次心跳请求
    private volatile long heartbeatCounter = 0;
    //心跳定时器
    private ScheduledFuture<?> heartbeatTimer;
    //正在执行的心跳任务
    private Future<Message> heartbeatInFly;
    //代表发送了多少次appendEntries请求
    private volatile long appendEntriesCounter = 0;
    //当前领导者中没有日志的时候，复制器无法向跟随者发送日志消息，于是就会等待日志产生
    //这个就是为当前复制器分配的等待ID
    private long waitId = -1L;


    //这个枚举类中的对象代表的是复制器当前的状态
    public enum State {
        //复制器已被创建
        Created,
        //复制器发送了探针消息
        Probe,
        //复制器正在给跟随者安装快照
        Snapshot,
        //复制器正在向跟随者传输日志
        Replicate,
        //复制器已被销毁
        Destroyed
    }


    //构造方法
    public Replicator(final ReplicatorOptions replicatorOptions, final RaftOptions raftOptions) {
        super();
        this.options = replicatorOptions;
        //要发送的下一条日志的索引默认初始值为领导者最后一条日志索引加1
        this.nextIndex = this.options.getLogManager().getLastLogIndex() + 1;
        this.timerManager = replicatorOptions.getTimerManager();
        this.raftOptions = raftOptions;
        this.rpcService = replicatorOptions.getRaftRpcService();
    }


    //这个枚举类中的对象代表的是当前复制器的运行状态
    enum RunningState {
        //表示空闲状态
        IDLE,
        //正在阻塞中
        BLOCKING,
        //正在向跟随者传输日志
        APPENDING_ENTRIES,
        //给跟随者安装快照
        INSTALLING_SNAPSHOT
    }


    static class Stat {
        //复制器当前的运行状态
        RunningState runningState;
        //第一条日志索引
        long firstLogIndex;
        //快照中最后包含的日志
        long lastLogIncluded;
        //最后一条日志索引
        long lastLogIndex;
        //快照中最后包含的任期
        long lastTermIncluded;

        @Override
        public String toString() {
            return "<running=" + this.runningState + ", firstLogIndex=" + this.firstLogIndex + ", lastLogIncluded="
                    + this.lastLogIncluded + ", lastLogIndex=" + this.lastLogIndex + ", lastTermIncluded="
                    + this.lastTermIncluded + ">";
        }

    }


    //设置当前复制器的状态
    void setState(final State state) {
        this.state = state;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:该类的核心方法，在该方法中创建了复制器对象
     */
    public static ThreadId start(final ReplicatorOptions opts, final RaftOptions raftOptions) {
        //对相关组件进行判空操作，看看日志管理器，投票箱，当前节点是否为空
        if (opts.getLogManager() == null || opts.getBallotBox() == null || opts.getNode() == null) {
            throw new IllegalArgumentException("Invalid ReplicatorOptions.");
        }
        //创建复制器对象，这时候这两个配置参数对象就派上用场了
        final Replicator r = new Replicator(opts, raftOptions);
        //在这里远程连接一下这个复制器对象对应的节点，连接失败返回null
        if (!r.rpcService.connect(opts.getPeerId().getEndpoint())) {
            LOG.error("Fail to init sending channel to {}.", opts.getPeerId());
            return null;
        }
        //创建ThreadId句柄对象，这个对象持有了刚才创建的复制器对象
        r.id = new ThreadId(r, r);
        //这里的代码很有意思，直接解释的话就是给当前复制器对象上了一个锁
        //上锁期间，其他任何线程都不能操纵当前的复制器对象，准确的说不是不能操作当前的复制器对象
        //而是不能对当前的复制器对象中的属性进行修改，实际上，为每一个复制器对象创建的ThreadId对象
        //就相当于当前复制器对象的可重入锁对象，因为在复制器对象工作的时候，还会有其他线程对复制器对象进行操作
        //比如NodeImpl类中就有一些方法要被其他线程调用，然后修改复制器对象中的属性，这时候就要加锁，防止
        //并发问题出现
        //这个上锁和释放锁的操作在复制器对象中非常常见，比如说发送心跳消息呢
        //肯定不能多个线程都在操纵这个复制器对象，来向集群中对应的节点发送心跳消息
        //所以一会大家就会看到，在发送心跳消息的时候是怎么上锁和释放锁的
        //这里上锁之后，会在发送完探针消息之后把锁释放了
        r.id.lock();
        //记录一条日志
        LOG.info("Replicator={}@{} is started", r.id, r.options.getPeerId());
        //记录最后一次发送信息的时间
        r.lastRpcSendTimestamp = Utils.monotonicMs();
        //启动心跳定时器，这个定时器会定时向其他节点发送心跳消息
        r.startHeartbeatTimer(Utils.nowMs());
        //发送探针请求，这里我想多解释几句，探针请求是领导者刚刚当选之后就要向集群中其他节点发送的，这个探针请求的作用也很简单
        //就是通知其他节点领导者已经选举出来了，其他节点会更新它们的领导者信息
        //其他节点在回复这个探针的响应时，还会把自己的日志记录告诉领导者，让领导者决定传输日志的方式
        //看是正常传输还是使用快照
        r.sendProbeRequest();
        //返回ThreadId对象
        return r.id;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:启动心跳定时器的方法
     */
    private void startHeartbeatTimer(final long startMs) {
        final long dueTime = startMs + this.options.getDynamicHeartBeatTimeoutMs();
        try {//使用全局定时任务管理器提交一个定时任务，在这个任务中执行发送心跳消息的请求
            this.heartbeatTimer = this.timerManager.schedule(() -> onTimeout(this.id), dueTime - Utils.nowMs(),
                    TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            LOG.error("Fail to schedule heartbeat timer", e);
            onTimeout(this.id);
        }
    }

    //发送心跳消息的方法，在该方法内部会调用setError方法，在setError方法内部会调用发送心跳消息的方法
    private static void onTimeout(final ThreadId id) {
        if (id != null) {
            id.setError(RaftError.ETIMEDOUT.getNumber());
        } else {
            LOG.warn("Replicator id is null when timeout, maybe it's destroyed.");
        }
    }



    private void sendProbeRequest() {
        sendEmptyEntries(false);
    }

    private void sendEmptyEntries(final boolean isHeartbeat) {
        sendEmptyEntries(isHeartbeat, null);
    }

    //发送心跳消息
    private static void sendHeartbeat(final ThreadId id) {
        //发送之前先上锁
        final Replicator r = (Replicator) id.lock();
        if (r == null) {
            return;
        }
        r.sendEmptyEntries(true);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:收到心跳消息的响应之后，会调用这个方法来处理响应，这个方法
     */
    static void onHeartbeatReturned(final ThreadId id, final Status status, final RpcRequests.AppendEntriesRequest request,
                                    final RpcRequests.AppendEntriesResponse response, final long rpcSendTime) {
        //如果ThreadId为null，这个threadId对象可能已经被销毁了
        if (id == null) {
            return;
        }
        final long startTimeMs = Utils.nowMs();
        Replicator r;
        //先上锁
        if ((r = (Replicator) id.lock()) == null) {
            return;
        }
        boolean doUnlock = true;
        try {//判断是否启动了debug日志模式
            final boolean isLogDebugEnabled = LOG.isDebugEnabled();
            StringBuilder sb = null;
            if (isLogDebugEnabled) {
                sb = new StringBuilder("Node ")
                        .append(r.options.getGroupId())
                        .append(':')
                        .append(r.options.getServerId())
                        .append(" received HeartbeatResponse from ")
                        .append(r.options.getPeerId())
                        .append(" prevLogIndex=")
                        .append(request.getPrevLogIndex())
                        .append(" prevLogTerm=")
                        .append(request.getPrevLogTerm());
            }
            //心跳响应中的任期更大
            if (response.getTerm() > r.options.getTerm()) {
                if (isLogDebugEnabled) {
                    sb.append(" fail, greater term ")
                            .append(response.getTerm())
                            .append(" expect term ")
                            .append(r.options.getTerm());
                    LOG.debug(sb.toString());
                }
                final NodeImpl node = r.options.getNode();
                //这里我删减了部分代码，后面会重构完整，现在的版本还不足以处理脑裂问题
                return;
            }//响应状态为不成功
            if (!response.getSuccess() && response.hasLastLogIndex()) {
                if (isLogDebugEnabled) {
                    sb.append(" fail, response term ")
                            .append(response.getTerm())
                            .append(" lastLogIndex ")
                            .append(response.getLastLogIndex());
                    LOG.debug(sb.toString());
                }
                LOG.warn("Heartbeat to peer {} failure, try to send a probe request.", r.options.getPeerId());
                doUnlock = false;
                //发送探针消息
                r.sendProbeRequest();
                //启动心跳定时器
                r.startHeartbeatTimer(startTimeMs);
                return;
            }
            if (isLogDebugEnabled) {
                LOG.debug(sb.toString());
            }
            if (rpcSendTime > r.lastRpcSendTimestamp) {
                r.lastRpcSendTimestamp = rpcSendTime;
            }//走到这里不管前面结果怎样，都会启动心跳定时器
            //在这个方法中又会重新提交一个心跳定时任务给调度器
            //这里大家应该就能意识到了，领导者的心跳消息就是这样发送的，每一次收到心跳之后
            //在处理心跳的响应时，开启下一次心跳定时任务
            r.startHeartbeatTimer(startTimeMs);
        } finally {
            if (doUnlock) {
                //在这里把锁释放了
                id.unlock();
            }
        }
    }


    //填充AppendEntriesRequest请求中公共字段的方法
    private boolean fillCommonFields(final RpcRequests.AppendEntriesRequest.Builder rb, long prevLogIndex, final boolean isHeartbeat) {
        //这里要注意一种情况，这个地方很重要，如果是整个集群刚启动的情况下，领导者中还没有日志的时候
        //第一条日志的索引默认为1，前一条日志的索引肯定就是0，根据前一条日志索引获取前一条日志的任期
        //这里得到的就是0，这个要理清楚，这样在跟随者那里校验索引和日志的时候，才能校验成功
        final long prevLogTerm = this.options.getLogManager().getTerm(prevLogIndex);
        //该方法在这里省略了一段校验是否需要安装快照的逻辑，在代码的第7版本将会为大家补全
        rb.setTerm(this.options.getTerm());
        rb.setGroupId(this.options.getGroupId());
        rb.setServerId(this.options.getServerId().toString());
        //要把消息发送给的目标节点的节点信息
        rb.setPeerId(this.options.getPeerId().toString());
        rb.setPrevLogIndex(prevLogIndex);
        rb.setPrevLogTerm(prevLogTerm);
        rb.setCommittedIndex(this.options.getBallotBox().getLastCommittedIndex());
        return true;
    }


   /**
    * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
    * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
    * @Date:2023/12/7
    * @Description:发送日志给跟随者的方法，这里要再次强调一下，在sofajraft中，领导者并不会每次只发送一条日志给跟随者，而是会批量发送日志
    * 每次都会发送一批日志，每一次具体发送多少日志，实现逻辑请看sendEntries方法
    */
    void sendEntries() {
        boolean doUnlock = true;
        try {
            long prevSendIndex = -1;
            while (true) {
                //在一个循环中，不断获取要发送的日志的索引，然后发送日志，直到获取到的索引
                //超过了能够发送的日志，也就是说，领导者中的日志已经暂时发送完毕了
                //这时候就会跳出循环
                final long nextSendingIndex = getNextSendIndex();
                if (nextSendingIndex > prevSendIndex) {
                    if (sendEntries(nextSendingIndex)) {
                        prevSendIndex = nextSendingIndex;
                    } else {//走到这里意味着发送失败，可以退出该方法了
                        //但是这并不是意味着领导者就不再向跟随者发送日志了，有可能
                        //是领导者中目前没有日志了，这时候复制器会给领导者的日志管理器
                        //注册一个回调方法，当日志管理器中有新的日志放到内存了，就会通知
                        //复制器继续向跟随者复制日志，很快就会看到具体的逻辑了
                        doUnlock = false;
                        break;
                    }
                } else {//走到这里意味着获得的下一次要发送的日志的索引并不大于上一次
                    //其实是等于，这是另一种情况了，需要在第六版本才能展开讲解，这里就先不展开了
                    break;
                }
            }
        } finally {
            if (doUnlock) {
                unlockId();
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/7
     * @Description:批量发送日志给跟随者的方法，nextSendingIndex是下一条要发送的日志
     */
    private boolean sendEntries(final long nextSendingIndex) {
        final RpcRequests.AppendEntriesRequest.Builder rb = RpcRequests.AppendEntriesRequest.newBuilder();
        //填充公共字段
        if (!fillCommonFields(rb, nextSendingIndex - 1, false)) {
            //填充失败就安装快照，但是这里把安装快照的代码省略了
            //这里也有一个安装快照的操作
            return false;
        }//定义一个ByteBufferCollector对象，要发送的所有日志条目中的data都会放到这个对象中
        ByteBufferCollector dataBuf = null;
        //得到单次可以发送给跟随者的日志条目的最大值
        final int maxEntriesSize = this.raftOptions.getMaxEntriesSize();
        //得到一个RecyclableByteBufferList对象，因为要发送的日志条目会先放到RecyclableByteBufferList对象中
        //RecyclableByteBufferList对象其实继承了ArrayList，所以可以先把要发送的日志条目中的
        //data放到集合的数组中，然后再从数组中放到上面创建的ByteBufferCollector对象中
        final RecyclableByteBufferList byteBufList = RecyclableByteBufferList.newInstance();
        try {//开始在一个循环中封装要发送的日志条目了
            for (int i = 0; i < maxEntriesSize; i++) {
                //创建封装日志元信息的对象，注意，每一个要发送的日志条目对象都对应着一个元信息对象
                //这些日志条目对象的元信息对象会构成一个元信息集合，最后会被放到AppendEntriesRequest请求中
                //发送给跟随者
                final RaftOutter.EntryMeta.Builder emb = RaftOutter.EntryMeta.newBuilder();
                //准备发送日志，在prepareEntry方法中，会把日志条目的元信息和日志条目的data分开存放
                //元信息存放到emb对象中，data存放到byteBufList集合的数组中
                if (!prepareEntry(nextSendingIndex, i, emb, byteBufList)) {
                    //准备失败就意味着没有日志可以发送了，直接退出循环即可
                    break;
                }//在这里把本批次发送的日志条目的元信息集合设置到AppendEntriesRequest请求中
                rb.addEntries(emb.build());
            }
            //这里也是自己添加的，重置下一条要发送的日志的索引，主要是为了配合getNextSendIndex方法的使用
            //第7个版本就不会有这行代码了， rb.getEntriesCount()是本批次要发送的日志条目
            this.nextIndex += rb.getEntriesCount();
            //如果AppendEntriesRequest对象发现在经历了上面的填充日志信息的循环后
            //自己根本没有获得任何日志条目，就意味着领导者目前根本没哟日志可以发送给跟随者
            if (rb.getEntriesCount() == 0) {
                //这里本来有一个判断是否安装快照的操作，但是被我省略了
                //如果领导者内部真的没有日志，这时候就可以让复制器向日志管理器组件提交一个回调方法
                //等有日志了就通知复制器对象继续发送日志，具体的逻辑就在下面的方法中
                waitMoreEntries(nextSendingIndex);
                //因为没有日志可发送，这里就直接退出当前方法了
                return false;
            }//在上面的for循环中，每一个日志条目对象的data都放到了byteBufList集合中
            //这个集合中的数据还没有放到AppendEntriesRequest请求请求中呢，接下来的操作就是将所有的
            //data放到AppendEntriesRequest请求对象中
            //先判断byteBuffer集合中是否存放了数据
            if (byteBufList.getCapacity() > 0) {
                //如果存放了就根据集合中存放的总的数据的大小创建一个ByteBufferCollector对象
                //ByteBufferCollector对象终于要派上用场了
                dataBuf = ByteBufferCollector.allocateByRecyclers(byteBufList.getCapacity());
                //遍历这个byteBufList集合
                for (final ByteBuffer b : byteBufList) {
                    //把集合中的每个data对象都存放到ByteBufferCollector对象的内部成员变量bytebuffer中
                    dataBuf.put(b);
                }//得到ByteBufferCollector对象内部的ByteBuffer对象，这时候，这个ByteBuffer对象已经存放了所有日志条目的data
                final ByteBuffer buf = dataBuf.getBuffer();
                //设置ByteBuffer读模式
                BufferUtils.flip(buf);
                //在ZeroByteStringHelper的帮助细把ByteBuffer中的数据读取到ByteString对象中，这是protobuf协议传输数据用到的对象
                //然后把ByteString对象设置到请求中
                rb.setData(ZeroByteStringHelper.wrap(buf));
            }
        } finally {//在这里将RecyclableByteBufferList对象回收到对象池中
            RecycleUtil.recycle(byteBufList);
        }//构建AppendEntriesRequest请求对象，接下来就要开始发送请求了
        final RpcRequests.AppendEntriesRequest request = rb.build();
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Node {} send AppendEntriesRequest to {} term {} lastCommittedIndex {} prevLogIndex {} prevLogTerm {} logIndex {} count {}",
                    this.options.getNode().getNodeId(), this.options.getPeerId(), this.options.getTerm(),
                    request.getCommittedIndex(), request.getPrevLogIndex(), request.getPrevLogTerm(), nextSendingIndex,
                    request.getEntriesCount());
        }//统计复制器对象当前的一些信息，运行状态为APPENDING_ENTRIES，本批次发送的日志的第一条日志索引
        this.statInfo.runningState = RunningState.APPENDING_ENTRIES;
        this.statInfo.firstLogIndex = rb.getPrevLogIndex() + 1;
        //本批次发送的日志的最后一条日志的索引
        this.statInfo.lastLogIndex = rb.getPrevLogIndex() + rb.getEntriesCount();
        //得到ByteBufferCollector对象，用于收到响应后的回收
        final Recyclable recyclable = dataBuf;
        //记录发送请求的事件
        final long monotonicSendTimeMs = Utils.monotonicMs();
        //自增发送请求的次数
        this.appendEntriesCounter++;
        Future<Message> rpcFuture = null;
        try {//这里还用不到这个rpcFuture，第六版本会用到，发送请求给跟随者
            rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(), request, -1,
                    new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                        @Override
                        public void run(final Status status) {
                            //该方法会在收到AppendEntriesResponse响应后被回调
                            //在这里回收了ByteBufferCollector对象，在源码中就是在这里回收的
                            //但是在这里回收的意义是什么呢？在发送了请求后立刻回收好像也没什么
                            //如果本批次日志没有发送成功，直接再次发送就行了，在源码中为这行代码写了行注释
                            //意思是可以把这个对象的回收提前到请求发送成功之后
                            RecycleUtil.recycle(recyclable);
                            onRpcReturned(Replicator.this.id, status, request, getResponse(),monotonicSendTimeMs);
                        }
                    });
        } catch (final Throwable t) {
            RecycleUtil.recycle(recyclable);
            ThrowUtil.throwException(t);
        }
        return true;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:在该方法中，会把日志条目的元信息和日志条目的data分开存放，日志条目的元信息存放到emb对象中，data存放到byteBufList集合的数组中
     * nextSendingIndex代表下一条要发送的日志索引，offset就是从外层方法传进来的那个i，初始值为0
     */
    boolean prepareEntry(final long nextSendingIndex, final int offset, final RaftOutter.EntryMeta.Builder emb,
                         final RecyclableByteBufferList dateBuffer) {
        //判断dateBuffer集合中存放的数据的字节总量是否已经超过了最大值
        if (dateBuffer.getCapacity() >= this.raftOptions.getMaxBodySize()) {
            return false;
        } //得到要发送的下一条日志的索引，offset就是外层方法中的i，是从0开始的
        final long logIndex = nextSendingIndex + offset;
        //根据索引获取日志条目对象
        final LogEntry entry = this.options.getLogManager().getEntry(logIndex);
        if (entry == null) {
            //没有日志则返回false
            return false;
        }//把日志条目的任期设置到元信息对象中
        emb.setTerm(entry.getId().getTerm());
        //计算校验和
        if (entry.hasChecksum()) {
            //设置校验和
            emb.setChecksum(entry.getChecksum());
        }//设置日志条目的类型，也许是配置变更日志，也许是业务日志
        emb.setType(entry.getType());
        //判断日志中是否有集群节点信息
        if (entry.getPeers() != null) {
            Requires.requireTrue(!entry.getPeers().isEmpty(), "Empty peers at logIndex=%d", logIndex);
            //填充元信息到emb对象中，这里就是把集群节点信息填充到元信息对象中
            fillMetaPeers(emb, entry);
        } else {//走到这里意味着日志条目对象中没有集群节点信息，这时候就要判断一下日志条目是不是配置变更日志
            //如果是配置变更日志就意味着出错了，如果是普通的业务日志，那就没问题
            Requires.requireTrue(entry.getType() != EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION,
                    "Empty peers but is ENTRY_TYPE_CONFIGURATION type at logIndex=%d", logIndex);
        }//接下来就该封装日志中的data数据了，这个才是有效的日志信息，也就是业务指令
        //这里得data的字节长度
        final int remaining = entry.getData() != null ? entry.getData().remaining() : 0;
        //把字节长度填充到元信息对象中，在跟随者节点就是根据这个字节长度从ByteBuffer中获取每一条日志的data的，这个逻辑在第五版本会实现
        emb.setDataLen(remaining);
        if (entry.getData() != null) {
            //把data自身存放到dateBuffer集合中
            dateBuffer.add(entry.getData().slice());
        }//这里大家可能会疑惑，为什么方法都结束了，却没有把每一条日志的索引设置到元信息对象中呢？
        //这里我简单解释一下，这个逻辑并不是在领导者这一端实现的，而是在跟随者接收到日志之后
        //通过领导者发送过来的请求中的上一条日志的索引，用这个索引递增，然后给这一批日志设置索引
        return true;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:该方法的作用是把集群节点信息填充到元信息对象中，逻辑非常简单，就不添加详细注释了
     */
    private void fillMetaPeers(final RaftOutter.EntryMeta.Builder emb, final LogEntry entry) {
        for (final PeerId peer : entry.getPeers()) {
            emb.addPeers(peer.toString());
        }
        if (entry.getOldPeers() != null) {
            for (final PeerId peer : entry.getOldPeers()) {
                emb.addOldPeers(peer.toString());
            }
        }
        if (entry.getLearners() != null) {
            for (final PeerId peer : entry.getLearners()) {
                emb.addLearners(peer.toString());
            }
        }
        if (entry.getOldLearners() != null) {
            for (final PeerId peer : entry.getOldLearners()) {
                emb.addOldLearners(peer.toString());
            }
        }
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/7
     * @Description:得到下一个要发送的日志索引的方法
     */
    long getNextSendIndex() {
        return this.nextIndex;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:当前的领导者节点中暂时没有日志可以发送给跟随者节点时，复制器对象会向日志管理组件提交一个回调方法，当
     * 日志管理器组件有日志落盘的时候，会通知复制器继续发送日志，这时候是直接从内存中获得日志的
     */
    private void waitMoreEntries(final long nextWaitIndex) {
        try {
            LOG.debug("Node {} waits more entries", this.options.getNode().getNodeId());
            //如果等待ID大于0，意味着当前复制器对象已经向日志管理器提交过回调方法了
            if (this.waitId >= 0) {
                //直接退出即可
                return;
            } //向日志管理器提交一个回调方法，日志管理器的wait方法会返回一个waitID
            this.waitId = this.options.getLogManager().wait(nextWaitIndex - 1,
                    (arg, errorCode) -> continueSending((ThreadId) arg, errorCode), this.id);
            //设置当前复制器的运行状态
            this.statInfo.runningState = RunningState.IDLE;
        } finally {
            unlockId();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:继续发送日志给跟随者节点的方法，errCode参数是运行过程中的状态码，这个状态码虽然叫errCode
     * 但并不是就代表着程序出错，这个状态码也会被SUCCESS赋值，这里的这个状态码是从日志管理组件中传递过来的
     * 具体逻辑在第五版本就会实现，ThreadId其实就代表当前复制器对象
     */
    static boolean continueSending(final ThreadId id, final int errCode) {
        if (id == null) {
            return true;
        }
        final Replicator r = (Replicator) id.lock();
        //如果当前复制器对象被销毁了，就直接退出
        if (r == null) {
            return false;
        }
        //下面这个if分支先不讲解，这个是第六版本内容，就两行代码所以就不删除了
        //面的到了第六版本忘了重构回来
        if (errCode == RaftError.ETIMEDOUT.getNumber()) {
            r.sendProbeRequest();
        } else if (errCode != RaftError.ESTOP.getNumber()) {
            //走到这个分支中，表示当前的continueSending方法是被日志管理器调用
            //这也就意味着领导者中有了新的日志，可以继续向跟随者发送了
            //所以会在上面的条件判断中判断一下状态码是不是ESTOP，如果是ESTOP就代表日志管理器组件停止工作了
            //如果不是才进入这个分支中，然后开始继续发送日志给跟随者
            //发送之前重置waitID
            r.waitId = -1;
            //继续发送日志给跟随者
            r.sendEntries();
        } else {//走到这里意味着日志管理器组件停止工作了，复制器对象也就停止向跟随者发送日志了
            LOG.warn("Replicator {} stops sending entries.", id);
            id.unlock();
        }
        return true;
    }




    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:发送心跳或者探针请求的方法，根据方法中的第一个参数判断是否发送心跳还是探针消息，第二个是收到响应后的回调方法
     */
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private void sendEmptyEntries(final boolean isHeartbeat, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> heartBeatClosure) {
        //开是构建AppendEntriesRequest请求，该请求是共用的，日志传输，心跳消息，探针消息都会使用这个请求封装消息内容
        final RpcRequests.AppendEntriesRequest.Builder rb = RpcRequests.AppendEntriesRequest.newBuilder();
        //填充请求中的公共属性，如果填充失败就执行安装快照的操作
        //fillCommonFields方法中省略了一段校验是否需要安装快照的逻辑
        if (!fillCommonFields(rb, this.nextIndex - 1, isHeartbeat)) {
            //这里暂时删掉了安装快照的操作
            if (isHeartbeat && heartBeatClosure != null) {
                RpcUtils.runClosureInThread(heartBeatClosure, new Status(RaftError.EAGAIN,
                        "Fail to send heartbeat to peer %s", this.options.getPeerId()));
            }
            return;
        }
        try {
            //当前发送请求的时间
            final long monotonicSendTimeMs = Utils.monotonicMs();
            //根据方法中的参数判断是否为心跳请求
            if (isHeartbeat) {
                LOG.info("LEADER发送了心跳消息！");
                //创建AppendEntriesRequest对象封装消息内容
                final RpcRequests.AppendEntriesRequest request = rb.build();
                //心跳计数器自增
                this.heartbeatCounter++;
                //定义一个回调方法，该方法会在接收到心跳响应后被回调
                RpcResponseClosure<RpcRequests.AppendEntriesResponse> heartbeatDone;
                //判断参数中的回调方法是否为空
                if (heartBeatClosure != null) {
                    heartbeatDone = heartBeatClosure;
                } else {//如果为空就创建一个新的
                    heartbeatDone = new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                        @Override
                        public void run(final Status status) {
                            //在创建的新的回调方法中，收到心跳响应之后，会回调这个方法，在这个方法中会提交下一个心跳消息任务给全局定时器
                            onHeartbeatReturned(Replicator.this.id, status, request, getResponse(), monotonicSendTimeMs);
                        }
                    };
                }
                //发送心跳消息
                this.heartbeatInFly = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(), request,
                        this.options.getElectionTimeoutMs() / 2, heartbeatDone);
            } else {
                //走到这里意味着不是心跳消息，而是探针消息
                rb.setData(ByteString.EMPTY);
                //创建AppendEntriesRequest对象封装探针消息
                final RpcRequests.AppendEntriesRequest request = rb.build();
                //这里可以看到，发送探针消息时，会给复制器的运行状态设置为APPENDING_ENTRIES
                this.statInfo.runningState = RunningState.APPENDING_ENTRIES;
                //这里的这个statInfo对象，严格来说其实是按照传输日志的状态来设置的
                //传输日志的时候，复制器是批量传输给跟随者的，所以会设置传输的这批日志中的
                //第一条日志的索引，也会设置最后一条日志的索引
                //但现在是在发送探针消息，发送探针消息的作用已经在start方法中解释过了
                //探针消息中并不会涉及日志的传输，所以下面给firstLogIndex和lastLogIndex
                //赋值就使用nextIndex来操作了，表示本次发送的消息一条日志也没有传输
                this.statInfo.firstLogIndex = this.nextIndex;
                this.statInfo.lastLogIndex = this.nextIndex - 1;
                //发送探测消息的次数加1
                this.probeCounter++;
                //设置复制器当前的状态，表示刚刚发送了探针消息
                setState(State.Probe);
                //发送探针消息
                final Future<Message> rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(),
                        request, -1, new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                            @Override
                            public void run(final Status status) {
                                //该方法在第四版本实现了
                                onRpcReturned(Replicator.this.id, status, request, getResponse(), monotonicSendTimeMs);
                            }
                        });

            }
            LOG.debug("Node {} send HeartbeatRequest to {} term {} lastCommittedIndex {}", this.options.getNode()
                    .getNodeId(), this.options.getPeerId(), this.options.getTerm(), rb.getCommittedIndex());
        } finally {
            //释放锁，这里释放的是sendHeartbeat(final ThreadId id)方法中获得的锁，就是这个方法
            //private static void sendHeartbeat(final ThreadId id) {
            //        //发送之前先上锁
            //        final Replicator r = (Replicator) id.lock();
            //        if (r == null) {
            //            return;
            //        }
            //        r.sendEmptyEntries(true);
            //    }
            unlockId();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/7
     * @Description:该方法会在探针请求收到响应后被回调，也会在给跟随者发送传输日志的请求，收到响应后被回调
     * 在第四版本的代码中，该方法做了大量删减，很多方法参数用不上，第五版本会重构完整，这里这个status还没有用到，等到第六版本就会用到了
     * 这个status还和Bolt框架有关系，所以牵扯到的知识还是有点复杂的，在实现了领导者传输日志给跟随者的功能之后再具体讲解
     */
    @SuppressWarnings("ContinueOrBreakFromFinallyBlock")
    static void onRpcReturned(final ThreadId id, final Status status, final Message request, final Message response,final long rpcSendTime) {
        //对ThreadId判空
        if (id == null) {
            return;
        }
        Replicator r;
        //上锁，同时也对复制器对象进行了判空以及是否被销毁的判断处理
        if ((r = (Replicator) id.lock()) == null) {
            return;
        }
       //定义一个是否继续发送日志的标记
        boolean continueSendEntries = false;
        final boolean isLogDebugEnabled = LOG.isDebugEnabled();
        StringBuilder sb = null;
        if (isLogDebugEnabled) {
            sb = new StringBuilder("Replicator ")
                    .append(r)
                    .append(" is processing RPC responses, ");
        }
        try {//在这里开始处理响应了，如果返回为true，说明上一次发送的请求没有问题
            //可以继续发送日志给跟随者了
            continueSendEntries = onAppendEntriesReturned(id,(RpcRequests.AppendEntriesRequest)request,(RpcRequests.AppendEntriesResponse)response,rpcSendTime, r);
        } finally {
            if (isLogDebugEnabled) {
                sb.append("after processed, continue to send entries: ")
                        .append(continueSendEntries);
                LOG.debug(sb.toString());
            }
            //判断是否可以继续发送日志给跟随者节点
            if (continueSendEntries) {
                //在这里继续发送下一批日志
                r.sendEntries();
            }
        }
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:具体处理领导者发送了AppendEntriesRequest请求后，接收到跟随者节点的响应的方法，这里我要多解释一下，这里的处理响应的方法
     * 逻辑本身是很长的，只不过我在第四版本删减了一些，这个方法的逻辑大体上可以分为两部分，第一部分是是通过status判断响应本身有没有问题，着一部分和Bolt框架远程调用有关
     * 可能是响应超时了，也可能是根本就没发出去，因为跟随者节点崩溃了，或者是其他的未知原因，这种情况下有一个统一的处理手段，那就是让当前发送日志的复制器阻塞一会
     * 过了一定的时间后再向跟随者节点发送日志。第二部分逻辑就是判断响应的内容有没有问题，比如跟随者节点的日志任期和领导者不一致，跟随者节点的日志进度和领导者不一致等等
     * 也许已经发生网络分区了，这也是响应失败的情况，这时候，就要具体情况具体处理了。当然，再有就是响应成功的情况，如果响应成功了
     * 就说明日志已经被跟随者节点投票了，这时候就要根据投票结果判断日志是不是可以在领导者中提交状态机应用了
     */
    private static boolean onAppendEntriesReturned(final ThreadId id, final RpcRequests.AppendEntriesRequest request, final RpcRequests.AppendEntriesResponse response, final long rpcSendTime,final Replicator r) {
        //下面记录日志的操作就不添加注释了
        final boolean isLogDebugEnabled = LOG.isDebugEnabled();
        StringBuilder sb = null;
        if (isLogDebugEnabled) {
            sb = new StringBuilder("Node ")
                    .append(r.options.getGroupId())
                    .append(':')
                    .append(r.options.getServerId())
                    .append(" received AppendEntriesResponse from ")
                    .append(r.options.getPeerId())
                    .append(" prevLogIndex=")
                    .append(request.getPrevLogIndex())
                    .append(" prevLogTerm=")
                    .append(request.getPrevLogTerm())
                    .append(" count=")
                    .append(request.getEntriesCount());
        }
        //响应失败，这里处理的响应和日志传输的请求有关
        //处理的是响应内容失败，而不是响应本身有问题，第一部分响应失败的逻辑被我删除了，第六版本会重构完整
        if (!response.getSuccess()) {
            //响应的状态码是EBUSY，这种情况就是跟随者节点是繁忙状态，所谓繁忙状态就是日志处理不过来，日志超负载了
            //这个逻辑会在第五版本重构，在第四版本我还没有为大家实现跟随者节点处理日志请求的逻辑
            if(response.getErrorResponse().getErrorCode() == RaftError.EBUSY.getNumber()) {
                //如果是跟随者节点繁忙，记录一条日志
                if (isLogDebugEnabled) {
                    sb.append(" is busy, sleep, errorMsg='")
                            .append(response.getErrorResponse().getErrorMsg()).append("'");
                    LOG.debug(sb.toString());
                }//重置当前复制器对象的状态
                r.setState(State.Probe);
                //这里我把下面一行代码注释了，现在还不展开具体的逻辑，等第六版本再实现
                //但这里可以简单讲解一下这个逻辑，和上面那行代码有关，在上面这行代码中，把当前复制器的状态
                //设置为发送探针消息状态了，这是因为在下面这行被注释掉的代码中会给当前复制器对象的运行状态设置为阻塞状态
                //然后设置一个定时任务，当超过了定时任务的触发事件，就会重新向跟随者节点发送探针消息，然后
                //继续给跟随者发送日志，也许这时候跟随者节点已经没有那么繁忙了
                //r.block(startTimeMs, status.getCode());
                return false;
            }
            //这里仍然是在响应失败的分支中呢，这时候判断一下响应中的跟随者节点的任期是否比当前领导者大
            //如果大显然就是发生过网络分区，这时候就要让当前领导者进行身份降级
            if (response.getTerm() > r.options.getTerm()) {
                if (isLogDebugEnabled) {
                    sb.append(" fail, greater term ")
                            .append(response.getTerm())
                            .append(" expect term ")
                            .append(r.options.getTerm());
                    LOG.debug(sb.toString());
                } //获取表示当前领导者的节点对象
                final NodeImpl node = r.options.getNode();
                //因为当前领导者要身份降级了，不是领导者显然就不能管理复制器对象
                //因此复制器对象也要销毁了，这个逻辑暂且不实现，后面再实现
                //r.destroy();
                //将自己的任期值增加，然后将自己身份降级
                node.increaseTermTo(response.getTerm(), new Status(RaftError.EHIGHERTERMRESPONSE,
                        "Leader receives higher term heartbeat_response from peer:%s", r.options.getPeerId()));
                return false;
            }//下面是记录日志的操作
            if (isLogDebugEnabled) {
                sb.append(" fail, find nextIndex remote lastLogIndex ").append(response.getLastLogIndex())
                        .append(" local nextIndex ").append(r.nextIndex);
                LOG.debug(sb.toString());
            } //更新上一次向跟随者发送请求的事件
            if (rpcSendTime > r.lastRpcSendTimestamp) {
                r.lastRpcSendTimestamp = rpcSendTime;
            }//下面是复制日志的对应响应，大家应该还记得，这个方法在发送探针消息和复制日志请求，收到响应后都会被回调
            //判断跟随者的要接收的下一条日志是否比领导者要发送的下一条日志索引小
            //如果小则意味着跟随者节点进度落后
            if (response.getLastLogIndex() + 1 < r.nextIndex) {
                LOG.debug("LastLogIndex at peer={} is {}", r.options.getPeerId(), response.getLastLogIndex());
                //这里就把下一条要发送的日志索引设置为跟随者节点的，
                r.nextIndex = response.getLastLogIndex() + 1;
            } else {
                //走到这里意味着跟随者的下一条要接接收的日志索引比领导者还大
                if (r.nextIndex > 1) {
                    LOG.debug("logIndex={} dismatch", r.nextIndex);
                    //这个要从跟随者的handleAppendEntriesRequest方法中查看一下，是怎么根据领导者要发送的下一条日志的前一条日志的索引
                    //从自己的日志组件中获得对应日志的，如果获取不到，其实就是获取到了，但是跟随者节点的日志的任期比领导者还大
                    //这时候需要让领导者的要发送的下一条日志递减，直到可以跟跟随者匹配到相同任期的日志，然后让领导者开始传输复制即可
                    //之前跟随者的日志比较大可能是因为旧的领导者发送的，比如整个集群就发送给了这一个节点，但是还没有提交，还没来得及提交旧的领导者就宕机了
                    r.nextIndex--;
                } else {
                    LOG.error("Peer={} declares that log at index=0 doesn't match, which is not supposed to happen",
                            r.options.getPeerId());
                }
            }
            //这里发送的就是探针消息，发送这个是为了开启下一次发送日志的操作
            r.sendProbeRequest();
            return false;
        }
        if (isLogDebugEnabled) {
            sb.append(", success");
            LOG.debug(sb.toString());
        }
        //到这里会再判断一下跟随者响应给领导者的任期，如果任期不相等，肯定是
        //跟随者的任期小于领导者，因为上面已经处有了一个跟随者的任期大于领导者的分支了
        //但我没想到那种情况返回的响应中的任期会小于当前领导者的任期
        //走到这这里就意味着响应是成功的，已经不在上面的响应错误的分支中了
        //如果响应是成功的，这里处理的也可能是探针消息的响应，但如果是探针消息，并且探针消息的响应也是成功
        //跟随者的handleAppendEntriesRequest方法可是全程加锁的，不可能出现并发问题
        //那么跟随者的任期肯定是和领导者相等的，这里也许只是个兜底的判断
        if (response.getTerm() != r.options.getTerm()) {
            r.setState(State.Probe);
            LOG.error("Fail, response term {} dismatch, expect term {}", response.getTerm(), r.options.getTerm());
            id.unlock();
            return false;
        } //更新上一次向跟随者发送请求的事件
        if (rpcSendTime > r.lastRpcSendTimestamp) {
            r.lastRpcSendTimestamp = rpcSendTime;
        }  //得到这次请求批量发送的日志的数量
        final int entriesSize = request.getEntriesCount();
        //如果数量大于0，就说明这次的请求肯定是日志复制的请求，并且也成功了
        if (entriesSize > 0) {
            //判断当前复制器对象对应的身份是不是跟随者
            if (r.options.getReplicatorType().isFollower()) {
                //如果是的话，就要在领导者判断发送成功的日志是否收到过半投票了
                //如果收到了就把这些日志应用到状态机中
                r.options.getBallotBox().commitAt(r.nextIndex, r.nextIndex + entriesSize - 1, r.options.getPeerId());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Replicated logs in [{}, {}] to peer {}", r.nextIndex, r.nextIndex + entriesSize - 1,
                        r.options.getPeerId());
            }
        }//把复制器状态设置为传输日志的状态
        r.setState(State.Replicate);
        //更新接下来要发送的日志的索引，然后会跳出该方法，来到外层onRpcReturned方法，在finally中继续发送日志给跟随者节点
        r.nextIndex += entriesSize;
        return true;
    }



    //释放锁的方法
    private void unlockId() {
        if (this.id == null) {
            return;
        }
        this.id.unlock();
    }

    //在该方法中执行了心跳定时任务的执行
    @Override
    public void onError(final ThreadId id, final Object data, final int errorCode) {
        final Replicator r = (Replicator) data;
        RpcUtils.runInThread(() -> sendHeartbeat(id));
    }
}
