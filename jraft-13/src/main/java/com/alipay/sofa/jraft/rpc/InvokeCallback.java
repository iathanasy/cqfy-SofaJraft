package com.alipay.sofa.jraft.rpc;


import java.util.concurrent.Executor;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:封装回调方法的接口
 */
public interface InvokeCallback {

    void complete(final Object result, final Throwable err);

    default Executor executor() {
        return null;
    }
}
