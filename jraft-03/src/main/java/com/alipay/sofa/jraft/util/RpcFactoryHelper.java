package com.alipay.sofa.jraft.util;


import com.alipay.sofa.jraft.rpc.RaftRpcFactory;
import com.alipay.sofa.jraft.rpc.RpcResponseFactory;

public class RpcFactoryHelper {

    private static final RaftRpcFactory RPC_FACTORY = JRaftServiceLoader.load(RaftRpcFactory.class) .first();

    public static RaftRpcFactory rpcFactory() {
        return RPC_FACTORY;
    }

    public static RpcResponseFactory responseFactory() {
        return RPC_FACTORY.getRpcResponseFactory();
    }
}
