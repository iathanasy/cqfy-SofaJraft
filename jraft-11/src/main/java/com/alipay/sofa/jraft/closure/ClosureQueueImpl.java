package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.ThreadPoolsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/18
 * @方法描述：在第七版本中这个类发挥作用了，这个类就是用来存放用户在业务层定义的Closure对象的，在第七版本的测试类中，我定义的是ExpectClosure这个对象
 * 每一个日志条目都有一个对应的ExpectClosure对象，当领导者中的日志提交成功了，就会调用ExpectClosure对象中的回调方法通知业务层，这些ExpectClosure对象就存放在这个队列中
 */
public class ClosureQueueImpl implements ClosureQueue {

    private static final Logger LOG = LoggerFactory.getLogger(ClosureQueueImpl.class);

    private String groupId;
    private final Lock lock;
    private long firstIndex;
    private LinkedList<Closure> queue;


    public long getFirstIndex() {
        return firstIndex;
    }


    public LinkedList<Closure> getQueue() {
        return queue;
    }

    public ClosureQueueImpl() {
        super();
        this.lock = new ReentrantLock();
        this.firstIndex = 0;
        this.queue = new LinkedList<>();
    }

    public ClosureQueueImpl(final String groupId) {
        this();
        this.groupId = groupId;
    }

    @Override
    public void clear() {
        List<Closure> savedQueue;
        this.lock.lock();
        try {
            this.firstIndex = 0;
            savedQueue = this.queue;
            this.queue = new LinkedList<>();
        } finally {
            this.lock.unlock();
        }
        final Status status = new Status(RaftError.EPERM, "Leader stepped down");
        ThreadPoolsFactory.runInThread(this.groupId, () -> {
            for (final Closure done : savedQueue) {
                if (done != null) {
                    done.run(status);
                }
            }
        });
    }

    @Override
    public void resetFirstIndex(final long firstIndex) {
        this.lock.lock();
        try {
            Requires.requireTrue(this.queue.isEmpty(), "Queue is not empty.");
            this.firstIndex = firstIndex;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void appendPendingClosure(final Closure closure) {
        //先上锁，这个很重要，因为投票箱很可能会在状态机应用日志的时候，还一直向队列中添加要被回调的方法
        this.lock.lock();
        try {
            this.queue.add(closure);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public long popClosureUntil(final long endIndex, final List<Closure> closures) {
        return popClosureUntil(endIndex, closures, null);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：把队列中的封装回调方法的对象都添加到closures和taskClosures集合中，然后返回队列的起始索引，如果我们就是以领导者刚刚当选后内部产生的第一条索引为1的配置变更日志为例
     * 这时候queue中存放的元素只有1个，而firstIndex也为1
     */
    @Override
    public long popClosureUntil(final long endIndex, final List<Closure> closures, final List<TaskClosure> taskClosures) {
        //清空closures集合
        closures.clear();
        //清空taskClosures集合，这里清空这两个集合，是为了接下来将新的数据存放到这两个集合中
        if (taskClosures != null) {
            taskClosures.clear();
        }//上锁，这里为什么要上锁呢？说到底这个方法也是在disruptor框架的批处理其中被一个线程执行的，只会有一个线程执行当前的方法
        //在程序内部是不会有并发问题的呀，又看了看源码，突然意识到自己把BallotBox类的appendPendingTask方法给忘了，这个ClosureQueue队列本身就是被投票箱和
        //状态机组件同时持有的，只要业务层向领导者提交了日志，同时也提交了要被回调的方法，投票箱很可能会在状态机应用日志的时候，还一直向队列中添加要被回调的方法
        //所以上锁是很有必要的
        this.lock.lock();
        try {
            //得到队列中存放Closure对象的数量
            final int queueSize = this.queue.size();
            //判断队列是否为空，或者已提交的最新的日志的索引小于队列中的起始索引
            //别忘了跟随者节点还要把日志应用到状态机上呢，跟随者节点可没有什么回调队列，也没有什么通知业务层的回调方法
            //queueSize肯定就是空呀
            if (queueSize == 0 || endIndex < this.firstIndex) {
                return endIndex + 1;
            }
            //这里表示传入的最新的已提交的日志的索引比队列中存放的数据还大，这里就记录错误日志
            if (endIndex > this.firstIndex + queueSize - 1) {
                LOG.error("Invalid endIndex={}, firstIndex={}, closureQueueSize={}", endIndex, this.firstIndex,
                        queueSize);
                return -1;
            }
            //得到队列的初始索引，这个初始索引很重要，在外层方法中会跟据初始索引来决定调用队列中的哪一个回调方法
            final long outFirstIndex = this.firstIndex;
            //在循环中将队列中的Closure对象取出来
            for (long i = outFirstIndex; i <= endIndex; i++) {
                final Closure closure = this.queue.pollFirst();
                //如果Closure是TaskClosure类型的，那就把Closure对象放到taskClosures队列中
                //这个封装回调方法的对象是可以被用户定义的，这一点大家可以去看看源码，源码中也有使用TaskClosure作为封装回调方法的对象的例子
                //在我这里就不再展示了
                if (taskClosures != null && closure instanceof TaskClosure) {
                    taskClosures.add((TaskClosure) closure);
                }
                //把Closure存放到closures队列中
                closures.add(closure);
            }
            //更新队列的起始索引，这时候这个索引就会被更新为2
            this.firstIndex = endIndex + 1;
            //返回旧的起始索引，也就是1
            return outFirstIndex;
        } finally {
            this.lock.unlock();
        }
    }
}
