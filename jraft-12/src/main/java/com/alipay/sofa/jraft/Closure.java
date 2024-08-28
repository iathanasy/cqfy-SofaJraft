package com.alipay.sofa.jraft;



/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:回调函数接口，在NodeImpl类中，有几个内部类，都实现了该接口
 * 那些内部类的对象就封装着要被回调的方法，当jraft内部的客户端发送的请求接收到响应后，这些方法就会被回调
 */
public interface Closure {


    void run(final Status status);
}
