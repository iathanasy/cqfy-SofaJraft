package com.alipay.sofa.jraft.util.concurrent;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:执行器选择器工厂的接口
 */
public interface ExecutorChooserFactory {

    /**
     * Returns a new {@link ExecutorChooser}.
     */
    ExecutorChooser newChooser(final SingleThreadExecutor[] executors);

    interface ExecutorChooser {

        /**
         * Returns the next {@link SingleThreadExecutor} to use.
         */
        SingleThreadExecutor next();

        /**
         * Returns the chosen {@link SingleThreadExecutor} to use.
         */
        SingleThreadExecutor select(final int index);
    }
}
