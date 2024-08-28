package com.alipay.sofa.jraft.storage;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.option.LogStorageOptions;

import java.util.List;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/30
 * @Description:日志存储器要实现的接口
 */
public interface LogStorage extends Lifecycle<LogStorageOptions>, Storage {

    //得到第一条日志索引的方法
    long getFirstLogIndex();

    //得到最后一条日志索引的方法
    long getLastLogIndex();

    int appendEntries(final List<LogEntry> entries);

    //根据索引得到指定日志的方法
    LogEntry getEntry(final long index);

    boolean truncatePrefix(final long firstIndexKept);

    boolean truncateSuffix(final long firstIndexKept);

    //根据日志索引得到对应日志任期的方法
    @Deprecated
    long getTerm(final long index);

    boolean reset(final long nextLogIndex);

    boolean appendEntry(final LogEntry entry);

}