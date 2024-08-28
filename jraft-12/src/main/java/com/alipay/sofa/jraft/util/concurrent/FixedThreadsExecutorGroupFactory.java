package com.alipay.sofa.jraft.util.concurrent;

import java.util.concurrent.ExecutorService;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:创建执行器组工厂类要实现的接口
 */
public interface FixedThreadsExecutorGroupFactory {

    FixedThreadsExecutorGroup newExecutorGroup(final int nThreads, final String poolName,
                                               final int maxPendingTasksPerThread);

    FixedThreadsExecutorGroup newExecutorGroup(final int nThreads, final String poolName,
                                               final int maxPendingTasksPerThread, final boolean useMpscQueue);

    FixedThreadsExecutorGroup newExecutorGroup(final SingleThreadExecutor[] children);

    FixedThreadsExecutorGroup newExecutorGroup(final SingleThreadExecutor[] children,
                                               final ExecutorChooserFactory.ExecutorChooser chooser);

    FixedThreadsExecutorGroup newExecutorGroup(final ExecutorService[] children);

    FixedThreadsExecutorGroup newExecutorGroup(final ExecutorService[] children,
                                               final ExecutorChooserFactory.ExecutorChooser chooser);

}
