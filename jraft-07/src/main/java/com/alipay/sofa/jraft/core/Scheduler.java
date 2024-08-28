package com.alipay.sofa.jraft.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public interface Scheduler {


    ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit);


    ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                           final TimeUnit unit);


    ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                              final TimeUnit unit);

    void shutdown();
}
