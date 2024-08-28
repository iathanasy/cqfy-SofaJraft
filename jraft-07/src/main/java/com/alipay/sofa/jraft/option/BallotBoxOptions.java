package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.FSMCaller;
import com.alipay.sofa.jraft.closure.ClosureQueue;


public class BallotBoxOptions {

    private FSMCaller    waiter;
    private ClosureQueue closureQueue;

    public FSMCaller getWaiter() {
        return this.waiter;
    }

    public void setWaiter(FSMCaller waiter) {
        this.waiter = waiter;
    }

    public ClosureQueue getClosureQueue() {
        return this.closureQueue;
    }

    public void setClosureQueue(ClosureQueue closureQueue) {
        this.closureQueue = closureQueue;
    }
}