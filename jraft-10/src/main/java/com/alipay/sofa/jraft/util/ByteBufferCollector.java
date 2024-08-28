package com.alipay.sofa.jraft.util;

import java.nio.ByteBuffer;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/8
 * @Description:这个类从名字上看叫做ByteBuffer收集器，实际上其内部有一个ByteBuffer成员变量
 * 要发送给跟随者的日志条目中的内容都会放到这个ByteBuffer中，并且还会随着存放内容的增多，不断扩充
 * buffer的容量，当然底层实际上是新创建了一个容量更大的ByteBuffer对象，然后把旧的数据和新的数据
 * 都放到了新的ByteBuffer对象中，然后再把新的ByteBuffer对象赋值给buffer成员变量
 * 在这个类中使用到了对象池技术，ByteBufferCollector对象有自己的对象池
 */
public final class ByteBufferCollector implements Recyclable {

    //4M大小
    private static final int MAX_CAPACITY_TO_RECYCLE = 4 * 1024 * 1024;

    private ByteBuffer buffer;

    public int capacity() {
        return this.buffer != null ? this.buffer.capacity() : 0;
    }

    public void expandIfNecessary() {
        if (!hasRemaining()) {
            getBuffer(Utils.RAFT_DATA_BUF_SIZE);
        }
    }

    public void expandAtMost(final int atMostBytes) {
        if (this.buffer == null) {
            this.buffer = Utils.allocate(atMostBytes);
        } else {
            this.buffer = Utils.expandByteBufferAtMost(this.buffer, atMostBytes);
        }
    }

    public boolean hasRemaining() {
        return this.buffer != null && this.buffer.hasRemaining();
    }

    private ByteBufferCollector(final int size, final Recyclers.Handle handle) {
        if (size > 0) {
            this.buffer = Utils.allocate(size);
        }
        this.handle = handle;
    }

    public static ByteBufferCollector allocate(final int size) {
        return new ByteBufferCollector(size, Recyclers.NOOP_HANDLE);
    }

    public static ByteBufferCollector allocate() {
        return allocate(Utils.RAFT_DATA_BUF_SIZE);
    }

    public static ByteBufferCollector allocateByRecyclers(final int size) {
        final ByteBufferCollector collector = recyclers.get();
        collector.reset(size);
        return collector;
    }

    public static ByteBufferCollector allocateByRecyclers() {
        return allocateByRecyclers(Utils.RAFT_DATA_BUF_SIZE);
    }

    public static int threadLocalCapacity() {
        return recyclers.threadLocalCapacity();
    }

    public static int threadLocalSize() {
        return recyclers.threadLocalSize();
    }

    private void reset(final int expectSize) {
        if (this.buffer == null) {
            this.buffer = Utils.allocate(expectSize);
        } else {
            if (this.buffer.capacity() < expectSize) {
                this.buffer = Utils.allocate(expectSize);
            }
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:根据指定容量获得buffer的方法
     */
    private ByteBuffer getBuffer(final int expectSize) {
        //如果成员变量buffer为null
        if (this.buffer == null) {
            //根据指定的容量获取一个新的buffer
            this.buffer = Utils.allocate(expectSize);
        } else if (this.buffer.remaining() < expectSize) {
            //走到这里意味着buffer不为null，并且buffer中剩余的容量不足以存放
            //期望容量的数据，这时候就要创建一个新的容量更大的buffer，然后把新旧数据都存放到
            //新的buffer中
            this.buffer = Utils.expandByteBufferAtLeast(this.buffer, expectSize);
        }
        return this.buffer;
    }


    //把ButeBuffer中的数据放到该类的成员变量中
    public void put(final ByteBuffer buf) {
        //这里先根据要存放的buf的大小，获得一个可以存放这么多数据的buffer
        //其实就是在getBuffer内部判断当前类的buffer成员变量是否还有足够空间
        //存放buf中的数据，如果空间不够就重新分配一个容量更大的buffer，然后把新旧数据一起放进去
        getBuffer(buf.remaining()).put(buf);
    }

    public void put(final byte[] bs) {
        getBuffer(bs.length).put(bs);
    }

    public void setBuffer(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public boolean recycle() {
        if (this.buffer != null) {
            if (this.buffer.capacity() > MAX_CAPACITY_TO_RECYCLE) {
                //如果buffer的容量超过4M大小就直接垃圾回收了，不会被放到对象池中
                this.buffer = null;
            } else {
                BufferUtils.clear(this.buffer);
            }
        }
        return recyclers.recycle(this, handle);
    }

    private transient final Recyclers.Handle handle;

    private static final Recyclers<ByteBufferCollector> recyclers = new Recyclers<ByteBufferCollector>(
            Utils.MAX_COLLECTOR_SIZE_PER_THREAD) {

        @Override
        protected ByteBufferCollector newObject(final Handle handle) {
            return new ByteBufferCollector(0, handle);
        }
    };
}
