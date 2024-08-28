package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.JRaftServiceFactory;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.RaftMetaStorage;
import com.alipay.sofa.jraft.storage.impl.LocalRaftMetaStorage;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.SPI;
import org.apache.commons.lang.StringUtils;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:默认的JRaft服务工厂类，这个类上有SPI注解，在程序启动的时候，显然是要被SPI机制加载到内存中的
 */
@SPI
public class DefaultJRaftServiceFactory implements JRaftServiceFactory {

    public static DefaultJRaftServiceFactory newInstance() {
        return new DefaultJRaftServiceFactory();
    }

    //真正创建元数据存储器的方法
    //url是元数据文件的路径
    //raftOptions中封装着jRaft需要用到的一些配置参数，这些信息从RaftOptions对象创建完成的那一刻就初始化好的
    //因为这些信息都是RaftOptions对象的成员变量
    @Override
    public RaftMetaStorage createRaftMetaStorage(final String uri, final RaftOptions raftOptions) {
        Requires.requireTrue(!StringUtils.isBlank(uri), "Blank raft meta storage uri.");
        //在这里创建元数据存储器
        return new LocalRaftMetaStorage(uri, raftOptions);
    }

}
