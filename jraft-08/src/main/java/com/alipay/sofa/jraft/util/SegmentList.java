package com.alipay.sofa.jraft.util;

import com.alipay.sofa.jraft.util.internal.ReferenceFieldUpdater;
import com.alipay.sofa.jraft.util.internal.UnsafeUtil;
import com.alipay.sofa.jraft.util.internal.Updaters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/4
 * @Description:缓存日志条目对象的组件
 */
public class SegmentList<T> {
    //用来辅助计算每个Segment对象内部存放数据的数组容量大小的成员变量
    //也就是elements[]数组的大小
    private static final int SEGMENT_SHIFT = 7;
    //这里就是计算出真正的elements数组的容量大小，2左移6位，也就是128
    public static final int SEGMENT_SIZE = 2 << (SEGMENT_SHIFT - 1);
    //这个内部队列是用来存放segment对象的，segment对象是用来存放每一个日志条目对象的
    private final ArrayDeque<Segment<T>> segments;
    //这里的这个成员变量代表的是一共有多少日志条目存放到SegmentList的队列中了
    private int size;
    //segments队列中第一个segment对象的偏移量，这个偏移量初始为0
    //这个偏移量的具体使用方法，我建议大家去本类的public T get(int index)方法内查看
    private int firstOffset;
    //该成员变量表示是否启用对象池
    private final boolean recycleSegment;

