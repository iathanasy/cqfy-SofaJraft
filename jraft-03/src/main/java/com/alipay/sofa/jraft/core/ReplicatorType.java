package com.alipay.sofa.jraft.core;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:这个枚举类是用来表示复制器的角色的，这里面有一个复制器的概念，也就是一个Replicator对象
 * 简单来说，当一个节点称为领导者之后，这个集群中的其他节点的信息都会在领导者节点中封装为一个个复制器对象
 * 领导者向每一个跟随者同步数据的时候，都是通过每一个复制器对象来进行的，这个枚举类的对象就是用来判断
 * 一个复制器对象的身份究竟是跟随着，还是学习者，因为跟随着和学习者能够进行的活动还是不一样的
 */
public enum ReplicatorType {
    Follower, Learner;

    public final boolean isFollower() {
        return this == Follower;
    }

    public final boolean isLearner() {
        return this == Learner;
    }
}