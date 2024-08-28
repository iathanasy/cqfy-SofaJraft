package com.alipay.sofa.jraft.rpc;

import java.util.concurrent.Executor;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:处理器对象，用户自定义的处理器要实现这个接口，这些处理器会被RPC服务端使用
 * 实际上就是Netty构建的服务端会在handler中调用用户自己定义的处理器来处理业务逻辑
 */
public interface RpcProcessor<T> {

    /**
     * Async to handle request with {@link RpcContext}.
     *
     * @param rpcCtx  the rpc context
     * @param request the request
     */
    void handleRequest(final RpcContext rpcCtx, final T request);

    /**
     * The class name of user request.
     * Use String type to avoid loading class.
     *
     * @return interested request's class name
     */
    String interest();

    /**
     * Get user's executor.
     *
     * @return executor
     */
    default Executor executor() {
        return null;
    }

    /**
     *
     * @return the executor selector
     */
    default ExecutorSelector executorSelector() {
        return null;
    }

    /**
     * Executor selector interface.
     */
    interface ExecutorSelector {

        /**
         * Select a executor.
         *
         * @param reqClass  request class name
         * @param reqHeader request header
         * @return a executor
         */
        Executor select(final String reqClass, final Object reqHeader);
    }
}
