大家好，这一章我将延续上一章结尾的内容，为大家接着实现日志落盘功能。在上一章的结尾，我们已经实现了 Disruptor 框架异步消费日志，以及对日志进行批处理。在为 NodeImpl 对象引入的 DIsruptor 框架中，我为它定义了一个 LogEntryAndClosureHandler 消费处理器，在该处理器中对日志分批处理，每一次都从 Disruptor 的环形数组队列中获取 32 个待消费的日志条目对象，然后对这些日志条目对象做一些简单的校验，就可以为这些日志条目对象的任期和日志类型进行赋值了。当这些操作完毕之后，会将这一批日志交给日志管理器，也就是 logManager 对象，调用 logManager 日志管理器的 appendEntries 方法对这批日志做进一步处理。所以，这一节章我们的内容就从 logManager 对象的 appendEntries 方法开始讲起。

<a name="C0xDV"></a>
## 为 LogManagerImpl 引入 Disruptor 框架功能

现在我们都知道这个 logManager 日志管理器实际上就是一个 LogManagerImpl 对象，并且也知道了重构后的 appendEntries 方法究竟是怎么定义的。这时候，就可以继续分析一下，在 LogManagerImpl 的 appendEntries 方法中应该再对这批日志条目对象进行什么操作。<br />我首先想到的就是应该给这批日志条目的每一个 LogEntry 对象的索引进行赋值，在上一章我为大家解释过，因为在 LogManagerImpl 的 appendEntries 方法中还要对这批日志索引进行一次判断，然后再根据判断情况对这批日志的索引进行赋值。所以当一批日志刚刚被提交给 LogManagerImpl 的 appendEntries 方法时，这批日志条目对象仅仅是对任期和日志类型进行了赋值，日志索引还没有被赋值。所以，在** LogManagerImpl 的 appendEntries 方法中肯定要对这一批 LogEntry 对象的索引进行赋值**。<br />这里我还要再解释一下，我知道可能有些朋友会困惑，为什么在 LogManagerImpl 的 appendEntries 方法要对这一批日志的索引再校验一次。或者说，日志难道不是领导者自己产生的吗？把客户端指令包装成了 LogEntry 日志对象，这个时候刚刚创建的 LogEntry 日志对象的索引肯定还没被赋值呀，直接根据本地日志最新的日志索引一直递增赋值不就行了吗？要做什么校验呢？这就是我要补充说明的地方，因为这里要讲解的本来是下一版本代码的知识。**实际上在 raft 集群中，无论是领导者节点还是跟随者节点部署的程序都是一样的。客户端将指令发送给领导者之后，会被领导者包装为 LogEntry 对象，然后经过一系列方法调用，LogManagerImpl 的 appendEntries 方法会将一批日志持久化到本地。这是领导者节点内部持久化日志的流程。我刚才说了，领导者和跟随者节点部署的程序都是一样的，只不过根据各自节点状态的不同执行不同的操作，但是，将日志落盘这个操作不管是在领导者还是跟随者节点内部都会执行，所以，当领导者将一批日志传输给跟随者之后，跟随者节点也会调用 LogManagerImpl 的 appendEntries 方法将这一批日志落盘(由此也可以判断领导者向跟随者节点传输日志是一批一批传输的，并不是一条一条的传输，这个功能很快就会实现)**。这时候就会出现一个什么情况呢？**那就是程序自身要判断即将落盘的这批日志是领导者自己产生的还是从领导者传输过来的，具体的判断方法就是通过这批日志的索引来判断，如果是领导者自己生成的日志，这批日志中的每一个 LogEntry 对象的索引肯定都是 0，还没有被赋值；如果是从领导者传输过来的日志，那么这批日志中的每一个 LogEntry 对象的索引肯定都不为 0 了，它们的索引肯定已经在领导者哪里被赋值了**。<br />**如果在 LogManagerImpl 的 appendEntries 方法内部经过判断之后，发现即将落盘的这批日志都还没有索引，那就意味着当前节点是领导者，直接将索引递增为这批日志赋值即可；如果发现这批日志已经拥有索引了，接下来就要做一个非常重要的操作，那就是判断领导者传输过来的日志和跟随者节点本地日志是否存在冲突，如果存在冲突，应该如何解决冲突**。但这是第五版本代码的知识，所以我就不在这里为大家具体讲解了。我想，这时候大家应该也明白了为什么要在 LogManagerImpl 的 appendEntries 方法中对这批日志进行索引判断了吧？<br />上面的内容都理解了之后，接下来请大家再想想，除了给这批日志的索引赋值，在 LogManagerImpl 的 appendEntries 方法中，还要进行什么操作呢？**如果不考虑解决日志冲突等等后面代码版本的知识，就目前的情况来说，似乎也不用再执行什么操作了，可以直接把这批索引赋值成功的日志提交给日志存储器，也就是 RocksDBLogStorage 对象，让 RocksDBLogStorage 对象进行日志的最终落盘即可**。RocksDBLogStorage 是 LogManagerImpl 内部的成员变量，这一点大家应该还有印象。如果把 LogManagerImpl 的 appendEntries 方法用代码展示出来，目前可以写成下面这样。请看下面代码块。
```java
public class LogManagerImpl implements LogManager {


    //日志存储器，LogManagerImpl这个类虽然持有日志管理器，但是并不能意味着
    //这个类就是对日志管理器做了层代理
    //实际上，这个类做了很多其他的工作，为了提高日志落盘的性能等等
    private LogStorage logStorage;

    //最后一条日志的索引，这两个成员变量在checkConsistency方法中会用到
    //也就是检查日志索引一致性的时候，lastLogIndex默认为0，在日志组件初始化的时候会被赋值
    private volatile long lastLogIndex;


    //其他内容省略


    //将一批日志持久化的方法，在第三版本代码中，大家可以先不必过分关注这个done回调对象
    //注意，这里的这个回调对象是和这批日志对应的，也就是说，这一批日志在落盘成功后
    //done回调对象中的方法就会被回调，具体作用就是用来向状态机提交日志的，目前版本代码中还不需要关注这个回调方法
    //我在为大家提供的第三版本代码中，把这个回调对象中的回调方法作了一下修改，就只是简单打印了一句话，也展示在当前代码块中了
    //就是LeaderStableClosure类，该类的父类为StableClosure
    //当这批日志被落盘成功后，回调方法就会被回调，控制台就会输出领导者的日志成功落盘这句话
    public void appendEntries(final List<LogEntry> entries, final StableClosure done) {

        //进行一些非空判断
        assert(done != null);
        Requires.requireNonNull(done, "done");


        //获得写锁
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

            //在这里将本批次日志交给日志存储器，让日志存储器将日志落盘
            this.logStorage.appendEntries(entries)

            
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }




    //这个方法就是校验判断当前日志是被领导者还是跟随者处理的方法，这个方法在第三版本删减了很多，因为还不涉及日志传输
    //所以我把跟随者对传输过来的日志的判断都给删掉了
    private boolean checkAndResolveConflict(final List<LogEntry> entries, final StableClosure done, final Lock lock) {
        //取出集合中第一条日志
        final LogEntry firstLogEntry = ArrayDeque.peekFirst(entries);
        //判断日志索引是否为0
        if (firstLogEntry.getId().getIndex() == 0) {
            //判断是否为领导者节点的日志处理逻辑，如果是领导者，就给每一条日志设置索引
            for (int i = 0; i < entries.size(); i++) {
                //一直让最后一条索引递增即可，如果是整个集群中的第一条日志，这时候lastLogIndex默认值为0
                //所以第一条日志的索引为1
                entries.get(i).getId().setIndex(++this.lastLogIndex);
            }
            return true;
        }
        //这里的逻辑是我自己加的，源码中这里的逻辑非常长，是跟随者节点处理从领导者传输过来的日志的逻辑
        //比如判断传输过来的日志和本地有没有冲突，是需要覆盖还是需要截断等等，第五版本，我就会为大家实现了
        return false;
    }

    
    
}






//注意该类并不在LogManagerImpl类中，而是NodeImpl的内部类，这里只是展示在同一个代码块中了
//这个类很快就会被用到，大家先混个眼熟
class LeaderStableClosure extends LogManager.StableClosure {
    
    public LeaderStableClosure(final List<LogEntry> entries) {
            super(entries);

        //该方法会在本批日志落盘成功后被回调
        @Override
        public void run(final Status status) {
            if (status.isOk()) {
                System.out.println("日志在领导者内部写入数据库成功！！！！！！！！！！！！！");
            } else {
                LOG.error("Node {} append [{}, {}] failed, status={}.", getNodeId(), this.firstLogIndex,
                        this.firstLogIndex + this.nEntries - 1, status);
            }
        }
    }
}




//LeaderStableClosure的父类，这里也要强调一下，该类同样不在LogManagerImpl类中
//而是在LogManager接口中，大家直接去看第三版本代码即可，代码中的逻辑非常清楚
abstract class StableClosure implements Closure {
    
    protected long firstLogIndex = 0;
    protected List<LogEntry> entries;
    protected int nEntries;
   
    public List<LogEntry> getEntries() {
        return this.entries;
    }
    
    public void setEntries(final List<LogEntry> entries) {
        this.entries = entries;
        if (entries != null) {
            this.nEntries = entries.size();
        } else {
            this.nEntries = 0;
        }
    }

    
}

    
```
上面代码块中的内容看起来比较多，但是核心逻辑非常简单，就是在 appendEntries 方法中执行的两个操作，**第一就是对本批次要被持久化的日志索引赋值，这个操作是在 appendEntries 方法内部的 checkAndResolveConflict 方法中执行的；第二就是把索引赋值成功的这批日志交给 RocksDBLogStorage 日志存储器，让日志存储器将日志持久化。说到底日志是要存放到 RocksDBLogStorage 存储器中持有的 RocksDB 数据库中，让数据库持久化日志**。<br />好了，现在这批要被持久化的日志的索引也已经赋值成功了，接下来，似乎就应该继续向下分析，看看当这批日志被提交给 RocksDBLogStorage 日志存储器之后，RocksDBLogStorage 日志存储器是怎么把这批日志落盘成功的。但是，这里我不得不再次要求大家停下来，修正一下我们的思考方向。实际上，现在我们的注意力仍然需要集中在 LogManagerImpl 的 appendEntries 方法中。当然，我并不想让大家的思维局限在这个方法中，我想请大家先思考一下，即便现在我们已经分批处理日志了，那么在将日志持久化的时候，就可以直接把这批日志持久化到硬盘中吗？也许大家不明白我这个问题的意思，那我换一种方式，假如现在要持久化 32 个 LogEntry 日志条目，但是这 32 个日志条目对象所有的有效数据加起来才 1 KB，也就是这 32 个日志条目封装的客户端指令的总大小才 1 KB，在这种情况下，也要让程序立刻进行一次持久化操作吗？我们都知道，IO 操作是比较耗时的，所以最好等到要持久化的数据总量积累的更大一些，再一口气持久化。所以，这种情况下显然是不需要立刻将这 32 条日志持久化到硬盘的。既然这 32 条日志并不能被立刻持久化到硬盘，它们应该存放到哪里呢？<br />这时候，我就要为大家引入一个新的功能模块，那就是日志缓冲区，**当 NodeImpl 对象中 Disruptor 的批处理器调用了 LogManagerImpl 的 appendEntries 方法，将一批批日志的索引、任期、日志类型都赋值成功，就会把这批日志先提交给日志缓冲区，等日志缓冲区缓存的日志的字节总量积累到一定程度，才会被提交给  RocksDBLogStorage 日志存储器，被日志存储器持久化到硬盘**。这才是日志被持久化的真正流程。不管是在领导者节点还是在跟随者节点，都会遵从这个流程持久化日志。那这时候又有一个新的问题，日志缓冲区中缓存的字节总量达到多少，才能持久化到硬盘呢？别忘了我们现在开发的是一个 raft 共识算法框架，**共识算法严重依赖日志的持久化来保证集群各个节点状态的一致和数据备份，如果领导者内部创建的日志不能立刻持久化到硬盘中，肯定会有随时丢失的风险。当然，这个问题也对我么构不成什么威胁，因为我已经看过 sofajraft 框架的源码了，在 sofajraft 框架中把日志缓冲区可缓存的字节总量定义为了 256 KB，所以我们直接使用这个设定即可**。<br />好了，接下来我就该为大家用代码把这个日志缓冲区实现出来，然后再将一批批日志提交到日志缓冲区中即可。但现在还是不急，因为还有一些细节需要为大家讲解。请大家想一想，现在日志缓冲区也有了，**如果我给日志缓冲区单独配备一个单线程执行器(因为 raft 要求日志一定要按照索引顺序持久化，所以最好就是一个线程进行持久化操作)，让这个单线程执行器专门处理提交到日志缓冲区中的日志，而 NodeImpl 对象中 Disruptor 的批处理器只需要把一批日志的索引、任期、日志类型赋值完毕直接提交给单线程执行器即可，单线程执行器负责把这批日志放到日志缓冲区，并且在日志缓冲区缓存的字节容量达到限制后将日志落盘。这样一来，程序处理日志的速度不就更快了吗**？注意，这里我指的是处理日志的速度，所谓处理日志，就是将客户端指令包装成日志，给日志索引、任期、日志类型赋值，解决跟随者节点和领导者日志冲突等等操作。并不是日志持久化的速度，日志持久化的速度并不会有什么显著提升，原来持久化耗时多少，不可能换另一个线程后耗时就会减少。除非数据写入模式发生改变，比如顺序写入比随机写入快，或者使用操作系统缓存，数据直接被写入到操作系统缓存中了，或者是硬件得到提升了。<br />如果大家理解了这个逻辑，那么现在我就可以直接给大家说开了，**实际上 LogManagerImpl 类也引入了 Disruptor 框架的功能。在 LogManagerImpl 类中也定义了一个 Disruptor 成员变量，还有一个环形数组队列。从 LogManagerImpl 的 appendEntries 方法中提交过来的一批日志，在被赋值好了任期和索引等等之后，最终会被提交到 LogManagerImpl 类中定义的 Disruptor 的环形数组中，由 LogManagerImpl 类中 Disruptor 的批处理器将这批日志放到日志缓冲区，等待日志缓冲区满了之后将所有日志一起落盘**。如果大家觉得有点绕，可以看看第三版本代码的流程图。<br />总之，现在概括一些，目前程序就重构成了下面这样。
```java
NodeImpl          Disruptor        NodeImpl类中定义Disruptor对象以及环形数组队列，是为了让客户端指令能通过apply方法被包装成LogEntry
                                   对象后，能直接提交到环形数组队列中，然后让Disruptor的批处理器去异步批量地给LogEntry对象的索引、任期、
                                   日志类型赋值，并且解决日志冲突等问题。在执行完了这些操作之后，批处理器会把处理好的日志提交给LogManagerImpl
                                   类中定义的Disruptor的环形数组队列，具体操作就是在appendEntries方法中执行的


LogManagerImpl    Disruptor		   LogManagerImpl类中定义的Disruptor对象以及环形数组队列，是为了让已经被处理好的日志能直接从appendEntries
                                   方法中提交到环形数组队列中，然后再由本类的Disruptor的批处理器批量地把日志放到日志缓冲区中，等日志缓冲区容量达到
                                   限制，批处理器就可以调用RocksDBLogStorage日志存储器的appendEntries方法，通过日志存储器将日志持久化到硬盘


这样一来，NodeImpl类中的Disruptor就可以专心处理日志，为日志条目对象的相关属性进行赋值或其他操作
而LogManagerImpl类中Disruptor则专注和IO打交道，把日志存放到日志缓冲区，然后在恰当的时机把日志落盘
分工合作，提高了处理日志的效率
```
如果大家理解了上面的这个逻辑，接下来我就要为大家编写代码了。首先应该在 LogManagerImpl 类中把 Disruptor 和环形数组队列定义出来，当然，还有什么事件工厂，数据传输器，消费处理器等等，这些我就不一一说明了，因为在为 NodeImpl 类引入 Disruptor 框架功能的时候已经讲解过一遍了，这里就不再重复了，直接写在代码中了。大家可以根据代码中的注释来查看学习。当然，引入了 Disruptor 框架的功能之后，LogManagerImpl 的 appendEntries 方法也应该重构一下，该方法的逻辑几乎没什么变化，只不过在方法结束之前，需要把本批处理的日志提交到 LogManagerImpl 类中的环形数组队列成员变量中。具体实现请看下面代码块。
```java
public class LogManagerImpl implements LogManager {


    //日志存储器，LogManagerImpl这个类虽然持有日志管理器，但是并不能意味着
    //这个类就是对日志管理器做了层代理
    //实际上，这个类做了很多其他的工作，为了提高日志落盘的性能等等
    private LogStorage logStorage;

    //最后一条日志的索引，这两个成员变量在checkConsistency方法中会用到
    //也就是检查日志索引一致性的时候，lastLogIndex默认为0，在日志组件初始化的时候会被赋值
    private volatile long lastLogIndex;

    //又一个Disruptor框架，从NodeImpl的executeApplyingTasks方法中传递过来的批量日志
    //最终会被这个Disruptor异步落盘，但是这个disruptor并不只是处理日志落盘事件，还处理其他的一些事件
    //这些事件后面会慢慢重构完整
    private Disruptor<StableClosureEvent> disruptor;
    
    //环形队列
    private RingBuffer<StableClosureEvent> diskQueue;



    //这个枚举类中的对象很快就会用到，在把日志提交到环形数组队列中时就会用到
    private enum EventType {
        //其他事件，日志落盘对应的就是这个事件
        OTHER,
        //得到最后一条日志ID事件，这个枚举对象先不用关心
        LAST_LOG_ID
    }



    //存放到环形数组中中的对象
    private static class StableClosureEvent {
        StableClosure done;
        EventType type;

        void reset() {
            this.done = null;
            this.type = null;
        }
    }

    
    
    //disruptor要使用的事件工厂
    private static class StableClosureEventFactory implements EventFactory<StableClosureEvent> {

        @Override
        public StableClosureEvent newInstance() {
            return new StableClosureEvent();
        }
    }


    
    //disruptor框架中要使用的消费者处理器
    private class StableClosureEventHandler implements EventHandler<StableClosureEvent> {

        //该处理器暂时不做实现
    }
    
    
    
    //初始化方法，这个方法的逻辑比较简单，所以注释也添加的简单一些
    //disruptor就在这个方法中创建
    @Override
    public boolean init(final LogManagerOptions opts) {
        this.writeLock.lock();
        try {

            //省略其他内容

            
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

            //在这里启动disruptor
            this.diskQueue = this.disruptor.start();


            
        } finally {
            this.writeLock.unlock();
        }
        return true;
    }
    

    //其他内容省略


    //将一批日志持久化的方法，在第三版本代码中，大家可以先不必过分关注这个done回调对象
    //注意，这里的这个回调对象是和这批日志对应的，也就是说，这一批日志在落盘成功后
    //done回调对象中的方法就会被回调，具体作用就是用来向状态机提交日志的，目前版本代码中还不需要关注这个回调方法
    //我在为大家提供的第三版本代码中，把这个回调对象中的回调方法作了一下修改，就只是简单打印了一句话，也展示在当前代码块中了
    //就是LeaderStableClosure类，该类的父类为StableClosure
    //当这批日志被落盘成功后，回调方法就会被回调，控制台就会输出领导者的日志成功落盘这句话
    public void appendEntries(final List<LogEntry> entries, final StableClosure done) {

        //进行一些非空判断
        assert(done != null);
        Requires.requireNonNull(done, "done");


        //获得写锁
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


            //在这里，方法中的done参数对象派上用场了，之前我为大家简单展示了这个对象的类型代码
            //把这批日志交给StableClosure对象，这个对象被封装到当前类中的Disruptor队列的生产者数据中
            //被Disruptor的事件处理器获取，并且将设置的这批日志落盘
            //具体逻辑就在下面
            //注意这个时候，appendEntries方法的done这个参数对象既持有了本批次要被持久化的日志
            //也持有日志持久化之后要被回调的方法，这个逻辑一定要理清楚，可以适当看看之前的代码，多看几遍
            //或者直接去第三版本代码中查看对应代码，一定要把这个逻辑理清楚，因为后面还要用到日志持久化之后要被回调的方法
            done.setEntries(entries);

            //发布生产者数据，Disruptor可以将日志异步落盘了
            //这里大家也可已看到，日志落盘整个系列操作几乎就是用Disruptor衔接起来的，关键步骤都是异步和回调
            this.diskQueue.publishEvent((event, sequence) -> {
                event.reset();
                //这里设置了事件类型，具体逻辑请看StableClosureEventHandler类的onEvent方法
                event.type = EventType.OTHER;
                //StableClosureEvent类中定义了一个StableClosure类型的成员变量
                //而方法参数中的done就是StableClosure类型的对象，这个对象刚刚持有了本批次要持久化的日志
                //现在又在数据传输器中，把自己赋值给了StableClosureEvent类中的StableClosure类型的成员变量
                //这样一来，这一批要被持久化到硬盘的日志不就被数据传输器放到环形数组队列中了吗？费了点劲，终于放进来了
                event.done = done;
            });

            
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }
}







//注意该类并不在LogManagerImpl类中，而是NodeImpl的内部类，这里只是展示在同一个代码块中了
//这个类很快就会被用到，大家先混个眼熟
class LeaderStableClosure extends LogManager.StableClosure {
    
    public LeaderStableClosure(final List<LogEntry> entries) {
            super(entries);

        //该方法会在本批日志落盘成功后被回调
        @Override
        public void run(final Status status) {
            if (status.isOk()) {
                System.out.println("日志在领导者内部写入数据库成功！！！！！！！！！！！！！");
            } else {
                LOG.error("Node {} append [{}, {}] failed, status={}.", getNodeId(), this.firstLogIndex,
                        this.firstLogIndex + this.nEntries - 1, status);
            }
        }
    }
}




//LeaderStableClosure的父类，这里也要强调一下，该类同样不在LogManagerImpl类中
//而是在LogManager接口中，大家直接去看第三版本代码即可，代码中的逻辑非常清楚
abstract class StableClosure implements Closure {
    
    protected long firstLogIndex = 0;
    protected List<LogEntry> entries;
    protected int nEntries;
   
    public List<LogEntry> getEntries() {
        return this.entries;
    }
    
    public void setEntries(final List<LogEntry> entries) {
        this.entries = entries;
        if (entries != null) {
            this.nEntries = entries.size();
        } else {
            this.nEntries = 0;
        }
    }

    
}

```
好了，到此为止，我知道肯定有很多朋友已经有点晕了，disruptor 对象和环形数组对象的定义和创建能看懂，事件工厂也能看懂，应该存放到环形数组中的对象也能看懂，但是上面代码块中展示枚举类究竟是怎么回事呢？以及要被处理的这批日志添加到环形数组队列中，是怎么被 LogManagerImpl 类中 Disruptor 的批处理器消费的呢？这时候就应该让消费处理器登场了，也就是上面代码块中没有实现的 StableClosureEventHandler 内部类，接下来，我就要为大家实现这个消费处理器。**在这个消费者处理器中，Disruptor 的批处理器会从环形数组中取出要消费的数据，这些数据中包含了一批批要被持久化的日志，批处理器会先把这些日志放到日志缓冲区中，如果日志缓冲区达到容量上限了，就会把日志持久化到硬盘中**。所以，接下来不仅要实现这个消费处理器，还要把日志缓冲区为大家实现出来。当然，代码我已经编写好了，具体实现，请看下面代码块。
```java
public class LogManagerImpl implements LogManager {


    //日志存储器，LogManagerImpl这个类虽然持有日志管理器，但是并不能意味着
    //这个类就是对日志管理器做了层代理
    //实际上，这个类做了很多其他的工作，为了提高日志落盘的性能等等
    private LogStorage logStorage;

    //最后一条日志的索引，这两个成员变量在checkConsistency方法中会用到
    //也就是检查日志索引一致性的时候，lastLogIndex默认为0，在日志组件初始化的时候会被赋值
    private volatile long lastLogIndex;

    //又一个Disruptor框架，从NodeImpl的executeApplyingTasks方法中传递过来的批量日志
    //最终会被这个Disruptor异步落盘，但是这个disruptor并不只是处理日志落盘事件，还处理其他的一些事件
    //这些事件后面会慢慢重构完整
    private Disruptor<StableClosureEvent> disruptor;
    
    //环形队列
    private RingBuffer<StableClosureEvent> diskQueue;




    //其他内容省略



    //disruptor框架中要使用的消费者处理器
    private class StableClosureEventHandler implements EventHandler<StableClosureEvent> {

        //这行代码的逻辑可以直接忽略，暂时还用不到
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
                //这里就是把日志存放到日志缓冲区的操作
                this.ab.append(done);
                
                
                
                //下面这个else分支中的逻辑其实不用看，大家学习第三版本代码的时候，直接去我提供的源码中查看会更清楚，这里我就不删减代码了
                //就当是提前混个眼熟吧
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
            }


            //这里的操作非常重要，请大家想一下，如果日志缓冲区中的日志一直没有满，结果领导者内部没有日志产生了，那么日志缓冲区就一直不会满
            //这样的话，缓冲区中的日志还怎么持久化呢？这时候就要靠下面这个操作了
            //endOfBatch是当前onEvent方法的第三个参数，就是判断当前消费的数据是不是环形数组中最后一个可消费的数据
            //如果是说明环形数组中没有数据了，那么就直接把缓冲区中的日志落盘即可
            //ab.flush()就是刷新缓冲区的意思，也就是把缓冲区数据落盘的操作
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







    //该类的对象就是日志缓冲区
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

        
        
        //将日志暂存到缓冲区的方法
        void append(final StableClosure done) {
            //先判断日主缓冲区中的容量是否达到最大了，判断缓冲区中可存放大最大Buffer达到最大限制了
            //raftOptions.getMaxAppendBufferSize()得到的就是256KB
            if (this.size == this.cap || this.bufferSize >= LogManagerImpl.this.raftOptions.getMaxAppendBufferSize()) {
                //如果达到最大限制就刷盘，这里也可以意识到，如果上一次添加日志到缓冲区时，添加完毕后缓冲区内容益出了，并不会立刻刷新
                //而是等到下一批日志到来的时候再刷新。当然，也可能是在StableClosureEventHandler处理器的onEvent方法中就直接刷新了
                //大家可以对刷新日志的情况做一下分析，注释中其实已经做了很详细的说明了
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



        //刷新日志到硬盘的方法
        LogId flush() {
            //判断是否有数据
            if (this.size > 0) {
                //将日志落盘的真正方法，在这里将日志落盘了
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
                        }//日志落盘成功之后，在这里回调了每一批日志对应的，被封装在StableClosure对象中的回调方法
                        //这时候控制台就会输出日志在领导者内部写入数据库成功这句话了
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




    //将日志刷新到硬盘的方法
    private LogId appendToStorage(final List<LogEntry> toAppend) {
       
        //在这里把日志落盘了，使用的日志存储器落盘的，返回的是落盘到数据库的日志条目的数量
        final int nAppent = this.logStorage.appendEntries(toAppend);
        ////这里得到了最后一条日志ID，并且返回最后一条日志ID
        return toAppend.get(nAppent - 1).getId();;
    }


    //其他内容省略


}
```
可以看到，最后日志还是由批处理器执行了 logStorage.appendEntries(toAppend) 这个方法，将要持久化的日志交给了日志存储器，所以程序的流程就来到了日志存储器，也就是 RocksDBLogStorage 类的 appendEntries 方法中。而这个方法的逻辑就很简单了，**因为 RocksDBLogStorage 日志存储器中持有了 RocksDB 数据库，最终日志是要通过数据库持久化到本地的。所以，直接让 RocksDBLogStorage 日志存储器把日志存放到 RocksDB 数据库中就行了**。这里我就只简单展示一下 RocksDBLogStorage 对象的 appendEntries 方法，就是在这个方法中，把日志存放到了 RocksDB 数据库中了。请看下面代码块。
```java
//日志存储器，存储日志的所有功能都是由这个类提供的，数据库存储日志是以键值对的形式存放发
//键就是日志索引，值就是日志条目本身
public class RocksDBLogStorage implements LogStorage, Describer {


    //其他内容省略


    
    //将一批日志存储到数据库中的方法
    @Override
    public int appendEntries(final List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }//得到日志条目的数量
        final int entriesCount = entries.size();
        //执行批处理方法，executeBatch就是批处理方法
        //executeBatch方法的参数就是使用lambda表达式创建了一个批处理模版对象
        final boolean ret = executeBatch(batch -> {
            for (int i = 0; i < entriesCount; i++) {
                final LogEntry entry = entries.get(i);
                if (entry.getType() == EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                    //这里根据日志条目类型做了一个判断，看看是要把日志添加到confHandle列族中
                    //还是添加到defaultHandle列族中
                    addConfBatch(entry, batch);
                } else {
                    //这里就是把业务日志存放到数据库中了
                    addDataBatch(entry, batch);
                }
            }
        });
        if (ret) {//操作成功，返回本次落盘日志的数量
            return entriesCount;
        } else {
            return 0;
        }
    }



    //把日志添加到defaultHandle列族中的方法
    private void addDataBatch(final LogEntry entry, final WriteBatch batch) throws RocksDBException, IOException, InterruptedException {
        //得到日志索引，也就是key
        final long logIndex = entry.getId().getIndex();
        //对日志进行编码，得到键值对中的value
        final byte[] content = this.logEntryEncoder.encode(entry);
        //把键值对放到批量写入器中
        batch.put(this.defaultHandle, getKeyBytes(logIndex),content);
    }



    //将索引变成字节数组的方法，可以看到键值对中的key是8个字节
    protected byte[] getKeyBytes(final long index) {
        final byte[] ks = new byte[8];
        Bits.putLong(ks, 0, index);
        return ks;
    }

    //其他内容省略

    
}
```
到此为止，我就把日志落盘的全部流程实现完毕了。大家可以再看看上面的这些代码块，也可以结合第三版本代码流程图，直接去看我为大家提供的第三版本代码。只要把文章中讲解的内容掌握了，大家一定可以看懂代码。好了，我们的课程还不到结束的时候，接下来让我们再来思考另一个问题。

