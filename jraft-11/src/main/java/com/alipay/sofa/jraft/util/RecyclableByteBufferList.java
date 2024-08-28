package com.alipay.sofa.jraft.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/8
 * @Description:该类继承了ArrayList，要发送的日志条目的data会先存放到集合的数组中，这个类也使用了对象池技术
 */
public final class RecyclableByteBufferList extends ArrayList<ByteBuffer> implements Recyclable {

    private static final long serialVersionUID = -8605125654176467947L;

    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    //总共存放的字节容量
    private int capacity = 0;

    /**
     * Create a new empty {@link RecyclableByteBufferList} instance
     */
    public static RecyclableByteBufferList newInstance() {
        return newInstance(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Create a new empty {@link RecyclableByteBufferList} instance with the given capacity.
     */
    public static RecyclableByteBufferList newInstance(final int minCapacity) {
        final RecyclableByteBufferList ret = recyclers.get();
        ret.ensureCapacity(minCapacity);
        return ret;
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public boolean addAll(final Collection<? extends ByteBuffer> c) {
        throw reject("addAll");
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends ByteBuffer> c) {
        throw reject("addAll");
    }

    @Override
    public boolean add(final ByteBuffer element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        this.capacity += element.remaining();
        return super.add(element);
    }

    @Override
    public void add(final int index, final ByteBuffer element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        this.capacity += element.remaining();
        super.add(index, element);
    }

    @Override
    public ByteBuffer set(final int index, final ByteBuffer element) {
        throw reject("set");
    }

    @Override
    public ByteBuffer remove(final int index) {
        throw reject("remove");
    }

    @Override
    public boolean remove(final Object o) {
        throw reject("remove");
    }

    @Override
    public boolean recycle() {
        clear();
        this.capacity = 0;
        return recyclers.recycle(this, handle);
    }

    public static int threadLocalCapacity() {
        return recyclers.threadLocalCapacity();
    }

    public static int threadLocalSize() {
        return recyclers.threadLocalSize();
    }

    private static UnsupportedOperationException reject(final String message) {
        return new UnsupportedOperationException(message);
    }

    private RecyclableByteBufferList(final Recyclers.Handle handle) {
        this(handle, DEFAULT_INITIAL_CAPACITY);
    }

    private RecyclableByteBufferList(final Recyclers.Handle handle, final int initialCapacity) {
        super(initialCapacity);
        this.handle = handle;
    }

    private transient final Recyclers.Handle handle;

    private static final Recyclers<RecyclableByteBufferList> recyclers = new Recyclers<RecyclableByteBufferList>(512) {

        @Override
        protected RecyclableByteBufferList newObject(final Handle handle) {
            return new RecyclableByteBufferList(
                    handle);
        }
    };
}
