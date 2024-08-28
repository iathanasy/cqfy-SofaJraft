package com.alipay.sofa.jraft.rpc;


import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.util.Endpoint;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:创建RPC客户端和服务端的工厂
 */
public interface RaftRpcFactory {

    RpcResponseFactory DEFAULT = new RpcResponseFactory() {};

    //注册protobuf序列化器
    void registerProtobufSerializer(final String className, final Object... args);

    //创建RPC客户端
    default RpcClient createRpcClient() {
        return createRpcClient(null);
    }

    //创建RPC客户端
    RpcClient createRpcClient(final ConfigHelper<RpcClient> helper);

    //创建RPC服务端
    default RpcServer createRpcServer(final Endpoint endpoint) {
        return createRpcServer(endpoint, null);
    }

    //创建RPC服务端
    RpcServer createRpcServer(final Endpoint endpoint, final ConfigHelper<RpcServer> helper);

    default RpcResponseFactory getRpcResponseFactory() {
        return DEFAULT;
    }

    //是否启用复制器对象的Pipeline功能，这个功能是日志复制时非常重要的一个知识
    default boolean isReplicatorPipelineEnabled() {
        return true;
    }

    //确保RPC框架支持Pipeline功能
    default void ensurePipeline() {}

    @SuppressWarnings("unused")
    default ConfigHelper<RpcClient> defaultJRaftClientConfigHelper(final RpcOptions opts) {
        return null;
    }

    @SuppressWarnings("unused")
    default ConfigHelper<RpcServer> defaultJRaftServerConfigHelper(final RpcOptions opts) {
        return null;
    }

    interface ConfigHelper<T> {

        void config(final T instance);
    }
}