<a name="fAjor"></a>
## 引入日志缓存池，提高日志传输效率

在上一小节，我已经为大家实现了日志落盘的全流程。但是，实现了日志落盘并不意味着日志组件的功能就齐全了。**实际上，日志组件负责很多工作，日志落盘只是其中的一项。当领导者内部产生了新的日志之后，除了在本地持久化日志，还需要把日志传输给集群中的其他跟随者节点**。那传输日志的时候该怎么传输呢？是先将日志持久化到本地，然后再从本地加载日志到内存中，最终传递给跟随者节点；还是先把日志传输给跟随者节点，再将日志持久化到本地，或者是一边持久化到本地，一边传输给跟随者节点呢？这就是我刚才让大家思考的问题。<br />很显然，日志持久化与日志传输给跟随者节点这两个操作并不存在什么因果关系，没必要先让一个操作成功了，再执行另一个操作。所以，当然可以让领导者一边执行持久化日志的操作，一边把日志传输给跟随者节点。这时候，我就要再为大家引入一个组件了，**那就是日志缓存池，我用 SegmentList 对象来表示日志缓存池**。<br />当然，我还是要为大家详细解释一下什么是日志缓存池，**日志缓存池并不同于上一小节我为大家实现的日志缓冲区，日志缓冲区是配合日志持久化使用的组件。而日志缓存池是配合日志传输使用的组件。当领导者将客户端指令封装为 LogEntry 对象之后，这些对象会被 Disruptor 的批处理器分批处理，等它们的索引、任期、日志类型等等属性都被赋值成功，即将在 LogManagerImpl 的 appendEntries 方法中被提交给 LogManagerImpl 中 Disruptor 的环形数组队列中，这时候，我们可以先把这批日志放到日志缓存池中，就相当于把日志存放到了内存中。这里还涉及到一个监听机制，实际上，领导者中的每一个 Replicator 复制器对象都在监听日志缓存池中是否有新的日志，一旦监听到新的日志了，就会立刻把日志传输给每一个 Replicator 复制器对象对应的跟随者节点。当然，这是下一章的知识了。现在我只是简单提一下，让大家明白这个日志缓存池的具体作用。至于 Replicator 复制器对象怎么发送日志，怎么重构，下一章我会为大家具体讲解和实现**。<br />好了，说了这么多，也都是一些程序执行的流程，接下来就让我们看看代码吧，看看这个日志缓存池究竟该怎么实现。也就是 SegmentList 类究竟该怎么定义。这里我就不为大家分析得特别详细了，因为这个 SegmentList 类内部使用了 Netty 的对象池，我认为这些都是旧知识，所以就直接为大家展示类实现了，这里面也没有什么难点，都是很常规的逻辑，我想通过之前很多框架的学习，现在大家学习这种常规代码应该是很容易了。<br />首先让我来为大家阐述一下 SegmentList 类的内部结构，这个类内部虽然定义了一些成员变量，但是最重要的只有一个 ArrayDeque 类型的成员变量。它是一个继承了 ArrayList 的双端队列，具体实现请看下面代码块。
```java
//sofajraft框架自己定义的一个双端队列
public class ArrayDeque<E> extends java.util.ArrayList<E> {

    private static final long serialVersionUID = -4929318149975955629L;

   
    public static <E> E peekFirst(List<E> list) {
        return list.get(0);
    }

    
    public static <E> E pollFirst(List<E> list) {
        return list.remove(0);
    }

    
    public static <E> E peekLast(List<E> list) {
        return list.get(list.size() - 1);
    }

   
    public static <E> E pollLast(List<E> list) {
        return list.remove(list.size() - 1);
    }

   
    public E peekFirst() {
        return peekFirst(this);
    }

   
    public E peekLast() {
        return peekLast(this);
    }

    
    public E pollFirst() {
        return pollFirst(this);
    }

   
    public E pollLast() {
        return pollLast(this);
    }

    
    @Override
    public void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}

```
**ArrayDeque 队列是 SegmentList 类内部的一个成员变量，并且这个 ArrayDeque 队列内部存放的都是一个个 Segment 对象。Segment 是 SegmentList 的一个内部类，在 Segment 类中，定义了一个 elements 数组，每一个 LogEntry 对象都存放在 elements 数组中，并且每一个 Segment 对象中的 elements 数组都可以存放 128 个 LogEntry 对象**。光这样说可能很抽象，甚至有点蠢，接下来请大家看一下 SegmentList 类的结构图。
```java
                            SegmentList
                                |
                            ArrayDeque
                                |
                 [segment, segment, segment ...]
              /                 |                    \
          elements            elements              elements
       [0, 1 ... 127]    [128, 129 ... 255]    [256, 257 ... 383]
```
这就是 SegmentList 类的类结构，虽然展示得不完整，但是和核心存储功能相关的结构都展示出来了。现在，我还要再补充最后一点，那就是在 SegmentList 类中，其内部类 segment 的对象拥有自己的对象池。之前我跟大家说这个 SegmentList 类用到了 Netty 的对象池技术，就用在 segment 中了。<br />好了，接下来，我就可以为大家展示 SegmentList 类的部分代码了。请看下面代码块。
```java
//缓存日志条目对象的组件
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




    //Segment内部类的对象就是用来真正缓存日志条目的，Segment对象内部的数组可以存放128个日志条目
    //egment对象会存放到SegmentList对象的内部队列segments中
    private final static class Segment<T> implements Recyclable {

        //对象池本身，这里就用到了对象池技术，为Segment对象创建了对象池
        private static final Recyclers<Segment<?>> recyclers = new Recyclers<Segment<?>>(16_382 / SEGMENT_SIZE) {

            @Override
            protected Segment<?> newObject(final Handle handle) {
                //创建新的Segment对象
                return new Segment<>(handle);
            }
        };

        //重写的对象池中的方法
        public static Segment<?> newInstance(final boolean recycleSegment) {
            if (recycleSegment) {
                //如果开启了对象池，就从对象池中获得对象，默认开启了对象池
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



        //剩余的方法暂时省略，大家直接去我提供的第三版本代码中学习即可
    
    
    }




    //这里又回到SegmentList类中了

    //根据指定索引获得对应日志条目的方法
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





    //直接操纵ArrayList的Object[]elementData成员变量的原子引用器
    private static final ReferenceFieldUpdater<ArrayList<?>, Object[]> LIST_ARRAY_GETTER = Updaters.newReferenceFieldUpdater(ArrayList.class, "elementData");


    
    
    //把一批日志添加到缓存中的方法，在LogManagerImpl的appendEntries方法中就调用了这个方法
    //把一批日志添加到日志缓存池中了
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


    
    //将集合中的数据直接赋值给一个数组的方法，最后将数组返回
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

    
}
```
到此为止，日志缓存池也就为大家实现完毕了，大家可以多看看上面代码块中的代码。如果梳理清楚 SegmentList 类中代码的执行流程，最后我只需要为大家把 LogManagerImpl 的 appendEntries 方法重构一下，在日志被持久化之前，把这批日志放到日志缓存池中即可。请看下面代码块。
```java
public class LogManagerImpl implements LogManager {


    //省略其他内容


    //下面这个成员变量是用来在内存中缓存条目日志对象的，所有的日志条目对象都会先缓存到内存里
    //然后就直接开始向跟随者传输日志了
    private final SegmentList<LogEntry> logsInMemory = new SegmentList<>(true);

    


    public void appendEntries(final List<LogEntry> entries, final StableClosure done) {

        //进行一些非空判断
        assert(done != null);
        Requires.requireNonNull(done, "done");


        //获得写锁
        this.writeLock.lock();
        try {
            
            if (!entries.isEmpty() && !checkAndResolveConflict(entries, done, this.writeLock)) {
                entries.clear();
                return;
            }


            if (!entries.isEmpty()) {
                //日志在正式落盘之前，会把日志向缓存池中存放一份，这样传输起来更快更方便
                this.logsInMemory.addAll(entries);
            }

            
            done.setEntries(entries);
            this.diskQueue.publishEvent((event, sequence) -> {
                event.reset();
                event.type = EventType.OTHER;
                event.done = done;
            });

            
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }
    








    //省略其他内容
}
```
看完上面的代码块之后，最后我还想问大家一个问题，当一批日志被存放到日志缓存池中，并且这批日志已经被领导者传输到了跟随者节点了，那么这批日志什么时候会从缓存池中被清除呢？**这里我就简单解释一下，当这批日志被应用之后就可以从缓存池中清除了。因为一条日志一旦被应用成功，就意味着这条日志被传输给了集群中大部分节点，并且也持久化成功了，这时候这批日志就可以从缓存池中清除了**。当然，这中间还会涉及很复杂的流程，比如领导者发送日志失败，跟随者日志和领导者冲突了等等，总之，领导者无法避免重新发送日志的情况。这些知识就留到下一章为大家讲解吧。好了，朋友们，我们下一章见！

