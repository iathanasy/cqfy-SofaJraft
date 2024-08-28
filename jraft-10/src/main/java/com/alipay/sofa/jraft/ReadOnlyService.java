package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.ReadOnlyOption;
import com.alipay.sofa.jraft.option.ReadOnlyServiceOptions;


public interface ReadOnlyService extends Lifecycle<ReadOnlyServiceOptions> {

    /**
     * Adds a ReadIndex request.
     *
     * @param reqCtx    request context of readIndex
     * @param closure   callback
     */
    void addRequest(final byte[] reqCtx, final ReadIndexClosure closure);

    /**
     * Adds a ReadIndex request.
     *
     * @param readOnlyOptions how the read only request is processed
     * @param reqCtx          request context of readIndex
     * @param closure         callback
     */
    void addRequest(final ReadOnlyOption readOnlyOptions, final byte[] reqCtx, final ReadIndexClosure closure);

    /**
     * Waits for service shutdown.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    void join() throws InterruptedException;

    /**
     * Called when the node is turned into error state.
     * @param error error with raft info
     */
    void setError(final RaftException error);

}
