package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.rpc.impl.core.*;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;

import java.util.concurrent.Executor;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/24
 * @Description:创建raft节点服务端的工厂类
 */
public class RaftRpcServerFactory {

    static {
        ProtobufMsgFactory.load();
    }

    /**
     * Creates a raft RPC server with default request executors.
     *
     * @param endpoint server address to bind
     * @return a rpc server instance
     */
    public static RpcServer createRaftRpcServer(final Endpoint endpoint) {
        return createRaftRpcServer(endpoint, null, null);
    }

    /**
     * Creates a raft RPC server with executors to handle requests.
     *
     * @param endpoint      server address to bind
     * @param raftExecutor  executor to handle RAFT requests.
     * @param cliExecutor   executor to handle CLI service requests.
     * @return a rpc server instance
     */
    public static RpcServer createRaftRpcServer(final Endpoint endpoint, final Executor raftExecutor,
                                                final Executor cliExecutor) {
        final RpcServer rpcServer = RpcFactoryHelper.rpcFactory().createRpcServer(endpoint);
        addRaftRequestProcessors(rpcServer, raftExecutor, cliExecutor);
        return rpcServer;
    }

    /**
     * Adds RAFT and CLI service request processors with default executor.
     *
     * @param rpcServer rpc server instance
     */
    public static void addRaftRequestProcessors(final RpcServer rpcServer) {
        addRaftRequestProcessors(rpcServer, null, null);
    }

    /**
     * Adds RAFT and CLI service request processors.
     *
     * @param rpcServer    rpc server instance
     * @param raftExecutor executor to handle RAFT requests.
     * @param cliExecutor  executor to handle CLI service requests.
     */
    public static void addRaftRequestProcessors(final RpcServer rpcServer, final Executor raftExecutor,
                                                final Executor cliExecutor) {
        // raft core processors
        final AppendEntriesRequestProcessor appendEntriesRequestProcessor = new AppendEntriesRequestProcessor(raftExecutor);
        rpcServer.registerProcessor(new GetFileRequestProcessor(raftExecutor));
        rpcServer.registerConnectionClosedEventListener(appendEntriesRequestProcessor);
        rpcServer.registerProcessor(appendEntriesRequestProcessor);
        rpcServer.registerProcessor(new InstallSnapshotRequestProcessor(raftExecutor));
        rpcServer.registerProcessor(new RequestVoteRequestProcessor(raftExecutor));
        rpcServer.registerProcessor(new ReadIndexRequestProcessor(raftExecutor));
    }

    /**
     * Creates a raft RPC server and starts it.
     *
     * @param endpoint server address to bind
     * @return a rpc server instance
     */
    public static RpcServer createAndStartRaftRpcServer(final Endpoint endpoint) {
        return createAndStartRaftRpcServer(endpoint, null, null);
    }

    /**
     * Creates a raft RPC server and starts it.
     *
     * @param endpoint     server address to bind
     * @param raftExecutor executor to handle RAFT requests.
     * @param cliExecutor  executor to handle CLI service requests.
     * @return a rpc server instance
     */
    public static RpcServer createAndStartRaftRpcServer(final Endpoint endpoint, final Executor raftExecutor,
                                                        final Executor cliExecutor) {
        final RpcServer server = createRaftRpcServer(endpoint, raftExecutor, cliExecutor);
        server.init(null);
        return server;
    }
}
