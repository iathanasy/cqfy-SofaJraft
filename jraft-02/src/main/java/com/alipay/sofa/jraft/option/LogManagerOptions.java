package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.conf.ConfigurationManager;
import com.alipay.sofa.jraft.core.NodeMetrics;
import com.alipay.sofa.jraft.entity.codec.LogEntryCodecFactory;
import com.alipay.sofa.jraft.entity.codec.v2.LogEntryV2CodecFactory;
import com.alipay.sofa.jraft.storage.LogStorage;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/1
 * @Description:该类的对象封装的是日志管理器需要的配置参数
 */
public class LogManagerOptions {
    private String groupId;
    private LogStorage logStorage;
    private ConfigurationManager configurationManager;
    //这个成员变量暂时还用不上，下一版本代码就会用上了
    private int disruptorBufferSize = 1024;
    private RaftOptions raftOptions;
    private NodeMetrics nodeMetrics;
    //编解码器工厂在这里创建了
    private LogEntryCodecFactory logEntryCodecFactory = LogEntryV2CodecFactory.getInstance();

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public LogEntryCodecFactory getLogEntryCodecFactory() {
        return this.logEntryCodecFactory;
    }

    public void setLogEntryCodecFactory(final LogEntryCodecFactory logEntryCodecFactory) {
        this.logEntryCodecFactory = logEntryCodecFactory;
    }

    public NodeMetrics getNodeMetrics() {
        return this.nodeMetrics;
    }

    public void setNodeMetrics(final NodeMetrics nodeMetrics) {
        this.nodeMetrics = nodeMetrics;
    }

    public RaftOptions getRaftOptions() {
        return this.raftOptions;
    }

    public void setRaftOptions(final RaftOptions raftOptions) {
        this.raftOptions = raftOptions;
    }

    public int getDisruptorBufferSize() {
        return this.disruptorBufferSize;
    }

    public void setDisruptorBufferSize(final int disruptorBufferSize) {
        this.disruptorBufferSize = disruptorBufferSize;
    }

    public LogStorage getLogStorage() {
        return this.logStorage;
    }

    public void setLogStorage(final LogStorage logStorage) {
        this.logStorage = logStorage;
    }

    public ConfigurationManager getConfigurationManager() {
        return this.configurationManager;
    }

    public void setConfigurationManager(final ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

}
