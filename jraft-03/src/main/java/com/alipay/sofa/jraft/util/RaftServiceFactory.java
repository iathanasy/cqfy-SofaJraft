package com.alipay.sofa.jraft.util;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.core.NodeImpl;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/21
 * @Description:该工厂类是用来创建节点，并且初始化节点的
 */
public final class RaftServiceFactory {

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/21
     * @Description:根据用户配置的信息创建一个节点的实现类对象
     */
    public static Node createRaftNode(final String groupId, final PeerId serverId) {
        return new NodeImpl(groupId, serverId);
    }

   /**
    * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
    * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
    * @Date:2023/11/21
    * @Description:根据用户配置的信息创建一个NodeImpl对象，并且初始化这个节点对象，在初始化节点的过程中，当前节点需要的组件就一一启动了
    */
    public static Node createAndInitRaftNode(final String groupId, final PeerId serverId, final NodeOptions opts) {
        final Node ret = createRaftNode(groupId, serverId);
        //初始化节点
        if (!ret.init(opts)) {
            throw new IllegalStateException("Fail to init node, please see the logs to find the reason.");
        }
        return ret;
    }



    //下面这两个方法暂时用不到，就直接注释了
//    public static CliService createCliService() {
//        return new CliServiceImpl();
//    }
//

//    public static CliService createAndInitCliService(final CliOptions cliOptions) {
//        final CliService ret = createCliService();
//        if (!ret.init(cliOptions)) {
//            throw new IllegalStateException("Fail to init CliService");
//        }
//        return ret;
//    }
}
