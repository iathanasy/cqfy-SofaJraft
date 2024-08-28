package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LogEntry;

import java.nio.ByteBuffer;


/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/18
 * @方法描述：这个类就是对IteratorImpl做了一层简单的包装，让用户在业务层定义的状态机可以获得日志的有效数据，也就是LogEntry的data
 */
public class IteratorWrapper implements Iterator {

    private final IteratorImpl impl;

    public IteratorWrapper(IteratorImpl iterImpl) {
        super();
        this.impl = iterImpl;
    }

    @Override
    public boolean hasNext() {
        return this.impl.isGood() && this.impl.entry().getType() == EnumOutter.EntryType.ENTRY_TYPE_DATA;
    }

    @Override
    public ByteBuffer next() {
        final ByteBuffer data = getData();
        if (hasNext()) {
            this.impl.next();
        }
        return data;
    }

    @Override
    public ByteBuffer getData() {
        final LogEntry entry = this.impl.entry();
        return entry != null ? entry.getData() : null;
    }

    @Override
    public long getIndex() {
        return this.impl.getIndex();
    }

    @Override
    public long getTerm() {
        return this.impl.entry().getId().getTerm();
    }

    @Override
    public boolean commit() {
        return this.impl.commit();
    }

    @Override
    public void commitAndSnapshotSync(Closure done) {
        //this.impl.commitAndSnapshotSync(done);
    }

    @Override
    public Closure done() {
        return this.impl.done();
    }

    @Override
    public void setErrorAndRollback(final long ntail, final Status st) {
        this.impl.setErrorAndRollback(ntail, st);
    }
}
