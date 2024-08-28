package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;


@ThreadSafe
public interface ClosureQueue {

    //清除队列
    void clear();

    //重置队列的第一个索引
    void resetFirstIndex(final long firstIndex);

    //添加新的回调方法到队列中
    void appendPendingClosure(final Closure closure);


    long popClosureUntil(final long endIndex, final List<Closure> closures);


    long popClosureUntil(final long endIndex, final List<Closure> closures, final List<TaskClosure> taskClosures);
}