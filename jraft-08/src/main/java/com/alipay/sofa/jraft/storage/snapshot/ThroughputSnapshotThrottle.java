package com.alipay.sofa.jraft.storage.snapshot;


import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.util.Utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


//快照传输限流器，但是这个用不到，就先不讲了
public class ThroughputSnapshotThrottle implements SnapshotThrottle {


    private final long throttleThroughputBytes;

    private final long checkCycleSecs;

    private long lastThroughputCheckTimeUs;

    private long currThroughputBytes;

    private final Lock lock = new ReentrantLock();

    private final long baseAligningTimeUs;


    public ThroughputSnapshotThrottle(final long throttleThroughputBytes, final long checkCycleSecs) {
        this.throttleThroughputBytes = throttleThroughputBytes;
        this.checkCycleSecs = checkCycleSecs;
        this.currThroughputBytes = 0L;
        this.baseAligningTimeUs = 1000 * 1000 / checkCycleSecs;
        this.lastThroughputCheckTimeUs = this.calculateCheckTimeUs(Utils.monotonicUs());
    }


    private long calculateCheckTimeUs(final long currTimeUs) {
        return currTimeUs / this.baseAligningTimeUs * this.baseAligningTimeUs;
    }


    @Override
    public long throttledByThroughput(final long bytes) {
        long availableSize;
        final long nowUs = Utils.monotonicUs();
        final long limitPerCycle = this.throttleThroughputBytes / this.checkCycleSecs;
        this.lock.lock();
        try {
            if (this.currThroughputBytes + bytes > limitPerCycle) {
                if (nowUs - this.lastThroughputCheckTimeUs <= 1000 * 1000 / this.checkCycleSecs) {
                    availableSize = limitPerCycle - this.currThroughputBytes;
                    this.currThroughputBytes = limitPerCycle;
                } else {
                    availableSize = bytes > limitPerCycle ? limitPerCycle : bytes;
                    this.currThroughputBytes = availableSize;
                    this.lastThroughputCheckTimeUs = calculateCheckTimeUs(nowUs);
                }
            } else {
                availableSize = bytes;
                this.currThroughputBytes += availableSize;
            }
        } finally {
            this.lock.unlock();
        }
        return availableSize;
    }
}
