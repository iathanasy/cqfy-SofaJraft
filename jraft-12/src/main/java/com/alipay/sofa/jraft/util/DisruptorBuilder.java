package com.alipay.sofa.jraft.util;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ThreadFactory;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/3
 * @Description:Disruptor框架的构建器
 */
public class DisruptorBuilder<T> {
    private EventFactory<T> eventFactory;
    private Integer ringBufferSize;
    private ThreadFactory threadFactory = new NamedThreadFactory("Disruptor-", true);
    private ProducerType producerType = ProducerType.MULTI;
    private WaitStrategy waitStrategy = new BlockingWaitStrategy();

    private DisruptorBuilder() {
    }

    public static <T> DisruptorBuilder<T> newInstance() {
        return new DisruptorBuilder<>();
    }

    public EventFactory<T> getEventFactory() {
        return this.eventFactory;
    }

    public DisruptorBuilder<T> setEventFactory(final EventFactory<T> eventFactory) {
        this.eventFactory = eventFactory;
        return this;
    }

    public int getRingBufferSize() {
        return this.ringBufferSize;
    }

    public DisruptorBuilder<T> setRingBufferSize(final int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
        return this;
    }

    public ThreadFactory getThreadFactory() {
        return this.threadFactory;
    }

    public DisruptorBuilder<T> setThreadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public ProducerType getProducerType() {
        return this.producerType;
    }

    public DisruptorBuilder<T> setProducerType(final ProducerType producerType) {
        this.producerType = producerType;
        return this;
    }

    public WaitStrategy getWaitStrategy() {
        return this.waitStrategy;
    }

    public DisruptorBuilder<T> setWaitStrategy(final WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    public Disruptor<T> build() {
        Requires.requireNonNull(this.ringBufferSize, " Ring buffer size not set");
        Requires.requireNonNull(this.eventFactory, "Event factory not set");
        return new Disruptor<>(this.eventFactory, this.ringBufferSize, this.threadFactory, this.producerType,
                this.waitStrategy);
    }

}
