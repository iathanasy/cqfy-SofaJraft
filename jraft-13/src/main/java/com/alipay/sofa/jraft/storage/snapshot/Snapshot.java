package com.alipay.sofa.jraft.storage.snapshot;

import com.alipay.sofa.jraft.Status;
import com.google.protobuf.Message;

import java.util.Set;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/21
 * @方法描述：快照抽象类，该类中定义了快照信息要存放的文件的名称
 */
public abstract class Snapshot extends Status {

    //快照元数据文件名称
    public static final String JRAFT_SNAPSHOT_META_FILE   = "__raft_snapshot_meta";
    //快照文件名称
    public static final String JRAFT_SNAPSHOT_PREFIX      = "snapshot_";
    //这个成员变量第八版本还用不到，第九版本就会用到了，远程复制快照时会用到
    public static final String REMOTE_SNAPSHOT_URI_SCHEME = "remote://";


    public abstract String getPath();


    public abstract Set<String> listFiles();


    public abstract Message getFileMeta(final String fileName);
}
