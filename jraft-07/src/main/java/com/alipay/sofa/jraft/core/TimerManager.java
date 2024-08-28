package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.util.NamedThreadFactory;
import com.alipay.sofa.jraft.util.ThreadPoolUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:全局定时任务管理器，这里的全局是和选举超时器以及剩下的三个定时器做了一下区分，第一版本先不讲解的那么详细了
 * 后面把其他的定时任务引入了再来做对比吧，在第一版本中，大家只需要知道心跳信息就是由这个定时任务管理器来提交的即可
 */
public class TimerManager implements Scheduler {

    private final ScheduledExecutorService executor;

    public TimerManager(int workerNum) {
        this(workerNum, "JRaft-Node-ScheduleThreadPool");
    }

    public TimerManager(int workerNum, String name) {
        //通过线程池工具类创建了一个定时任务调度器
        this.executor = ThreadPoolUtil.newScheduledBuilder()
                .poolName(name)
                .coreThreads(workerNum)
                //默认开启性能监控
                .enableMetric(true)
                .threadFactory(new NamedThreadFactory(name, true))
                .build();
    }

    //下面几个都是提交定时任务的方法
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return this.executor.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                                  final TimeUnit unit) {
        return this.executor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                                     final TimeUnit unit) {
        return this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        this.executor.shutdownNow();
    }
}
