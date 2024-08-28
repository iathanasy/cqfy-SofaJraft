大家好，在上一章，我基本上已经为大家把 BallotBox 这个类重构完整了，要说还缺点什么东西，其实就差最后一个成员变量，那就是状态机了。但在正式讲解状态机之前，我还是想再罗嗦几句。前两章的内容讲完之后，我相信大家对一条日志应用到状态机的整体流程应该有了一个比较清晰的认识。如果让我来概括一下，我可以总结成以下几点。<br />**1 在节点初始化的时候，也就是 NodeImpl 对象执行 init 方法的时候，创建一个 BallotBox 投票箱对象。这个投票箱对象就是专门用来给每一条日志计算获得的成功票数的。**<br />**2 领导者节点接收了客户端指令，把客户端指令包装成一条日志。**<br />**3 领导者开始将这条日志进行持久化操作，在进行持久化的时候，会为这条日志创建一个 Ballot 投票计数器对象，把这个投票计数器对象添加到投票箱的队列中，同时也会把这条日志对应的要回调给客户端的方法对象添加到 BallotBox 对象内部的 closureQueue 队列中。**<br />**4 领导者将这条日志发送给跟随者节点，各个跟随者节点将这条日志持久化。**<br />**5 领导者接收各个跟随者节点的响应，在 onAppendEntriesReturned 方法中判断是否为成功响应，如果是成功响应，就调用 BallotBox 投票箱的 commitAt 方法，给这条日志对应的 Ballot 投票计数器投票。**<br />**6 当这条日志收到足够的票数后，就在 BallotBox 的 commitAt 方法中执行状态机组件的 onCommitted 方法提交日志，所谓提交日志就是将日志应用到状态机上。**<br />以上就是我为大家总结出来的 6 个要点，这 6 个要点就把一条日志从被创建到被应用的整个过程概括了，大家可以再思考一下这个流程。然后再跟着我的思路继续探索，现在我们的程序显然已经实现得差不多了，BallotBox 也重构得差不多了，处理日志响应的过程在前两章也为大家分析得很清楚了。现在就差最后一步，那就是把得到足够票数的日志应用到状态机上。当然，要是实现了这最后一步，上一章遗留得两个问题也就解决了。还记得之前提出的 7 个问题吗？我又一次为大家把那几个问题搬运到下面了。<br />**1 为什么要给每一条日志都创建 Ballot 投票计数器对象，难道不能给每一批日志创建一个唯一的 Ballot 对象吗？**<br />**2 Ballot 对象究竟在哪里被创建呢？**<br />**3 BallotBox 对象究竟是在什么时候被创建的？**<br />**4 BallotBox 对象内部还应该定义什么成员变量和方法？**<br />**5 创建好的 Ballot 对象怎么存放到 BallotBox 对象的内部队列中呢？**<br />**6 BallotBox 对象要怎么应用已被提交的日志呢？肯定是把要被应用的日志交给状态机，但状态机在哪里呢？是怎么实现的呢？**<br />**7 BallotBox 对象内部队列中的 Ballot 投票计数器什么时候被清除呢？**<br />当然，在这 7 个问题中已经解决了 5 个，只剩下第 4 个和第 6 个还没有解决。说得严谨一点，实际上第 4 个问题已经解决得差不多了，因为 BallotBox 这个类重构了好几次，不管是该类中定义的成员变量还方法都经过了多次重构，目前为止，这个 BallotBox 类已经被重构成了下面这样。请看下面代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);


    //存放每一个日志条目对象的回调方法的队列
    //这个类我就不具体展开了，大家直接去第7版本代码中查看即可，逻辑非常简单
    private ClosureQueue closureQueue;

    //最后被提交的日志的索引，初始化时为0
    private long lastCommittedIndex = 0;
    
    
    //要提交的下一条日志的索引
    private long pendingIndex;

    //考虑到在领导者内部可能是多个Replicator对象同时执行commitAt方法
    //还应该再定义一个同步锁成员变量，这样就可以在commitAt方法内上锁来保证并发安全了
    //读写锁
    private final StampedLock stampedLock = new StampedLock();


    //这个队列存放的就是为每一个日志条目对象创建的Ballot对象，注意，这里使用了对象池技术
    //详情就不展开了，可以直接去代码中查看，这个对象池在之前已经见过了
    private final SegmentList<Ballot> pendingMetaQueue = new SegmentList<>(false);




    //定义的提交日志的方法，别忘了该方法会在Replicator的onAppendEntriesReturned方法中被调用
    //并且只要该方法被调用了，说明对应的某批日志一定收到了成功响应，那么在该方法中直接给这批日志的
    //每一条日志投票即可
    //该类最核心的方法，提交firstLogIndex和lastLogIndex之间的日志，当然，在提交的时候还要判断这些日志是否收到了集群中过半节点的投票，该方法会被领导者调用
    public void commitAt(final long firstLogIndex, final long lastLogIndex, final PeerId peer) {
        
        
        
        //省略部分逻辑



        //如果这批日志对应的投票计数器收到了足够的票数，就应用这批日志，代码暂且不实现
        //假如这是一批索引为为20-30的日志，经过循环之后发现每一条日志都可以提交，那么lastCommittedIndex的值就为30
        //onCommitted方法就会应用30和30之前的所有还未应用的日志，这个方法暂且不实现
        onCommitted(lastCommittedIndex);
        return true;
    }


    
    //设置领导者要提交的下一条日志的索引
    public boolean resetPendingIndex(final long newPendingIndex) {
       //方法本身就不展示了
    }




    //该方法的作用很重要，但是逻辑很简单，就是给每一个日志条目创建一个Ballot对象，Ballot对象是投票计数器，同时把每一个日志条目的回调方法存放到closureQueue队列中
    //存放到这个队列中，就意味着状态机组件可以使用这个队列中的数据了
    public boolean appendPendingTask(final Configuration conf, final Configuration oldConf, final Closure done) {
       //方法本身就不展示了
    }

    

    
}
```
上面代码块中展示的内容就是目前我们重构完成的 BallotBox 类，这个类中的成员变量和三个重要方法的作用我们也都清楚了，所以我就把方法本身的逻辑省略了，大家在学习的时候可以打开第七版本的代码，结合文章一起学习。刚才我跟大家说其实第 4 个问题也解决得差不多了，确实是这样，**如果把最后一个成员变量状态机也引入了，那么这个 BallotBox 类就算彻底重构完整了**。因为当领导者中的日志接收到足够的票数后，领导者就会应用这条日志，也就是把这条日志包装的指令应用到状态机上，而应用指令得入口就在 BallotBox 类的 commitAt 方法中。在这个方法中，如果判断出日志接收到的票数足够了，最后就会执行 onCommitted 方法，把日志应用到状态机上。既然是把日志应用到状态机上，那么这个 BallotBox 类肯定就要持有状态机对象的引用，否则状态机无法直接获得要应用的日志。当我们为 BallotBox 类引入了状态机之后，不仅是第 4 个问题，就连第 6 个问题也会跟着一起解决了。所以现在我们的问题就变成了，**怎么定义这个状态机，如何为 BallotBox 引入状态机**。要为 BallotBox 类引入状态机组件，首先就要知道状态机该怎么定义吧？

<a name="JUb63"></a>
## 引入 FSMCallerImpl 状态机组件

在前两章我为大家解释过状态机是什么，简单来说，状态机就是可以把客户端指令应用到业务层的一个功能组件。这么说可能还是有点抽象，接下来，让我来为大家举一个具体的例子。假如我们开发了一个业务框架，比如就是一个 kv 数据库，这个数据库集成了目前正在开发的 raft 框架，这样一来，数据库的业务执行流程就变成了这样：**数据库的客户端首先会向 raft 集群的领导者发送一条消息，当然，领导者本身就是一个 kv 数据库的服务器。这条指令到达了领导者之后，会先被领导者包装成一条日志，然后执行一系列操作，让这条指令在集群的各个节点之间达成共识，一旦达成共识，领导者就要应用这条指令。这时候，领导者就会把这条指令直接交给状态机组件，比如说就定义一个名为 MapStateMachine 的状态机类，在这个类中定义一个 onApply 方法，当一条日志在集群中达成共识后，领导者就可以直接调用 MapStateMachine 的 onApply 方法来应用这条日志中的指令**。具体实现请看下面代码块。<br />因为我们实现的是一个 kv 数据库，所以这里就直接用一个 Map 类模拟数据库。
```java
public class MapStateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(MapStateMachine.class);

    //模拟数据库实现的map，数据就暂时存放到map中
    private HashMap<String,String> map = new HashMap<>();


    public void onApply(final LogEntry logEntry) {
        //从日志中得到有效数据，也就是指令本身
        final ByteBuffer data = logEntry.getData();
        //接下来就是程序员自己根据业务编写的逻辑，这里我就写成伪代码了
        //比如说这里可以解析这条指令

        1.得到指令本身，是get、put、remove等等
        2.得到key
        3.得到value
        4.对成员变量map执行指令
       
    }


}
```
很好，在展示了上面的代码块之后，我相信大家应该对状态机是什么有了比较清楚的理解。现在我的问题是，怎么将 raft 框架和这个业务框架联系起来？换句话说，就是怎么让 raft 框架把已经达成共识的日志提交给 MapStateMachine。因为达成共识的日志肯定是在 raft 集群的领导者被应用的，这个时候就要从 raft 共识算法层把日志传递给业务层，也就是 MapStateMachine 状态机。这个其实也简单，请大家想一想，经过前面几章的学习，我们都已经知道了，当一条日志在集群中达成了共识，就要在 BallotBox 投票箱对象的 commitAt 方法中被应用，也就是说，只要我们把 MapStateMachine 状态机交给 BallotBox 对象使用，就可以在应用日志的时候，直接把要应用的日志交给 MapStateMachine 对象，然后把日志应用到业务层。具体实现请看下面代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);

    //省略其他成员变量

    //状态机组件
    private MapStateMachine stateMachine;




    //定义的提交日志的方法，别忘了该方法会在Replicator的onAppendEntriesReturned方法中被调用
    //并且只要该方法被调用了，说明对应的某批日志一定收到了成功响应，那么在该方法中直接给这批日志的
    //每一条日志投票即可
    //该类最核心的方法，提交firstLogIndex和lastLogIndex之间的日志，当然，在提交的时候还要判断这些日志是否收到了集群中过半节点的投票，该方法会被领导者调用
    public void commitAt(final long firstLogIndex, final long lastLogIndex, final PeerId peer) {
        
        
        
        //省略部分逻辑



        //如果这批日志对应的投票计数器收到了足够的票数，就应用这批日志，代码暂且不实现
        //假如这是一批索引为为20-30的日志，经过循环之后发现每一条日志都可以提交，那么lastCommittedIndex的值就为30
        //onCommitted方法就会应用30和30之前的所有还未应用的日志，这个方法暂且不实现
        //先把这个方法注释掉
        //onCommitted(lastCommittedIndex);

        //在这里把要应用的日志直接交给stateMachine去应用，但是这里遇到问题了
        //stateMachine的onApply方法的参数是LogEntry，现在无法得到具体的日志，无法传递给onApply方法
        //因为日志只能从LogManager日志管理器组件中获得，但BallotBox中并没有LogManager
        
        return true;
    }


    //省略部分逻辑
    
}
```
可以看到，当我们把状态机定义为 BallotBox 的成员变量之后，并且打算在 commitAt 方法中应用日志时，遇到了一个问题：**那就是 stateMachine 的 onApply 方法的参数是 LogEntry，但是在 BallotBox 投票箱中并不能得到日志本身。在我们开发的 raft 框架中，每一个组件的分工都很明确，日志只能从 LogManager 日志管理器中获得。那这样一来，是不是就要把 LogManager 日志管理器也定义为 BallotBox 投票箱对象的成员变量了呢**？这个问题请大家先记住，然后把它放到一边，接下来先让我们来看另一个问题。之前我在为大家分析 BallotBox 的 commitAt 方法时，告诉大家领导者应用日志的方式是成批成批地应用日志，从 commitAt 方法的参数就能看出来，根据本批日志的 firstLogIndex 和 lastLogIndex 将这批日志全都应用到状态机上。**可是目前我们实现的状态机 MapStateMachine 的 onApply 方法的参数是 LogEntry，也就是说，即便我们能够从 LogManager 日志管理器中得到要被应用的日志，也不能全部交给 MapStateMachine 状态机的组件应用到业务层，除非在一个循环中，根据日志索引得到每一条日志，然后应用到状态机上**。<br />好了，现在这两个问题已经提出来了，解决问题的思路也有了，那就是在 BallotBox 投票箱中定义一个 LogManager 日志管理器组件，然后在一个循环中根据日志索引得到对应的日志，再把日志应用到状态机上就行。具体实现请看下面代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);

    //省略其他成员变量

    //状态机组件
    private MapStateMachine stateMachine;

    //日志管理器组件
    private LogManager logManager;




    //定义的提交日志的方法，别忘了该方法会在Replicator的onAppendEntriesReturned方法中被调用
    //并且只要该方法被调用了，说明对应的某批日志一定收到了成功响应，那么在该方法中直接给这批日志的
    //每一条日志投票即可
    //该类最核心的方法，提交firstLogIndex和lastLogIndex之间的日志，当然，在提交的时候还要判断这些日志是否收到了集群中过半节点的投票，该方法会被领导者调用
    public void commitAt(final long firstLogIndex, final long lastLogIndex, final PeerId peer) {
        
        
        
        //省略部分逻辑


        //下面就是应用这一批日志的逻辑
        for (long i = firstLogIndex; i <= lastLogIndex; i++) {
            //根据每一条日志的索引得到日志本身
            LogEntry logEntry = logManager.getEntry(i);
            //接下来就可以把日志应用到状态机上了
            stateMachine.onApply(logEntry);
        }
        return true;
    }


    //省略部分逻辑
    
}
```
在上面的代码块中，已经完美处理了刚才提出来的两个问题，解决的方法很简单，只不过是把 LogManager 日志管理器组件定义为 BallotBox 投票箱的成员变量，然后在 commitAt 方法中定义一个循环，把要应用的本批日志在循环中全都应用到状态机上。到此为止就给大家把客户端指令在 raft 共识算法层和业务层的执行流程展示完整了。当然，现在程序不是没有瑕疵，实际上，应该说目前的程序存在一个十分严重的缺陷。<br />请大家想一想，如果是领导者要应用日志，那么肯定是在领导者接收到日志请求的响应后，在每次响应中对日志是否收到了足够的票数做判断，具体方法请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
@ThreadSafe
public class Replicator implements ThreadId.OnError {

    //省略其他内容

    private final ReplicatorOptions options;


     //构造方法
    public Replicator(final ReplicatorOptions replicatorOptions, final RaftOptions raftOptions) {
        super();
        //在这里给options成员变量赋值了，注意，options是被方法参数replicatorOptions赋值的
        //这就意味着在创建Replicator对象之前，BallotBox对象就已经创建了，并且封装到了
        //replicatorOptions对象中，然后在创建Replicator对象的时候，赋值给了该对象内部的options成员变量
        //那么，这个BallotBox对象究竟是在什么时候被创建的呢？
        this.options = replicatorOptions;
        //要发送的下一条日志的索引默认初始值为领导者最后一条日志索引加1
        this.nextIndex = this.options.getLogManager().getLastLogIndex() + 1;
        this.timerManager = replicatorOptions.getTimerManager();
        this.raftOptions = raftOptions;
        this.rpcService = replicatorOptions.getRaftRpcService();
    }


     private static boolean onAppendEntriesReturned(final ThreadId id, final Inflight inflight, final Status status,final RpcRequests.AppendEntriesRequest request,final RpcRequests.AppendEntriesResponse response, final long rpcSendTime,final long startTimeMs, final Replicator r) {

         //省略部分内容

         
         //走到这里，就意味着不管是探针请求响应还是日志请求响应，都成功回复了
         //下面就是公共操作了
         
         //得到这次请求批量发送的日志的数量
        final int entriesSize = request.getEntriesCount();

        //在这里就可以把日志请求的成功响应收集起来
        //如果数量大于0，就说明这次的请求肯定是日志复制的请求，并且也成功了
        if (entriesSize > 0) {
            
            //要在领导者判断发送成功的日志是否收到过半投票了
            //如果收到了就把这些日志应用到状态机中
            //r.nextIndex得到的就是本批传输日志的起始索引，r.nextIndex + entriesSize - 1得到的就是本批日志最后一条日志索引
            r.options.getBallotBox().commitAt(r.nextIndex, r.nextIndex + entriesSize - 1, r.options.getPeerId());
        }
         
       
        //更新接下来要发送的日志的索引，然后会跳出该方法，来到外层onRpcReturned方法，在finally中继续发送日志给跟随者节点
        r.nextIndex += entriesSize;
        return true;
         
     }


    //省略其他内容
}
```
在上面代码块的 onAppendEntriesReturned 方法中，领导者在每次接收到成功的日志传输响应后，都要判断一批日志是否可以被应用到状态机上了，如果可以应用，就会直接在 BallotBox 的 commitAt 方法中把日志应用到状态机上，执行业务框架中的业务逻辑了。但是请大家别忘了，**执行当前 onAppendEntriesReturned 方法的线程是领导者为 Replicator 复制器对象专门分配的单线程执行器，这个单线程执行器的任务队列中可能还有很多人物等待处理呢，有很多响应等待处理，但这个时候因为有一批日志可以应用了，就要将这一批日志全都应用到状态机上，等应用完毕之后，当前的这个响应才算处理完毕，然后 Replicator 对象的单线程执行器才能去任务队列中获得下一个任务。这样一来，显然对单线程执行器处理响应的效率造成一定影响**。这个逻辑大家可以思索思索，如果这个逻辑理清楚了，接下来我们就一起分析一下，怎么解决这个问题。<br />其实大家可以回顾一下之前我们实现的组件，比如日志管理器组件，在这个组件发挥做的时候，也就是将一批日志持久化到硬盘的时候，使用的是 disruptor 框架的批处理器。**现在我们要将一批日志应用到状态机上了，为了不拖累 Replicator 复制器对象的单线程执行器工作，显然我们也可以再引入一个 disruptor 组件，使用 disruptor 框架的批处理器将一批已经达成共识的日志应用到状态机上**。那么具体该怎么做呢？难道还要在 BallotBox 对象中再定义一个 disruptor 成员变量，还有环形队列成员变量，以及消费处理器等等内部类吗？这个时候我想请大家再整理一下思路，刚才我已经在 BallotBox 框架中引入了 LogManager 成员变量了，然后实现了 commitAt 方法后半部分的逻辑。看起来很方便，但现在要是再引入更多的成员变量，编写更多的内部类，这么做会带来什么影响呢？首先 BallotBox 类中的内容会越来越多，可能会显得杂乱一些，但这不是重点，要论内容多，哪个类的内容能他妈的比 NodeImpl 类的内容多啊？**最重要的响应是会让 BallotBox 类的职责和功能变得混乱，明明 BallotBox 最初只是一个投票箱，只负责对一条日志计算票数即可，票数足够的日志就会被投票箱提交给状态机组件，让状态机去应用日志。但现在随着程序不断重构和完善，BallotBox 类中定义的成员变量也越来越多，甚至有很多状态机组件的功能都要定义在 BallotBox 类中了，这显然是不合适的**。要解决这个问题的方法也很简单，就是再为状态机本身做一层封装，我们现在定义的 MapStateMachine 对象就是一个纯粹的状态机，只负责执行每一条日志指令即可，**但在执行日志指令之前还有一些前置工作要做，这些工作逻辑本来是被定义在了 BallotBox 投票箱对象中了，现在可以把这些逻辑剥离出来了，剥离出来的这些逻辑，就定义在新引入的组件中。这个新引入的组件就是 FSMCallerImpl 类**。FSM 就是有限状态机的意思，大家就可以把这个 FSMCallerImpl 类看作状态机执行类，状态机的所有操作全由这个类的对象来执行，当然，这样一来，这个 FSMCallerImpl 肯定就要持有 MapStateMachine 成员变量了。具体实现请看下面代码块。
```java
//状态机组件的实现类,注意，我这里直接把FSMCallerImpl的接口FSMCaller引入到程序中了
public class FSMCallerImpl implements FSMCaller {


    private static final Logger LOG = LoggerFactory.getLogger(FSMCallerImpl.class);

    //日志管理器组件
    private LogManager logManager;
    
    //真正的用户定义的状态机
    private MapStateMachine stateMachine;

    //存放了用户定义的回调方法的队列，这些回调方法是用来通知业务层状态机执行结果的
    private ClosureQueue closureQueue;
    
    //最新的被应用的日志的ID，初始化为0
    private final AtomicLong lastAppliedIndex;
    
    //最新的被提交的日志的ID，初始化为0
    private final AtomicLong lastCommittedIndex;
    
    //最新的被应用的日志的任期
    private long lastAppliedTerm;

    //disruptor启动器
    private Disruptor<ApplyTask> disruptor;
    
    //disruptor的环形数组
    private RingBuffer<ApplyTask> taskQueue;

    //状态机组件目前的状态，在构造方法中被初始化，一开始是空闲状态
    private volatile TaskType currTask;

    //当前节点本身
    private NodeImpl node;



    //该枚举类中的对象表示的是提交到状态机组件的disruptor中事件的类型
    //在第七版本主要只使用COMMITTED这个枚举类对象，其他的后面版本再讲解
    private enum TaskType {
        //空闲状态
        IDLE,
        //日志提交
        COMMITTED,
        //快照保存
        SNAPSHOT_SAVE,
        //快照加载
        SNAPSHOT_LOAD;
        //后面其实还有很多枚举对象，这里就省略了
    }



    //构造方法
    public FSMCallerImpl() {
        super();
        this.currTask = TaskType.IDLE;
        this.lastAppliedIndex = new AtomicLong(0);
        this.applyingIndex = new AtomicLong(0);
        this.lastCommittedIndex = new AtomicLong(0);
    }




    //初始化状态机组件的方法，这个方法就不添加注释了，因为目前为止已经见过很多这样的方法了，日志组件初始化的逻辑也是这一套，状态机组件初始化的逻辑也是如此
    //都是设置一些最初的配置参数，然后创建disruptor框架，启动disruptor框架
    @SuppressWarnings("unchecked")
    @Override
    public boolean init(final FSMCallerOptions opts) {
        //下面是给成员变量赋值的操作
        this.logManager = opts.getLogManager();
        //给真正的状态机成员变量赋值
        this.fsm = opts.getFsm();
        this.closureQueue = opts.getClosureQueue();
        this.node = opts.getNode();
        this.lastCommittedIndex.set(opts.getBootstrapId().getIndex());
        this.lastAppliedIndex.set(opts.getBootstrapId().getIndex());
        this.lastAppliedTerm = opts.getBootstrapId().getTerm();
        //下面就是创建disruptor组件的操作
        this.disruptor = DisruptorBuilder.<ApplyTask> newInstance()
                .setEventFactory(new ApplyTaskFactory())
                .setRingBufferSize(opts.getDisruptorBufferSize())
                .setThreadFactory(new NamedThreadFactory("JRaft-FSMCaller-Disruptor-", true))
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        //设置消费处理器
        this.disruptor.handleEventsWith(new ApplyTaskHandler());
        this.disruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(getClass().getSimpleName()));
        //启动disruptor组件
        this.taskQueue = this.disruptor.start();
        LOG.info("Starts FSMCaller successfully.");
        return true;
    }

    
    //要提交给disruptor框架的异步任务
    private static class ApplyTask {
        //任务类型
        TaskType type;
        //提交的日志ID
        long committedIndex;
        //任期
        long term;
        Status status;
        LeaderChangeContext leaderChangeCtx;
        //要回调的方法
        Closure done;
        CountDownLatch shutdownLatch;

        public void reset() {
            this.type = null;
            this.committedIndex = 0;
            this.term = 0;
            this.status = null;
            this.leaderChangeCtx = null;
            this.done = null;
            this.shutdownLatch = null;
        }
    }


    //事件工厂，该工厂会创建ApplyTask，然后将ApplyTask放到disruptor框架的环形数组中
    private static class ApplyTaskFactory implements EventFactory<ApplyTask> {

        @Override
        public ApplyTask newInstance() {
            return new ApplyTask();
        }
    }


    //事件处理器，也就是disruptor框架要用的消费者处理器
    private class ApplyTaskHandler implements EventHandler<ApplyTask> {
        boolean firstRun = true;
        //这个是最大的可以应用的日志的索引，初始化为-1，随着日志不断被应用，这个值也会被不断更新
        //注意，这个是消费者处理器中的成员变量，消费者处理器会处理所有要被应用的日志，所以这个成员变量是被所有日志共享的
        //当然，这个共享也是有限制了，就是被同一批处理的日志共享，在每一批新的日志应用的时候，这个值会被重置
        private long maxCommittedIndex = -1;

        @Override
        public void onEvent(final ApplyTask event, final long sequence, final boolean endOfBatch) throws Exception {
            //在这里执行每一个异步任务，这个runApplyTask方法就是把日志应用到状态机上的入口方法
            this.maxCommittedIndex = runApplyTask(event, this.maxCommittedIndex, endOfBatch);
        }
    }




    //该方法就是把已经提交成功的日志，应用到状态机中的入口方法
    //在BallotBox的commitAt方法中调用的就是下面这个方法
    @Override
    public boolean onCommitted(final long committedIndex) {
        return enqueueTask((task, sequence) -> {
            //设置为COMMITTED类型
            task.type = TaskType.COMMITTED;
            //committedIndex是从BallotBox类的commitAt方法中传递过来的参数
            //就是最新被提交的日志的索引
            task.committedIndex = committedIndex;
        });
    }


    //该方法会把生产者数据存放到disruptor的环形队列中
    private boolean enqueueTask(final EventTranslator<ApplyTask> tpl) {
        //在这里把要被应用的这批日志提交到disruptor的环形数组中
        this.taskQueue.publishEvent(tpl);
        return true;
    }


    //本类中的核心方法，也是disruptor框架的消费处理器中要调用的方法
    //在第七版本只会触发COMMITTED事件，所以我们就只看这个
    //这里我要强调一下，文章中的这个方法是最简化的，我提供的第七版本代码中的该方法比文章中
    //展示的方法复杂了很多，大家掌握了实现原理，更多的细节可以去第七版本代码中仔细学习
    @SuppressWarnings("ConstantConditions")
    private long runApplyTask(final ApplyTask task, long maxCommittedIndex, final boolean endOfBatch) {

        //在这里判断task是否为COMMITTED，如果是的话，就给maxCommittedIndex赋值
        if (task.type == TaskType.COMMITTED) {
            if (task.committedIndex > maxCommittedIndex) {
                //这时候maxCommittedIndex就不是-1了
                maxCommittedIndex = task.committedIndex;
            }
            task.reset();
        } 
        
        else {//执行到这里，意味着当前提交的异步任务并不是COMMITTED类型的
            //但是要判断一下当前的批处理中的maxCommittedIndex是否大于0，如果大于0说明现在有日志要被应用到状态机上
            //所以尽管要处理的是其他类型的任务，也要先把日志应用到状态机上
            if (maxCommittedIndex >= 0) {
                this.currTask = TaskType.COMMITTED;
                //在这里执行真正的提交日志的方法
                doCommitted(maxCommittedIndex);
                //重置maxCommittedIndex的值
                maxCommittedIndex = -1L;
            }
        
            //其他逻辑省略
        }

        
        //在这里会有一个总的判断，判断当前要消费的数据是不是本批数据中的最后一个，这是disruptor框架中的知识
        //如果是最后一个，并且maxCommittedIndex大于0，说明肯定有一些日志要被应用到状态机上
        //那就直接应用这批日志即可，如果不是本批数据中的最后一个，那就什么也不做
        //除非有其他非COMMITTED类型的任务被提交了，这时候就会在上面的else分支中把还未应用的日志都应用到状态机上
        if (endOfBatch && maxCommittedIndex >= 0) {
            this.currTask = TaskType.COMMITTED;
            //应用日志到状态机上
            doCommitted(maxCommittedIndex);
            //重置maxCommittedIndex为-1
            maxCommittedIndex = -1L;
        }
        this.currTask = TaskType.IDLE;
        return maxCommittedIndex;
        
    }


    //根据日志索引，将对应的日志应用到状态机中的方法，committedInde是已提交的最新的日志的索引
    private void doCommitted(final long committedIndex) {

        //该方法暂时不做实现，总之会在该方法中调用下面的代码
        //this.stateMachine.onApply()
        //因为现在是FSMCallerImpl类持有logManager日志管理器组件了，可以得到日志条目对象了
        
    }
    
    
}
```
好了，到此为止这个 FSMCallerImpl 类就展示给大家了，当然还有很多方法被我省略了，这些方法目前还用不到，所以大家也可以暂时忽略。我知道，一下子在 FSMCallerImpl  类中看到这么多方法，大家肯定反应不过来，对程序的执行流程没有头绪，这个不用担心，请先让我为大家 BallotBox 类还原成原来纯粹的面貌，然后再带领大家一起梳理日志在 FSMCallerImpl 状态机组件中被应用的完整流程。请大家先看下面的代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);


    //存放每一个日志条目对象的回调方法的队列
    //这个类我就不具体展开了，大家直接去第7版本代码中查看即可，逻辑非常简单
    private ClosureQueue closureQueue;

    //最后被提交的日志的索引，初始化时为0
    private long lastCommittedIndex = 0;
    
    
    //要提交的下一条日志的索引
    private long pendingIndex;

    //考虑到在领导者内部可能是多个Replicator对象同时执行commitAt方法
    //还应该再定义一个同步锁成员变量，这样就可以在commitAt方法内上锁来保证并发安全了
    //读写锁
    private final StampedLock stampedLock = new StampedLock();


    //这个队列存放的就是为每一个日志条目对象创建的Ballot对象，注意，这里使用了对象池技术
    //详情就不展开了，可以直接去代码中查看，这个对象池在之前已经见过了
    private final SegmentList<Ballot> pendingMetaQueue = new SegmentList<>(false);

    //状态机组件
    private FSMCaller waiter;


    //在该方法内部，就把状态机要使用的回调队列交给投票箱了，其实可以这么理解，状态机组件要使用这个回调队列，但是并不执行向回调队列中存放回调方法的操作
    //这个操作交给投票箱完成，然后队列中就有封装了回调方法的数据了，这样状态机在应用日志的时候，就可以回调这个队列中的方法，通知业务层执行结果了
    //这里也能看到，状态机中的回调队列和投票箱中的其实是同一个，只要投票箱中的回调队列有数据了，状态机就能从自己的回调队列中获得到
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


    //定义的提交日志的方法，别忘了该方法会在Replicator的onAppendEntriesReturned方法中被调用
    //并且只要该方法被调用了，说明对应的某批日志一定收到了成功响应，那么在该方法中直接给这批日志的
    //每一条日志投票即可
    //该类最核心的方法，提交firstLogIndex和lastLogIndex之间的日志，当然，在提交的时候还要判断这些日志是否收到了集群中过半节点的投票，该方法会被领导者调用
    public void commitAt(final long firstLogIndex, final long lastLogIndex, final PeerId peer) {
        
        
        
        //省略部分逻辑



        //在这里终于调用了状态机的onCommitted方法，剩下的就交给状态机来做就行了
        //到此为止，BallotBox投票箱和状态机的业务职责终于拆分开了
        this.waiter.onCommitted(lastCommittedIndex);
        return true;
    }


    
    //设置领导者要提交的下一条日志的索引
    public boolean resetPendingIndex(final long newPendingIndex) {
       //方法本身就不展示了
    }




    //该方法的作用很重要，但是逻辑很简单，就是给每一个日志条目创建一个Ballot对象，Ballot对象是投票计数器，同时把每一个日志条目的回调方法存放到closureQueue队列中
    //存放到这个队列中，就意味着状态机组件可以使用这个队列中的数据了
    public boolean appendPendingTask(final Configuration conf, final Configuration oldConf, final Closure done) {
       //方法本身就不展示了
    }

    

    
}
```
好了朋友们，在这一小节结束的时候，我就为大家把之前提出来的 7 个问题全部解决完毕了。之前提出来的 7 个问题中的第 4 个问题和第 6 个问题如下：<br />**4 BallotBox 对象内部还应该定义什么成员变量和方法？**<br />**6 BallotBox 对象要怎么应用已被提交的日志呢？肯定是把要被应用的日志交给状态机，但状态机在哪里呢？是怎么实现的呢？**<br />随着本小节的结束，这两个问题也都解决了。但是随着一些问题的解决，总会衍生出一些新的问题。比如就目前的情况来说吧，我们定义的这个 FSMCallerImpl 状态机组件在哪里被创建，又在哪里被初始化呢？怎么交给 BallotBox 投票箱使用呢？再比如，现在我们定义的这个 FSMCallerImpl 状态机中持有的是用户自己定义的 MapStateMachine 状态机成员变量，假如我们开发的这个 raft 框架并不是被集成在 kv 数据库中了，那么用户自己定义的状态机肯定就发生变化了，但是在 raft 框架的 FSMCallerImpl 类中，仍然持有 MapStateMachine 为成员变量显然就不合适了，这个问题又该怎么解决呢？这就是我们目前面临的几个新问题，接下来就让我先为大家把这几个问题解决了，然后再为大家梳理重构之后的程序的执行流程。

<a name="zjLUG"></a>
## 梳理本章程序的执行流程

在正式开始解决上一小节遗留下来的几个问题之前，让我再来为大家总结一下，我们目前面临的问题有哪些。<br />**1 FSMCallerImpl 状态机组件在哪里创建，又在哪里初始化？**<br />**2 FSMCallerImpl 状态机怎么交给 BallotBox 投票箱使用？**<br />**3 业务程序员自己定义的状态机对象怎么和 FSMCallerImpl 解耦？**<br />第三个问题其实是很好解决的，既然我们希望业务程序员自己定义的业务状态机和 FSMCallerImpl 解耦，那就直接定义一个业务状态机接口好了，这样用户想使用什么业务框架继承这个 raft 框架都可以了。比如引入一个 StateMachine 接口，让用户自己定义的业务状态机实现这个接口。这样一来，现在这个 FSMCallerImpl 类就可以重构成下面这样。请看下面代码块。<br />首先是实现了 StateMachine 接口的业务状态机本身。
```java
public class MapStateMachine implements StateMachine{

