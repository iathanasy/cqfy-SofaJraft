package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:收到请求之后的回调方法就会封装到这个对象中，在第一版本代码中，这个回调的对象还没用到
 * 在后面就会用到了，这里我先为大家解释一下，这里说的收到请求后的回调是什么意思，一般来说，如果收到的是心跳消息
 * 或者是投票的请求消息，这些请求处理起来很简单，那么直接返回响应即可。但是如果处理的是日志复制消息呢？收到领导者发送过来的
 * 日志复制的请求，处理这个请求的时候，需要等到日志复制成功才能回复响应吧？所以，在JRaft框架中，为某些请求定义了回调方法
 * 当请求处理完毕后，再回调这个请求的回调方法，在回调方法中把处理好请求后获得的响应回复给目标节点
 * 这个回调方法中就定义了发送响应的方法
 */
public class RpcRequestClosure implements Closure {

    private static final Logger LOG           = LoggerFactory
            .getLogger(RpcRequestClosure.class);

    private static final AtomicIntegerFieldUpdater<RpcRequestClosure> STATE_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(
                    RpcRequestClosure.class,
                    "state");

    private static final int                                          PENDING       = 0;
    private static final int                                          RESPOND       = 1;

    private final RpcContext                                          rpcCtx;
    private final Message defaultResp;

    private volatile int                                              state         = PENDING;

    public RpcRequestClosure(RpcContext rpcCtx) {
        this(rpcCtx, null);
    }

    public RpcRequestClosure(RpcContext rpcCtx, Message defaultResp) {
        super();
        this.rpcCtx = rpcCtx;
        this.defaultResp = defaultResp;
    }

    public RpcContext getRpcCtx() {
        return rpcCtx;
    }

    public void sendResponse(final Message msg) {
        if (!STATE_UPDATER.compareAndSet(this, PENDING, RESPOND)) {
            LOG.warn("A response: {} sent repeatedly!", msg);
            return;
        }
        this.rpcCtx.sendResponse(msg);
    }

    @Override
    public void run(final Status status) {
        sendResponse(RpcFactoryHelper.responseFactory().newResponse(this.defaultResp, status));
    }
}
