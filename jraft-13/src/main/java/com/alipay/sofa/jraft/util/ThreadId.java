package com.alipay.sofa.jraft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:该类的对象会持有复制器对象的引用，算是对复制器对象的包装吧，提供了同步锁的功能
 */
public class ThreadId {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadId.class);
    private final Object data;
    private final ReentrantLock lock = new ReentrantLock();
    private final OnError onError;
    private volatile boolean destroyed;


    public interface OnError {
        void onError(final ThreadId id, final Object data, final int errorCode);
    }

    public ThreadId(final Object data, final OnError onError) {
        super();
        this.data = data;
        this.onError = onError;
        this.destroyed = false;
    }

    public Object getData() {
        return this.data;
    }

    public Object lock() {
        if (this.destroyed) {
            return null;
        }
        this.lock.lock();
        if (this.destroyed) {
            this.lock.unlock();
            return null;
        }
        return this.data;
    }

    public void unlock() {
        if (!this.lock.isHeldByCurrentThread()) {
            LOG.warn("Fail to unlock with {}, the lock is not held by current thread {}.", this.data,
                    Thread.currentThread());
            return;
        }
        this.lock.unlock();
    }

    //让当前线程让出执行行权
    public void join() {
        while (!this.destroyed) {
            ThreadHelper.onSpinWait();
        }
    }

    @Override
    public String toString() {
        return this.data.toString();
    }

    public void unlockAndDestroy() {
        if (this.destroyed) {
            return;
        }
        this.destroyed = true;
        unlock();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:这个方法中会执行  this.onError.onError(this, this.data, errorCode) 这行代码
     * 在这行代码中就会继续发送下一个心跳消息
     */
    public void setError(final int errorCode) {
        if (this.destroyed) {
            LOG.warn("ThreadId: {} already destroyed, ignore error code: {}", this.data, errorCode);
            return;
        }
        this.lock.lock();
        try {
            if (this.destroyed) {
                LOG.warn("ThreadId: {} already destroyed, ignore error code: {}", this.data, errorCode);
                return;
            }
            if (this.onError != null) {
                this.onError.onError(this, this.data, errorCode);
            }
        } finally {
            if (!this.destroyed) {
                this.lock.unlock();
            }
        }
    }
}
