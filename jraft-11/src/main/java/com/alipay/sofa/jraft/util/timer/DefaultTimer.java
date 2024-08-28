package com.alipay.sofa.jraft.util.timer;

import com.alipay.sofa.jraft.util.ExecutorServiceHelper;
import com.alipay.sofa.jraft.util.NamedThreadFactory;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.ThreadPoolUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:默认的定时任务管理器，这个类看不看都行，主要还是去看看时间轮那个定时任务管理器是怎么实现的
 * 因为在集群中使用的就是时间轮
 */
public class DefaultTimer implements Timer {

    private final ScheduledExecutorService scheduledExecutorService;

    public DefaultTimer(int workerNum, String name) {
        this.scheduledExecutorService = ThreadPoolUtil.newScheduledBuilder()
                .coreThreads(workerNum)
                .poolName(name)
                .enableMetric(true)
                .threadFactory(new NamedThreadFactory(name, true))
                .build();
    }

    @Override
    public Timeout newTimeout(final TimerTask task, final long delay, final TimeUnit unit) {
        Requires.requireNonNull(task, "task");
        Requires.requireNonNull(unit, "unit");
        final TimeoutTask timeoutTask = new TimeoutTask(task);
        final ScheduledFuture<?> future = this.scheduledExecutorService.schedule(new TimeoutTask(task), delay, unit);
        timeoutTask.setFuture(future);
        return timeoutTask.getTimeout();
    }

    @Override
    public Set<Timeout> stop() {
        ExecutorServiceHelper.shutdownAndAwaitTermination(this.scheduledExecutorService);
        return Collections.emptySet();
    }

    //定时任务管理器要调度的任务会被包装成一个TimeoutTask对象
    private class TimeoutTask implements Runnable {

        private final TimerTask             task;
        private final Timeout               timeout;
        private volatile ScheduledFuture<?> future;

        private TimeoutTask(TimerTask task) {
            this.task = task;
            this.timeout = new Timeout() {

                @Override
                public Timer timer() {
                    return DefaultTimer.this;
                }

                @Override
                public TimerTask task() {
                    return task;
                }

                @Override
                public boolean isExpired() {
                    return false; // never use
                }

                @Override
                public boolean isCancelled() {
                    final ScheduledFuture<?> f = future;
                    return f != null && f.isCancelled();
                }

                @Override
                public boolean cancel() {
                    final ScheduledFuture<?> f = future;
                    return f != null && f.cancel(false);
                }
            };
        }

        public Timeout getTimeout() {
            return timeout;
        }

        public ScheduledFuture<?> getFuture() {
            return future;
        }

        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public void run() {
            try {
                this.task.run(this.timeout);
            } catch (final Throwable ignored) {
                // never get here
            }
        }
    }
}
