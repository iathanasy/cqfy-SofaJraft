package com.alipay.sofa.jraft.entity;

import com.alipay.sofa.jraft.rpc.RpcRequests;

import java.util.List;




public class ReadIndexStatus {

    private final RpcRequests.ReadIndexRequest request; // raw request
    private final List<ReadIndexState> states; // read index requests in batch.
    private final long index;  // committed log index.

    public ReadIndexStatus(List<ReadIndexState> states, RpcRequests.ReadIndexRequest request, long index) {
        super();
        this.index = index;
        this.request = request;
        this.states = states;
    }

    public boolean isApplied(long appliedIndex) {
        return appliedIndex >= this.index;
    }

    public boolean isOverMaxReadIndexLag(long applyIndex, int maxReadIndexLag) {
        if (maxReadIndexLag < 0) {
            return false;
        }
        return this.index - applyIndex > maxReadIndexLag;
    }

    public long getIndex() {
        return index;
    }

    public RpcRequests.ReadIndexRequest getRequest() {
        return request;
    }

    public List<ReadIndexState> getStates() {
        return states;
    }

}