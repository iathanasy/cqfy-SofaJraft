package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.ReplicatorGroup;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.rpc.impl.AbstractClientService;
import com.alipay.sofa.jraft.rpc.impl.FutureImpl;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.Utils;
import com.alipay.sofa.jraft.util.concurrent.DefaultFixedThreadsExecutorGroupFactory;
import com.alipay.sofa.jraft.util.concurrent.FixedThreadsExecutorGroup;
import com.google.protobuf.Message;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:JRaft框架内部的客户端，这个客户端是用来在集群中访问其他节点，进行各种raft活动的，比如选主，投票，预投票，发送日志等等
 */
public class DefaultRaftClientService extends AbstractClientService implements RaftClientService {

    private final FixedThreadsExecutorGroup appendEntriesExecutors;

    private final ConcurrentMap<Endpoint, Executor> appendEntriesExecutorMap = new ConcurrentHashMap<>();

    private NodeOptions nodeOptions;
    private final ReplicatorGroup rgGroup;

    @Override
    protected void configRpcClient(final RpcClient rpcClient) {
        rpcClient.registerConnectEventListener(this.rgGroup);
    }

    public DefaultRaftClientService(final ReplicatorGroup rgGroup) {
        this(rgGroup, DefaultFixedThreadsExecutorGroupFactory.INSTANCE.newExecutorGroup(
                Utils.APPEND_ENTRIES_THREADS_SEND, "Append-Entries-Thread-Send", Utils.MAX_APPEND_ENTRIES_TASKS_PER_THREAD,
                true));
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:这个构造方法会在NodeImpl类的init方法中被调用，调用的时候会传进来执行器组，会为该类的执行器组成员变量赋值
     */
    public DefaultRaftClientService(final ReplicatorGroup rgGroup,
                                    final FixedThreadsExecutorGroup customAppendEntriesExecutors) {
        this.rgGroup = rgGroup;
        this.appendEntriesExecutors = customAppendEntriesExecutors;
    }

    //初始化的方法，初始化的核心逻辑在该类的父类init方法中进行
    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        final boolean ret = super.init(rpcOptions);
        if (ret) {
            this.nodeOptions = (NodeOptions) rpcOptions;
        }
        return ret;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:客户端发送预投票请求的方法
     * @Description:
     */
    @Override
    public Future<Message> preVote(final Endpoint endpoint, final RpcRequests.RequestVoteRequest request,
                                   final RpcResponseClosure<RpcRequests.RequestVoteResponse> done) {
        if (!checkConnection(endpoint, true)) {
            return onConnectionFail(endpoint, request, done, this.rpcExecutor);
        }

        return invokeWithDone(endpoint, request, done, this.nodeOptions.getElectionTimeoutMs());
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:发送正式投票请求的方法
     */
    @Override
    public Future<Message> requestVote(final Endpoint endpoint, final RpcRequests.RequestVoteRequest request,
                                       final RpcResponseClosure<RpcRequests.RequestVoteResponse> done) {
        if (!checkConnection(endpoint, true)) {
            return onConnectionFail(endpoint, request, done, this.rpcExecutor);
        }

        return invokeWithDone(endpoint, request, done, this.nodeOptions.getElectionTimeoutMs());
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:发送心跳和日志给跟随者的方法
     */
    @Override
    public Future<Message> appendEntries(final Endpoint endpoint, final RpcRequests.AppendEntriesRequest request,
                                         final int timeoutMs, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> done) {

        //这里可以发现，发送日志和心跳信息之后，在收到响应执行器回调方法时使用的是这个appendEntriesExecutors执行器组中的一个执行器
        //这里有点Netty中从执行器组中获得一个单线程执行器的感觉，实际上也正是如此，这个appendEntriesExecutors执行器组中管理的就是一个个单线程执行器
        //也就是一个SingleThreadExecutor对象，这里的关系也很明确，通过一个Map，以及Map的computeIfAbsent方法
        //让appendEntriesExecutors执行器组中的每一个单线程执行器都对应一个endpoint。这样一来，每一个单线程执行器都负责一个专属节点的响应回调了
        //为什么这么设计呢？原因说简单很简单，说复杂真的也有点复杂，心跳消息响应的回调到没什么，关键是领导者向每一个跟随者节点发送日志请求后收到响应的回调
        //也就是收到响应之后要做哪些工作，这是后面的内容，要做的内容还是很多的，如果是在多线程情况下处理，还会出现并发问题，所以就给每一个跟随者复制器对象
        //分配了一个单线程执行器，具体处理什么工作讲到日志复制的知识时再说。这里有点像xxljob执行器执行定时任务一样，给每一个定时任务分配一个线程
        //关于这个单线程执行器的被调用的逻辑，我录制了一个小视频为大家讲解。因为这涉及到和bolt这个基于Netty的RPC框架的执行流程，这个bolt框架的源码我也是刚看
        //流程也看了个大概，明白回调方法是在哪里被执行的了，为了让大家对这个框架的工作流程更清晰，我有必要为大家借着讲解回调方法的回调时机，为大家梳理一下
        //这个bolt框架的工作流程
        final Executor executor = this.appendEntriesExecutorMap.computeIfAbsent(endpoint, k -> appendEntriesExecutors.next());

        if (!checkConnection(endpoint, true)) {
            return onConnectionFail(endpoint, request, done, executor);
        }
        return invokeWithDone(endpoint, request, done, timeoutMs, executor);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/22
     * @Description:剩下的几个方法在第一版本中不必关注
     */
    @Override
    public Future<Message> getFile(final Endpoint endpoint, final RpcRequests.GetFileRequest request, final int timeoutMs,
                                   final RpcResponseClosure<RpcRequests.GetFileResponse> done) {
        final InvokeContext ctx = new InvokeContext();
        ctx.put(InvokeContext.CRC_SWITCH, true);
        return invokeWithDone(endpoint, request, ctx, done, timeoutMs);
    }

    @Override
    public Future<Message> installSnapshot(final Endpoint endpoint, final RpcRequests.InstallSnapshotRequest request,
                                           final RpcResponseClosure<RpcRequests.InstallSnapshotResponse> done) {
        return invokeWithDone(endpoint, request, done, this.rpcOptions.getRpcInstallSnapshotTimeout());
    }

    @Override
    public Future<Message> timeoutNow(final Endpoint endpoint, final RpcRequests.TimeoutNowRequest request, final int timeoutMs,
                                      final RpcResponseClosure<RpcRequests.TimeoutNowResponse> done) {
        return invokeWithDone(endpoint, request, done, timeoutMs);
    }

    @Override
    public Future<Message> readIndex(final Endpoint endpoint, final RpcRequests.ReadIndexRequest request, final int timeoutMs,
                                     final RpcResponseClosure<RpcRequests.ReadIndexResponse> done) {
        return invokeWithDone(endpoint, request, done, timeoutMs);
    }

    private Future<Message> onConnectionFail(final Endpoint endpoint, final Message request, Closure done, final Executor executor) {
        final FutureImpl<Message> future = new FutureImpl<>();
        executor.execute(() -> {
            final String fmt = "Check connection[%s] fail and try to create new one";
            if (done != null) {
                try {
                    done.run(new Status(RaftError.EINTERNAL, fmt, endpoint));
                } catch (final Throwable t) {
                    LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                }
            }
            if (!future.isDone()) {
                future.failure(new RemotingException(String.format(fmt, endpoint)));
            }
        });
        return future;
    }
}
