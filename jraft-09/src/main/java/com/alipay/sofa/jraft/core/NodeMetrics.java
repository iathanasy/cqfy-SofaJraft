package com.alipay.sofa.jraft.core;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:该类的作用就是用来监控节点性能的，这个类的作用先不做讲解
 */
public class NodeMetrics {

    //这个是真正提供监控和追踪功能的对象，是第三方框架的功能
    private final MetricRegistry metrics;


    public NodeMetrics(final boolean enableMetrics) {
        if (enableMetrics) {
            this.metrics = new MetricRegistry();
        } else {
            this.metrics = null;
        }
    }


    public Map<String, Metric> getMetrics() {
        if (this.metrics != null) {
            return this.metrics.getMetrics();
        }
        return Collections.emptyMap();
    }


    public MetricRegistry getMetricRegistry() {
        return this.metrics;
    }


    public boolean isEnabled() {
        return this.metrics != null;
    }

    public void recordTimes(final String key, final long times) {
        if (this.metrics != null) {
            this.metrics.counter(key).inc(times);
        }
    }


    public void recordSize(final String key, final long size) {
        if (this.metrics != null) {
            this.metrics.histogram(key).update(size);
        }
    }


    public void recordLatency(final String key, final long duration) {
        if (this.metrics != null) {
            this.metrics.timer(key).update(duration, TimeUnit.MILLISECONDS);
        }
    }
}
