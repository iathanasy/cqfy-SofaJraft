package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.rpc.ProtobufMsgFactory;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.Utils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:框架的集群服务类，这个类中的start方法，就是启动集群的入口方法
 */
public class RaftGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(RaftGroupService.class);

    static {
        ProtobufMsgFactory.load();
    }
    //集群服务是否启动的标志
    private volatile boolean started = false;

    //当前服务器的信息
    private PeerId serverId;

    //当前服务器被抽象为集群的一个节点
    //这个成员变量封装了节点需要的配置参数
    private NodeOptions nodeOptions;

    //节点与节点之间进行通信的RPC服务端
    private RpcServer rpcServer;

    //默认不共享这个服务端
    //有时候也会有一些共享的情况，比如当前节点是领导者，服务端既要和集群内部的节点通信
    //也要和业务客户端进行交互，如果要共享一个服务端也不是不行
    private final boolean sharedRpcServer;

    //当前节点所在的组ID，也就是集群ID
    private String groupId;
    //当前节点本身
    private Node node;

    public RaftGroupService(final String groupId, final PeerId serverId, final NodeOptions nodeOptions) {
        this(groupId, serverId, nodeOptions, RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint(),
                JRaftUtils.createExecutor("RAFT-RPC-executor-", nodeOptions.getRaftRpcThreadPoolSize()),
                JRaftUtils.createExecutor("CLI-RPC-executor-", nodeOptions.getCliRpcThreadPoolSize())));
    }

    public RaftGroupService(final String groupId, final PeerId serverId, final NodeOptions nodeOptions,
                            final RpcServer rpcServer) {
        this(groupId, serverId, nodeOptions, rpcServer, false);
    }

    public RaftGroupService(final String groupId, final PeerId serverId, final NodeOptions nodeOptions,
                            final RpcServer rpcServer, final boolean sharedRpcServer) {
        super();
        this.groupId = groupId;
        this.serverId = serverId;
        this.nodeOptions = nodeOptions;
        this.rpcServer = rpcServer;
        this.sharedRpcServer = sharedRpcServer;
    }

    public synchronized Node getRaftNode() {
        return this.node;
    }

    //启动当前节点以及节点所在集群的方法
    public synchronized Node start() {
        return start(true);
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/25
     * @Description:启动当前节点以及节点所在集群的方法
     */
    public synchronized Node start(final boolean startRpcServer) {
        //判断集群服务是否已经启动过了
        if (this.started) {
            return this.node;
        }
        //对服务ID和集群ID做非空检验
        if (this.serverId == null || this.serverId.getEndpoint() == null
                || this.serverId.getEndpoint().equals(new Endpoint(Utils.IP_ANY, 0))) {
            throw new IllegalArgumentException("Blank serverId:" + this.serverId);
        }
        if (StringUtils.isBlank(this.groupId)) {
            throw new IllegalArgumentException("Blank group id:" + this.groupId);
        }
        //把当前节点的端口号和IP地址信息交给节点管理器来管理
        NodeManager.getInstance().addAddress(this.serverId.getEndpoint());
        //创建当前服务器代表的节点
        this.node = RaftServiceFactory.createAndInitRaftNode(this.groupId, this.serverId, this.nodeOptions);
        if (startRpcServer) {
            //启动RPC服务端
            this.rpcServer.init(null);
        } else {
            LOG.warn("RPC server is not started in RaftGroupService.");
        }
        this.started = true;
        LOG.info("Start the RaftGroupService successfully.");
        return this.node;
    }



    public synchronized void shutdown() {
        if (!this.started) {
            return;
        }
        if (this.rpcServer != null) {
            try {
                if (!this.sharedRpcServer) {
                    this.rpcServer.shutdown();
                }
            } catch (final Exception ignored) {
                // ignore
            }
            this.rpcServer = null;
        }
        this.node.shutdown();
        NodeManager.getInstance().removeAddress(this.serverId.getEndpoint());
        this.started = false;
        LOG.info("Stop the RaftGroupService successfully.");
    }


    public boolean isStarted() {
        return this.started;
    }


    public String getGroupId() {
        return this.groupId;
    }


    public void setGroupId(final String groupId) {
        if (this.started) {
            throw new IllegalStateException("Raft group service already started");
        }
        this.groupId = groupId;
    }


    public PeerId getServerId() {
        return this.serverId;
    }


    public void setServerId(final PeerId serverId) {
        if (this.started) {
            throw new IllegalStateException("Raft group service already started");
        }
        this.serverId = serverId;
    }


    public RpcOptions getNodeOptions() {
        return this.nodeOptions;
    }

    public void setNodeOptions(final NodeOptions nodeOptions) {
        if (this.started) {
            throw new IllegalStateException("Raft group service already started");
        }
        if (nodeOptions == null) {
            throw new IllegalArgumentException("Invalid node options.");
        }
        nodeOptions.validate();
        this.nodeOptions = nodeOptions;
    }


    public RpcServer getRpcServer() {
        return this.rpcServer;
    }

    public void setRpcServer(final RpcServer rpcServer) {
        if (this.started) {
            throw new IllegalStateException("Raft group service already started");
        }
        if (this.serverId == null) {
            throw new IllegalStateException("Please set serverId at first");
        }
        if (rpcServer.boundPort() != this.serverId.getPort()) {
            throw new IllegalArgumentException("RPC server port mismatch");
        }
        this.rpcServer = rpcServer;
    }
}

