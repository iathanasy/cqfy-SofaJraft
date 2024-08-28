package com.alipay.sofa.jraft.util;

import com.alipay.sofa.jraft.util.internal.UnsafeUtil;
import org.jctools.queues.MpscChunkedArrayQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.atomic.MpscGrowableAtomicArrayQueue;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.util.Queue;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:该类的作用是提供Mpsc队列
 */
public final class Mpsc {

    private static final int MPSC_CHUNK_SIZE = 1024;
    private static final int MIN_MAX_MPSC_CAPACITY = MPSC_CHUNK_SIZE << 1;

    public static Queue<Runnable> newMpscQueue() {
        return UnsafeUtil.hasUnsafe() ? new MpscUnboundedArrayQueue<>(MPSC_CHUNK_SIZE)
                : new MpscUnboundedAtomicArrayQueue<>(MPSC_CHUNK_SIZE);
    }

    public static Queue<Runnable> newMpscQueue(final int maxCapacity) {
        final int capacity = Math.max(MIN_MAX_MPSC_CAPACITY, maxCapacity);
        return UnsafeUtil.hasUnsafe() ? new MpscChunkedArrayQueue<>(MPSC_CHUNK_SIZE, capacity)
                : new MpscGrowableAtomicArrayQueue<>(MPSC_CHUNK_SIZE, capacity);
    }
}
