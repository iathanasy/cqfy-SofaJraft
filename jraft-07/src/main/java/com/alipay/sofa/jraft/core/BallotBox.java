package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.FSMCaller;
import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.closure.ClosureQueue;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.Ballot;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.BallotBoxOptions;
import com.alipay.sofa.jraft.util.Describer;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.SegmentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.locks.StampedLock;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/7
 * @Description:该类的对象就是一个投票箱，这个类在第四版本引入只是为了代码不报错，在这一版本还不进行讲解
 * 第七版本才会开始讲解这个类的内容
 */
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {


    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);

    //存放每一个日志条目对象的回调方法的队列
    private ClosureQueue closureQueue;
    //读写锁
    private final StampedLock stampedLock = new StampedLock();
    //最后被提交的日志的索引，初始化时为0
    private long lastCommittedIndex = 0;
    //要提交的下一条日志的索引
    private long pendingIndex;
    //这个队列存放的就是为每一个日志条目对象创建的Ballot对象
    private final SegmentList<Ballot> pendingMetaQueue = new SegmentList<>(false);
    //状态机组件
    private FSMCaller waiter;


    long getPendingIndex() {
        return this.pendingIndex;
    }


    SegmentList<Ballot> getPendingMetaQueue() {
        return this.pendingMetaQueue;
    }

    //得到最后一条被提交的日志索引
    public long getLastCommittedIndex() {
        long stamp = this.stampedLock.tryOptimisticRead();
        final long optimisticVal = this.lastCommittedIndex;
        if (this.stampedLock.validate(stamp)) {
            return optimisticVal;
        }
        stamp = this.stampedLock.readLock();
        try {
            return this.lastCommittedIndex;
        } finally {
            this.stampedLock.unlockRead(stamp);
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/17
     * @方法描述：在该方法内部，就把状态机要使用的回调队列交给投票箱了，其实可以这么理解，状态机组件要使用这个回调队列，但是并不执行向回调队列中存放回调方法的操作
     * 这个操作交给投票箱完成，然后队列中就有封装了回调方法的数据了，这样状态机在应用日志的时候，就可以回调这个队列中的方法，通知业务层执行结果了
     */
    @Override
    public boolean init(final BallotBoxOptions opts) {
        if (opts.getWaiter() == null || opts.getClosureQueue() == null) {
            LOG.error("waiter or closure queue is null.");
            return false;
        }
        this.waiter = opts.getWaiter();
        this.closureQueue = opts.getClosureQueue();
        return true;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/17
     * @方法描述：该类最核心的方法，提交firstLogIndex和lastLogIndex之间的日志，当然，在提交的时候还要判断这些日志是否收到了集群中过半节点的投票，该方法会被领导者调用
     */
    public boolean commitAt(final long firstLogIndex, final long lastLogIndex, final PeerId peer) {
        //这里肯定是需要上锁的，因为领导者的投票箱对象只有一个，但是这一个投票箱对象要被所有的复制器对象使用
        //而每一个复制器对象都有一个自己的单线程执行器来处理任务，也就会调用到该方法，显然是并发的情况，所以应该上锁
        final long stamp = this.stampedLock.writeLock();
        //定义一个局部变量，记录最后提交的日志的索引
        long lastCommittedIndex = 0;
        try {
            //要提交的下一个索引为0则直接退出该方法
            if (this.pendingIndex == 0) {
                return false;
            }
            //这里就要分析一下了，什么时候会发生下面这种情况。首先请大家思考这样一种场景，那就是领导者领先跟随者很多，需要向跟随者发送以前的日志进度，比如说领导者的日志为1-100
            //一个重新上线的跟随者的日志为1-40，领导者先把41-70这批日志发送给跟随着了，但这些日志已经被领导者应用了，领导者的pendingIndex已经更新到101了，所以领导者收到跟随者的响应后
            //会发现lastLogIndex < this.pendingIndex，这种情况下，领导者就不必再次应用，直接返回即可
            //当然，也可能是另一种情况，领导者向跟随者发送了两批日志，分别是1-10，11-20，发送了这两批日志，因为领导者向跟随者发送日志使用的是Pipeline模式，并且每个跟随者节点处理日志然后
            //给领导者发送响应可能并不是同步的，这也就意味着复制器对象在接收到跟随者节点发送过来的响应也可能不是同步的，如果集群中过半节点已经把第一批日志的响应发送过来了
            //领导者把pendingIndex更新到了11，在处理第二批日志的响应时，集群中某个节点才把第一批日志的响应才发送回来，这时候也会发生lastLogIndex < this.pendingIndex的情况，直接退出该方法就行，否则日志重复提交，状态机命令就会重复执行了
            if (lastLogIndex < this.pendingIndex) {
                return true;
            }
            //这里就是要给兜底判断，如果发现要处理的这批日志的最后一条索引比pendingMetaQueue队列的数据还多
            //这时候就发生了数据越界异常，直接抛出异常即可，原因很简单，领导者的每一条日志都会对应一个投票计数器，投票计数器存放到pendingMetaQueue队列中
            //现在要处理的日志索引比队列中的投票计数器还多，这显然是不正常的
            if (lastLogIndex >= this.pendingIndex + this.pendingMetaQueue.size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            //这里情况就复杂很多了，需要仔细分析两种情况
            //这里先说firstLogIndex大于pendingIndex的情况，这种情况很可能是，领导者向跟随者发送日志的时候，一下子发送了好几批
            //但是第一批日志还没有被应用呢，第二批日志有些已经有收到回应了，这时候肯定是大于接下来要提交的索引
            //所以，判断一下，然后给得到的这个startAt投票就行，这里大家一定要注意，这里只是投票，并不是真的提交
            //因为日志复制是采用Pipeline模式，前面的没有处理完，是不会先提交后面的日志的
            //每一个复制器对象收到响应不可能是完全同步的，有可能有些复制器对象已经完全处理完了自己的日志复制响应，其他复制器对象还没收到几个响应呢
            //接下来就是pendingIndex大于firstLogIndex的情况了
            //这里可能就是新的领导者把旧日志同步给某个进度落后的节点时，可能会出现的情况。比如领导者现在有1-4索引的日志，这时候有一个节点故障了，在节点故障期间，领导者内部有产生了4条新日志
            //把这四条新日志同步给其他跟随者节点，然后领导者成功提交了日志，这时候pendingIndex就变成了9，然后故障的节点恢复正常了，这时候领导者又产生了两条新日志，现在一共有10条日志了
            //领导者就把5-10都发送给这个节点了，在收到刚刚故障恢复的节点的投票响应时，肯定是从日志索引9开始继续处理呀，否则不就重复执行命令了吗
            final long startAt = Math.max(this.pendingIndex, firstLogIndex);
            //下面的逻辑就简单多了接下来就是给对应的日志索引投票的逻辑
            Ballot.PosHint hint = new Ballot.PosHint();
            //用上面得到的startAt开始便利要投票的日志
            for (long logIndex = startAt; logIndex <= lastLogIndex; logIndex++) {
                //根据日志索引得到对应的投票计数器
                final Ballot bl = this.pendingMetaQueue.get((int) (logIndex - this.pendingIndex));
                //进行投票操作
                hint = bl.grant(peer, hint);
                //判断该索引对应的日志是否收到了集群过半的投票
                if (bl.isGranted()) {
                    //如果收到了过半投票，就意味着日志同步成功了，这时候给最后提交的日志索引赋值
                    lastCommittedIndex = logIndex;
                    //这行代码是我自己添加的
                    System.out.println("领导者发送的日志收到过半投票，可以把日志应用到状态机了！！！");
                }
            }
            //如果没有提交日志，就直接退出该方法，没有提交日志意味着收到的投票还不够
            //继续等待跟随者节点回复投票响应即可
            if (lastCommittedIndex == 0) {
                return true;
            }
            //如果日志提交成功了，那就从pendingMetaQueue队列中把对应的投票计数器全部都移除
            this.pendingMetaQueue.removeFromFirst((int) (lastCommittedIndex - this.pendingIndex) + 1);
            //记录日志的提交范围
            LOG.debug("Committed log fromIndex={}, toIndex={}.", this.pendingIndex, lastCommittedIndex);
            //更新pendingIndex
            this.pendingIndex = lastCommittedIndex + 1;
            //更新lastCommittedIndex
            this.lastCommittedIndex = lastCommittedIndex;
        } finally {
            //解锁
            this.stampedLock.unlockWrite(stamp);
        }
        //终于要调用状态机组件了，这里就把已经提交的最后一条日志告诉状态机组件，然后让状态机开始应用这些被提交的日志
        this.waiter.onCommitted(lastCommittedIndex);
        return true;
    }


    public void clearPendingTasks() {
        final long stamp = this.stampedLock.writeLock();
        try {
            this.pendingMetaQueue.clear();
            this.pendingIndex = 0;
            this.closureQueue.clear();
        } finally {
            this.stampedLock.unlockWrite(stamp);
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/18
     * @方法描述：设置领导者要提交的下一条日志的索引，同时也设置了closureQueue队列中的初始索引，大家不妨就自己假设一个例子，领导者刚刚当选的时候会产生一条索引为1的配置变更日志
     * 用这个日志为例，看看日志的提交以及回调方法的执行究竟是怎么执行的，详细的执行流程我也会在视频中讲解
     */
    public boolean resetPendingIndex(final long newPendingIndex) {
        final long stamp = this.stampedLock.writeLock();
        try {
            if (!(this.pendingIndex == 0 && this.pendingMetaQueue.isEmpty())) {
                LOG.error("resetPendingIndex fail, pendingIndex={}, pendingMetaQueueSize={}.", this.pendingIndex,
                        this.pendingMetaQueue.size());
                return false;
            }
            if (newPendingIndex <= this.lastCommittedIndex) {
                LOG.error("resetPendingIndex fail, newPendingIndex={}, lastCommittedIndex={}.", newPendingIndex,
                        this.lastCommittedIndex);
                return false;
            }
            this.pendingIndex = newPendingIndex;
            this.closureQueue.resetFirstIndex(newPendingIndex);
            return true;
        } finally {
            this.stampedLock.unlockWrite(stamp);
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/17
     * @方法描述：该方法的作用很重要，但是逻辑很简单，就是给每一个日志条目创建一个Ballot对象，Ballot对象是投票计数器，同时把每一个日志条目的回调方法存放到closureQueue队列中
     * 存放到这个队列中，就意味着状态机组件可以使用这个队列中的数据了
     */
    public boolean appendPendingTask(final Configuration conf, final Configuration oldConf, final Closure done) {
        //为日志条目创建投票技术器对象
        final Ballot bl = new Ballot();
        //初始化投票计数器，因为要计数，判断收到的票数是否超过集群过半节点
        //所以肯定需要知道集群中有多少节点
        if (!bl.init(conf, oldConf)) {
            LOG.error("Fail to init ballot.");
            return false;
        }
        final long stamp = this.stampedLock.writeLock();
        try {//要处理的索引小于0则记录错误日志
            if (this.pendingIndex <= 0) {
                LOG.error("Fail to appendingTask, pendingIndex={}.", this.pendingIndex);
                return false;
            }//把创建的投票计数器存放到pendingMetaQueue队列中
            this.pendingMetaQueue.add(bl);
            //把用户为业务层的每一个操作定义的回调方法存放到closureQueue队列中
            //注意，这个回调方法并不是在日志提交成功后执行状态机操作的，仅仅是用来通知业务层操作的执行结果的
            this.closureQueue.appendPendingClosure(done);
            return true;
        } finally {
            this.stampedLock.unlockWrite(stamp);
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/17
     * @方法描述：该方法会在跟随者节点中被调用，在接收到领导者发送过来的要提交的日志的索引时，跟随者节点会调用这个方法，将对应的日志提交
     */
    public boolean setLastCommittedIndex(final long lastCommittedIndex) {
        boolean doUnlock = true;
        final long stamp = this.stampedLock.writeLock();
        try {//这个是跟随者节点调用的方法，pendingIndex对跟随者节点是无效的，只有领导者才能维护pendingIndex
            //所以在跟随着节点中，如果判断出pendingIndex不为0了，说明当前节点已经变成了领导者
            if (this.pendingIndex != 0 || !this.pendingMetaQueue.isEmpty()) {
                Requires.requireTrue(lastCommittedIndex < this.pendingIndex,
                        "Node changes to leader, pendingIndex=%d, param lastCommittedIndex=%d", this.pendingIndex,
                        lastCommittedIndex);
                return false;
            }
            if (lastCommittedIndex < this.lastCommittedIndex) {
                return false;
            }
            if (lastCommittedIndex > this.lastCommittedIndex) {
                this.lastCommittedIndex = lastCommittedIndex;
                this.stampedLock.unlockWrite(stamp);
                doUnlock = false;
                //在这里根据日志索引应用日志
                this.waiter.onCommitted(lastCommittedIndex);
            }
        } finally {
            if (doUnlock) {
                this.stampedLock.unlockWrite(stamp);
            }
        }
        return true;
    }


    @Override
    public void shutdown() {
        clearPendingTasks();
    }

    @Override
    public void describe(final Printer out) {
        long _lastCommittedIndex;
        long _pendingIndex;
        long _pendingMetaQueueSize;
        long stamp = this.stampedLock.tryOptimisticRead();
        if (this.stampedLock.validate(stamp)) {
            _lastCommittedIndex = this.lastCommittedIndex;
            _pendingIndex = this.pendingIndex;
            _pendingMetaQueueSize = this.pendingMetaQueue.size();
        } else {
            stamp = this.stampedLock.readLock();
            try {
                _lastCommittedIndex = this.lastCommittedIndex;
                _pendingIndex = this.pendingIndex;
                _pendingMetaQueueSize = this.pendingMetaQueue.size();
            } finally {
                this.stampedLock.unlockRead(stamp);
            }
        }
        out.print("  lastCommittedIndex: ")
                .println(_lastCommittedIndex);
        out.print("  pendingIndex: ")
                .println(_pendingIndex);
        out.print("  pendingMetaQueueSize: ")
                .println(_pendingMetaQueueSize);
    }
}
