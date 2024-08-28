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
        //该方法会在bolt方法内部被调用，这里其实和multi-raft-group有关，再sofajraft框架中
        //同一个进程内的Raft组会共同使用同一个服务器，所以，要在这里区分一下，到来的请求要被转交给进程中
        //的那个节点来处理，这就要根据这个节点所在的集群ID，也就是Raft组和领导者以及当前节点的信息来判断了
        @Override
        public Executor select(final String reqClass, final Object reqHeader) {
            final RpcRequests.AppendEntriesRequestHeader header = (RpcRequests.AppendEntriesRequestHeader) reqHeader;
            //得到当前跟随者节点所在的集群ID
            final String groupId = header.getGroupId();
            //得到跟随者节点的节点字符转信息
            final String peerId = header.getPeerId();
            //得到给当前节点发送消息的领导者的信息
            final String serverId = header.getServerId();
            final PeerId peer = new PeerId();
            //得到当前节点的节点信息对象
            if (!peer.parse(peerId)) {
                return executor();
            }//从节点管理器中取出对应的节点，现在就获得了当前跟随者节点本身
            final Node node = NodeManager.getInstance().get(groupId, peer);
            //对节点判空，看看节点是否没有开启Pipeline传输模式
            if (node == null || !node.getRaftOptions().isReplicatorPipeline()) {
                return executor();
            }
            //走到这里就意味着当前节点开启了Pipeline模式，这里只是再确保开启了这个模式
            RpcFactoryHelper.rpcFactory().ensurePipeline();
            //这一步就非常重要了，这里就是得到了当前节点的请求上下文
            final PeerRequestContext ctx = getOrCreatePeerRequestContext(groupId, pairOf(peerId, serverId), null);
            //返回单线程执行器，这里其实需要结合bolt框架中的代码来理解，这里返回的其实是一个单线程执行器，这个单线陈执行器就专门处理当前跟随者节点的请求
            //如果是在multi-raft-group模式下，那么进程中的每一个节点都会有自己的单线程执行器处理自己的请求
            return ctx.executor;
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/15
     * @方法描述：这个类的对象包装的回调方法，会在跟随者节点将日志成功罗盘后回调，回调的时候会直接调用这个内部类中的sendResponse方法，会把要回复给领导者的响应先放到响应队列中
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
            } else {//在这里判断了要回复的响应不是心跳响应后，就会在下面这个方法中把要回复的响应放到响应队列中
                sendSequenceResponse(this.groupId, this.pair, this.reqSequence, getRpcCtx(), msg);
            }
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/16
     * @方法描述：包装响应的类
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

        //发送响应给领导者的方法
        void sendResponse() {
            this.rpcCtx.sendResponse(this.msg);
        }

        //根据序号比较大小的方法，该方法会在把响应对象放到responseQueue队列时会用到
        @Override
        public int compareTo(final SequenceMessage o) {
            return Integer.compare(this.sequence, o.sequence);
        }
    }

    //存放PeerPair对象的Map
    private final Map<String, Map<String, PeerPair>> pairConstants = new HashMap<>();

    //该方法是把跟随者自身节点的信息和领导者节点信息封装到PeerPair对象中
    PeerPair pairOf(final String peerId, final String serverId) {
        synchronized (this.pairConstants) {
            Map<String, PeerPair> pairs = this.pairConstants.computeIfAbsent(peerId, k -> new HashMap<>());
            //在这里把PeerPair对象放到了pairConstants的value中，这个value是一个Map
            //其实就是把pairConstants放到了pairConstants中了
            PeerPair pair = pairs.computeIfAbsent(serverId, k -> new PeerPair(peerId, serverId));
            return pair;
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/15
     * @方法描述：这各类的对象可以看成一个对等节点对儿，简而言之，就是一个PeerPair对象就封装着一个领导者和领导者所属的跟随者节点
     */
    static class PeerPair {
        //这个是当前节点自己本身的字符串信息，也就是跟随者信息
        final String local;
        //这个是领导者字符串信息
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

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/15
     * @方法描述：该类的对象封装的是对等节点请求的上下文信息
     */
    static class PeerRequestContext {

        private final String groupId;
        private final PeerPair pair;

        //执行请求处理的单线程执行器
        private SingleThreadExecutor executor;
        //请求序号
        private int sequence;
        //下一个要回复的请求序号
        private int nextRequiredSequence;
        //存放响应的队列，这个队列不是并发安全的
        private final PriorityQueue<SequenceMessage> responseQueue;
        //响应队列容量最大值
        private final int maxPendingResponses;

        //构造方法
        public PeerRequestContext(final String groupId, final PeerPair pair, final int maxPendingResponses) {
            super();
            this.pair = pair;
            this.groupId = groupId;
            //单线程执行器，这个单线程执行器会处理跟随者节点接收到的请求
            this.executor = new MpscSingleThreadExecutor(Utils.MAX_APPEND_ENTRIES_TASKS_PER_THREAD,
                    JRaftUtils.createThreadFactory(groupId + "/" + pair + "-AppendEntriesThread"));
            this.sequence = 0;
            this.nextRequiredSequence = 0;
            this.maxPendingResponses = maxPendingResponses;
            this.responseQueue = new PriorityQueue<>(50);
        }
        //判断响应队列中是否有太多响应没有回复，超过了默认的配置数量
        boolean hasTooManyPendingResponses() {
            return this.responseQueue.size() > this.maxPendingResponses;
        }

        //得到请求序号，到这里大家应该也能意识到，发送日志的时候不仅仅是在领导者实现了Pipeline模式，在跟随者节点发送消息的时候也实现了Pipeline模式
        int getAndIncrementSequence() {
            final int prev = this.sequence;
            this.sequence++;
            if (this.sequence < 0) {
                this.sequence = 0;
            }
            return prev;
        }

        //销毁上下文对象，销毁的时候要把单线程执行器优雅关闭了，也就是优雅释放资源
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

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/15
     * @方法描述：获得对等节点请求的上下文信息对象
     */
    PeerRequestContext getPeerRequestContext(final String groupId, final PeerPair pair) {
        //根据集群ID从peerRequestContexts这个Map中先获得对等节点对象和对等节点的请求请下文Map
        ConcurrentMap<PeerPair, PeerRequestContext> groupContexts = this.peerRequestContexts.get(groupId);
        if (groupContexts == null) {
            return null;
        }//得到对等节点请求的上下文对象
        return groupContexts.get(pair);
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/15
     * @方法描述：该方法的作用就是把跟随者节点要回复给领导者的响应放到响应队列中
     */
    void sendSequenceResponse(final String groupId, final PeerPair pair, final int seq, final RpcContext rpcCtx,
                              final Message msg) {
        //得到对等节点请求的上下文对象
        final PeerRequestContext ctx = getPeerRequestContext(groupId, pair);
        //判空
        if (ctx == null) {
            return;
        }
        final PriorityQueue<SequenceMessage> respQueue = ctx.responseQueue;
        assert (respQueue != null);
        //这里加了一个同步锁，为什么会有并发问题呢？
        //现在这个并发问题已经很明显了，因为如果是日志传输请求，这个方法会被日志管理器的disruptor的线程异步回调，到时候就是多个线程
        //同时向这个队列中存放响应，所以应该加一个同步锁，避免出现并发问题
        synchronized (Utils.withLockObject(respQueue)) {
            //把要发送的响应包装成SequenceMessage对象，存放到响应队列中，这里的逻辑和复制器对象那边的逻辑非常类似
            respQueue.add(new SequenceMessage(rpcCtx, msg, seq));
            //判断响应队列中存放的响应是否超过了最大值，没有超过则继续向下执行
            if (!ctx.hasTooManyPendingResponses()) {
                //如果响应队列不为空
                while (!respQueue.isEmpty()) {
                    //取出第一个要发送的响应
                    final SequenceMessage queuedPipelinedResponse = respQueue.peek();
                    //判断要发送的响应的序列号是不是应该发送的那个
                    if (queuedPipelinedResponse.sequence != ctx.getNextRequiredSequence()) {
                        //如果序号不匹配直接推出循环
                        break;
                    }//如果匹配则把要发送的响应从响应队列移除
                    respQueue.remove();
                    try {//在这里把响应回复给领导者
                        queuedPipelinedResponse.sendResponse();
                    } finally {//最后别忘记递增下一个要发送的序号
                        ctx.getAndIncrementNextRequiredSequence();
                    }
                }
            } else {//走到这里意味着有太多响应没有回复给领导者，这种情况可能是跟随者和领导者连接出现问题了
                final Connection connection = rpcCtx.getConnection();
                LOG.warn("Closed connection to peer {}/{}, because of too many pending responses, queued={}, max={}",
                        ctx.groupId, pair, respQueue.size(), ctx.maxPendingResponses);
                //关闭链接
                connection.close();
                //从Map中移除对等节点的请求上下文信息对象
                removePeerRequestContext(groupId, pair);
            }
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/16
     * @方法描述：给每一个对等节点创建请求上下文对象的方法，并且把这个请求上下文对象存放到peerRequestContexts这个Map中，这里的这个逻辑非常简单，就不再详细注释了，整体逻辑就是先判断Map中有没有上下文对象
     * 如果没有就创建了再放进去
     */
    @SuppressWarnings("unchecked")
    PeerRequestContext getOrCreatePeerRequestContext(final String groupId, final PeerPair pair, final Connection conn) {
        ConcurrentMap<PeerPair, PeerRequestContext> groupContexts = this.peerRequestContexts.get(groupId);
        //先判断peerRequestContexts中是否存在与集群ID对应的Map，这个Map中存放的就是对等节点对象与对等节点请求上下文的信息对象的映射关系
        if (groupContexts == null) {
            //如果没有就创建Map，然后把Map放到peerRequestContexts中
            groupContexts = new ConcurrentHashMap<>();
            final ConcurrentMap<PeerPair, PeerRequestContext> existsCtxs = this.peerRequestContexts.putIfAbsent(
                    groupId, groupContexts);
            if (existsCtxs != null) {
                groupContexts = existsCtxs;
            }
        }//现在判断上面获得的groupContexts中是否有对等节点的请求上下文信息对象
        PeerRequestContext peerCtx = groupContexts.get(pair);
        if (peerCtx == null) {
            //如果没有就创建了再放到Map中
            synchronized (Utils.withLockObject(groupContexts)) {
                peerCtx = groupContexts.get(pair);
                //双重检验
                if (peerCtx == null) {
                    //下面就是创建PeerRequestContext对象的逻辑了
                    final PeerId peer = new PeerId();
                    final boolean parsed = peer.parse(pair.local);
                    assert (parsed);
                    final Node node = NodeManager.getInstance().get(groupId, peer);
                    assert (node != null);
                    //这里非常重要，在PeerRequestContext的构造方法中，执行了 new MpscSingleThreadExecutor这个方法
                    //给当前的节点创建了一个单线程执行器，这里大家也能意识到了，每一个节点都有自己私有的单线程执行器来处理日志和探针请求
                    peerCtx = new PeerRequestContext(groupId, pair, node.getRaftOptions()
                            .getMaxReplicatorInflightMsgs());
                    groupContexts.put(pair, peerCtx);
                }
            }
        }
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


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/16
     * @方法描述：从Map中移除对等节点的请求上下文信息对象
     */
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


    //String其实就是GroupId
    private final ConcurrentMap<String, ConcurrentMap<PeerPair, PeerRequestContext>> peerRequestContexts = new ConcurrentHashMap<>();

    //执行器选择器
    private final ExecutorSelector executorSelector;

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

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬
     * @date:2023/12/16
     * @方法描述：判断是否为心跳请求的方法
     */
    private boolean isHeartbeatRequest(final RpcRequests.AppendEntriesRequest request) {
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
            //得到对等节点信息对象
            final PeerPair pair = pairOf(request.getPeerId(), request.getServerId());
            //判断当前请求是否为心跳请求
            boolean isHeartbeat = isHeartbeatRequest(request);
            //初始化一个请求序列号
            int reqSequence = -1;
            if (!isHeartbeat) {
                //走到这里意味着不是心跳请求，不是心跳请求就意味着是日志复制请求和探针请求
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
