package com.alipay.sofa.jraft.storage.impl;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.conf.ConfigurationEntry;
import com.alipay.sofa.jraft.conf.ConfigurationManager;
import com.alipay.sofa.jraft.core.NodeMetrics;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.LogId;
import com.alipay.sofa.jraft.error.LogEntryCorruptedException;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.LogManagerOptions;
import com.alipay.sofa.jraft.option.LogStorageOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.LogManager;
import com.alipay.sofa.jraft.storage.LogStorage;
import com.alipay.sofa.jraft.util.*;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/30
 * @Description:日志管理组件，这个类的对象是用来管理日志的，提供了根据索引得到日志，得到日志的任期
 * 把日志存放到数据库中等等方法，日志存储系统其实分三个层次，一个是日志快照，一个是内存，最后就是硬盘
 * 第三个版本会实现完整的日志落盘功能，第二版本只实现了日志组件初始化
 */
public class LogManagerImpl implements LogManager {

    private static final Logger LOG = LoggerFactory.getLogger(LogManagerImpl.class);
    //集群ID
    private String groupId;
    //日志存储器，这个类虽然持有日志管理器，但是并不能意味着
    //这个类就是对日志管理器做了层代理
    //实际上，这个类做了很多其他的工作，为了提高日志落盘的性能等等
    private LogStorage logStorage;
    private ConfigurationManager configManager;
    //读写锁
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = this.lock.writeLock();
    private final Lock readLock = this.lock.readLock();
    //日志管理器是否停止工作
    private volatile boolean stopped;
    private volatile boolean hasError;
    //这里指的是存储到硬盘上的最后一条日志的ID
    private LogId diskId = new LogId(0, 0);
    //最新的被应用到状态机的日志ID
    private LogId appliedId = new LogId(0, 0);
    //第一条日志的索引，默认从1开始
    private volatile long firstLogIndex;
    //最后一条日志的索引，这两个成员变量在checkConsistency方法中会用到
    //也就是检查日志索引一致性的时候，lastLogIndex默认为0
    private volatile long lastLogIndex;
    private RaftOptions raftOptions;
    private NodeMetrics nodeMetrics;
    //存放等待日志的复制器的Map
    private final Map<Long, WaitMeta> waitMap = new HashMap<>();
    //为等待的复制器分配key的成员变量
    private long nextWaitId = 1;

    //下面这个成员变量是用来在内存中缓存条目日志对象的，所有的日志条目对象都会先缓存到内存里
    //然后就直接开始向跟随者传输日志了
    private final SegmentList<LogEntry> logsInMemory = new SegmentList<>(true);
    //又一个Disruptor框架，从NodeImpl的executeApplyingTasks方法中传递过来的批量日志
    //最终会被这个Disruptor异步落盘，但是这个disruptor并不只是处理日志落盘事件，还处理其他的一些事件
    //这些事件后面会慢慢重构完整
    private Disruptor<StableClosureEvent> disruptor;
    //环形队列
    private RingBuffer<StableClosureEvent> diskQueue;


    private enum EventType {
        //其他事件，日志落盘对应的就是这个事件
        OTHER,
        RESET,
        TRUNCATE_PREFIX,
        TRUNCATE_SUFFIX,
        SHUTDOWN,
        //得到最后一条日志ID事件
        LAST_LOG_ID
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:存放到环形数组中中的对象
     */
    private static class StableClosureEvent {
        StableClosure done;
        EventType type;