<a name="jPg43"></a>
## 总结

这一章和上一章的内容确实非常多，有些地方确实不容易理解，我在文中也没有给大家提供简单的流程图。这是因为我们的每一版本代码都配置全功能流程图，如果我在文章中继续画图，会耗费更多时间，希望大家理解一下。还有就是我写完这两章确实也有点累了，很坦诚地说，我也确实尽力去把文章写得通俗易懂。当然，我也很清楚，再写文章的过程中，尽管我脑海里的思路非常清晰，但并不能百分之百保证大家看文章时的思路和我一样清晰。所以我最后又为大家提出了三个重点问题。<br />**1 每一批日志究竟是怎么在 LogManagerImpl 的 appendEntries 方法中，被提交到 LogManagerImpl 中 Disruptor 的环形数组队列中的。**<br />**2 当日志被存放到日志缓冲区中之后，究竟会在哪些情况下被刷新到硬盘中呢？**<br />**3 与每一批日志对应的回调方法，注意，是与每一批日志对应的回调方法，也就是在执行 logManager.appendEntries(entries, new LeaderStableClosure(entries) 这个方法时创建的回调方法，究竟会在什么时候被回调呢？**<br />这几个问题的答案我都在文章中为大家解释清楚了，大家可以在看完文章之后再看看这三个问题，如果弄清楚了这几个问题的答案，那么第三版本代码的内容大家就完全掌握了。
