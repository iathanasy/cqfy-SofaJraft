package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.closure.ClosureQueue;

/**
 * Ballot box options.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 * 2018-Apr-04 2:58:36 PM
 */
public class BallotBoxOptions {

    private ClosureQueue closureQueue;


    public ClosureQueue getClosureQueue() {
        return this.closureQueue;
    }

    public void setClosureQueue(ClosureQueue closureQueue) {
        this.closureQueue = closureQueue;
    }
}