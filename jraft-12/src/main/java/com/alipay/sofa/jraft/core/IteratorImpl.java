package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.error.LogEntryCorruptedException;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.LogManager;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.ThreadPoolsFactory;
import com.alipay.sofa.jraft.util.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/18
 * @方法描述：该类的对象就是一批日志条目的迭代器实现，这个类没什么好说的，方法逻辑都很简单，只要FSMCallerImpl类的doCommitted方法弄清楚了，这个类就没一点难度
 * 注释就不添加了，都是迭代器的一些操作，非常简单，大家简单看看就行
 */
public class IteratorImpl {

    private final FSMCallerImpl fsmCaller;
    private final LogManager logManager;
    private final List<Closure> closures;
    private final long firstClosureIndex;
    private long currentIndex;
    private final long committedIndex;
    private long fsmCommittedIndex;
    private LogEntry currEntry = new LogEntry();
    private final AtomicLong applyingIndex;
    private RaftException error;

    public IteratorImpl(final FSMCallerImpl fsmCaller, final LogManager logManager, final List<Closure> closures,
                        final long firstClosureIndex, final long lastAppliedIndex, final long committedIndex,
                        final AtomicLong applyingIndex) {
        super();
        this.fsmCaller = fsmCaller;
        this.fsmCommittedIndex = -1L;
        this.logManager = logManager;
        this.closures = closures;
        this.firstClosureIndex = firstClosureIndex;
        this.currentIndex = lastAppliedIndex;
        this.committedIndex = committedIndex;
        this.applyingIndex = applyingIndex;
        next();
    }

    @Override
    public String toString() {
        return "IteratorImpl [fsmCaller=" + fsmCaller + ", logManager=" + logManager + ", closures=" + closures
                + ", firstClosureIndex=" + firstClosureIndex + ", currentIndex=" + currentIndex + ", committedIndex="
                + committedIndex + ", fsmCommittedIndex=" + fsmCommittedIndex + ", currEntry=" + currEntry
                + ", applyingIndex=" + applyingIndex + ", error=" + error + "]";
    }

    public LogEntry entry() {
        return this.currEntry;
    }

    public RaftException getError() {
        return this.error;
    }

    public boolean isGood() {
        return this.currentIndex <= this.committedIndex && !hasError();
    }

    public boolean hasError() {
        return this.error != null;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：迭代器得到下一个要处理的日志条目对象
     */
    public void next() {
        this.currEntry = null;
        if (this.currentIndex <= this.committedIndex) {
            //自增日志索引
            ++this.currentIndex;
            if (this.currentIndex <= this.committedIndex) {
                try {//根据索引得到下一个要处理的日志对象
                    this.currEntry = this.logManager.getEntry(this.currentIndex);
                    //对日志做判空检验
                    if (this.currEntry == null) {
                        getOrCreateError().setType(EnumOutter.ErrorType.ERROR_TYPE_LOG);
                        getOrCreateError().getStatus().setError(-1,
                                "Fail to get entry at index=%d while committed_index=%d", this.currentIndex,
                                this.committedIndex);
                    }
                } catch (final LogEntryCorruptedException e) {
                    getOrCreateError().setType(EnumOutter.ErrorType.ERROR_TYPE_LOG);
                    getOrCreateError().getStatus().setError(RaftError.EINVAL, e.getMessage());
                }//设置正在应用的日志索引，这个值也会被传递到状态机组件中，因为这个成员变量本来就是从状态机组件中传递过来的
                this.applyingIndex.set(this.currentIndex);
            }
        }
    }

    public long getIndex() {
        return this.currentIndex;
    }

    //得到要被回调的方法
    public Closure done() {
        if (this.currentIndex < this.firstClosureIndex) {
            return null;
        }
        return this.closures.get((int) (this.currentIndex - this.firstClosureIndex));
    }

    protected void runTheRestClosureWithError() {
        for (long i = Math.max(this.currentIndex, this.firstClosureIndex); i <= this.committedIndex; i++) {
            final Closure done = this.closures.get((int) (i - this.firstClosureIndex));
            if (done != null) {
                Requires.requireNonNull(this.error, "error");
                Requires.requireNonNull(this.error.getStatus(), "error.status");
                final Status status = this.error.getStatus();
                ThreadPoolsFactory.runClosureInThread(this.fsmCaller.getNode().getGroupId(), done, status);
            }
        }
    }

    public boolean commit() {
        if (isGood() && this.currEntry != null && this.currEntry.getType() == EnumOutter.EntryType.ENTRY_TYPE_DATA) {
            fsmCommittedIndex = this.currentIndex;
            //Commit last applied.
            this.fsmCaller.setLastApplied(currentIndex, this.currEntry.getId().getTerm());
            return true;
        }
        return false;
    }

//    public void commitAndSnapshotSync(Closure done) {
//        if (commit()) {
//            this.fsmCaller.getNode().snapshotSync(done);
//        } else {
//            Utils.runClosure(done, new Status(RaftError.ECANCELED, "Fail to commit, logIndex=" + currentIndex
//                    + ", committedIndex=" + committedIndex));
//        }
//    }

    public void setErrorAndRollback(final long ntail, final Status st) {
        Requires.requireTrue(ntail > 0, "Invalid ntail=" + ntail);
        if (this.currEntry == null || this.currEntry.getType() != EnumOutter.EntryType.ENTRY_TYPE_DATA) {
            this.currentIndex -= ntail;
        } else {
            this.currentIndex -= ntail - 1;
        }
        if (fsmCommittedIndex >= 0) {
            this.currentIndex = Math.max(this.currentIndex, fsmCommittedIndex + 1);
        }
        this.currEntry = null;
        getOrCreateError().setType(EnumOutter.ErrorType.ERROR_TYPE_STATE_MACHINE);
        getOrCreateError().getStatus().setError(RaftError.ESTATEMACHINE,
                "StateMachine meet critical error when applying one or more tasks since index=%d, %s", this.currentIndex,
                st != null ? st.toString() : "none");

    }

    private RaftException getOrCreateError() {
        if (this.error == null) {
            this.error = new RaftException();
        }
        return this.error;
    }
}
