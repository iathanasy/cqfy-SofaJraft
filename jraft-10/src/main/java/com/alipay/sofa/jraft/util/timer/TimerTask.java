package com.alipay.sofa.jraft.util.timer;


//要提交到时间轮中的定时任务接口
public interface TimerTask {

    void run(final Timeout timeout) throws Exception;
}