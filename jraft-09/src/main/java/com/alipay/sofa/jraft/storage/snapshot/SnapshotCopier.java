package com.alipay.sofa.jraft.storage.snapshot;

import com.alipay.sofa.jraft.Status;

import java.io.Closeable;

/**
 * Copy snapshot from the give resources.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 * 2018-Mar-12 4:55:26 PM
 */
public abstract class SnapshotCopier extends Status implements Closeable {

    /**
     * Cancel the copy job.
     */
    public abstract void cancel();

    /**
     * Block the thread until this copy job finishes, or some error occurs.
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public abstract void join() throws InterruptedException;

    /**
     * Start the copy job.
     */
    public abstract void start();

    /**
     * Get the the SnapshotReader which represents the copied Snapshot
     */
    public abstract SnapshotReader getReader();
}