package com.alipay.sofa.jraft.util.concurrent;

import java.util.concurrent.RejectedExecutionException;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:拒绝策略实现类
 */
public final class RejectedExecutionHandlers {

    private static final RejectedExecutionHandler REJECT = (task, executor) -> {
        throw new RejectedExecutionException();
    };


    public static RejectedExecutionHandler reject() {
        return REJECT;
    }

    private RejectedExecutionHandlers() {
    }
}