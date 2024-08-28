package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.util.RpcFactoryHelper;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/24
 * @Description:服务端处理器，这个处理器对象中的方法会在Netty中的handler中调用
 */
public abstract class RpcRequestProcessor<T extends Message> implements RpcProcessor<T> {

    protected static final Logger LOG = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final Executor executor;
    private final Message defaultResp;

    public abstract Message processRequest(final T request, final RpcRequestClosure done);

    public RpcRequestProcessor(Executor executor, Message defaultResp) {
        super();
        this.executor = executor;
        this.defaultResp = defaultResp;
    }



    @Override
    public void handleRequest(final RpcContext rpcCtx, final T request) {
        try {//继续处理请求
            final Message msg = processRequest(request, new RpcRequestClosure(rpcCtx, this.defaultResp));
            if (msg != null) {
                rpcCtx.sendResponse(msg);
            }
        } catch (final Throwable t) {
            LOG.error("handleRequest {} failed", request, t);
            rpcCtx.sendResponse(RpcFactoryHelper
                    .responseFactory()
                    .newResponse(defaultResp(), -1, "handleRequest internal error"));
        }
    }

    @Override
    public Executor executor() {
        return this.executor;
    }

    public Message defaultResp() {
        return this.defaultResp;
    }
}