    //构造方法
    public SegmentList(final boolean recycleSegment) {
        this.segments = new ArrayDeque<>();
        this.size = 0;
        this.firstOffset = 0;
        this.recycleSegment = recycleSegment;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:Segment内部类的对象就是用来真正缓存日志条目的，Segment对象内部的数组可以存放128个日志条目
     * Segment对象会存放到SegmentList对象的内部队列segments中
     */
    private final static class Segment<T> implements Recyclable {

        //对象池本身
        private static final Recyclers<Segment<?>> recyclers = new Recyclers<Segment<?>>(16_382 / SEGMENT_SIZE) {

            @Override
            protected Segment<?> newObject(final Handle handle) {
                return new Segment<>(handle); // 创建新的 Segment 对象
            }
        };

        //重写的对象池中的方法
        public static Segment<?> newInstance(final boolean recycleSegment) {
            if (recycleSegment) {
                //如果开启了对象池，就从对象池中获得对象
                return recyclers.get();
            } else {//否则就直接创建
                return new Segment<>();
            }
        }

        //对象池句柄
        private transient Recyclers.Handle handle;

        //Segment对象内部存放数据的数组
        final T[] elements;
        //Segment对象内部数组存放的最后一个数据的位置指针
        int pos;
        //Segment对象内部数组存储数据的起始位置
        int offset;

        //构造方法
        Segment() {
            this(Recyclers.NOOP_HANDLE);
        }

        //构造方法
        @SuppressWarnings("unchecked")
        Segment(final Recyclers.Handle handle) {
            //初始化数组，数组容量就是上面那个成员变量，默认为128
            //也就意味着一个Segment对象可以缓存128个日志条目
            this.elements = (T[]) new Object[SEGMENT_SIZE];
            //初始化数组的位置指针
            this.pos = this.offset = 0;
            this.handle = handle;
        }


        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:重置当前Segment对象
         */
        void clear() {
            this.pos = this.offset = 0;
            Arrays.fill(this.elements, null);
        }

        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:回收当前Segment对象到对象池的方法
         */
        @Override
        public boolean recycle() {
            //重置Segment对象
            clear();
            //回收Segment对象，这是对象池的旧知识了，就不再重复讲解了
            return recyclers.recycle(this, this.handle);
        }

        //当前Segment对象剩余的可用容量
        int cap() {
            return SEGMENT_SIZE - this.pos;
        }

        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:把src数组中的数据添加到当前Segment对象elements数组中的方法
         * 其实就是一个简单的赋值，src是数据源数组，srcPos数据开始复制的位置，len是赋值多少条
         */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        private void addAll(final Object[] src, final int srcPos, final int len) {
            System.arraycopy(src, srcPos, this.elements, this.pos, len);
            //复制完毕后，给当前Segment对象的指针赋值，当前Segment对象中缓存了多少数据，指针就增加多少
            this.pos += len;
        }

        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:当前Segment对象的是否已经用完了
         */
        boolean isReachEnd() {
            return this.pos == SEGMENT_SIZE;
        }

        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:下面这些方法的逻辑都很简单，就不再一一注释了，大家简单看看就行
         */
        boolean isEmpty() {
            return this.size() == 0;
        }

        void add(final T e) {
            this.elements[this.pos++] = e;
        }

        T get(final int index) {
            if (index >= this.pos || index < this.offset) {
                throw new IndexOutOfBoundsException("Index=" + index + ", Offset=" + this.offset + ", Pos=" + this.pos);
            }
            return this.elements[index];
        }

        T peekLast() {
            return this.elements[this.pos - 1];
        }

        int size() {
            return this.pos - this.offset;
        }

        T peekFirst() {
            return this.elements[this.offset];
        }

        int removeFromLastWhen(final Predicate<T> predicate) {
            int removed = 0;
            for (int i = this.pos - 1; i >= this.offset; i--) {
                T e = this.elements[i];
                if (predicate.test(e)) {
                    this.elements[i] = null;
                    removed++;
                } else {
                    break;
                }
            }
            this.pos -= removed;
            return removed;
        }

        int removeFromFirstWhen(final Predicate<T> predicate) {
            int removed = 0;
            for (int i = this.offset; i < this.pos; i++) {
                T e = this.elements[i];
                if (predicate.test(e)) {
                    this.elements[i] = null;
                    removed++;
                } else {
                    break;
                }
            }
            this.offset += removed;
            return removed;
        }

        int removeFromFirst(final int toIndex) {
            int removed = 0;
            for (int i = this.offset; i < Math.min(toIndex, this.pos); i++) {
                this.elements[i] = null;
                removed++;
            }
            this.offset += removed;
            return removed;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (int i = this.offset; i < this.pos; i++) {
                b.append(this.elements[i]);
                if (i != this.pos - 1) {
                    b.append(", ");
                }
            }
            return "Segment [elements=" + b.toString() + ", offset=" + this.offset + ", pos=" + this.pos + "]";
        }

    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:根据指定索引获得对应日志条目的方法
     */
    public T get(int index) {
        //假如我现在就要获得日志索引为1的日志条目，这里直接得到了第一个segment对象的偏移量
        //index也就是1，下面就让(index >> SEGMENT_SHIFT)，也就是右移7位
        //原因很简单，因为segments队列中的每个segment对象可以缓存128个日志条目
        //不管你要获得哪个索引对应的日志，你总要先找到这个日志在哪个segment对象中缓存着吧
        //现在index是1，所以右移之后是0，就直接从segments队列的第0个segment对象中获得日志就行了
        //index & (SEGMENT_SIZE - 1)这行代码的作用也很简单，就是计算索引为1的日志在segment对象内部数组中的索引
        index += this.firstOffset;
        return this.segments.get(index >> SEGMENT_SHIFT).get(index & (SEGMENT_SIZE - 1));
    }

    public T peekLast() {
        Segment<T> lastSeg = getLast();
        return lastSeg == null ? null : lastSeg.peekLast();
    }

    public T peekFirst() {
        Segment<T> firstSeg = getFirst();
        return firstSeg == null ? null : firstSeg.peekFirst();
    }

    private Segment<T> getFirst() {
        if (!this.segments.isEmpty()) {
            return this.segments.peekFirst();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void add(final T e) {
        Segment<T> lastSeg = getLast();
        if (lastSeg == null || lastSeg.isReachEnd()) {
            lastSeg = (Segment<T>) Segment.newInstance(this.recycleSegment);
            this.segments.add(lastSeg);
        }
        lastSeg.add(e);
        this.size++;
    }

    private Segment<T> getLast() {
        if (!this.segments.isEmpty()) {
            return this.segments.peekLast();
        }
        return null;
    }

    public int size() {
        return this.size;
    }

    public int segmentSize() {
        return this.segments.size();
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void removeFromFirstWhen(final Predicate<T> predicate) {
        Segment<T> firstSeg = getFirst();
        while (true) {
            if (firstSeg == null) {
                this.firstOffset = this.size = 0;
                return;
            }
            int removed = firstSeg.removeFromFirstWhen(predicate);
            if (removed == 0) {
                break;
            }
            this.size -= removed;
            this.firstOffset = firstSeg.offset;
            if (firstSeg.isEmpty()) {
                RecycleUtil.recycle(this.segments.pollFirst());
                firstSeg = getFirst();
                this.firstOffset = 0;
            }
        }
    }

    public void clear() {
        while (!this.segments.isEmpty()) {
            RecycleUtil.recycle(this.segments.pollLast());
        }
        this.size = this.firstOffset = 0;
    }

    public void removeFromLastWhen(final Predicate<T> predicate) {
        Segment<T> lastSeg = getLast();
        while (true) {
            if (lastSeg == null) {
                this.firstOffset = this.size = 0;
                return;
            }
            int removed = lastSeg.removeFromLastWhen(predicate);
            if (removed == 0) {
                break;
            }
            this.size -= removed;
            if (lastSeg.isEmpty()) {
                RecycleUtil.recycle(this.segments.pollLast());
                lastSeg = getLast();
            }
        }
    }


    public void removeFromFirst(final int toIndex) {
        int alignedIndex = toIndex + this.firstOffset;
        int toSegmentIndex = alignedIndex >> SEGMENT_SHIFT;
        int toIndexInSeg = alignedIndex & (SEGMENT_SIZE - 1);
        if (toSegmentIndex > 0) {
            this.segments.removeRange(0, toSegmentIndex);
            this.size -= ((toSegmentIndex << SEGMENT_SHIFT) - this.firstOffset);
        }
        Segment<T> firstSeg = this.getFirst();
        if (firstSeg != null) {
            this.size -= firstSeg.removeFromFirst(toIndexInSeg);
            this.firstOffset = firstSeg.offset;
            if (firstSeg.isEmpty()) {
                RecycleUtil.recycle(this.segments.pollFirst());
                this.firstOffset = 0;
            }
        } else {
            this.firstOffset = this.size = 0;
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:直接操纵ArrayList的Object[] elementData成员变量的原子引用器
     */
    private static final ReferenceFieldUpdater<ArrayList<?>, Object[]> LIST_ARRAY_GETTER = Updaters.newReferenceFieldUpdater(ArrayList.class, "elementData");


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:把一批日志添加到缓存中的方法
     */
    @SuppressWarnings("unchecked")
    public void addAll(final Collection<T> coll) {
        //得到包含了所有日志条目的数组
        Object[] src = coll2Array(coll);
        //这个局部变量代表的是src数组中有多少条日志存放到SegmentList的内部队列中了
        //其实就是src数组中的日志数据缓存到哪个位置了，该位置之前的日志数据都已经缓存到
        //SegmentList的内部队列中了
        int srcPos = 0;
        //得到要缓存的日志条目的数量
        int srcSize = coll.size();
        //得到segments队列中最后一个Segment对象，这里可以发现所有数据都是按顺序添加的
        Segment<T> lastSeg = getLast();
        //如果srcPos等于srcSize了。就意味着src数组中的所有日志条目都缓存到SegmentList的内部队列中了
        //就可以直接结束循环了
        while (srcPos < srcSize) {
            //如果从segments队列中得到的Segment对象为null
            //或者这个Segment对象的容量用完了
            if (lastSeg == null || lastSeg.isReachEnd()) {
                //就从Segment的对象池中获得一个新的Segment对象
                //recycleSegment为true，意味着使用对象池，所以直接从对象池中获得即可
                lastSeg = (Segment<T>) Segment.newInstance(this.recycleSegment);
                //然后把得到的这个Segment对象添加到队列中
                this.segments.add(lastSeg);
            }//这里得到的是刚刚得到的那个Segment对象可用的最大容量和要缓存到Segment对象中的
            //日志条目的数量的最小值，防止Segment对象内容溢出
            int len = Math.min(lastSeg.cap(), srcSize - srcPos);
            //根据刚才计算出的要存放到Segment对象中的日志条目的数量
            //把数组中的日志条目放到Segment对象中
            lastSeg.addAll(src, srcPos, len);
            //更新srcPos，表示数组中已经有srcPos对条日志存放到Segment对象中了
            srcPos += len;
            //更新size，这里的这个size代表的是一共有多少日志条目存放到SegmentList的队列中了
            this.size += len;
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:将集合中的数据直接赋值给一个数组的方法，最后将数组返回
     */
    private Object[] coll2Array(final Collection<T> coll) {
        //定义一个数组
        Object[] src;
        if (coll instanceof ArrayList && UnsafeUtil.hasUnsafe()) {
            //这里使用的是本类定义的可以直接操纵ArrayList的原子引用器
            //通过这个原子引用器可以直接得到ArrayList中的Object[] elementData这个成员变量
            //这个成员变量存放的就是集合中的数据
            //这里得到了ArrayList中的elementData，然后直接复制给刚才定义的数组
            src = LIST_ARRAY_GETTER.get((ArrayList<T>) coll);
        } else {//当前传进方法内的集合不是ArrayList，或者程序内部没有启用Unsafe功能
            //就直接把集合变成数组，然后赋值
            src = coll.toArray();
        }//返回数组
        return src;
    }

    @Override
    public String toString() {
        return "SegmentList [segments=" + this.segments + ", size=" + this.size + ", firstOffset=" + this.firstOffset
                + "]";
    }

}
