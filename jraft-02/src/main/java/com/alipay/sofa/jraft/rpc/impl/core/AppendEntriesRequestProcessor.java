package com.alipay.sofa.jraft.rpc.impl.core;

import com.alipay.sofa.jraft.JRaftUtils;
import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.NodeManager;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.rpc.impl.ConnectionClosedEventListener;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;
import com.alipay.sofa.jraft.util.Utils;
import com.alipay.sofa.jraft.util.concurrent.ConcurrentHashSet;
import com.alipay.sofa.jraft.util.concurrent.MpscSingleThreadExecutor;
import com.alipay.sofa.jraft.util.concurrent.SingleThreadExecutor;
import com.google.protobuf.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/24
 * @Description:JRaft框架服务端需要的处理器，这个处理器是专门处理AppendEntriesRequest请求的，但在第一版本中该类中的大部分方法都用不上
 * 基本上不加注释的都用不上，后面版本才会用上，而且非常重要，后面会详细讲解
 */
public class AppendEntriesRequestProcessor extends NodeRequestProcessor<RpcRequests.AppendEntriesRequest> implements
        ConnectionClosedEventListener {

    static final String PAIR_ATTR = "jraft-peer-pairs";

    //执行器选择器，bolt框架中会调用当前处理器对象的这个方法，然后返回当前处理器对象中定义的执行器
    //这个执行器就会专门用来处理当前执行器对象中定义的处理AppendEntriesRequest请求的方法
    //也就是执行processRequest0方法
    final class PeerExecutorSelector implements ExecutorSelector {

        PeerExecutorSelector() {
            super();
        }

        @Override
        public Executor select(final String reqClass, final Object reqHeader) {
            final RpcRequests.AppendEntriesRequestHeader header = (RpcRequests.AppendEntriesRequestHeader) reqHeader;
            final String groupId = header.getGroupId();
            final String peerId = header.getPeerId();
            final String serverId = header.getServerId();

            final PeerId peer = new PeerId();

            if (!peer.parse(peerId)) {
                return executor();
            }

            final Node node = NodeManager.getInstance().get(groupId, peer);

            if (node == null || !node.getRaftOptions().isReplicatorPipeline()) {
                return executor();
            }

            // The node enable pipeline, we should ensure bolt support it.
            RpcFactoryHelper.rpcFactory().ensurePipeline();

            final PeerRequestContext ctx = getOrCreatePeerRequestContext(groupId, pairOf(peerId, serverId), null);

            return ctx.executor;
        }
    }

    /**
     * RpcRequestClosure that will send responses in pipeline mode.
     *
     * @author dennis
     */
    class SequenceRpcRequestClosure extends RpcRequestClosure {

        private final int      reqSequence;
        private final String   groupId;
        private final PeerPair pair;
        private final boolean  isHeartbeat;

        public SequenceRpcRequestClosure(final RpcRequestClosure parent, final Message defaultResp,
                                         final String groupId, final PeerPair pair, final int sequence,
                                         final boolean isHeartbeat) {
            super(parent.getRpcCtx(), defaultResp);
            this.reqSequence = sequence;
            this.groupId = groupId;
            this.pair = pair;
            this.isHeartbeat = isHeartbeat;
        }

        @Override
        public void sendResponse(final Message msg) {
            if (this.isHeartbeat) {
                super.sendResponse(msg);
            } else {
                sendSequenceResponse(this.groupId, this.pair, this.reqSequence, getRpcCtx(), msg);
            }
        }
    }

    /**
     * Response message wrapper with a request sequence number and asyncContext.done
     *
     * @author dennis
     */
    static class SequenceMessage implements Comparable<SequenceMessage> {
        public final Message     msg;
        private final int        sequence;
        private final RpcContext rpcCtx;

        public SequenceMessage(final RpcContext rpcCtx, final Message msg, final int sequence) {
            super();
            this.rpcCtx = rpcCtx;
            this.msg = msg;
            this.sequence = sequence;
        }

        /**
         * Send the response.
         */
        void sendResponse() {
            this.rpcCtx.sendResponse(this.msg);
        }

        /**
         * Order by sequence number
         */
        @Override
        public int compareTo(final SequenceMessage o) {
            return Integer.compare(this.sequence, o.sequence);
        }
    }

    // constant pool for peer pair
    private final Map<String, Map<String, PeerPair>> pairConstants = new HashMap<>();

    PeerPair pairOf(final String peerId, final String serverId) {
        synchronized (this.pairConstants) {
            Map<String, PeerPair> pairs = this.pairConstants.computeIfAbsent(peerId, k -> new HashMap<>());

            PeerPair pair = pairs.computeIfAbsent(serverId, k -> new PeerPair(peerId, serverId));
            return pair;
        }
    }

    /**
     * A peer pair
     * @author boyan(boyan@antfin.com)
     *
     */
    static class PeerPair {
        // peer in local node
        final String local;
        // peer in remote node
        final String remote;

        PeerPair(final String local, final String remote) {
            super();
            this.local = local;
            this.remote = remote;
        }

        @Override
        public String toString() {
            return "PeerPair[" + this.local + " -> " + this.remote + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.local == null) ? 0 : this.local.hashCode());
            result = prime * result + ((this.remote == null) ? 0 : this.remote.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PeerPair other = (PeerPair) obj;
            if (this.local == null) {
                if (other.local != null) {
                    return false;
                }
            } else if (!this.local.equals(other.local)) {
                return false;
            }
            if (this.remote == null) {
                if (other.remote != null) {
                    return false;
                }
            } else if (!this.remote.equals(other.remote)) {
                return false;
            }
            return true;
        }
    }

    static class PeerRequestContext {

        private final String                         groupId;
        private final PeerPair                       pair;

        // Executor to run the requests
        private SingleThreadExecutor executor;
        // The request sequence;
        private int                                  sequence;
        // The required sequence to be sent.
        private int                                  nextRequiredSequence;
        // The response queue,it's not thread-safe and protected by it self object monitor.
        private final PriorityQueue<SequenceMessage> responseQueue;

        private final int                            maxPendingResponses;

        public PeerRequestContext(final String groupId, final PeerPair pair, final int maxPendingResponses) {
            super();
            this.pair = pair;
            this.groupId = groupId;
            // multiple producer single consumer
            this.executor = new MpscSingleThreadExecutor(Utils.MAX_APPEND_ENTRIES_TASKS_PER_THREAD,
                    JRaftUtils.createThreadFactory(groupId + "/" + pair + "-AppendEntriesThread"));

            this.sequence = 0;
            this.nextRequiredSequence = 0;
            this.maxPendingResponses = maxPendingResponses;
            this.responseQueue = new PriorityQueue<>(50);
        }

        boolean hasTooManyPendingResponses() {
            return this.responseQueue.size() > this.maxPendingResponses;
        }

        int getAndIncrementSequence() {
            final int prev = this.sequence;
            this.sequence++;
            if (this.sequence < 0) {
                this.sequence = 0;
            }
            return prev;
        }

        synchronized void destroy() {
            if (this.executor != null) {
                LOG.info("Destroyed peer request context for {}/{}", this.groupId, this.pair);
                this.executor.shutdownGracefully();
                this.executor = null;
            }
        }

        int getNextRequiredSequence() {
            return this.nextRequiredSequence;
        }

        int getAndIncrementNextRequiredSequence() {
            final int prev = this.nextRequiredSequence;
            this.nextRequiredSequence++;
            if (this.nextRequiredSequence < 0) {
                this.nextRequiredSequence = 0;
            }
            return prev;
        }
    }

    PeerRequestContext getPeerRequestContext(final String groupId, final PeerPair pair) {
        ConcurrentMap<PeerPair, PeerRequestContext> groupContexts = this.peerRequestContexts.get(groupId);

        if (groupContexts == null) {
            return null;
        }
        return groupContexts.get(pair);
    }

    /**
     * Send request in pipeline mode.
     */
    void sendSequenceResponse(final String groupId, final PeerPair pair, final int seq, final RpcContext rpcCtx,
                              final Message msg) {
        final PeerRequestContext ctx = getPeerRequestContext(groupId, pair);
        if (ctx == null) {
            // the context was destroyed, so the response can be ignored.
            return;
        }
        final PriorityQueue<SequenceMessage> respQueue = ctx.responseQueue;
        assert (respQueue != null);
        //这里加了一个同步锁，为什么会有并发问题呢？第三版本代码中，大家就会知道答案了
        //也许是第四版本代码。这个框架我会讲得特别细，所以进度可能慢一点，讲得越多越好，越详细越好
        synchronized (Utils.withLockObject(respQueue)) {
            respQueue.add(new SequenceMessage(rpcCtx, msg, seq));
            if (!ctx.hasTooManyPendingResponses()) {
                while (!respQueue.isEmpty()) {
                    final SequenceMessage queuedPipelinedResponse = respQueue.peek();
                    if (queuedPipelinedResponse.sequence != ctx.getNextRequiredSequence()) {
                        // sequence mismatch, waiting for next response.
                        break;
                    }
                    respQueue.remove();
                    try {
                        queuedPipelinedResponse.sendResponse();
                    } finally {
                        ctx.getAndIncrementNextRequiredSequence();
                    }
                }
            } else {
                final Connection connection = rpcCtx.getConnection();
                LOG.warn("Closed connection to peer {}/{}, because of too many pending responses, queued={}, max={}",
                        ctx.groupId, pair, respQueue.size(), ctx.maxPendingResponses);
                connection.close();
                // Close the connection if there are too many pending responses in queue.
                removePeerRequestContext(groupId, pair);
            }
        }
    }

    @SuppressWarnings("unchecked")
    PeerRequestContext getOrCreatePeerRequestContext(final String groupId, final PeerPair pair, final Connection conn) {
        ConcurrentMap<PeerPair, PeerRequestContext> groupContexts = this.peerRequestContexts.get(groupId);
        if (groupContexts == null) {
            groupContexts = new ConcurrentHashMap<>();
            final ConcurrentMap<PeerPair, PeerRequestContext> existsCtxs = this.peerRequestContexts.putIfAbsent(
                    groupId, groupContexts);
            if (existsCtxs != null) {
                groupContexts = existsCtxs;
            }
        }
        PeerRequestContext peerCtx = groupContexts.get(pair);
        if (peerCtx == null) {
            synchronized (Utils.withLockObject(groupContexts)) {
                peerCtx = groupContexts.get(pair);
                // double check in lock
                if (peerCtx == null) {
                    // only one thread to process append entries for every jraft node
                    final PeerId peer = new PeerId();
                    final boolean parsed = peer.parse(pair.local);
                    assert (parsed);
                    final Node node = NodeManager.getInstance().get(groupId, peer);
                    assert (node != null);
                    peerCtx = new PeerRequestContext(groupId, pair, node.getRaftOptions()
                            .getMaxReplicatorInflightMsgs());
                    groupContexts.put(pair, peerCtx);
                }
            }
        }
        // Add the pair to connection attribute metadata.
        if (conn != null) {
            Set<PeerPair> pairs;
            if ((pairs = (Set<PeerPair>) conn.getAttribute(PAIR_ATTR)) == null) {
                pairs = new ConcurrentHashSet<>();
                Set<PeerPair> existsPairs = (Set<PeerPair>) conn.setAttributeIfAbsent(PAIR_ATTR, pairs);
                if (existsPairs != null) {
                    pairs = existsPairs;
                }
            }

            pairs.add(pair);
        }
        return peerCtx;
    }


    void removePeerRequestContext(final String groupId, final PeerPair pair) {
        final ConcurrentMap<PeerPair, PeerRequestContext> groupContexts = this.peerRequestContexts.get(groupId);
        if (groupContexts == null) {
            return;
        }
        synchronized (Utils.withLockObject(groupContexts)) {
            final PeerRequestContext ctx = groupContexts.remove(pair);
            if (ctx != null) {
                ctx.destroy();
            }
        }
    }

    /**
     * RAFT group peer request contexts.
     */
    private final ConcurrentMap<String /*groupId*/, ConcurrentMap<PeerPair, PeerRequestContext>> peerRequestContexts = new ConcurrentHashMap<>();

    /**
     * The executor selector to select executor for processing request.
     */
    private final ExecutorSelector                                                                executorSelector;

    public AppendEntriesRequestProcessor(final Executor executor) {
        super(executor, RpcRequests.AppendEntriesResponse.getDefaultInstance());
        this.executorSelector = new PeerExecutorSelector();
    }

    @Override
    protected String getPeerId(final RpcRequests.AppendEntriesRequest request) {
        return request.getPeerId();
    }

    @Override
    protected String getGroupId(final RpcRequests.AppendEntriesRequest request) {
        return request.getGroupId();
    }

    private int getAndIncrementSequence(final String groupId, final PeerPair pair, final Connection conn) {
        return getOrCreatePeerRequestContext(groupId, pair, conn).getAndIncrementSequence();
    }

    private boolean isHeartbeatRequest(final RpcRequests.AppendEntriesRequest request) {
        // No entries and no data means a true heartbeat request.
        // TODO(boyan) refactor, adds a new flag field?
        return request.getEntriesCount() == 0 && !request.hasData();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/24
     * @Description:处理请求的方法，Raft节点的服务器处理请求时，最终会调用到这个方法中。
     */
    @Override
    public Message processRequest0(final RaftServerService service, final RpcRequests.AppendEntriesRequest request,
                                   final RpcRequestClosure done) {
        //因为NodeImpl实现了Node、RaftServerService接口，所以可以直接把接口转换为Node节点
        final Node node = (Node) service;
        //判断节点在处理日志复制消息时日否开启了Pipeline模式
        if (node.getRaftOptions().isReplicatorPipeline()) {
            //下面这两行代码先不用关注，这里的行为其实和日志复制中使用的Pipeline模式有关
            final String groupId = request.getGroupId();
            final PeerPair pair = pairOf(request.getPeerId(), request.getServerId());
            //判断当前请求是否为心跳请求
            boolean isHeartbeat = isHeartbeatRequest(request);
            //初始化一个请求序列号
            int reqSequence = -1;
            if (!isHeartbeat) {
                //走到这里意味着不是心跳请求，先不必关注了
                reqSequence = getAndIncrementSequence(groupId, pair, done.getRpcCtx().getConnection());
            }
            //这里就是调用NodeImpl对象的方法，具体处理请求
            //注意，如果是心跳请求，handleAppendEntriesRequest方法会直接返回一个响应
            //如果不是心跳请求，则直接返回一个null或者是其他的响应，具体逻辑可以查看NodeImpl类的handleAppendEntriesRequest方法
            final Message response = service.handleAppendEntriesRequest(request, new SequenceRpcRequestClosure(done,
                    defaultResp(), groupId, pair, reqSequence, isHeartbeat));
            if (response != null) {
                if (isHeartbeat) {
                    //走到这里意味着是心跳请求
                    //直接回复响应即可
                    //这里这行代码是我自己添加的，为了让大家看一一下，执行AppendEntriesRequestProcessor处理器中的线程究竟是哪个
                    //实际上，这里使用的并不是bolt服务端默认的业务线程池，而是Jraft框架内部定义的一个单线程执行器
                    //这个单线程执行器内部使用了Mpsc队列
                    //System.out.println(Thread.currentThread().getName());
                    done.getRpcCtx().sendResponse(response);
                } else {
                    //走到这里意味着返回了其他的响应
                    sendSequenceResponse(groupId, pair, reqSequence, done.getRpcCtx(), response);
                }
            }
            return null;
        } else {
            //走到这里则意味着没有开启Pipeline，程序中默认开始Pipeline
            return service.handleAppendEntriesRequest(request, done);
        }
    }

    @Override
    public String interest() {
        return RpcRequests.AppendEntriesRequest.class.getName();
    }

    @Override
    public ExecutorSelector executorSelector() {
        return this.executorSelector;
    }

    // TODO called when shutdown service.
    public void destroy() {
        for (final ConcurrentMap<PeerPair, PeerRequestContext> map : this.peerRequestContexts.values()) {
            for (final PeerRequestContext ctx : map.values()) {
                ctx.destroy();
            }
        }
        this.peerRequestContexts.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClosed(final String remoteAddress, final Connection conn) {
        final Set<PeerPair> pairs = (Set<PeerPair>) conn.getAttribute(PAIR_ATTR);
        if (pairs != null && !pairs.isEmpty()) {
            // Clear request contexts when connection disconnected.
            for (final Map.Entry<String, ConcurrentMap<PeerPair, PeerRequestContext>> entry : this.peerRequestContexts
                    .entrySet()) {
                final ConcurrentMap<PeerPair, PeerRequestContext> groupCtxs = entry.getValue();
                synchronized (Utils.withLockObject(groupCtxs)) {
                    for (PeerPair pair : pairs) {
                        final PeerRequestContext ctx = groupCtxs.remove(pair);
                        if (ctx != null) {
                            ctx.destroy();
                        }
                    }
                }
            }
        } else {
            LOG.info("Connection disconnected: {}", remoteAddress);
        }
    }
}
