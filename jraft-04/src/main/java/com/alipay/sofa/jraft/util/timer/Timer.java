package com.alipay.sofa.jraft.util.timer;

import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;


public interface Timer {


    Timeout newTimeout(final TimerTask task, final long delay, final TimeUnit unit);

    Set<Timeout> stop();
}
