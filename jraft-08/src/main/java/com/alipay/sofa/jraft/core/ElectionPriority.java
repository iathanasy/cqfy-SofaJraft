package com.alipay.sofa.jraft.core;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:节点选举的优先级
 */
public class ElectionPriority {


    //这些优先级信息都会封装在PeerId对象中，在第一版本中并没有展示这方面的功能
    //优先级为-1时，表示该节点已禁用了按照优先级选举功能
    public static final int Disabled   = -1;

    //为0时，表示该节点永远不会参数选举，也就意味着永远不会成为领导者
    public static final int NotElected = 0;

    //选举优先级的最小值，选举的时候优先级最低，比如在集群中就有一台的服务器性能最好，我就希望这个服务器当作领导者
    //那就可以通过优先级配置，使这个服务器成为领导者的概率更大
    public static final int MinValue   = 1;
}