    private static final Logger LOG = LoggerFactory.getLogger(MapStateMachine.class);

    //模拟数据库实现的map，数据就暂时存放到map中
    private HashMap<String,String> map = new HashMap<>();

    @Override
    public void onApply(final LogEntry logEntry) {
        //从日志中得到有效数据，也就是指令本身
        final ByteBuffer data = logEntry.getData();
        //接下来就是程序员自己根据业务编写的逻辑，这里我就写成伪代码了
        //比如说这里可以解析这条指令

        1.得到指令本身，是get、put、remove等等
        2.得到key
        3.得到value
        4.对成员变量map执行指令
       
    }


}
```
然后是重构之后的 FSMCallerImpl 类。
```java
//状态机组件的实现类,注意，我这里直接把FSMCallerImpl的接口FSMCaller引入到程序中了
public class FSMCallerImpl implements FSMCaller {


    private static final Logger LOG = LoggerFactory.getLogger(FSMCallerImpl.class);

    //省略其他成员变量
    
    //真正的用户定义的状态机
    private StateMachine fsm;

    //省略其他内容
    

}
```
 这样一来，第 3 个问题就解决了。但是用户自己定义的状态机怎么交给 FSMCallerImpl 对象呢？这就要回到刚才提出的第 1 个和第 2 个问题了。现在先让我们看一下这个 FSMCallerImpl 组件是在哪里被创建，以及什么时候交给 BallotBox 投票箱对象的。这两个问题就更好说了，也没必要卖关子，通过前面那么多章节的学习，大家肯定也都有这样一个认知了：**凡是涉及到组件初始化的操作，必然是在 NodeImpl 对象的 init 方法中执行的。所以，FSMCallerImpl 状态机就是在 NodeImpl 对象的 init 方法中被创建然后初始化的，紧接着就可以交给 BallotBox 投票箱对象使用了**。当然，这里还应该多说一句，因为用户要把自己定义的真正的业务状态机交给 FSMCallerImpl 使用，而 FSMCallerImpl 是在 NodeImpl 对象的 init 方法中创建并且初始化的，所以用户自己定义的业务状态机也是在 NodeImpl 对象的 init 方法中被交给 FSMCallerImpl 的。这就意味着用户自己定义的业务状态机要在 NodeImpl 节点初始化之前就要被创建好。好了，接下来就请大家跟着我的思路一起来看看 FSMCallerImpl 组件的初始化过程吧。请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {

    //省略其他内容


    //投票箱终于引入了
    private BallotBox ballotBox;

    //状态机组件
    private FSMCaller fsmCaller;

    //节点需要的配置参数
    private NodeOptions options;



    //本类最最核心的方法，初始化节点的方法，jraft框架中的所有组件都是在这个方法中初始化的，但是我们这是第一版本代码
    //所以初始化的组件非常少，只有集群内部使用的客户端服务初始化了，还有元数据存储器初始化了
    @Override
    public boolean init(final NodeOptions opts) {

        //省略部分内容


        
        //创建状态机管理器
        this.fsmCaller = new FSMCallerImpl();
        //初始化日志存储组件
        if (!initLogStorage()) {
            LOG.error("Node {} initLogStorage failed.", getNodeId());
            return false;
        }
        //初始化元数据存储器组件
        if (!initMetaStorage()) {
            LOG.error("Node {} initMetaStorage failed.", getNodeId());
            return false;
        }
        //初始化状态机组件
        if (!initFSMCaller(new LogId(0, 0))) {
            LOG.error("Node {} initFSMCaller failed.", getNodeId());
            return false;
        }
        //创建投票箱
        this.ballotBox = new BallotBox();
        //设置投票箱需要的配置参数对象
        final BallotBoxOptions ballotBoxOpts = new BallotBoxOptions();
        //把FSMCallerImpl状态机和状态机回调队列设置到投票箱的配置参数对象中
        ballotBoxOpts.setWaiter(this.fsmCaller);
        //这里把状态机的回调队列也交给投票箱使用了
        ballotBoxOpts.setClosureQueue(this.closureQueue);
        //初始化ballotBox的属性
        if (!this.ballotBox.init(ballotBoxOpts)) {
            LOG.error("Node {} init ballotBox failed.", getNodeId());
            return false;
        }


        //省略其他内容
        
    }


    //初始化状态机组件的方法
    private boolean initFSMCaller(final LogId bootstrapId) {
        if (this.fsmCaller == null) {
            LOG.error("Fail to init fsm caller, null instance, bootstrapId={}.", bootstrapId);
            return false;
        }//创建状态机需要的回调队列，这个队列中封装着需要和业务层面沟通的回调方法
        this.closureQueue = new ClosureQueueImpl(this.groupId);
        //创建状态机初始化需要的参数配置对象
        final FSMCallerOptions opts = new FSMCallerOptions();
        //设置日志管理器组件
        opts.setLogManager(this.logManager);
        //这一步很关键，就是把用户定义的真正的业务状态机设置到参数对象中了
        //注意，这里的状态机是从options中获得的，因为在启动程序的时候，用户需要把状态机设置到
        //options对象中
        opts.setFsm(this.options.getFsm());
        opts.setClosureQueue(this.closureQueue);
        opts.setNode(this);
        //设置状态机的Disruptor环形数组的容量大小
        opts.setDisruptorBufferSize(this.raftOptions.getDisruptorBufferSize());
        //初始化状态机组件，这里我还是再多解释一句，虽然我把fsmCaller直接称为状态机组件
        //其实，是这个组件对用户定义的真正的状态机做了一层包装，确切地说，是fsmCaller内部
        //持有了用户自己定义的状态机，当有日志要被应用到状态机时，会通过fsmCaller调用方法
        //然后进一步把要应用的日志应用到用户定义的状态机中
        return this.fsmCaller.init(opts);
    }


}




//在上面的NodeImpl类中大家可以看到，调用NodeImpl类的init方法时，需要传递一个NodeOptions参数对象
//这个NodeOptions参数对象中就封装着用户自己定义的业务状态机，具体实现可以从我提供的测试类中查看
//这里我只是简单展示一下代码
public class ServerTest {

    private RaftGroupService raftGroupService;

    public ServerTest(final String dataPath, final String groupId, final PeerId serverId,
                      final NodeOptions nodeOptions) throws IOException {

        //根据用户配置的本地路径创建文件夹
        FileUtils.forceMkdir(new File(dataPath));
        //创建RPC服务器
        final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        //设置日志存储文件的本地路径
        nodeOptions.setLogUri(dataPath + File.separator + "log");
        //设置元数据存储文件的本地路径
        nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");

        //可以看到，在这里把用户自己定义的业务状态机对象设置到了NodeOptions对象中，NodeOptions对象又会交给
        //NodeImpl做初始化操作
        //设置用户自己定义的状态机对象
        nodeOptions.setFsm(new MapStateMachine());

        
        //创建集群服务对象并赋值
        this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);

    }


    //省略main方法

}
```
朋友们，到此为止我就把和状态机有关的组件的初始化流程以及相互依赖的关系都讲解完毕了。接下来就该回到正题，看看一批已经达成共识的日志，究竟会经过怎样的流程才会被应用到用户自己定义的业务状态机上。<br />这个还要从一批日志被达成共识开始说起，当一批日志在集群中达成共识之后，这批日志就会被 BallotBox 投票箱对象交给 FSMCallerImpl 状态机去处理，也不能说是交给，应该说是通知 FSMCallerImpl 状态机去处理对应的日志。这时候肯定是通过 BallotBox 对象的 commitAt 方法通知 FSMCallerImpl 把可以提交的日志应用到用户自己定义的业务状态机上。在 BallotBox 对象的 commitAt 方法中调用了 this.waiter.onCommitted(lastCommittedIndex) 这个方法后，程序就会执行到 FSMCallerImpl 对象的 onCommitted 方法中。也就是下面代码块展示的方法，请看下面代码块。
```java
//该方法就是把已经提交成功的日志，应用到状态机中的入口方法   
@Override
public boolean onCommitted(final long committedIndex) {
    return enqueueTask((task, sequence) -> {
        //设置为COMMITTED类型
        task.type = TaskType.COMMITTED;
        //committedIndex是从BallotBox类的commitAt方法中传递过来的参数
        //就是最新被提交的日志的索引
        task.committedIndex = committedIndex;
    });
}
```
可以看到，在上面的方法中，**又调用了 enqueueTask 方法，实际上就是创建了一个 disruptor 组件要使用的数据转换器，把消费者数据放到了 disruptor 的唤醒数组中了。并且消费者数据，也就是 ApplyTask 对象中封装了本批次可以消费最大日志的索引，这样一来，当 disruptpr 组件的批处理器消费数据时，就会判断每一个数据中的 committedIndex 的值是多少，也就知道本次可以把索引为多少的日志和之前的所有日志都应用到状态机上了**。向 disruptor 的环形数组中存放了待消费的数据之后，我们的目光就要转到 disruptor 的消费处理器中了，因为 disruptor 的批处理器会在一个循环中不断执行消费处理器中的方法，这样一来批处理器就会一直消费环形数组中的数据，直到环形数组中没有数据可消费。请看下面代码块。
```java
事件处理器，也就是disruptor框架要用的消费者处理器
private class ApplyTaskHandler implements EventHandler<ApplyTask> {
    boolean firstRun = true;
    //这个是最大的可以应用的日志的索引，初始化为-1，随着日志不断被应用，这个值也会被不断更新
    //注意，这个是消费者处理器中的成员变量，消费者处理器会处理所有要被应用的日志，所以这个成员变量是被所有日志共享的
    //当然，这个共享也是有限制了，就是被同一批处理的日志共享，在每一批新的日志应用的时候，这个值会被重置
    private long maxCommittedIndex = -1
    @Override
    public void onEvent(final ApplyTask event, final long sequence, final boolean endOfBatch) throws Exception {
        //在这里执行每一个异步任务
        this.maxCommittedIndex = runApplyTask(event, this.maxCommittedIndex, endOfBatch);
    
}
```
在上面的消费处理器中，又调用了 runApplyTask 方法，在 runApplyTask 方法中，就会判断本次从环形数组中获取到的数据是否为 TaskType.COMMITTED 类型的，如果是 TaskType.COMMITTED 类型的，就意味着要要把一批日志应用到用户自己定义的业务状态机上了。请看下面代码块。
```java
private long runApplyTask(final ApplyTask task, long maxCommittedIndex, final boolean endOfBatch) {

    //在这里判断task是否为COMMITTED，如果是的话，就给maxCommittedIndex赋值
    if (task.type == TaskType.COMMITTED) {
        if (task.committedIndex > maxCommittedIndex) {
            //这时候maxCommittedIndex就不是-1了
            maxCommittedIndex = task.committedIndex;
        }
        task.reset();
    } 
    
   //其他逻辑省略
    
    //在这里会有一个总的判断，判断当前要消费的数据是不是本批数据中的最后一个，这是disruptor框架中的知识
    //如果是最后一个，并且maxCommittedIndex大于0，说明肯定有一些日志要被应用到状态机上
    //那就直接应用这批日志即可，如果不是本批数据中的最后一个，那就什么也不做
    //除非有其他非COMMITTED类型的任务被提交了，这时候就会在上面的else分支中把还未应用的日志都应用到状态机上
    if (endOfBatch && maxCommittedIndex >= 0) {
        this.currTask = TaskType.COMMITTED;
        //应用日志到状态机上
        doCommitted(maxCommittedIndex);
        //重置maxCommittedIndex为-1
        maxCommittedIndex = -1L;
    }
    this.currTask = TaskType.IDLE;
    return maxCommittedIndex;
    
}
```
当然，不管最后怎么调用，都会调用到 doCommitted 方法，在该方法中，就会把对应的日志应用到用户自己定义的业务状态及上。但是这个 doCommitted 方法我们还没有实现。在之前重构的 FSMCallerImpl 类中，doCommitted 方法是这样定义的，请看下面代码块。
```java
//根据日志索引，将对应的日志应用到状态机中的方法，committedInde是已提交的最新的日志的索引
private void doCommitted(final long committedIndex) 
    //该方法暂时不做实现，总之会在该方法中调用下面的代码
    // this.fsm.onApply()
    //因为现在是FSMCallerImpl类持有logManager日志管理器组件了，可以得到日志条目对象了
    
}
```
这个方法我就不在文章中为大家实现了，当然，本来是应该实现的，但是请允许我为自己辩解几句，首先 doCommitted 方法内容有点长，如果全在文章中展示的话，内容就太多了，还不如让大家直接去看代码。而且，尽管 doCommitted 方法的内容有点长，但是执行流程和逻辑非常常规，和我一开始为大家在 BallotBox 投票箱对象的 commitAt 方法中实现的逻辑是一样的。请大家思考一下，之前在  BallotBox 投票箱对象的 commitAt 方法中，我是不是定义了一个循环？然后根据日志索引，在循环中使用 LogManager 日志管理器组件得到每一个日志条目对象，再把日志应用到业务状态及上。这个逻辑大家应该还有印象。现在我们已经定义了最新的 FSMCallerImpl 组件，并且该组件也持有 LogManager 日志管理器组件。这样一来，**我就可以直接把之前的逻辑直接照搬到 FSMCallerImpl 的 doCommitted 方法中，在该方法中也定义一个循环，然后根据日志索引，在循环中使用 LogManager 日志管理器组件得到每一个日志条目对象，再把日志应用到业务状态及上，这么做显然是很合理的吧**？应该说也是唯一的应用日志到业务状态机上的手段了。当然，在 doCommitted 方法中还有一些其他的操作，比如当日志应用成功之后，每一条日志对应的回调方法也应该被回调，通知客户端操作执行成功了。还有就是判断每一条日志的类型，看看获得的每一条日志是配置变更日志还是业务日志，然后根据日志类型执行不同的操作。等循环结束之后，就意味着这一批日志已经应用到业务状态机上了，这时候就可以更新 FSMCallerImpl 对象的一些成员变量的值了，lastAppliedIndex、lastAppliedTerm 这些成员变量的值都可以更新了。这就是 doCommitted 方法的大概流程。<br />因为都是很常规的流程和逻辑，所以我就不在文章中展示 doCommitted 方法的代码了，并且我还在第七版本代码中也提供了状态机测试类，大家可以直接去第七版本代码中学习。好了朋友们，本章内容就到此结束了，和状态有关的核心功能也都实现完毕了。整个 sofajraft 框架还剩下快照生成、快照安装、线性一致读、配置变更、优雅停机这些知识点。虽然看着是五个知识点，但是这五个知识点中比较重要的也就是线性一致读和配置变更，优雅停机只是一个简单的关闭服务的操作，代码量非常少。快照模块的代码量虽然比较多，操作流程长一些，繁琐一些，但是逻辑非常顺畅，只要掌握了日志传输的功能实现，学习快照生成安装就会非常简单。所以，在这里我要稍微解释一下，在我个人看来，快照功能和优雅停机的重要性是要次于线性一致读和配置变更的，所以接下来我会调整一些文章的顺序，尽管在迭代的代码中，第八和第九版本实现的是快照生成和快照安装功能，第十和第十一版本才是线性一致读和配置变更功能的实现，但是在文章中，我会把第十和第十一版本代码对应的文章提前一下，放到第 22 和 23 章来写。所以下一章讲解的就是线性一致读功能的实现，第 23 章讲解的就是配置变更功能的实现。这两个功能实现完毕了，作为一个 raft 共识算法框架，可以说它提供的核心功能已经全部完善了。剩下的知识就可以慢慢补充了。好了朋友们，我们下一章见