        void reset() {
            this.done = null;
            this.type = null;
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:disruptor要使用的事件工厂
     */
    private static class StableClosureEventFactory implements EventFactory<StableClosureEvent> {

        @Override
        public StableClosureEvent newInstance() {
            return new StableClosureEvent();
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/1
     * @Description:初始化方法，这个方法的逻辑比较简单，所以注释也添加的简单一些
     */
    @Override
    public boolean init(final LogManagerOptions opts) {
        this.writeLock.lock();
        try {
            if (opts.getLogStorage() == null) {
                LOG.error("Fail to init log manager, log storage is null");
                return false;
            }
            this.groupId = opts.getGroupId();
            this.raftOptions = opts.getRaftOptions();
            this.nodeMetrics = opts.getNodeMetrics();
            this.logStorage = opts.getLogStorage();
            this.configManager = opts.getConfigurationManager();
            //在这里创建一个封装日志存储器需要的参数对象
            LogStorageOptions lsOpts = new LogStorageOptions();
            lsOpts.setGroupId(opts.getGroupId());
            //设置配置管理器
            lsOpts.setConfigurationManager(this.configManager);
            //设置编解码工厂，这个编解码工厂会交给日志管理器使用，当日志管理器将日志落盘
            //或者是从数据库读取日志时，就会用到这个编解码工厂提供的编解码器进行编码和解码
            lsOpts.setLogEntryCodecFactory(opts.getLogEntryCodecFactory());
            //初始化日志存储器
            if (!this.logStorage.init(lsOpts)) {
                //初始化失败则报错
                LOG.error("Fail to init logStorage");
                return false;
            }//下面就是简单的赋值操作了
            this.firstLogIndex = this.logStorage.getFirstLogIndex();
            this.lastLogIndex = this.logStorage.getLastLogIndex();
            //从硬盘中得到最后一条日志的ID，getTermFromLogStorage方法是根据日志的索引得到相对应的任期
            this.diskId = new LogId(this.lastLogIndex, getTermFromLogStorage(this.lastLogIndex));
            //创建disruptor
            this.disruptor = DisruptorBuilder.<StableClosureEvent> newInstance()
                    .setEventFactory(new StableClosureEventFactory())
                    .setRingBufferSize(opts.getDisruptorBufferSize())
                    .setThreadFactory(new NamedThreadFactory("JRaft-LogManager-Disruptor-", true))
                    .setProducerType(ProducerType.MULTI)
                     //设置阻塞策略，这里使用的是超时阻塞，超过10秒之后，就会抛异常
                    //这个异常就会被下面设置的异常处理器捕捉，然后调用异常处理器中的reportError方法报告异常
                    .setWaitStrategy(new TimeoutBlockingWaitStrategy(
                            this.raftOptions.getDisruptorPublishEventWaitTimeoutSecs(), TimeUnit.SECONDS))
                    .build();
            //设置消费者处理器
            this.disruptor.handleEventsWith(new StableClosureEventHandler());
            //设置异常处理器
            this.disruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(this.getClass().getSimpleName(),
                    (event, ex) -> reportError(-1, "LogManager handle event error")));
            this.diskQueue = this.disruptor.start();
        } finally {
            this.writeLock.unlock();
        }
        return true;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:disruptor框架中要使用的消费者处理器
     */
    private class StableClosureEventHandler implements EventHandler<StableClosureEvent> {
        //这里还有几个成员变量呢，一定要看一下，第三个成员变量的作用非常重要，相当于日志落盘前的缓冲区
        //这个是存储到硬盘上的最后一条日志的ID，日志落盘后会返回最后一条ID，会给这个成员变量重新赋值
        LogId lastId = LogManagerImpl.this.diskId;
        //专门存放回调方法的集合，集合中存放的每一个对象都封装了回调方法
        List<StableClosure> storage = new ArrayList<>(256);
        //这个就是日志缓冲区对象了
        AppendBatcher ab = new AppendBatcher(this.storage, 256, new ArrayList<>(), LogManagerImpl.this.diskId);
        //在这个方法中，执行了日志落盘的操作
        @Override
        public void onEvent(final StableClosureEvent event, final long sequence, final boolean endOfBatch) throws Exception {
            //获取封装了日志集合的对象，这个对象中还封装了日志落盘后要回调的方法
            final StableClosure done = event.done;
            //获取事件类型
            final EventType eventType = event.type;
            //重置事件对象，以便在环形数组中重复利用
            event.reset();
            //获取StableClosure对象中的日志集合
            if (done.getEntries() != null && !done.getEntries().isEmpty()) {
                //如果日志集合不为空，直接把日志刷新到缓冲区，等到缓冲区满了之后
                //就清空缓冲区，也就是把缓冲区中的日志全部落盘
                this.ab.append(done);
            } else {
                //走到这里意味着根本就没有日志集合，我在这个类的disruptor成员变量上也添加注释了
                //简单讲了一下这个disruptor成员变量并不只是处理日志落盘事件，还处理其他的一些事件
                //这些事件后面会慢慢重构完整，当然，在该类的getLastLogIndex方法中就有其他类型的事件，大家可以简单看看
                //这里就是不管缓冲区中有没有日志，总之先刷新一下，返回硬盘上的最后一条日志的ID
                this.lastId = this.ab.flush();
                //定义操作是否成功的标志
                //这里定义为true就不会变了，因为下面根本没有对这个局部变量进行操作
                //但是下个版本就有了，注意啊，走到这个else分支中，即意味着当前处理的事件和日志落盘无关了，不要再想着日志落盘的事了
                boolean ret = true;
                //根据上面得到的事件类型，处理具体事件
                switch (eventType) {
                    //如果是获取最后一条日志ID的事件，那就直接把刚才得到的最后一条日志ID设置到LastLogIdClosure对象中
                    case LAST_LOG_ID:
                        ((LastLogIdClosure) done).setLastLogId(this.lastId.copy());
                        break;
                    case TRUNCATE_SUFFIX:
                        //这里就是真正将日志从本地日志组件中移除的操作
                        //先记录一下操作开始的事件
                        long startMs = Utils.monotonicMs();
                        try {
                            final TruncateSuffixClosure tsc = (TruncateSuffixClosure) done;
                            LOG.warn("Truncating storage to lastIndexKept={}.", tsc.lastIndexKept);
                            //在这里从数据库组件中移除对应索引的日志
                            ret = LogManagerImpl.this.logStorage.truncateSuffix(tsc.lastIndexKept);
                            if (ret) {
                                //操作成功了记录硬盘上的最后一条日志的索引
                                this.lastId.setIndex(tsc.lastIndexKept);
                                //记录任期
                                this.lastId.setTerm(tsc.lastTermKept);
                                //校验合法性
                                Requires.requireTrue(this.lastId.getIndex() == 0 || this.lastId.getTerm() != 0);
                            }
                        } finally {//在这里记录移除日志的操作耗时
                            LogManagerImpl.this.nodeMetrics.recordLatency("truncate-log-suffix", Utils.monotonicMs()
                                    - startMs);
                        }
                        break;



                    default:
                        break;
                }
                if (!ret) {
                    //操作失败就直接报错
                    reportError(RaftError.EIO.getNumber(), "Failed operation in LogStorage");
                } else {//走到这里意味着操作成功了
                    //如果是上面的哪个LAST_LOG_ID事件，这里回调run方法会直接执行LastLogIdClosure内部的
                    //latch.countDown()方法，让执行getLastLogIndex方法的线程可以结束阻塞，继续向下执行
                    done.run(Status.OK());
                }
            }//判断当前消费的数据是不是最后一个可消费的数据
            if (endOfBatch) {
                //这里的逻辑也很简答，逻辑的具体实现是在disruptor框架中的
                //disruptor框架的批处理器在每次消费数据时，都会判断当前正消费的数据是不是所有可消费中数据中的最后一个
                //如果说环形队列中只有12个数据，当前正在消费第12个，那就意味着正在消费最后一个可消费的数据
                //这时候就直接刷新一下缓冲区，把缓冲区中的数据全刷新到硬盘上
                //这个是disruptor框架给用户暴露的一个扩展点，因为用户也不知道是不是真的没有数据可消费了，如果真的没有数据可消费了
                //在批处理器进入下一次循环的时候就会让线程阻塞了，所以在线程阻塞前赶快把日志落盘
                //当然，就算这里判断是最后一个可消费的数据，也不意味着在disruptor框架中，批处理器进入下一次循环的时候
                //会直接阻塞，这要看看生产者有没有继续向环形数组中发布数据，具体逻辑大家可以去disruptor框架中查看
                this.lastId = this.ab.flush();
                setDiskId(this.lastId);
            }
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:这个就是获得最后一条日志ID事件要使用的类，该类的对象封装了对应事件的回调方法
     */
    private static class LastLogIdClosure extends StableClosure {

        public LastLogIdClosure() {
            super(null);
        }

        private LogId lastLogId;

        void setLastLogId(final LogId logId) {
            Requires.requireTrue(logId.getIndex() == 0 || logId.getTerm() != 0);
            this.lastLogId = logId;
        }

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void run(final Status status) {
            this.latch.countDown();
        }

        void await() throws InterruptedException {
            this.latch.await();
        }

    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:该类的对象就是日志缓冲区
     */
    private class AppendBatcher {
        //存放StableClosure对象的集合，StableClosure对象中有回调方法，当一批日志落盘成功
        //就会回调StableClosure对象中的回调方法
        //这里是用一个集合来存放StableClosure对象的，说明会有多个对象存放到这里
        //现在，我想为大家简单梳理一下，也许有的朋友在这里已经晕了，从最外层的NodeImpl类的executeApplyingTasks
        //方法我们可以知道，日志其实是一批一批落盘的，以32个日志条目为一批，在executeApplyingTasks方法中
        //会把要落盘的一批日志封装到LeaderStableClosure对象中，交给日志管理器对象来进行落盘的操作，当日志落盘成功后
        //就会回调LeaderStableClosure对象中的回调方法，这个回调方法本来是要判断日志是否可以提交
        //然后进一步应用到状态机上了，但是在第三版本我还没有为大家引入状态机，只是打印了一句日志落盘成功的话
        //所以日志落盘成功后，大家就可以在控制台看到这句话了，当然，在日志管理器对象的appendEntries方法中
        //这个LeaderStableClosure对象又会交给StableClosureEvent对象，在StableClosureEvent对象被
        //disruptor处理的时候，又会从StableClosureEvent对象中重新获得LeaderStableClosure对象
        //在把日志放到日志缓冲区的时候，也会一起把每一批日志对应的这个封装了回调方法的LeaderStableClosure对象
        //交给日志缓冲区中的storage集合存放，等到每一批日志落盘成功，直接去下面这个集合中获得
        //对应的回调对象，回调器中的方法，就可以在控制台打印日志落盘成功了，当然这仅仅是第三本版本实现的功能，后面的版本就会重构成真正的回调逻辑了
        //这个逻辑大家还是要再仔细品味品味，不要被这些回调对象搞混了
        List<StableClosure> storage;
        //日志缓冲区的容量，默认为256
        int cap;
        //日志缓冲区的大小
        int size;
        //日志缓冲区中可存放的Buffer的大小
        //默认为256 * 1024
        int bufferSize;
        //日志缓冲区缓存日志条目的集合
        List<LogEntry> toAppend;
        //当前的最后一条日志ID
        LogId lastId;

        //构造方法
        public AppendBatcher(final List<StableClosure> storage, final int cap, final List<LogEntry> toAppend,
                             final LogId lastId) {
            super();
            this.storage = storage;
            this.cap = cap;
            this.toAppend = toAppend;
            this.lastId = lastId;
        }

        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:将日志暂存到缓冲区的方法
         */
        void append(final StableClosure done) {
            //先判断日主缓冲区中的容量是否达到最大了，判断缓冲区中可存放大最大Buffer达到最大限制了
            if (this.size == this.cap || this.bufferSize >= LogManagerImpl.this.raftOptions.getMaxAppendBufferSize()) {
                //如果达到最大限制就刷盘，这里也可以意识到，如果上一次添加日志到缓冲区时，添加完毕后缓冲区内容益处了，并不会立刻刷新
                //而是等到下一批日志到来的时候再刷新。当然，也可能是在StableClosureEventHandler处理器的onEvent方法中就直接刷新了
                //大家可以对刷新日志的情况做一下分析，具体分析我会为大家写在文章中
                flush();
            }//把封装回调方法的对象添加到集合中
            this.storage.add(done);
            //容量加一，这里可以看到，这个size针对的是可添加的回调对象的数量
            //因为一个回调对象StableClosure对应了一批日志，所以这里实际限制的是可以存放多少批日志在缓冲区中
            this.size++;
            //把这批日志放到缓冲区中
            this.toAppend.addAll(done.getEntries());
            //将这批日志中每个日志条目内容相加，给bufferSize赋值
            for (final LogEntry entry : done.getEntries()) {
                this.bufferSize += entry.getData() != null ? entry.getData().remaining() : 0;
            }
        }



        /**
         * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
         * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
         * @Date:2023/12/4
         * @Description:刷新日志到硬盘的方法
         */
        LogId flush() {
            //判断是否有数据
            if (this.size > 0) {
                //将日志落盘的真正方法
                this.lastId = appendToStorage(this.toAppend);
                //这里开始遍历storage集合，得到每一个封装了回调方法的对象
                for (int i = 0; i < this.size; i++) {
                    this.storage.get(i).getEntries().clear();
                    Status st = null;
                    try {
                        if (LogManagerImpl.this.hasError) {
                            //得到结果状态码
                            st = new Status(RaftError.EIO, "Corrupted LogStorage");
                        } else {
                            st = Status.OK();
                        }//在这里回调了StableClosure对象中的方法
                        this.storage.get(i).run(st);
                    } catch (Throwable t) {
                        LOG.error("Fail to run closure with status: {}.", st, t);
                    }
                }//下面就是重置缓冲区的一些操作
                this.toAppend.clear();
                this.storage.clear();
            }
            this.size = 0;
            this.bufferSize = 0;
            return this.lastId;
        }
    }




    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:将日志刷新到硬盘的方法
     */
    private LogId appendToStorage(final List<LogEntry> toAppend) {
        LogId lastId = null;
        if (!this.hasError) {
            //得到操作开始时间
            final long startMs = Utils.monotonicMs();
            //得到要刷新的日志条目数量
            final int entriesCount = toAppend.size();
            //向检测组件记录数据，这里其实不用关心和这个nodeMetrics有关的操作
            //我忘了删掉这方面的代码了
            this.nodeMetrics.recordSize("append-logs-count", entriesCount);
            try {
                int writtenSize = 0;
                for (int i = 0; i < entriesCount; i++) {
                    //得到每一个日志条目
                    final LogEntry entry = toAppend.get(i);
                    //记录日志条目内容大小
                    writtenSize += entry.getData() != null ? entry.getData().remaining() : 0;
                }
                this.nodeMetrics.recordSize("append-logs-bytes", writtenSize);
                //在这里把日志落盘了，使用的日志存储器落盘的，返回的是落盘到数据库的日志条目的数量
                final int nAppent = this.logStorage.appendEntries(toAppend);
                if (nAppent != entriesCount) {
                    LOG.error("**Critical error**, fail to appendEntries, nAppent={}, toAppend={}", nAppent,
                            toAppend.size());
                    reportError(RaftError.EIO.getNumber(), "Fail to append log entries");
                }
                if (nAppent > 0) {
                    //这里得到了最后一条日志ID
                    lastId = toAppend.get(nAppent - 1).getId();
                }
                toAppend.clear();
            } finally {//记录操作结束时间
                this.nodeMetrics.recordLatency("append-logs", Utils.monotonicMs() - startMs);
            }
        }//返回最后一条日志ID
        return lastId;
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:将一批日志持久化的方法
     */
    @Override
    public void appendEntries(final List<LogEntry> entries, final StableClosure done) {
        assert(done != null);
        Requires.requireNonNull(done, "done");
        if (this.hasError) {
            //走到这里意味着日志管理组件出问题了
            //清空日志集合
            entries.clear();
            //这里出现错误，就直接回调done中的方法，这时候大家要注意，这里的回调方法已经不是一开始Task中的了
            //而是在NodeImpl类的executeApplyingTasks方法中重新创建的LeaderStableClosure对象中的回调方法了
            //这个回调方法的作用是在日志落盘成功后回调，判断日志是否可以被提交应用到状态机中了
            //这里如果程序内部出错，显然就不能应用到状态机中，所以回调一下，设置错误的状态吗
            ThreadPoolsFactory.runClosureInThread(this.groupId, done, new Status(RaftError.EIO, "Corrupted LogStorage"));
            return;
        }//设置是否要释放锁的标志
        boolean doUnlock = true;
        //再次获得写锁，因为写锁在NodeImpl的外层方法的finally中释放了
        this.writeLock.lock();
        try {
            //走到这里会有一个判断，因为当前方法不管是领导者还是跟随者都适用，当跟随者接收到来自领导者的日志后
            //也会调用日志管理组件将日志落盘，所以这里会判断一下，如果这一批日志不为空，并且所有日志的索引都为0
            //那就意味着这批日志是业务日志，并且是领导者处理的。如果是跟随者接收到的日志，索引肯定已经被领导者填充了
            //所以索引肯定是有值的，具体的判断方法就是checkAndResolveConflict方法
            if (!entries.isEmpty() && !checkAndResolveConflict(entries, done, this.writeLock)) {
                //如果checkAndResolveConflict方法返回false，就直接退出当前方法
                entries.clear();
                return;
            }
            for (int i = 0; i < entries.size(); i++) {
                final LogEntry entry = entries.get(i);
                //计算日志校验和
                if (this.raftOptions.isEnableLogEntryChecksum()) {
                    entry.setChecksum(entry.checksum());
                }
                //如果日志类型为配置变更类型，就把日志向配置管理器中存放一份，也就相当于把配置变更的日志存放到内存中了
                //这里大家应该也能意识到，配置变更日志和业务日志的索引并没有分开设置，而是共用一套索引体系
                if (entry.getType() == EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                    //创建一个配置类对象
                    Configuration oldConf = new Configuration();
                    //得到旧配置
                    if (entry.getOldPeers() != null) {
                        oldConf = new Configuration(entry.getOldPeers(), entry.getOldLearners());
                    }//把配置日志的信息封装到ConfigurationEntry对象中
                    final ConfigurationEntry conf = new ConfigurationEntry(entry.getId(),
                            new Configuration(entry.getPeers(), entry.getLearners()), oldConf);
                    //把ConfigurationEntry对象添加到配置管理器中，相当于把配置变更日志存放到内存中了
                    this.configManager.add(conf);
                }
            }
            if (!entries.isEmpty()) {
                //走到这里就意味着是业务日志，注意这里是一批日志，这一批日志共有32条，现在要把这一批日志的第一条日志
                //索引设置到StableClosure对象中，这个对象中的方法会在日志落盘成功后被回调，到时候设置的这个属性就会发挥作用
                //具体逻辑等第5或第6版本，我就会为大家实现了
                done.setFirstLogIndex(entries.get(0).getId().getIndex());
                //日志在正式落盘之前，会把日志向内存中存放一份，这样访问起来更快更方便
                //当日志成功提交后，内存中的日志就会被清除掉了
                this.logsInMemory.addAll(entries);
            }//把这批日志交给StableClosure对象，这个对象被封装到当前类中的Disruptor队列的生产者数据中
            //被Disruptor的事件处理器获取，并且将设置的这批日志落盘
            //具体逻辑就在下面
            done.setEntries(entries);
            doUnlock = false;
            //这个逻辑是用来唤醒复制器的，复制器会一直将日志向跟随者传输，当领导者中没有
            //日志的时候，复制器就会停止发送日志，并且会注册一个回调函数监听领导者是不是有日志了，当有日志的时候就会通知复制器
            //复制器就会直接从刚才存放了日志的缓存组件logsInMemory中，把日志直接传输给跟随者
            wakeupAllWaiter(this.writeLock);
            //发布生产者数据，Disruptor可以将日志异步落盘了
            //这里大家也可已看到，日志落盘整个系列操作几乎就是用Disruptor衔接起来的，关键步骤都是异步和回调
            this.diskQueue.publishEvent((event, sequence) -> {
                event.reset();
                //这里设置了事件类型，具体逻辑请看StableClosureEventHandler类的onEvent方法
                event.type = EventType.OTHER;
                event.done = done;
            });
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/12
     * @Description:该方法的作用就是唤醒所有等待日志到来的复制器对象
     */
    private boolean wakeupAllWaiter(final Lock lock) {
        //判断waitMap非空，因为复制器提交的回调方法都封装在这个Map中，只要这个Map中有数据
        //就意味着有复制器正在等待日志到来
        if (this.waitMap.isEmpty()) {
            lock.unlock();
            return false;
        }//创建一个集合，专门用来存放WaitMeta对象，WaitMeta对象中封装着复制器提交的回调方法
        //并且把所有对象放到集合中了
        final List<WaitMeta> wms = new ArrayList<>(this.waitMap.values());
        //判断当前日志组件是否已经停止工作
        final int errCode = this.stopped ? RaftError.ESTOP.getNumber() : RaftError.SUCCESS.getNumber();
        //清空Map
        this.waitMap.clear();
        lock.unlock();
        //得到集合的长度
        final int waiterCount = wms.size();
        //遍历集合
        for (int i = 0; i < waiterCount; i++) {
            //得到对应的WaitMeta对象
            final WaitMeta wm = wms.get(i);
            wm.errorCode = errCode;
            //回调对象中封装的方法，该方法一旦被回调
            //复制器对象就会继续发送日志给跟随者节点了
            ThreadPoolsFactory.runInThread(this.groupId, () -> runOnNewLog(wm));
        }
        return true;
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:这个方法就是校验判断当前日志是被领导者还是跟随者处理的方法，这个方法在第三版本删减了很多，因为还不涉及日志传输
     * 所以我把跟随者对传输过来的日志的判断都给删掉了
     * 这里我想问大家一个问题，成为领导者后不是应该刷新一条空日志吗？给各个节点传输过去，这个功能在哪里实现呢？
     * 大家可以先自己思考思考，算了，还是放在配置变更那一版本实现吧，按照文章的节奏来
     */
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private boolean checkAndResolveConflict(final List<LogEntry> entries, final StableClosure done, final Lock lock) {
        final LogEntry firstLogEntry = ArrayDeque.peekFirst(entries);
        if (firstLogEntry.getId().getIndex() == 0) {
            //判断是否为领导者节点的日志处理逻辑，如果是领导者，就给每一条日志设置索引
            for (int i = 0; i < entries.size(); i++) {
                //一直让最后一条索引递增即可，如果是整个集群中的第一条日志，这时候lastLogIndex默认值为0
                //所以第一条日志的索引为1
                entries.get(i).getId().setIndex(++this.lastLogIndex);
            }
            return true;
        }
        //这里就是跟随者节点处理从领导者传输过来的日志的逻辑，比如判断传输过来的日志和本地有没有冲突，是需要覆盖还是需要截断等等
        else {
            //走到这里就意味着是跟随者节点在处理从领导者传输过来的日志
            //首先判断一下传输过来的这批日志中的第一条日志的索引是否大于当前跟随者节点想要接收的下一条日志的索引
            //不管是什么原因，总之跟随者和领导者的日志并不同步，中间缺失了一部分日志
            //如果没有出错，这两个值应该是想等着，出错了就返回给领导者错误响应
            if (firstLogEntry.getId().getIndex() > this.lastLogIndex + 1) {
                //在另一个线程中执行done包装的回调方法，其实就是给领导者回复错误的响应
                ThreadPoolsFactory.runClosureInThread(this.groupId, done, new Status(RaftError.EINVAL,
                        "There's gap between first_index=%d and last_log_index=%d", firstLogEntry.getId().getIndex(),
                        this.lastLogIndex));
                return false;
            }//这里先简单引入一下第七版本代码的知识，就是下面这个appliedId。这个成员变量代表着已经应用到状态机的最新的日志
            //在第七版本代码中，引入状态机之后，会看到状态机组件是怎么给这个成员变量赋值的
            //在这一版本还无法给这个成员变量赋值，但是大家直到他代表什么就行了
            //这里是得到当前跟随者节点已经应用到状态机中的最新日志的索引
            final long appliedIndex = this.appliedId.getIndex();
            //得到从领导者传递过来的最后一条日志的索引
            final LogEntry lastLogEntry = ArrayDeque.peekLast(entries);
            //然后判断这个索引和从领导者传递过来的这批日志中的第一条日志的索引的大小关系
            //如果从领导者传递过来的最后一条日志的索引小于当前跟随者节点已经应用了的日志
            //这就说明现在传递过来的这批日志已经过期了，因为在pipeline模式下，在某些情况下，领导者是有可能会向
            //跟随者节点发送重复的日志的，这个在第六版本会为大家详细讲解，总之，只要发现日志过期了，就不必再处理了
            if (lastLogEntry.getId().getIndex() <= appliedIndex) {
                LOG.warn(
                        "Received entries of which the lastLog={} is not greater than appliedIndex={}, return immediately with nothing changed.",
                        lastLogEntry.getId().getIndex(), appliedIndex);
                //这里给领导者回复响应，注意这里回复的是成功的响应，因为日志早就被跟随者处理了
                //当然可以回复成功的响应了
                ThreadPoolsFactory.runClosureInThread(this.groupId, done);
                return false;
            }//走到这里意味着从领导者传输过来的日志正好和跟随者当前要接收的日志对接上了
            if (firstLogEntry.getId().getIndex() == this.lastLogIndex + 1) {
                //设置当前跟随者节点的最后一条日志的索引，然后就直接返回true了，不必走下面的这个else分支了
                //之后在外层方法将日志放到内存，然后落盘就可以了
                this.lastLogIndex = lastLogEntry.getId().getIndex();
            } else {
                //走到这里有些棘手了，现在的情况是从领导者发送过来的这批日志中，第一条日志的索引是小于当前跟随者节点的最后一条日志索引加1
                //但是呢，又不等于当前跟随者节点要接收的下一条日志的索引，比如说当前跟随者节点的最后一条日志是10，那么下一条要接收的日志索引就是11
                //从领导者发送过来的这批日志的第一条日志的索引是8，这样一来，第一条日志的索引小于跟随者要接收的日志的索引，但是又不等于跟随者就要接收的这条日志
                //显然是跟随者和领导者的日志出现了重叠部分，出现这种情况的原因很简单，就是集群中的领导者发生过变更，旧的领导者向当前跟随者节点发送过日志
                //但还没来得及提交就宕机了，新的领导者被选举出来之后，产生了新的日志，肯定要覆盖其他跟随者节点的旧日志，这时候只有一种解决方法
                //那就是找到领导者和跟随者日志重叠的起点，在我举的例子中，就是跟随者的索引为7的日志，然后将当前跟随者节点的这个索引之后的日志全部截掉
                //这里大家可能会有些困惑，觉得为什么jraft框架不能把日志先放到内存中，等日志提交成功了再刷新到硬盘中呢？
                //这里我想简单说两句，这个jraft框架本身就是用来存放数据，使各个节点的数据保持一致的，和数据打交道的框架就要尽最大限度得保证数据丢失的风险
                //所以jraft框架会先把日志落盘，不管发生什么情况，总之要先把数据落盘，这样能尽量减少数据丢失的风险
                //下面就是找到跟随者和领导者日志冲突的交界点
                int conflictingIndex = 0;
                //在循环中遍历领导者传来的每一条日志，然后判断日志的索引
                for (; conflictingIndex < entries.size(); conflictingIndex++) {
                    //entries.get(conflictingIndex).getId().getIndex()的作用很明确，就是得到从领导者传来的日志的索引
                    //unsafeGetTerm方法就会根据领导者日志的索引去本地跟随者的日志组件中查看对应日志的任期
                    //entries.get(conflictingIndex).getId().getTerm()得到的就是领导者发送过来的日志任期
                    //然后将这两个任期做对比。这里为什么非要使用这个循环呢？我感觉日志有重叠，肯定是从领导者发送过来的本批次日志的第一条日志就开始重叠了呀
                    //还是用上面的例子，领导者发送过来的第一条日志的索引为8，当前跟随者节点要接收的下一条日志为11，肯定就是把跟随者日志索引从8开始，包括8在内的
                    //日志全部删除呀，为什么非要用循环找一遍呢？脑子秀逗了，找一遍的目的就是为了判断当前日志是不是重复传输了
                    if (unsafeGetTerm(entries.get(conflictingIndex).getId().getIndex()) != entries
                            .get(conflictingIndex).getId().getTerm()) {
                        break;
                    }
                }//这里就找到了要从哪个位置删除跟随者的日志，并且判断重叠的索引不等于日志条目
                //比如说领导者传递过来了索引为1-10的10条日志，并且这是第二次把这批日志传递给跟随者了，跟随者已经将这批日志落盘了，但是还没有应用呢
                //又一次收到这批日志之后，逻辑同样可以进行到这里，并且在走完了上面整个循环，也没找到两个相同索引的日志的任期不相等
                //最后conflictingIndex自增之后等于entries.size()了退出循环，所以，如果conflictingIndex = entries.size()的时候
                //就说明日志重复发送了，不用处理这批日志即可，如果不想等，才是日志有冲突的情况，这时候就要将跟随者节点的一些日志截取掉
                if (conflictingIndex != entries.size()) {
                    if (entries.get(conflictingIndex).getId().getIndex() <= this.lastLogIndex) {
                        //移除跟随者节点和领导者重叠的日志，可以看到，如果重叠位置是8，那就从8-1，也就是从7开始，把索引为7之后的日志全部移除了
                        unsafeTruncateSuffix(entries.get(conflictingIndex).getId().getIndex() - 1, lock);
                    }//设置跟随者最后一条日志索引
                    this.lastLogIndex = lastLogEntry.getId().getIndex();
                }//走到这里就意味着conflictingIndex = entries.size()，不用处理这批日志
                if (conflictingIndex > 0) {
                    //这里直接把重复的日志从集合中删除即可，会不会有这样一种情况，那就是领导者发送的部分日志和跟随者节点的日志重复了
                    //比如跟随者节点已经把1-10索引的日志落盘了，领导者发送过来的下一批日志是5-15，这样一来，跟随者在日志落盘的时候就要把
                    //重复的日志从日志集合中删除，但什么时候会出现这种情况呢？我不太明白，因为在NodeImpl的handleAppendEntriesRequest方法中
                    //实际上已经把日志重叠的索引为之确定了，领导者节点应该已经知道了日志是在哪个索引发生重叠的，会直接把重叠位置之后的日志都发送过来
                    entries.subList(0, conflictingIndex).clear();
                }
            }
            return true;
        }
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/12
     * @Description:从指定位置开始移除本地日志条目
     */
    private void unsafeTruncateSuffix(final long lastIndexKept, final Lock lock) {
        //判断要移除的日志是否已经被提交了
        if (lastIndexKept < this.appliedId.getIndex()) {
            LOG.error("FATAL ERROR: Can't truncate logs before appliedId={}, lastIndexKept={}", this.appliedId,
                    lastIndexKept);
            return;
        }//先从内存缓存日志的组件中删除日志
        this.logsInMemory.removeFromLastWhen(entry -> entry.getId().getIndex() > lastIndexKept);
        this.lastLogIndex = lastIndexKept;
        final long lastTermKept = unsafeGetTerm(lastIndexKept);
        Requires.requireTrue(this.lastLogIndex == 0 || lastTermKept != 0);
        LOG.debug("Truncate suffix :{}", lastIndexKept);
        //从配置管理器中尝试删除日志
        this.configManager.truncateSuffix(lastIndexKept);
        lock.unlock();
        //这里也能看出来对锁粒度的控制，不需要锁的时候就直接释放锁
        final TruncateSuffixClosure c = new TruncateSuffixClosure(lastIndexKept, lastTermKept);
        //提交事件给环形数组，在这个里面，会执行真正从数据库组件中删除日志的操作，并且是被disruptor框架异步执行的
        offerEvent(c, EventType.TRUNCATE_SUFFIX);
        //需要的时候再获取，这里再次获取是因为外层函数还需要这个锁，最终释放，会在外层方法释放
        lock.lock();
    }



    private static class TruncateSuffixClosure extends StableClosure {
        long lastIndexKept;
        long lastTermKept;

        public TruncateSuffixClosure(final long lastIndexKept, final long lastTermKept) {
            super(null);
            this.lastIndexKept = lastIndexKept;
            this.lastTermKept = lastTermKept;
        }

        @Override
        public void run(final Status status) {

        }

    }




    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:设置最后一条日志ID的方法
     */
    private void setDiskId(final LogId id) {
        if (id == null) {
            return;
        }
        LogId clearId;
        this.writeLock.lock();
        try {
            if (id.compareTo(this.diskId) < 0) {
                return;
            }//设置最后一条日志ID
            this.diskId = id;
            //下面这行代码暂且注释掉
            //clearId = this.diskId.compareTo(this.appliedId) <= 0 ? this.diskId : this.appliedId;
        } finally {
            this.writeLock.unlock();
        }
//        if (clearId != null) {
//            clearMemoryLogs(clearId);
//        }
    }




    private void reportError(final int code, final String fmt, final Object... args) {
        this.hasError = true;
        final RaftException error = new RaftException(EnumOutter.ErrorType.ERROR_TYPE_LOG);
        error.setStatus(new Status(code, fmt, args));
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/3
     * @Description:第三版本新添加的方法，从内存中获取日志的方法
     */
    protected LogEntry getEntryFromMemory(final long index) {
        LogEntry entry = null;
        if (!this.logsInMemory.isEmpty()) {
            //获取内存中的第一条日志索引
            final long firstIndex = this.logsInMemory.peekFirst().getId().getIndex();
            //获取内存中最后一条日志索引
            final long lastIndex = this.logsInMemory.peekLast().getId().getIndex();
            //如果日志的数量和内存中的不想等就报错
            if (lastIndex - firstIndex + 1 != this.logsInMemory.size()) {
                throw new IllegalStateException(String.format("lastIndex=%d,firstIndex=%d,logsInMemory=[%s]",
                        lastIndex, firstIndex, descLogsInMemory()));
            }//校验合规后从内存中获得索引对应的日志
            if (index >= firstIndex && index <= lastIndex) {
                entry = this.logsInMemory.get((int) (index - firstIndex));
            }
        }
        return entry;
    }

    //描述内存中存储日志详情的方法，该方法最后会返回拼接好的字符串，字符串展示的就是内存中存放日志的详细信息
    private String descLogsInMemory() {
        final StringBuilder sb = new StringBuilder();
        boolean wasFirst = true;
        for (int i = 0; i < this.logsInMemory.size(); i++) {
            LogEntry logEntry = this.logsInMemory.get(i);
            if (!wasFirst) {
                sb.append(",");
            } else {
                wasFirst = false;
            }
            sb.append("<id:(").append(logEntry.getId().getTerm()).append(",").append(logEntry.getId().getIndex())
                    .append("),type:").append(logEntry.getType()).append(">");
        }
        return sb.toString();
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/30
     * @Description:根据索引获得日志条目的方法，该方法重构了，加上了从内存中获取日志的步骤
     */
    @Override
    public LogEntry getEntry(long index) {
        //这里使用的是读锁，可以同时读，但不能修改
        this.readLock.lock();
        try {//检验日志索引是否合规，日志索引不能比最后一条索引大，不能比第一条索引小
            if (index > this.lastLogIndex || index < this.firstLogIndex) {
                return null;
            }//先从内存中获取日志，获取不到再从日志存储器中获取
            final LogEntry entry = getEntryFromMemory(index);
            if (entry != null) {
                return entry;
            }
        } finally {
            this.readLock.unlock();
        }//从日志存储器中获得指定索引的日志条目
        final LogEntry entry = this.logStorage.getEntry(index);
        if (entry == null) {
            //如果日志条目为空则报错
            reportError(RaftError.EIO.getNumber(), "Corrupted entry at index=%d, not found", index);
        }
        //这里判断了一下是否开启了校验和功能，如果开启了，就将从数据存储器中获得的日志的校验和之前记录的做一下对比
        //如果不想等则意味着日志有损坏
        if (entry != null && this.raftOptions.isEnableLogEntryChecksum() && entry.isCorrupted()) {
            String msg = String.format("Corrupted entry at index=%d, term=%d, expectedChecksum=%d, realChecksum=%d",
                    index, entry.getId().getTerm(), entry.getChecksum(), entry.checksum());
            reportError(RaftError.EIO.getNumber(), msg);
            throw new LogEntryCorruptedException(msg);
        }
        return entry;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/30
     * @Description:该方法也经过重构了，先从内存中获得，获取失败再去日志存储器中获得
     */
    @Override
    public long getTerm(long index) {
        if (index == 0) {
            return 0;
        }
        this.readLock.lock();
        try {
            //校验日志索引是否合规
            if (index > this.lastLogIndex || index < this.firstLogIndex) {
                return 0;
            }
            final LogEntry entry = getEntryFromMemory(index);
            if (entry != null) {
                return entry.getId().getTerm();
            }
        } finally {
            this.readLock.unlock();
        }
       //该方法就是直接从日志存储器中获得指定索引日志的任期
        return getTermFromLogStorage(index);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/1
     * @Description:从日志存储器中获得指定索引日志的任期
     */
    private long getTermFromLogStorage(final long index) {
        //从日志存储器中获得指定索引对应的日志条目
        final LogEntry entry = this.logStorage.getEntry(index);
        if (entry != null) {
            //通过对校验和来判断日志是否有存坏
            if (this.raftOptions.isEnableLogEntryChecksum() && entry.isCorrupted()) {
                final String msg = String.format(
                        "The log entry is corrupted, index=%d, term=%d, expectedChecksum=%d, realChecksum=%d", entry
                                .getId().getIndex(), entry.getId().getTerm(), entry.getChecksum(), entry.checksum());
                reportError(RaftError.EIO.getNumber(), msg);
                throw new LogEntryCorruptedException(msg);
            }
            //返回日志任期
            return entry.getId().getTerm();
        }
        return 0;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/1
     * @Description:得到第一条日志索引，下面这几个方法多多少少都有删减，下一版本将会重构完整
     */
    @Override
    public long getFirstLogIndex() {
        this.readLock.lock();
        try {
            return this.firstLogIndex;
        } finally {
            this.readLock.unlock();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/8
     * @Description:判断日志组件是否超负荷工作了
     */
    @Override
    public boolean hasAvailableCapacityToAppendEntries(final int requiredCapacity) {
        if (this.stopped) {
            return false;
        }//其实就是判断环形数组是否还有足够的空间放得下要消费的数据
        return this.diskQueue.hasAvailableCapacity(requiredCapacity);
    }


    @Override
    public long getLastLogIndex() {
        return getLastLogIndex(false);
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:获得最后一条日志的索引，该方法在第三版本也经过重构了
     */
    @Override
    public long getLastLogIndex(boolean isFlush) {
        LastLogIdClosure c;
        this.readLock.lock();
        try {
            if (!isFlush) {
                //如果不需要将缓冲区中的日志刷新到硬盘，就直接返回最后一条日志索引
                return this.lastLogIndex;
            } else {//如果需要的话，就创建封装了回调方法的对象，交给disruptor框架去异步执行，将日志落盘后
                //再获得最后一条日志的索引
//                if (this.lastLogIndex == this.lastSnapshotId.getIndex()) {
//                    return this.lastLogIndex;
//                }
                c = new LastLogIdClosure();
            }
        } finally {
            this.readLock.unlock();
        }//把事件交给disruptor框架去处理，其实就是向disruptor框架的环形队列中发布生产者数据
        offerEvent(c, EventType.LAST_LOG_ID);
        try {//等待事件异步执行完毕
            c.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return c.lastLogId.getIndex();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:将时间提交到disruptor环形数组中的方法
     */
    private void offerEvent(final StableClosure done, final EventType type) {
        assert(done != null);

        if (this.stopped) {
            ThreadPoolsFactory.runClosureInThread(this.groupId, done, new Status(RaftError.ESTOP, "Log manager is stopped."));
            return;
        }
        this.diskQueue.publishEvent((event, sequence) -> {
            event.reset();
            event.type = type;
            event.done = done;
        });
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/30
     * @Description:该方法也经过重构了，获得指定索引日志的任期
     */
    private long unsafeGetTerm(final long index) {
        if (index == 0) {
            return 0;
        }//忽略被注释掉的代码
//        final LogId lss = this.lastSnapshotId;
//        if (index == lss.getIndex()) {
//            return lss.getTerm();
//        }
        if (index > this.lastLogIndex || index < this.firstLogIndex) {
            return 0;
        }
        final LogEntry entry = getEntryFromMemory(index);
        if (entry != null) {
            return entry.getId().getTerm();
        }
        return getTermFromLogStorage(index);
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:获得最后一条日志ID的方法，被注释掉的代码都可以暂时忽略
     */
    @Override
    public LogId getLastLogId(final boolean isFlush) {
        LastLogIdClosure c;
        this.readLock.lock();
        try {
            if (!isFlush) {
                //if (this.lastLogIndex >= this.firstLogIndex) {
                    return new LogId(this.lastLogIndex, unsafeGetTerm(this.lastLogIndex));
                //}
                //return this.lastSnapshotId;
            } else {
//                if (this.lastLogIndex == this.lastSnapshotId.getIndex()) {
//                    return this.lastSnapshotId;
//                }
                c = new LastLogIdClosure();
            }
        } finally {
            this.readLock.unlock();
        }
        offerEvent(c, EventType.LAST_LOG_ID);
        try {
            c.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);        }
        return c.lastLogId;
    }




    //根据索引得到指定的配置日志条目
    @Override
    public ConfigurationEntry getConfiguration(final long index) {
        this.readLock.lock();
        try {
            return this.configManager.get(index);
        } finally {
            this.readLock.unlock();
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/30
     * @Description:把最新的配置条目信息返回出去
     */
    @Override
    public ConfigurationEntry checkAndSetConfiguration(final ConfigurationEntry current) {
        if (current == null) {
            return null;
        }
        this.readLock.lock();
        try {
            final ConfigurationEntry lastConf = this.configManager.getLastConfiguration();
            if (lastConf != null && !lastConf.isEmpty() && !current.getId().equals(lastConf.getId())) {
                return lastConf;
            }
        } finally {
            this.readLock.unlock();
        }
        return current;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/1
     * @Description:校验日志索引一致性的方法，这个方法在引入了日志快照后，才能真正发挥作用
     */
    @Override
    public Status checkConsistency() {
        this.readLock.lock();
        try {
            //对第一条日志索引和最后一条日志索引校验
            Requires.requireTrue(this.firstLogIndex > 0);
            Requires.requireTrue(this.lastLogIndex >= 0);
            //这里是因为firstLogIndex初始值默认就为1，而且在第二版本中默认集群中没有日志产生，所以就判断firstLogIndex是否仍未初始值即可
            if (this.firstLogIndex == 1) {
                return Status.OK();
            }
            return new Status(RaftError.EIO, "Missing logs in (0, %d)", this.firstLogIndex);
        } finally {
            this.readLock.unlock();
        }
    }


    private static class WaitMeta {
        //复制器提交的回调方法会被封装到这个成员变量中
        NewLogCallback onNewLog;
        //错误状态码
        int errorCode;

        Object arg;

        public WaitMeta(final NewLogCallback onNewLog, final Object arg, final int errorCode) {
            super();
            this.onNewLog = onNewLog;
            this.arg = arg;
            this.errorCode = errorCode;
        }

    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/7
     * @Description:如果领导者中暂时没有日志可以向跟随者发送了，那么跟随者对应的复制器对象会提交一个回调方法
     * 这个回调方法会被封装到NewLogCallback对象中，接着进一步被封装到WaitMeta对象中，然后存放到日志管理器的waitMap成员变量中
     * 只要领导者继续接收到业务日志或者配置变更日志了，就会回调复制器提交的这个方法，通知复制器可以继续发送日志给跟随者了
     * 这个wait方法就是用来把复制器提交的方法保存到日志管理器的waitMap中的
     */
    @Override
    public long wait(final long expectedLastLogIndex, final NewLogCallback cb, final Object arg) {
        //复制器的回调方法就会封装到这个WaitMeta对象中
        final WaitMeta wm = new WaitMeta(cb, arg, 0);
        //在下面这个方法中，就会是情况把创建好的WaitMeta对象缓存到waitMap中
        return notifyOnNewLog(expectedLastLogIndex, wm);
    }

    private long notifyOnNewLog(final long expectedLastLogIndex, final WaitMeta wm) {
        //上锁，课程进行到这里，我想大家已经具备了，哪些方法会被哪些线程同时调用的能力
        //大家可以在自己尝试分析一下，在这个方法中，这里为什么加锁，可能会有哪些线程同时调用这个方法呢？
        //换句话说，这里加了锁，是为了保证那个属性被修改的时候不会出现并发问题？
        //显然就是当前类的waitMap成员变量了，那么，大家可以再进一步思考一下，会有哪些线程
        //同时向Map中添加数据呢？什么情况下会呢？
        this.writeLock.lock();
        try {
            //这里有一个检验，就是判断从复制器对象中一路传递过来的这个expectedLastLogIndex方法参数
            //如果在复制器提交的回调方法被进一步包装的过程中，领导者内部又产生了新的日志
            //那么走到这里日志管理器的最后一条日志ID已经不等于复制器期望的最后一条日志ID了，这时候就意味着复制器可以继续传输日志给跟随者了
            //同时也校验了一下，看看当前的日志管理器的执行状态是不是已经终止工作了
            if (expectedLastLogIndex != this.lastLogIndex || this.stopped) {
                //走到这里会进一步判断是不是日志管理器终止工作了
                wm.errorCode = this.stopped ? RaftError.ESTOP.getNumber() : 0;
                //在这里回调复制器提交的回调方法，会根据刚刚得到的这个errorCode来做具体处理
                ThreadPoolsFactory.runInThread(this.groupId, () -> runOnNewLog(wm));
                //直接返回0，因为不管是日志管理器停止工作，还是领导者有新的日志了，都意味着复制器对象
                //提交的回调方法不用缓存到日志管理器的Map成员变量中了，所以直接返回即可
                return 0L;
            }
            //下面就是具体把WaitMeta对象缓存到waitMap中的逻辑
            //分配ID给WaitMeta对象
            long waitId = this.nextWaitId++;
            if (waitId < 0) {
                //这个ID怎么能小于0呢？初始化就是1，然后一直做自增
                //源码里面有些代码就是这样，明明什么情况下也不会发生，硬是要写一个判断
                waitId = this.nextWaitId = 1;
            }
            //把键值对放到Map中
            this.waitMap.put(waitId, wm);
            //返回ID
            return waitId;
        } finally {
            this.writeLock.unlock();
        }
    }

    void runOnNewLog(final WaitMeta wm) {
        //这里就是开始执行复制器在其waitMoreEntries方法中定义的回调方法了
        //该方法一被调用，就会进一步执行到复制器的continueSending方法中
        //看看是不是要继续发送日志给跟随者
        wm.onNewLog.onNewLog(wm.arg, wm.errorCode);
    }

    //根据键，从waitMap中移除一个WaitMeta对象
    @Override
    public boolean removeWaiter(final long id) {
        this.writeLock.lock();
        try {
            return this.waitMap.remove(id) != null;
        } finally {
            this.writeLock.unlock();
        }
    }



    @Override
    public void shutdown() {
        //暂且不做实现
    }



    @Override
    public void describe(final Printer out) {
        final long _firstLogIndex;
        final long _lastLogIndex;
        final String _diskId;
        final String _appliedId;
        final String _lastSnapshotId;
        this.readLock.lock();
        try {
            _firstLogIndex = this.firstLogIndex;
            _lastLogIndex = this.lastLogIndex;
            _diskId = String.valueOf(this.diskId);
            _appliedId = String.valueOf(this.appliedId);
        } finally {
            this.readLock.unlock();
        }
        out.print("  storage: [")
                .print(_firstLogIndex)
                .print(", ")
                .print(_lastLogIndex)
                .println(']');
        out.print("  diskId: ")
                .println(_diskId);
        out.print("  appliedId: ")
                .println(_appliedId);
    }


}