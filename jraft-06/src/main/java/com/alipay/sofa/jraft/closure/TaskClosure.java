package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;


//第四版本还用不到
public interface TaskClosure extends Closure {

    /**
     * Called when task is committed to majority peers of the
     * RAFT group but before it is applied to state machine.
     *
     * <strong>Note: user implementation should not block
     * this method and throw any exceptions.</strong>
     */
    void onCommitted();
}