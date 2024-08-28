package com.alipay.sofa.jraft.util;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.lmax.disruptor.RingBuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * Disruptor metric set including buffer-size, remaining-capacity etc.
 */
public final class DisruptorMetricSet implements MetricSet {

    private final RingBuffer<?> ringBuffer;

    public DisruptorMetricSet(RingBuffer<?> ringBuffer) {
        super();
        this.ringBuffer = ringBuffer;
    }

    /**
     * Return disruptor metrics
     * @return disruptor metrics map
     */
    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        gauges.put("buffer-size", (Gauge<Integer>) this.ringBuffer::getBufferSize);
        gauges.put("remaining-capacity", (Gauge<Long>) this.ringBuffer::remainingCapacity);
        return gauges;
    }
}
