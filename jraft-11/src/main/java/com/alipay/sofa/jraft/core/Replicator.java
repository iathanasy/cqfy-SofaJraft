package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.CatchUpClosure;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.ReplicatorOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.util.*;
import com.alipay.sofa.jraft.util.internal.ThrowUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.ZeroByteStringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
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
    //复制器状态阻塞时要用到的定时器
    private ScheduledFuture<?> blockTimer;
    //复制器对象阻塞的次数
    private volatile long blockCounter = 0;
    //复制器对象给跟随者发送的每一条请求的序号，这个序号是随着请求递增的
    //并且，这个序列号十分重要，要使用Pipeline模式发送消息，就必须使用这个请求序号
    //这样一来，每一个请求和响应都会根据这个序号一一对应了，处理日志结果的时候也就不会乱序了
    private int reqSeq = 0;
    //要求的响应序号，复制器每发送一条请求，都会给请求分配个序号，处理相应的时候，也会按照请求的序号进行处理
    //假如当前复制器向对应的跟随者发送了一条请求，这条请求的序号为1，不管收没收到响应，还会持续发送请求，请求序号会一直递增
    //但是，在接收到响应的时候，肯定是按照顺序处理的，如果不按顺序处理，那么在判断是否提交日志的时候就会乱成一团
    //而这个成员变量，就是记录的要处理的相应的序号，如果发送了序号为1的请求，就会把1赋值给这个成员变量
    //表示在处理下一个相应的时候，要求下一个响应的序号必须是1，这样才能使请求和响应一一对应，而日志处理也不会乱序
    private int requiredNextSeq = 0;
    //当前Pipeline队列的版本号
    private int version = 0;
    //所有发给跟随者的请求的信息都会被包装到Inflight对象中，在还没有收到对应的响应前，请求会一直存放在这个队列中
    //请求的响应被处理了，才会将请求从队列中移除
    //每次添加Inflight对象，都会从队列尾部添加，这样序号越小的Inflight对象，就会放在队首了
    private final java.util.ArrayDeque<Inflight> inflights = new ArrayDeque<>();
    //这个成员变量会被记录最后发送给跟随者的请求，也可以说是最新的请求
    private Inflight rpcInFly;
    //响应队列，从跟随者收到的没一个响应都会先放到这个相应队列中
    //因为收到的每一个响应的序号可能并不能和要求的请求序号一一对应，所以总要有地方存放这些响应
    private final PriorityQueue<RpcResponse> pendingResponses = new PriorityQueue<>(50);
    //心跳请求连续失败次数
    private int consecutiveErrorTimes = 0;
    //快照读取器
    private volatile SnapshotReader reader;
    //发送安装快照rpc的次数
    private volatile long installSnapshotCounter = 0;
    //操作是否成功的标志
    private boolean hasSucceeded;
    //当前节点的日志进度追上领导者之后，该回调对象封装的方法将会被回调
    private CatchUpClosure catchUpClosure;


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


    //发送的请求的类型
    enum RequestType {
        //安装快照的请求
        Snapshot,
        //日志，心跳，探针请求，这三个请求共用同一个类型
        AppendEntries
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：该方法就是用来给每个请求分配请求序号的，可以看出，请求序号是一直递增的
     */
    private int getAndIncrementReqSeq() {
        final int prev = this.reqSeq;
        this.reqSeq++;
        if (this.reqSeq < 0) {
            this.reqSeq = 0;
        }
        return prev;
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：该方法会在请求的响应成功处理之后被调用，调用的目的是为了递增requiredNextSeq，而递增requiredNextSeq就意味着可以推动请求处理的进度了
     * 比如说请求5收到对应的响应并被正确处理了，这样就可以把requiredNextSeq递增为6，意味着可以处理响应序号为6的响应了
     */
    private int getAndIncrementRequiredNextSeq() {
        final int prev = this.requiredNextSeq;
        this.requiredNextSeq++;
        if (this.requiredNextSeq < 0) {
            this.requiredNextSeq = 0;
        }
        return prev;
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：该类的对象封装的就是每一个发送给跟随者请求的信息
     */
    static class Inflight {
        //本次请求中发送的日志条目的数量
        final int count;
        //本批次日志的第一条日志的索引
        final long startIndex;
        //本批次发送日志的总字节大小
        final int size;
        //请求所对应的Future对象
        final Future<Message> rpcFuture;
        //发送的请求的类型，也许是传输日志请求，也许是安装快照请求
        final RequestType requestType;
        //本次发送请求的请求序号
        final int seq;
        //构造方法
        public Inflight(final RequestType requestType, final long startIndex, final int count, final int size,
                        final int seq, final Future<Message> rpcFuture) {
            super();
            this.seq = seq;
            this.requestType = requestType;
            this.count = count;
            this.startIndex = startIndex;
            this.size = size;
            this.rpcFuture = rpcFuture;
        }

        @Override
        public String toString() {
            return "Inflight [count=" + this.count + ", startIndex=" + this.startIndex + ", size=" + this.size
                    + ", rpcFuture=" + this.rpcFuture + ", requestType=" + this.requestType + ", seq=" + this.seq + "]";
        }

        //判断本次请求是否发送了日志条目
        boolean isSendingLogEntries() {
            return this.requestType == RequestType.AppendEntries && this.count > 0;
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：从跟随者节点收到的响应会被包装到这个类的对象中，在包装的时候就会给每个响应分配好响应序号了
     */
    static class RpcResponse implements Comparable<RpcResponse> {
        //响应的状态
        final Status status;
        //与相应对应的请求
        final Message request;
        //从跟随者节点接收到的响应
        final Message response;
        //请求发送的时间
        final long rpcSendTime;
        //响应序号
        final int seq;
        //本次相应对应的请求类型
        final RequestType requestType;

        //构造方法
        public RpcResponse(final RequestType reqType, final int seq, final Status status, final Message request,
                           final Message response, final long rpcSendTime) {
            super();
            this.requestType = reqType;
            this.seq = seq;
            this.status = status;
            this.request = request;
            this.response = response;
            this.rpcSendTime = rpcSendTime;
        }

        @Override
        public String toString() {
            return "RpcResponse [status=" + this.status + ", request=" + this.request + ", response=" + this.response
                    + ", rpcSendTime=" + this.rpcSendTime + ", seq=" + this.seq + ", requestType=" + this.requestType
                    + "]";
        }

        //将RpcResponse对象存放到pendingResponses队列时，会使用这方法进行对比
        @Override
        public int compareTo(final RpcResponse o) {
            return Integer.compare(this.seq, o.seq);
        }
    }

    //获取存放所有请求信息队列的方法
    ArrayDeque<Inflight> getInflights() {
        return this.inflights;
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
        //这里上锁之后，会在发送完探针消息之后把锁释放了，这个防止并发问题很关键，第六版本引入Pipeline收发消息模式后，就会体现出加锁的重要性了
        //关于这一段的注释，我又思考了一会，现在我觉得这个复制器对象中之所以会加锁，也是为了保证复制器对象在工作的过程中不被销毁
        //比如正在发送日志。心跳消息的时候
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

    //发送探针消息的方法
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

    //发送心跳消息的方法
    public static void sendHeartbeat(final ThreadId id, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> closure) {
        final Replicator r = (Replicator) id.lock();
        if (r == null) {
            RpcUtils.runClosureInThread(closure, new Status(RaftError.EHOSTDOWN, "Peer %s is not connected", id));
            return;
        }
        r.sendEmptyEntries(true, closure);
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

            if (!status.isOk()) {
                if (isLogDebugEnabled) {
                    sb.append(" fail, sleep, status=")
                            .append(status);
                    LOG.debug(sb.toString());
                }
                r.setState(State.Probe);
                if ((r.consecutiveErrorTimes++) % 10 == 0) {
                    LOG.warn("Fail to issue RPC to {}, consecutiveErrorTimes={}, error={}", r.options.getPeerId(),
                            r.consecutiveErrorTimes, status);
                }
                r.startHeartbeatTimer(startTimeMs);
                return;
            }//重置心跳请求连续失败次数
            r.consecutiveErrorTimes = 0;
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
                //r.destroy();
                node.increaseTermTo(response.getTerm(), new Status(RaftError.EHIGHERTERMRESPONSE,
                        "Leader receives higher term heartbeat_response from peer:%s", r.options.getPeerId()));
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
        //这里就是判断是否要安装快照给跟随着节点的逻辑，因为从日志组件中根据索引得到的prevLogTerm为0，但是prevLogIndex不为0
        //这就说明要发送给跟随者节点的前一条日志的任期已经被快照化了，对应的日志也都删除了
        if (prevLogTerm == 0 && prevLogIndex != 0) {
            //这时候就判断一下当前要发送的是不是心跳请求，如果是心跳请求就不必执行安装快照的操作
            //如果不是心跳请求，那就返回false，执行安装快照的操作
            if (!isHeartbeat) {
                Requires.requireTrue(prevLogIndex < this.options.getLogManager().getFirstLogIndex());
                LOG.debug("logIndex={} was compacted", prevLogIndex);
                return false;
            } else {
                prevLogIndex = 0;
            }
        }
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
                //这里我要多强调一句，这里很可能会发送多批次日志
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
            //填充失败就安装快照
            installSnapshot();
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
            //如果AppendEntriesRequest对象发现在经历了上面的填充日志信息的循环后
            //自己根本没有获得任何日志条目，就意味着领导者目前根本没哟日志可以发送给跟随者
            if (rb.getEntriesCount() == 0) {
                //判断是否需要安装快照
                if (nextSendingIndex < this.options.getLogManager().getFirstLogIndex()) {
                    installSnapshot();
                    return false;
                }
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
        System.out.println(request.getEntriesCount()+"领导者要想跟随者节点发送日志了！！！！！！！！！！！");
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
        //记录发送请求的时间
        final long monotonicSendTimeMs = Utils.monotonicMs();
        //得到版本号
        final int v = this.version;
        //得到本次请求的请求序号
        final int seq = getAndIncrementReqSeq();
        //自增发送请求的次数
        this.appendEntriesCounter++;
        Future<Message> rpcFuture = null;
        try {//发送请求给跟随者
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
                            //这个方法会在接收到响应后被回调，这里把本次请求的类型，复制器对象本身，响应状态码，本次请求对象，得到的响应
                            //请求序号，版本号，本次请求发送的时间都传到方法中了
                            onRpcReturned(Replicator.this.id, RequestType.AppendEntries, status, request, getResponse(),
                                    seq, v, monotonicSendTimeMs);
                        }
                    });
        } catch (final Throwable t) {
            RecycleUtil.recycle(recyclable);
            ThrowUtil.throwException(t);
        }
        //把本次请求的信息添加到inflights队列中
        addInflight(RequestType.AppendEntries, nextSendingIndex, request.getEntriesCount(), request.getData().size(),
                seq, rpcFuture);
        return true;
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：该方法的作用就是把一次请求的信息封装到一个Inflight对象中，然后把Inflight对象存放到inflights队列中
     */
    private void addInflight(final RequestType reqType, final long startIndex, final int count, final int size,
                             final int seq, final Future<Message> rpcInfly) {
        //记录最新发送给跟随者节点的请求
        this.rpcInFly = new Inflight(reqType, startIndex, count, size, seq, rpcInfly);
        this.inflights.add(this.rpcInFly);
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
        //根据索引获取日志条目对象，这里就会从内存中获取日志
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
        //如果存放所有请求信息的队列为空，说明没有发送请求
        //或者是发送的请求都已经收到响应，并且响应都成功处理完了，这时候要发送给跟随者节点的下一条日志已经更新到最新了
        //所以直接返回nextIndex即可
        if (this.inflights.isEmpty()) {
            return this.nextIndex;
        }
        //判断一下inflights队列中请求的数量是否大于配置的最大值了，这个最大值反应的是跟随者节点的负载程度
        //如果inflights队列中有太多请求，就意味着还有很多请求没有收到跟随者节点的响应，可能是在网络中阻塞了
        //也可能是跟随者节点还没有处理完请求，跟随者节点压力比较大
        //总之，只要inflights队列中未收到相应的请求太多，就直接返回—1，返回-1就不会再发送日志给跟随者了
        if (this.inflights.size() > this.raftOptions.getMaxReplicatorInflightMsgs()) {
            return -1L;
        }
        //判断最新的请求是否不为空，并且判断最新的请求是不是发送日志的请求
        if (this.rpcInFly != null && this.rpcInFly.isSendingLogEntries()) {
            //如果rpcInFly不为空，并且在请求中发送了一批日志，就直接用这批请求的第一条日志索引加上本批日志数量
            //这样就得到了要发送的下一条日志索引
            return this.rpcInFly.startIndex + this.rpcInFly.count;
        }
        return -1L;
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
        if (!fillCommonFields(rb, this.nextIndex - 1, isHeartbeat)) {
            //填充失败就安装快照安装快照
            installSnapshot();
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
                //获得当前Pipeline队列版本号
                final int stateVersion = this.version;
                //获得本次请求分配到的请求序号
                final int seq = getAndIncrementReqSeq();
                //在这里发送请求
                final Future<Message> rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(),
                        request, -1, new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                            @Override
                            public void run(final Status status) {
                                onRpcReturned(Replicator.this.id, RequestType.AppendEntries, status, request,
                                        getResponse(), seq, stateVersion, monotonicSendTimeMs);
                            }

                        });
                //把这个请求的信息存放到inflights队列中
                addInflight(RequestType.AppendEntries, this.nextIndex, 0, 0, seq, rpcFuture);
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
    static void onRpcReturned(final ThreadId id, final RequestType reqType, final Status status, final Message request,
                              final Message response, final int seq, final int stateVersion, final long rpcSendTime)  {
        //对ThreadId判空
        if (id == null) {
            return;
        }
        //得到处理响应的开始时间
        final long startTimeMs = Utils.nowMs();
        Replicator r;
        //上锁，同时也对复制器对象进行了判空以及是否被销毁的判断处理
        if ((r = (Replicator) id.lock()) == null) {
            return;
        }
        //判断版本号是否一致，这里我要解释一下，复制器对象发送给跟随者节点的每个请求可能并不会总是响应成功
        //比如说，复制器对象向跟随着发送了三个请求，分别传输了3批日志，第一批日志请求收到响应，并且响应成功了，收到过半投票，领导者就可以
        //提交这一批日志了，这时候还有两个请求没有收到响应，所以请求会放在inflights队列中，假如说第二个请求的响应接收到了，但是这个响应
        //是失败的，也就是跟随者节点并没有成功处理这批日志，这时候复制器对象就会重置inflights队列，并且增加inflights队列版本号
        //然后从传输失败的日志开始重新传输，这样才能保证日志处理一直是统一的
        //所以这里会判断一下版本号是否前后不一致了，如果是版本变更之前的请求收到回复了，就直接忽略
        if (stateVersion != r.version) {
            LOG.debug(
                    "Replicator {} ignored old version response {}, current version is {}, request is {}\n, and response is {}\n, status is {}.",
                    r, stateVersion, r.version, request, response, status);
            id.unlock();
            return;
        }
        //这里得到的是当前复制器对象的存放跟随者响应的队列
        final PriorityQueue<RpcResponse> holdingQueue = r.pendingResponses;
        //将接收到这这个响应放到响应队列中，这里看到，这个响应会被包装到RpcResponse对象中，然后把RpcResponse对象放到响应队列中
        //响应的序号其实就是请求序号，这个逻辑在调用onRpcReturned方法的外层方法中可以看到
        //这里还要注意，这个holdingQueue是一个优先级队列，存放RpcResponse对象的时候，是会按照响应序号排序的
        holdingQueue.add(new RpcResponse(reqType, seq, status, request, response, rpcSendTime));
        //这里判断一下响应队列中存放的响应是不是超过了配置的最大值，如果超过了，就意味着肯定有之前某个请求一直没有收到响应
        //因为只有处理了之前请求的响应，才会处理后面请求的响应
        if (holdingQueue.size() > r.raftOptions.getMaxReplicatorInflightMsgs()) {
            LOG.warn("Too many pending responses {} for replicator {}, maxReplicatorInflightMsgs={}",
                    holdingQueue.size(), r.options.getPeerId(), r.raftOptions.getMaxReplicatorInflightMsgs());
            //这时候进行了下面一些操作，首先通过resetInflights方法重置了存放请求队列和响应队列
            //并且会增加版本号，这样一来，旧的请求就都会被丢弃了，然后会重新发送一个探针消息，和跟随者节点确认一下节点状态
            //更新要发送给跟随者节点的下一条日志索引
            r.resetInflights();
            //这里就是设置当前复制器状态为发送探针消息状态
            r.setState(State.Probe);
            //发送探针消息，这里我想多解释一句，其实发送探针消息还是心跳消息，发送的请求内容都差不多，只不过是收到响应后的处理不同
            //心跳消息会持续发送，而探针消息只会发送一次，收到相应并且响应没有错误后就直接开始传输日志了
            r.sendProbeRequest();
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
        try {//定义一个局部变量，记录本次循环处理的相应的个数
            int processed = 0;
            //开始遍历队列
            while (!holdingQueue.isEmpty()) {
                //从响应队列中获得序号最小的那个响应，holdingQueue是优先级队列，排在队首的响应肯定是序号最小的响应
                //注意，现在处理的响应很可能并不是本次收到的响应，收到的响应直接就放到相应队列中了
                //然后根据序号由小到大依次处理
                final RpcResponse queuedPipelinedResponse = holdingQueue.peek();
                //判断这个响应的序号是不是等于要求处理的这个请求序号，如果不相等，说明本次接收到的响应不符合要求
                //也就不能处理，否则会造成日志处理混乱，注意啊，这里是第一次遇到不相等的时候，就会退出循环
                //但是之前循环了那么多次，也许每一次的响应序号和要求的请求序号都匹配上了
                //那就会直接处理这个响应，然后记录处理相应的个数
                if (queuedPipelinedResponse.seq != r.requiredNextSeq) {
                    //这里判断一下，如果在这次循环中成功处理响应了，就记录日志
                    if (processed > 0) {
                        if (isLogDebugEnabled) {
                            sb.append("has processed ").append(processed).append(" responses, ");
                        }
                        break;
                    } else {
                        //如果没有成功处理响应，一个响应也没有处理，那就不再继续发送日志
                        //注意，这里是一次响应也没有处理，也就是说在循环中没有一个响应能匹配requiredNextSeq
                        //那就不继续发送日志了，但只要成功处理过响应，哪怕只有一次，也不会走到这里，还是会继续发送日志的
                        continueSendEntries = false;
                        id.unlock();
                        return;
                    }
                }//从响应队列中，把匹配成功的响应从队列中删除，上面已经获得响应了
                holdingQueue.remove();
                //自增处理响应的次数
                processed++;
                //从请求队列中取出队首的请求，按理说，这时候请求和响应已经匹配上了
                //取出的时候，就会把请求从队列中移除了
                final Inflight inflight = r.pollInflight();
                //对请求进行判空处理，如果这里是null，说明请求队列是空的
                if (inflight == null) {
                    //如果为空，说明请求已经被处理过了，或者根本就没有发过请求
                    //可这里怎么会为空呢？是一个兜底操作吗？
                    if (isLogDebugEnabled) {
                        sb.append("ignore response because request not found: ")
                                .append(queuedPipelinedResponse)
                                .append(",\n");
                    }
                    continue;
                }
                //再次校验请求与响应的序号是否相等，这里怎么会不相等呢？不相等前面不就白判断了吗？
                //也是一个兜底操作吗？除非inflights队列中的请求移除不及时？
                if (inflight.seq != queuedPipelinedResponse.seq) {
                    LOG.warn(
                            "Replicator {} response sequence out of order, expect {}, but it is {}, reset state to try again.",
                            r, inflight.seq, queuedPipelinedResponse.seq);
                    //重置队列
                    r.resetInflights();
                    r.setState(State.Probe);
                    continueSendEntries = false;
                    //这里的这个方法就是设置一个定时器，在超过了设定时间后，再让当前复制器向跟随者节点发送探针消息
                    //然后再判断是否可以继续发送日志，一般这个方法都是在跟随者节点出问题时才会被调用，但是在当前的if分支中，跟随着节点会出什么问题呢？
                    r.block(Utils.nowMs(), RaftError.EREQUEST.getNumber());
                    return;
                }
                try {//这里就是根据具体的请求类型，其实也就是响应类型来处理具体的响应了
                    switch (queuedPipelinedResponse.requestType) {
                        case AppendEntries://复制日志请求收到的响应就交给下面这个方法处理
                            continueSendEntries = onAppendEntriesReturned(id, inflight, queuedPipelinedResponse.status,
                                    (RpcRequests.AppendEntriesRequest) queuedPipelinedResponse.request,
                                    (RpcRequests.AppendEntriesResponse) queuedPipelinedResponse.response, rpcSendTime, startTimeMs, r);
                            break;
                        case Snapshot://如果是安装快照操作的响应，就调用这个方法处理
                            continueSendEntries = onInstallSnapshotReturned(id, r, queuedPipelinedResponse.status,
                                    (RpcRequests.InstallSnapshotRequest) queuedPipelinedResponse.request,
                                    (RpcRequests.InstallSnapshotResponse) queuedPipelinedResponse.response);
                            break;
                    }
                } finally {
                    if (continueSendEntries) {
                        //经过上面的一系列判断和处理后，如果仍然可以继续发送日志，说明肯定序号最小的响应肯定被处理成功了
                        //这就意味着可以让要求处理的请求序号自增，然后进入下一轮循环判断
                        r.getAndIncrementRequiredNextSeq();
                    } else {
                        //走到这里说明序号最小的响应并没有处理成功，但是相应处理失败的时候，已经在onAppendEntriesReturned方法中解锁了
                        //所以这里直接退出即可
                        break;
                    }
                }
            }
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
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：从请求队列中获取请求的方法
     */
    private Inflight pollInflight() {
        return this.inflights.poll();
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：重置请求和响应队列的方法
     */
    void resetInflights() {
        //自增版本号
        this.version++;
        //清空请求和响应队列
        this.inflights.clear();
        this.pendingResponses.clear();
        //更新请求序号
        final int rs = Math.max(this.reqSeq, this.requiredNextSeq);
        //得到下一个要分配的请求序号
        this.reqSeq = this.requiredNextSeq = rs;
        releaseReader();
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/14
     * @方法描述：这里的这个方法就是设置一个定时器，在超过了设定时间后，再让当前复制器向跟随者节点发送探针消息，然后再判断是否可以继续发送日志，
     */
    void block(final long startTimeMs, @SuppressWarnings("unused") final int errorCode) {
        //如果阻塞定时器不为空，说明当前复制器对象已经在阻塞状态中了，直接退出即可
        if(this.blockTimer != null) {
            unlockId();
            return;
        }//阻塞次数自增
        this.blockCounter++;
        //得到定时任务触发时间
        final long dueTime = startTimeMs + this.options.getDynamicHeartBeatTimeoutMs();
        try {
            LOG.debug("Blocking {} for {} ms", this.options.getPeerId(), this.options.getDynamicHeartBeatTimeoutMs());
            //在这里向全局定时器提交了一个定时任务，返回一个ScheduledFuture，赋值给了blockTimer
            this.blockTimer = this.timerManager.schedule(() -> onBlockTimeout(this.id), dueTime - Utils.nowMs(),
                    TimeUnit.MILLISECONDS);
            //将当前复制器的状态设置为阻塞状态
            this.statInfo.runningState = RunningState.BLOCKING;
            unlockId();
        } catch (final Exception e) {
            this.blockTimer = null;
            LOG.error("Fail to add timer", e);
            //如果再添加定时任务的过程中出现异常，就直接发送一个探针请求
            sendProbeRequest();
        }
    }


    static void onBlockTimeout(final ThreadId arg) {
        //在一个线程池中执行onBlockTimeoutInNewThread方法
        RpcUtils.runInThread(() -> onBlockTimeoutInNewThread(arg));
    }

    static void onBlockTimeoutInNewThread(final ThreadId id) {
        if (id != null) {
            continueSending(id, RaftError.ETIMEDOUT.getNumber());
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
    private static boolean onAppendEntriesReturned(final ThreadId id, final Inflight inflight, final Status status,
                                                   final RpcRequests.AppendEntriesRequest request,
                                                   final RpcRequests.AppendEntriesResponse response, final long rpcSendTime,
                                                   final long startTimeMs, final Replicator r) {
        //再次对请求进行校验，这里如果inflight和request代表的是同一个请求
        //那么，下面这个if分支中的两个值应该相等才对。这里的逻辑还是有点绕着，要想知道下面这两个值什么时候相等，还是需要知道Inflight对象的
        //创建时机，以及创建原理，大家可以自己品味品味，这里我想简单谈谈我对jraft框架中一些判断的看法，有些判断可能仅仅只是流程上的谨慎
        //很可能某些情况根本不可能会出现，但是仍然出现了这样的判断，并且如果真的发生错误情况了，就简单粗暴的重置一下请求和响应队列
        //然后重新发送探针请求，进行后续的操作
        if (inflight.startIndex != request.getPrevLogIndex() + 1) {
            LOG.warn(
                    "Replicator {} received invalid AppendEntriesResponse, in-flight startIndex={}, request prevLogIndex={}, reset the replicator state and probe again.",
                    r, inflight.startIndex, request.getPrevLogIndex());
            //如果不相等，说明响应和请求乱序了，直接重置请求和响应队列
            r.resetInflights();
            r.setState(State.Probe);
            //发送探针请求
            r.sendProbeRequest();
            return false;
        }
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
        //这里会判断一下status是否有误，这个status也是从跟随者节点恢复过来的响应
        //也是一个错误响应，只不过这个响应只拥有错误状态码，领导者只需要根据这个错误状态吗判断错误即可
        //在文章中我会为大家讲解什么时候跟随者节点只会回复错误状态码
        if (!status.isOk()) {
            //如果响应有误，那就重置inflight队列
            if (isLogDebugEnabled) {
                sb.append(" fail, sleep, status=") //
                        .append(status);
                LOG.debug(sb.toString());
            }
            r.resetInflights();
            r.setState(State.Probe);
            //复制器对象阻塞一会，然后再向跟随者节点发送请求
            r.block(startTimeMs, status.getCode());
            return false;
        }
        //响应失败，这里处理的响应和日志传输的请求有关
        //处理的是响应内容失败，而不是响应本身有问题
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
                r.resetInflights();
                r.block(startTimeMs, status.getCode());
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
            }
            //重置请求和响应队列，下面会发送新的探针请求
            r.resetInflights();
            //下面是复制日志的对应响应，大家应该还记得，这个方法在发送探针消息和复制日志请求，收到响应后都会被回调
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
                    //从自己的日志组件中获得对应日志的，如果获取不到，其实就是获取到了，但是跟随者节点的日志的任期比领导者还小
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
            r.resetInflights();
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
        r.blockTimer = null;
        //更新接下来要发送的日志的索引，然后会跳出该方法，来到外层onRpcReturned方法，在finally中继续发送日志给跟随者节点
        r.nextIndex += entriesSize;
        //日志复制成功之后，看看复制器对象对应的跟随者是否追赶上了领导者的进度
        r.notifyOnCaughtUp(RaftError.SUCCESS.getNumber(), false);
        return true;
    }




    //该方法就是用来将领导者生成的日志快照远程安装给跟随者节点的
    void installSnapshot() {
        //首先判断一下当前复制器对象是否正处在安装快照的状态中，如果正在安装快照就直接退出该方法
        if (getState() == State.Snapshot) {
            LOG.warn("Replicator {} is installing snapshot, ignore the new request.", this.options.getPeerId());
            unlockId();
            return;
        }
        //定义一个是否需要释放锁的标识
        boolean doUnlock = true;
        //安装快照之前，先连接一下要安装快照的跟随者节点，如果连接失败，直接退出当前方法
        if (!this.rpcService.connect(this.options.getPeerId().getEndpoint())) {
            LOG.error("Fail to check install snapshot connection to peer={}, give up to send install snapshot request.", this.options.getPeerId().getEndpoint());
            block(Utils.nowMs(), RaftError.EHOSTDOWN.getNumber());
            return;
        }
        try {
            //判断快照读取器是否为null，因为快照传输并不是一个频繁的操作，往往只有节点刚加入到一个集群的时候，这个节点可能需要安装领导者的快照
            //或者一个节点故障重启，进度落后太多了，需要安装领导者的快照。这时候才需要创建一个快照读取器，并且使用完这个快照读取器之后
            //在快照成功安装之后，还会把这个快照读取器释放了
            Requires.requireTrue(this.reader == null,
                    "Replicator %s already has a snapshot reader, current state is %s", this.options.getPeerId(),
                    getState());
            //获得快照读取器
            this.reader = this.options.getSnapshotStorage().open();
            //判断快照读取器是否为空，为空则退出该方法
            if (this.reader == null) {
                final NodeImpl node = this.options.getNode();
                final RaftException error = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_SNAPSHOT);
                error.setStatus(new Status(RaftError.EIO, "Fail to open snapshot"));
                unlockId();
                doUnlock = false;
                node.onError(error);
                return;
            }
            //生成远程安装快照文件的url，跟随者节点会通过这个url远程下载快照文件
            final String uri = this.reader.generateURIForCopy();
            if (uri == null) {
                final NodeImpl node = this.options.getNode();
                final RaftException error = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_SNAPSHOT);
                error.setStatus(new Status(RaftError.EIO, "Fail to generate uri for snapshot reader"));
                releaseReader();
                unlockId();
                doUnlock = false;
                node.onError(error);
                return;
            }
            //从快照读取其中获得快照元数据
            final RaftOutter.SnapshotMeta meta = this.reader.load();
            if (meta == null) {
                final String snapshotPath = this.reader.getPath();
                final NodeImpl node = this.options.getNode();
                final RaftException error = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_SNAPSHOT);
                error.setStatus(new Status(RaftError.EIO, "Fail to load meta from %s", snapshotPath));
                releaseReader();
                unlockId();
                doUnlock = false;
                node.onError(error);
                return;
            }
            //在这里创建了安装快照的请求的构建器
            final RpcRequests.InstallSnapshotRequest.Builder rb = RpcRequests.InstallSnapshotRequest.newBuilder();
            //封装一些基本信息
            rb.setTerm(this.options.getTerm());
            rb.setGroupId(this.options.getGroupId());
            rb.setServerId(this.options.getServerId().toString());
            rb.setPeerId(this.options.getPeerId().toString());
            //在下面把快照元数据和url封装到安装快照的请求中
            rb.setMeta(meta);
            rb.setUri(uri);

            //更新复制器当前的运行状态，更新为正在安装快照状态
            this.statInfo.runningState = RunningState.INSTALLING_SNAPSHOT;
            //下面记录的就是本次快照中最后一条日志的索引以及对应任期
            this.statInfo.lastLogIncluded = meta.getLastIncludedIndex();
            this.statInfo.lastTermIncluded = meta.getLastIncludedTerm();

            //创建安装快照请求
            final RpcRequests.InstallSnapshotRequest request = rb.build();
            //设置复制器当前状态
            setState(State.Snapshot);
            //递增快照安装次数
            this.installSnapshotCounter++;
            //获取当前时间戳
            final long monotonicSendTimeMs = Utils.monotonicMs();
            //得到Pipeline的版本号
            final int stateVersion = this.version;
            //获取本次请求的序号
            final int seq = getAndIncrementReqSeq();
            //在这里发送了安装快照的请求
            final Future<Message> rpcFuture = this.rpcService.installSnapshot(this.options.getPeerId().getEndpoint(),
                    request, new RpcResponseClosureAdapter<RpcRequests.InstallSnapshotResponse>() {

                        @Override
                        public void run(final Status status) {
                            onRpcReturned(Replicator.this.id, RequestType.Snapshot, status, request, getResponse(), seq,
                                    stateVersion, monotonicSendTimeMs);
                        }
                    });
            //为该请求创建Inflight对象，并把该对象添加到inflights队列中
            addInflight(RequestType.Snapshot, this.nextIndex, 0, 0, seq, rpcFuture);
        } finally {
            if (doUnlock) {
                unlockId();
            }
        }
    }



    //领导者处理快照请求响应的方法
    @SuppressWarnings("unused")
    static boolean onInstallSnapshotReturned(final ThreadId id, final Replicator r, final Status status,
                                             final RpcRequests.InstallSnapshotRequest request,
                                             final RpcRequests.InstallSnapshotResponse response) {
        //定义一个响应成功的标志
        boolean success = true;
        //先把快照读取器释放了
        r.releaseReader();
        do {
            final StringBuilder sb = new StringBuilder("Node ").
                    append(r.options.getGroupId()).append(":").append(r.options.getServerId()). //
                    append(" received InstallSnapshotResponse from ").append(r.options.getPeerId()). //
                    append(" lastIncludedIndex=").append(request.getMeta().getLastIncludedIndex()). //
                    append(" lastIncludedTerm=").append(request.getMeta().getLastIncludedTerm());
            //判断响应状态是否正确，不正确则退出循环
            if (!status.isOk()) {
                sb.append(" error:").append(status);
                LOG.info(sb.toString());
                //notifyReplicatorStatusListener(r, ReplicatorEvent.ERROR, status);
                if ((r.consecutiveErrorTimes++) % 10 == 0) {
                    LOG.warn("Fail to install snapshot at peer={}, error={}", r.options.getPeerId(), status);
                }//把响应成功标志改为失败
                success = false;
                break;
            }//判断响应是否正确，不正确则把相应成功标志改为失败
            if (!response.getSuccess()) {
                sb.append(" success=false");
                LOG.info(sb.toString());
                success = false;
                break;
            }
            //走到这里意味着响应成功，安装快照成功了，在快照元信息中记录着快照文件最后一条日志的索引
            //这里直接让最后一条日志索引加1，就得到了领导者下一次要向跟随者节点传输日志的索引
            r.nextIndex = request.getMeta().getLastIncludedIndex() + 1;
            sb.append(" success=true");
            LOG.info(sb.toString());
        } while (false);
        //如果快照安装没有成功，就重置Pipeline模式的两个相应队列
        if (!success) {
            r.resetInflights();
            //设置当前复制器对象为探针状态，因为接下来要向跟随者节点发送探针请求，重新定位要发送的日志索引和方式
            r.setState(State.Probe);
            //阻塞一段时间，然后发送探针请求即可
            r.block(Utils.nowMs(), status.getCode());
            return false;
        }
        r.hasSucceeded = true;
        //添加了安装快照成功之后，回调catchUpClosure的方法
        r.notifyOnCaughtUp(RaftError.SUCCESS.getNumber(), false);
//        if (r.timeoutNowIndex > 0 && r.timeoutNowIndex < r.nextIndex) {
//            r.sendTimeoutNow(false, false);
//        }
        //走到这里意味着响应是成功的，直接把复制器状态更新为State.Replicate
        //意味着复制器要向跟随者节点传输日志了
        r.setState(State.Replicate);
        return true;
    }


    //释放快照读取器的方法
    private void releaseReader() {
        if (this.reader != null) {
            Utils.closeQuietly(this.reader);
            this.reader = null;
        }
    }


    State getState() {
        return this.state;
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

    public static boolean stop(final ThreadId id) {
        id.setError(RaftError.ESTOP.getNumber());
        return true;
    }


    //真正等待新添加的节点追赶上领导者的进度的方法
    public static void waitForCaughtUp(final String groupId, final ThreadId id, final long maxMargin, final long dueTime,
                                       final CatchUpClosure done) {
        //得到新添加节点的复制器对象
        final Replicator r = (Replicator) id.lock();
        if (r == null) {
            RpcUtils.runClosureInThread(done, new Status(RaftError.EINVAL, "No such replicator"));
            return;
        }
        try {
            //判断是否已经正在执行相同的操作了，如果回调对象成员变量被赋值了，说明已经执行过相同的操作了
            if (r.catchUpClosure != null) {
                LOG.error("Previous wait_for_caught_up is not over");
                ThreadPoolsFactory.runClosureInThread(groupId, done, new Status(RaftError.EINVAL, "Duplicated call"));
                return;
            }
            //设置新添加的节点和领导者进度的最大差值
            done.setMaxMargin(maxMargin);
            //下面就是向定时任务管理器提交了一个定时任务，当追赶进度超时后，就会执行onCatchUpTimedOut方法
            if (dueTime > 0) {
                done.setTimer(r.timerManager.schedule(() -> onCatchUpTimedOut(id), dueTime - Utils.nowMs(),
                        TimeUnit.MILLISECONDS));
            }
            //这里才会把从外层方法传递过来的回调对象赋值给当前复制器对象的catchUpClosure成员变量
            r.catchUpClosure = done;
        } finally {
            id.unlock();
        }
    }


    //得到领导者最后一次跟对应的跟随者节点发送消息的时间
    public static long getLastRpcSendTimestamp(final ThreadId id) {
        final Replicator r = (Replicator) id.getData();
        if (r == null) {
            return 0L;
        }
        return r.lastRpcSendTimestamp;
    }


    private static void onCatchUpTimedOut(final ThreadId id) {
        final Replicator r = (Replicator) id.lock();
        if (r == null) {
            return;
        }
        try {
            r.notifyOnCaughtUp(RaftError.ETIMEDOUT.getNumber(), false);
        } finally {
            id.unlock();
        }
    }



    //这个方法非常重要，不仅在新添加的节点追赶领导者进度超时后会被调用，还会在新添加的节点对应的复制器对象每次成功传输了一批日志之后被调用
    //该方法的作用也很简答，就是在某个时刻看看复制器对象对应的跟随者是否追赶上了领导者的进度
    private void notifyOnCaughtUp(final int code, final boolean beforeDestroy) {
        //如果回调对象为空直接退出当前方法
        if (this.catchUpClosure == null) {
            return;
        }//如果执行状态码不是超时，那就意味着这个方法是在onAppendEntriesReturned方法中被调用的
        //调用的目的很简单，就是看看传输了一批日志之后，新节点是否追上了领导者节点的进度
        if (code != RaftError.ETIMEDOUT.getNumber()) {
            //this.nextIndex - 1得到的就是新添加的节点拥有的日志数量，也就是最新日志的索引
            //this.catchUpClosure.getMaxMargin()得到的就是和领导者日志的最大差值
            //this.options.getLogManager().getLastLogIndex()得到的就是领导者最新的日志索引
            //如果前两个值加起来比领导者最新日志的索引小，就说明现在还没有追上领导者的进度，直接退出当前方法即可
            if (this.nextIndex - 1 + this.catchUpClosure.getMaxMargin() < this.options.getLogManager()
                    .getLastLogIndex()) {
                return;
            }//这里也是个预判，如果回调对象已经被设置了执行状态标志，那就直接退出当前方法
            if (this.catchUpClosure.isErrorWasSet()) {
                return;
            }//设置执行状态标志，注意，这里大家可能会有疑惑，为什么方法的名字是设置错误的意思
            //这是因为在jraft框架中，把执行的状态码封装到RaftError枚举类中了，就算是执行成功，设置的也是错误状态码中的RaftError.SUCCESS对象
            //大家知道是什么意思即可
            this.catchUpClosure.setErrorWasSet(true);
            if (code != RaftError.SUCCESS.getNumber()) {
                //走到这里意味着新节点追赶领导者进度的操作没有成功
                this.catchUpClosure.getStatus().setError(code, RaftError.describeCode(code));
            }
            if (this.catchUpClosure.hasTimer()) {
                //如果给新节点追赶领导者进度的操作设置了定时任务，这里还要取消定时任务
                if (!beforeDestroy && !this.catchUpClosure.getTimer().cancel(true)) {
                    return;
                }
            }
        } else {//走到这里意味着追赶进度超时了
            if (!this.catchUpClosure.isErrorWasSet()) {
                this.catchUpClosure.getStatus().setError(code, RaftError.describeCode(code));
            }
        }//最后走到这里，不管执行结果是成功还是失败，都要执行回调方法，在回调方法中处理下一个阶段的流程
        final CatchUpClosure savedClosure = this.catchUpClosure;
        this.catchUpClosure = null;
        RpcUtils.runClosureInThread(savedClosure, savedClosure.getStatus());
    }



}
