package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

//在第四版本中还用不到
@ThreadSafe
public interface ClosureQueue {

    /**
     * Clear all closure in queue.
     */
    void clear();

    /**
     * Reset the first index in queue.
     *
     * @param firstIndex the first index of queue
     */
    void resetFirstIndex(final long firstIndex);

    /**
     * Append a new closure into queue.
     *
     * @param closure the closure to append
     */
    void appendPendingClosure(final Closure closure);

    /**
     * Pop closure from queue until index(inclusion), returns the first
     * popped out index, returns -1 when out of range, returns index+1
     * when not found.
     *
     * @param endIndex     the index of queue
     * @param closures     closure list
     * @return returns the first popped out index, returns -1 when out
     * of range, returns index+1
     * when not found.
     */
    long popClosureUntil(final long endIndex, final List<Closure> closures);

    /**
     * Pop closure from queue until index(inclusion), returns the first
     * popped out index, returns -1 when out of range, returns index+1
     * when not found.
     *
     * @param endIndex     the index of queue
     * @param closures     closure list
     * @param taskClosures task closure list
     * @return returns the first popped out index, returns -1 when out
     * of range, returns index+1
     * when not found.
     */
    long popClosureUntil(final long endIndex, final List<Closure> closures, final List<TaskClosure> taskClosures);
}
