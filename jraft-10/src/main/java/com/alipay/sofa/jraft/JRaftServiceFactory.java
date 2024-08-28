package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.entity.codec.LogEntryCodecFactory;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.LogStorage;
import com.alipay.sofa.jraft.storage.RaftMetaStorage;
import com.alipay.sofa.jraft.storage.SnapshotStorage;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:创建服务的工厂，所谓的服务就是元数据持久化与初始化服务，日志初始化与出久化服务，快照，状态机等等服务
 * 在第一版本只引入了元数据相关服务，所谓元数据，就是每一个节点的任期，最后一次给哪个节点投票这些数据
 */
public interface JRaftServiceFactory {


    //创建日志存储器，实际上创建的就是一个RocksDBLogStorage对象
    //这个RocksDBLogStorage对象封装了RocksDB，jraft框架运行过程中
    //产生的所有日志都会存放在这个RocksDB数据库中，这个数据库是C++开发的
    LogStorage createLogStorage(final String uri, final RaftOptions raftOptions);

    //创建元数据服务的方法，可以把RaftMetaStorage看作一个元数据存储器
    RaftMetaStorage createRaftMetaStorage(final String uri, final RaftOptions raftOptions);

    //创建快照存储器
    SnapshotStorage createSnapshotStorage(final String uri, final RaftOptions raftOptions);

    //创建编解码工厂
    LogEntryCodecFactory createLogEntryCodecFactory();


}
