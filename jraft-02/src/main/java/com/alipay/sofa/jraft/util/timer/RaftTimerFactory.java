package com.alipay.sofa.jraft.util.timer;

import com.alipay.sofa.jraft.core.Scheduler;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:定时任务功能，该接口的实现类专门为程序内部提供给各种功能定时器
 */
public interface RaftTimerFactory {

    Timer getElectionTimer(final boolean shared, final String name);

    Timer getVoteTimer(final boolean shared, final String name);

    Timer getStepDownTimer(final boolean shared, final String name);

    Timer getSnapshotTimer(final boolean shared, final String name);

    Scheduler getRaftScheduler(final boolean shared, final int workerNum, final String name);

    Timer createTimer(final String name);

    Scheduler createScheduler(final int workerNum, final String name);
}