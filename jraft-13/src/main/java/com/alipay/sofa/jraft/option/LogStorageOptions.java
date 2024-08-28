package com.alipay.sofa.jraft.option;


import com.alipay.sofa.jraft.conf.ConfigurationManager;
import com.alipay.sofa.jraft.entity.codec.LogEntryCodecFactory;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/1
 * @Description:该类的对象封装的是日志存储器需要的配置参数
 */
public class LogStorageOptions {

    private String groupId;
    private ConfigurationManager configurationManager;

    private LogEntryCodecFactory logEntryCodecFactory;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public ConfigurationManager getConfigurationManager() {
        return this.configurationManager;
    }

    public void setConfigurationManager(final ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public LogEntryCodecFactory getLogEntryCodecFactory() {
        return this.logEntryCodecFactory;
    }

    public void setLogEntryCodecFactory(final LogEntryCodecFactory logEntryCodecFactory) {
        this.logEntryCodecFactory = logEntryCodecFactory;
    }

}
