package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.ReplicatorOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.util.ThreadId;
import com.alipay.sofa.jraft.util.Utils;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
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
    //心跳计数器，代表发送了多少次心跳请求
    private volatile long heartbeatCounter = 0;
    //心跳定时器
    private ScheduledFuture<?> heartbeatTimer;
    //正在执行的心跳任务
    private Future<Message> heartbeatInFly;

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


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:该类的核心方法，在该方法中创建了复制器对象
     */
    public static ThreadId start(final ReplicatorOptions opts, final RaftOptions raftOptions) {
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
        //上锁期间，其他任何线程都不能操纵当前的复制器对象
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
        rb.setTerm(this.options.getTerm());
        rb.setGroupId(this.options.getGroupId());
        rb.setServerId(this.options.getServerId().toString());
        rb.setPeerId(this.options.getPeerId().toString());
        rb.setPrevLogIndex(prevLogIndex);
        rb.setPrevLogTerm(0);
        rb.setCommittedIndex(0);
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
        //填充请求中的公共属性，这里的0是写死的，在第四版本就会把这里重构成和源码一致
        fillCommonFields(rb, 0, isHeartbeat);
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
                //发送探针消息，这里我把回调方法给省略了，探针消息收到响应后，会在回调方法中对日志模块进行一系列处理，在第一版本还不用讲那么多，就省略了
                final Future<Message> rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(),
                        request, -1, new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                            @Override
                            public void run(final Status status) {
                               //回调方法暂时不做处理
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
