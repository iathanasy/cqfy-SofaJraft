大家好，在上一章结尾我为我们的程序提出了 7 个问题，就是下面这 7 个。<br />**1 为什么要给每一条日志都创建 Ballot 投票计数器对象，难道不能给每一批日志创建一个唯一的 Ballot 对象吗？**<br />**2 Ballot 对象究竟在哪里被创建呢？**<br />**3 BallotBox 对象究竟是在什么时候被创建的？**<br />**4 BallotBox 对象内部还应该定义什么成员变量和方法？**<br />**5 创建好的 Ballot 对象怎么存放到 BallotBox 对象的内部队列中呢？**<br />**6 BallotBox 对象要怎么应用已被提交的日志呢？肯定是把要被应用的日志交给状态机，但状态机在哪里呢？是怎么实现的呢？**<br />**7 BallotBox 对象内部队列中的 Ballot 投票计数器什么时候被清除呢？**<br />接下来就让我赶快为大家逐一分析一下。

<a name="a2WUt"></a>
## 完善 BallotBox 投票箱，实现日志计数功能
 <br />首先看第一个问题，为什么要给每一条日志都创建一个 Ballot 投票计数器对象，实际上这个问题是针对 sofajraft 框架产生的，因为在 sofajraft 框架中就给每一条日志都创建了一个 Ballot 投票计数器对象。说实话，当时我看到 sofajraft 框架这么做我也十分不解，明明领导者是一批一批地向跟随者发送日志，为什么不为每一批日志创建一个 Ballot 对象呢？领导者处理日志请求的响应时，也是对每一批日志的响应做处理。并且因为有 Pipeline 模式的存在，只要哪一批日志没有传输成功，之后的日志就要全部重新传输。如果响应成功了，就意味着这一批日志都传输成功了，只要成功响应数量超过了集群半数节点，这一批日志就可以直接被应用了。这样分析起来，好像确实没必要给每一条日志创建 Ballot 对象，对吧？<br />但我后来仔细想了想，如果给每一批发送给跟随者节点的日志创建一个 Ballot 对象，这么做根本就行不通。请大家想象一个场景，在 raft 集群中，领导者向跟随者节点发送日志是成批发送的，每一批可发送的日志限制在 1024 条和 512 KB 之内，并且当领导者内部没有日志可以发送的时候，领导者就不会再向跟随者节点发送日志了。那么，假如领导者要向跟随者节点发送一批索引为 1-20 的日志，这批日志并没有达到发送内容的上限，但领导者内部已经没有日志了，所以领导者发送完这批日志之后，就不再发送日志了，当然已经为这批日志创建好了一个 Ballot 对象。但是跟随者节点接收到这批日志之后并没有落盘成功，所以回复给领导者一个复制失败的响应。领导者能做的就是重新传输这批日志，但这个时候，领导者内部又产生了 20 条日志，这一次领导者向跟随者节点传输日志的时候，就可以把索引为 1-40 的日志全部传输给跟随者节点了。既然是一次新的传输，传输的日志数量也和上一次不同，所以肯定也要为这个新的日志传输请求创建一个 Ballot 对象。这时候大家有没有发现什么问题了？没错，我又创建了一个 Ballot 对象，但是第一个 Ballot 对象还在呢？原因很简单，现在仅仅是 raft 集群中的一个节点回复给领导者日志传输失败的响应，其他节点也许都传输成功了呢？如果其他节点都传输成功了，那么这批日志显然就被领导者复制成功了，也就不用再传输索引 1-20 的日志了。但是对于传输失败的跟随者节点，领导者又发送了一个日志传输请求，并且又创建了一个 Ballot 对象。这时候这个 Ballot 对象好像就专属于这一个传输失败的节点了，因为现在领导者传输的这批索引为 1-40 的日志只发送给了一个跟随者节点，也就意味着只会收到 1 个跟随者节点的回复，那还怎么判断传输是成功还是失败了呢？这显然是不合理的。<br />接下来还是请大家回顾一下前面的知识，领导者向跟随者传输日志时，每一个跟随者节点各自拥有的日志进度可能并不相同，所以领导者内部的每一个 Replicator 复制器对象在使用 Pipeline 模式向跟随者传输日志的时候，可能各自传输的日志条目并不相同。比如有的 Replicator 向对应的跟随者节点传输的是索引为 1-5 的日志，有的 Replicator 复制器对象向对应跟随者节点传输的是索引为 21-40 的日志，如果是这种情况，领导者每发送一批日志就为这批日志创建一个 Ballot 对象，那究竟要为哪个 Replicator 复制器对象的请求创建呢？这些请求各不相同，根本不能使用一个 Ballot 对象来统计它们的响应数量。如果领导者内部的众多 Replicator 向跟随者节点批量传输日志是同步的，每一个 Replicator 对象在每个时刻传输的日志都是相同的，确实可以为每一个请求创建 Ballot 对象来判断某批日志是否应该被提交，但很显然让每一个 Replicator 复制器对象日志同步传输是理想情况，并不能用在真实环境中。<br />总之，上面的一大堆分析可以总结为两点：<br />**1 按照请求来创建 Ballot 对象，可能会造成 Ballot 对象重复创建的情况。**<br />**2 raft 集群中各个跟随者节点要接收的日志进度可能并不一致，也就意味着领导者内的每一个 Replicator 对象传输日志的情况并不同步，各自传输的每一批日志的数量并不相同，所以无法为每一批日志创建 Ballot 对象。**<br />我相信大家应该都能理解总结出来的这两点。好了朋友们，**到此为止我们已经解决了第一个问题，在上一章我一直跟大家说在领导者其实可以在发送日志的时候创建 Ballot 对象，然后把这个对象放到 BallotBox 投票箱内部的队列中。但现在经过分析和论证，领导者不能在日志传输的时候为每一个日志请求创建 Ballot 对象**。<br />既然领导者不能为发送的每一个日志请求创建 Ballot 对象，那么我的观点就是，**领导者应该为其内部产生的每一条日志创建一个 Ballot 对象**。当然，本来我们就是这么做的，但是这么做有什么显而易见的好处，大家也许还不太清楚，接下来让我来给大家再举一个简单的例子。<br />假如领导者内部有索引为 1-10 的 10 条日志，并且 为这 10 条日志创建了 10 个 Ballot 投票计数器对象。领导者raft 集群中除了领导者还有四个跟随者节点，分别是节点 1、2、3、4。领导者向节点 1 发送了索引为 1-5 的日志，向节点 2 发送了索引为 1-10 的日志，向节点 3 发送了索引为 1-5 的日志，向节点 4 发送了索引为 1-5 的日志。当领导者接收到了节点 1 和节点 2 的日志传输成功响应后，在领导者内部就会对每一条日志是否被复制成功计票。当节点 1 回复了领导者成功响应之后，这时候索引为 1-5 的日志，每一条日志都收到了 1 票，加上领导者本身也会把索引为 1-10 的日志持久化成功，所以此时索引为 1-5 的日志，每一条日志都有两票了。当节点 2 回复了领导者成功响应之后，索引为 1-5 的日志，每一条日志都有 3 票，已经超过了集群过半节点，这就意味着索引为 1-5 的日志可以被成功提交了。而索引为 6-10 的日志票数显然还不够，索然节点 2 也回复了成功响应，但加上领导者自己的票数，索引为 6-10 的日志，每一条日志才有两票，显然无法成功提交。只要再有一个节点对索引为 6-10 的日志回复了成功响应，那么这一批日志就可以提交了。所以，现在就需要等待领导者继续将索引为 6-10 的日志传输给节点 1、3、4，然后等待成功响应即可。假如领导者向节点 3 传输索引为 6-10 的日志失败了，需要重新传送，这也没关系，重新传输即可。总之，**如果是为每一条日志创建一个 Ballot 投票计数器对象，大家就可以发现，不管领导者怎么向众多跟随者节点传输日志，就算传输的日志条目数量不同，传输的次数不同，只要接收到了成功响应，最后票数超过了集群过半的节点，每一条日志都可以被应用**。这就是为每一条日志创建 Ballot 对象的好处。如果大家理解了这个流程，接下来我想为大家展示一下我重构之后的 BallotBox 类，请看下面代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {



    //这个队列存放的就是为每一个日志条目对象创建的Ballot对象，注意，这里使用了对象池技术
    //详情就不展开了，可以直接去代码中查看，这个对象池在之前已经见过了
    private final SegmentList<Ballot> pendingMetaQueue = new SegmentList<>(false);



    //定义的提交日志的方法，别忘了该方法会在Replicator的onAppendEntriesReturned方法中被调用
    //并且只要该方法被调用了，说明对应的某批日志一定收到了成功响应，那么在该方法中直接给这批日志的
    //每一条日志投票即可
    //该类最核心的方法，提交firstLogIndex和lastLogIndex之间的日志，当然，在提交的时候还要判断这些日志是否收到了集群中过半节点的投票，该方法会被领导者调用
    public void commitAt(final long firstLogIndex, final long lastLogIndex, final PeerId peer) {

        //定义循环的起始位置，起始就是本批次日志的起始索引
        final long startAt = firstLogIndex;
        
        //接下来就是给对应的日志索引投票的逻辑
        Ballot.PosHint hint = new Ballot.PosHint();
        
        //用上面得到的startAt开始便利要投票的日志
        for (long logIndex = startAt; logIndex <= lastLogIndex; logIndex++) {
            
            //根据日志索引从pendingMetaQueue队列中取出每一条日志对应的Ballot投票计数器对象
            final Ballot bl = this.pendingMetaQueue.get((int)logIndex);
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
        //如果这条日志对应的投票计数器收到了足够的票数，就应用这批日志，代码暂且不实现
        //假如这是一批索引为为20-30的日志，经过循环之后发现每一条日志都可以提交，那么lastCommittedIndex的值就为30
        //onCommitted方法就会应用30和30之前的所有还未应用的日志，这个方法暂且不事先
        onCommitted(lastCommittedIndex);
    }

    
}
```
上面重构之后的 commitAt 方法逻辑上并不难理解，或者说方法本身并不难理解，让人觉得困惑的是，该方法重构得并不严谨。按照我们刚才的分析，领导者内部有很多 Replicator 复制器对象，每一个复制器对象向各自对应的跟随者节点传输的日志进度可能各不相同，但我们就假设在领导者内部有一个时刻，各个 Replicator 复制器对象向对应的跟随者节点传输的日志是相同的，比如说向节点 1、2、3、4 传输的都是索引 1-5 的日志。等待片刻后，节点 1、2、3、4 都回复了日志持久化成功的响应。这样一来，在领导者内部，每一个 Replicator 复制器对象都会在 onAppendEntriesReturned 方法中调用 commitAt 方法处理成功响应，也就是给索引为 1-5 的每一条日志投票。可能节点 1 对应的 Replicator 复制器对象先执行了 commitAt 方法，然后是节点 2 对应的 Replicator 复制器对象执行了 commitAt 方法，这时候加上领导者自己本身的票数(领导者在那里投票呢？这个很快就会讲到)，索引为 1-5 的日志已经收获了足够的票数，所以就会执行 commitAt 方法中的 onCommitted 方法，应用索引 5 和索引 5 之前的所有未被应用的日志。**但是请大家别忘了，节点 3 和节点 4 对应的 Replicator 复制器对象还要接着执行 commitAt 方法呢？可这时候索引 1-5 的日志已经被提交，或者说被应用了。当节点 3 对应的 Replicator 又一次执行 commitAt 方法时，会在方法中发现索引 1-5 的日志票数足够了，于是又会调用 onCommitted 方法，把索引为 5 和之前的日志重新应用一遍.而之后节点 4 对应的 Replicator  对象又会重复节点 3 的流程**。显然，这么做就出问题了，相同的日志怎么能被重复应用好几次呢？<br />那要怎么解决上面的问题呢？我的观点是，**应该在 BallotBox 类中定义一个新的成员变量，就定义为一个 long 整数，叫做 pendingIndex，翻译过来就是下一条要提交的日志索引，当然还可以定义另一个成员变量，也是个 long 整数，叫做 lastCommittedIndex，翻译过来就是最后一条提交的日志索引**。这个成员变量可以先预留着，以后没准就用的上了。**当定义好 pendingIndex 成员变量后，可以在每应用完一批日志之后，更新 pendingIndex 的值。这个值初始化为 0，当领导者应用了索引为 1-5 的日志后，就可以把这个 pendingIndex 更新为 6，代表领导者下一条要应用的日志索引为 6。如果过了一会领导者有接收到一个成功响应，这个响应是索引为 1-5 的日志的，这时候就可以用这批日志的最后一条日志的索引和 pendingIndex 做对比，5 显然小于 6，这就意味着现在收到的响应是一个无效，或者说过期响应，因为索引为 1-5 的日志已经被应用了，再接收到这批日志的成功响应就可以不必再处理了**。**并且，我们还可以再执行另外一个操作，那就是当领导者已经决定应用索引为 1-5 的日志了，就可以把该批日志对应的 5 个 Ballot 对象从队列中移除了**。原因很简单，日志都要被应用了，还留着对应的 Ballot 对象干什么呢？如果这个逻辑大家都明白了，接下来请看我再次重构之后的 BallotBox 类。请看下面代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);


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


        //这里肯定是需要上锁的，因为领导者的投票箱对象只有一个，但是这一个投票箱对象要被所有的复制器对象使用
        //而每一个复制器对象都有一个自己的单线程执行器来处理任务，也就会调用到该方法，显然是并发的情况，所以应该上锁
        final long stamp = this.stampedLock.writeLock();

        //定义一个局部变量，记录最后提交的日志的索引
        long lastCommittedIndex = 0;


        try {
            
            //要提交的下一个索引为0则直接退出该方法
            //这里的判断很值得思考一下
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

            
            
             //定义循环的起始位置，起始就是本批次日志的起始索引
            final long startAt = firstLogIndex;
            
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
        
        //如果这条日志对应的投票计数器收到了足够的票数，就应用这批日志，代码暂且不实现
        //假如这是一批索引为为20-30的日志，经过循环之后发现每一条日志都可以提交，那么lastCommittedIndex的值就为30
        //onCommitted方法就会应用30和30之前的所有还未应用的日志，这个方法暂且不事先
        onCommitted(lastCommittedIndex);
        return true;
    }

    
}
```
到此为止，这个 BallotBox 类重构得也就差不多了，日志计数的功能也完善了，并且对于上面提出的 7 个问题，已经有两个得到了解决，分别是第 1 个和第 7 个。我们已经明确要给领导者中的每一条日志都创建一个 Ballot 对象了，并且这些存放在队列中的 Ballot 对象，会在日志达到被应用的条件后从队列中清除。接下来还有 5 个问题等待我们去解决。当然，还是没必要着急，在解决其余的 5 个问题之前，让我们先看看上面代码块中的第 46-48 行代码，这几行代码是一个条件判断，就是判断 pendingIndex 是否为 0，如果为零就可以直接退出 commitAt 方法。那么，为什么要进行这个判断呢？这个 pendingIndex 的默认值为 0，但是在上面的方法中却判断了一下，只有该值不为 0 的时候，才能顺利执行程序。请大家想一想，**这是不是就意味着在调用 commitAt 方法之前，一定在某个地方把 BallotBox 类中的 pendingIndex 成员变量的值给更新了**。那么什么时候更新了这个值呢？为什么要更新这个值呢？请大家想一想，将一批日志提交是谁具备的功能和职责？毫无疑问就是领导者，当一个 raft 集群中所有节点都不是领导者的情况下，这些节点内部也都没有日志，这时候 pendingIndex 就是默认值，也就是 0。而当集群中的一个节点成为领导者之后，就要产生日志了，也就要提交日志了，提交的第一条日志的索引肯定为 1，**所以当一个节点成为集群的领导者之后，就会立刻把 BallotBox 类的 pendingIndex 成员变量的值更新为 1。这也就意味着，BallotBox 对象在节点刚启动的时候就创建了，这样一来，当节点成为领导者之后，就可以直接修改 BallotBox 内部 pendingIndex 成员变量的值**。经过这个分析，上面的第 3 个问题也就解决了，**BallotBox 对象就是在节点刚启动的时候创建的。**而节点的启动流程是在 NodeImpl 类的 init 方法中执行的，所以现在我们的逻辑就该来到 NodeImpl 类中。

<a name="GaT7Q"></a>
## 重构 NodeImpl 类，完善 NodeImpl 类中的部分细节

让我们把目光拉回到 NodeImpl 类中，当节点启动的时候，会创建一个 NodeImpl 对象，然后执行 NodeImpl 对象的 init 方法。刚才我们已经明确了，BallotBox 对象就是在节点启动的时候创建的，所以我现在想把 BallotBox 对象定义为 NodeImpl 类的一个成员变量，然后在 NodeImpl 的 init 方法中创建这个 BallotBox 对象。当然，别忘记当一个节点成为领导者之后，要给这个 BallotBox 对象的 pendingIndex 赋值，所以 NodeImpl 类可以重构成下面这样。请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {

    //省略其他内容


    //投票箱终于引入了
    private BallotBox ballotBox;



    //本类最最核心的方法，初始化节点的方法，jraft框架中的所有组件都是在这个方法中初始化的，但是我们这是第一版本代码
    //所以初始化的组件非常少，只有集群内部使用的客户端服务初始化了，还有元数据存储器初始化了
    @Override
    public boolean init(final NodeOptions opts) {

        //省略部分内容


         //初始化元数据存储器组件
        if (!initMetaStorage()) {
            LOG.error("Node {} initMetaStorage failed.", getNodeId());
            return false;
        }//初始化状态机组件，这个状态机组件暂且不必关注
        if (!initFSMCaller(new LogId(0, 0))) {
            LOG.error("Node {} initFSMCaller failed.", getNodeId());
            return false;
        }
        //初始化投票箱
        this.ballotBox = new BallotBox();

        
    }




    //当前节点成功获得超过集群半数节点的投票后，就会调用该方法，让自己成为领导者
    private void becomeLeader() {
        //省略部分内容

        
        //遍历配置信息中的节点
        for (final PeerId peer : this.conf.listPeers()) {
            //如果遍历到的节点就是当前节点，就直接跳过这次循环
            if (peer.equals(this.serverId)) {
                continue;
            }
            //接下来要向复制器组中添加复制器对象了，记录一条日志
            LOG.debug("Node {} add a replicator, term={}, peer={}.", getNodeId(), this.currTerm, peer);
            //如果添加复制器失败，记录错误日志
            //这里有一点非常重要，在下面这个addReplicator方法中，跟随着节点就会被包装成一个个复制器对象
            //然后把每一个复制器对象添加到这个复制组器对象中，并且在下面这个方法内部，还会启动心跳定时器，以及发送探针请求
            //这时候，领导者就会向集群中的所有跟随者发送信息了
            if (!this.replicatorGroup.addReplicator(peer)) {
                LOG.error("Fail to add a replicator, peer={}.", peer);
            }
        }
        
        
        
        //这里的这行代码非常重要，设置投票箱要提交的下一条日志的索引
        //如果整个集群是第一次启动，就会在这里把ballotBox内部的pendingIndex的值设置为1
        this.ballotBox.resetPendingIndex(this.logManager.getLastLogIndex() + 1);

        
        //刷新配置
        this.confCtx.flush(this.conf.getConf(), this.conf.getOldConf());
    }


    //省略其他内容
}
```
当然，BallotBox 类中也要新添加一个方法，那就是 resetPendingIndex 方法。具体实现请看下面代码块。
```java
//该类的对象就是一个投票箱
@ThreadSafe
public class BallotBox implements Lifecycle<BallotBoxOptions>, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(BallotBox.class);


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


    //省略部分内容




    //设置领导者要提交的下一条日志的索引
    public boolean resetPendingIndex(final long newPendingIndex) {
        //上锁
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
            //在这里给pendingIndex赋值了
            this.pendingIndex = newPendingIndex;
            return true;
        } finally {
            this.stampedLock.unlockWrite(stamp);
        }
    }

    
}

```
上面两个代码块的逻辑非常简单，所以我就不再重复讲解了。接下来，让我们思考另一个至关重要的问题，那就是每一个 Ballot 对象要在哪里创建呢？以及应该怎么存放到 BallotBox 对象的队列中呢？这两个问题其实也就是之前提出的 7 个问题中的第 2 个和第 5 个问题。我之所以在这里才引导大家思考这两个问题也是有原因的，因为这个时候我们已经把 BallotBox 对象定义在 NodeImpl 类中了，换句话说，我们已经可以在 NodeImpl 类中直接得到 BallotBox 对象了。那么，为什么能够在 NodeImpl 类中直接得到 BallotBox 对象如此重要呢？请大家想一想，当我们解决了上面  7 个问题的第一个问题时，就已经明确了要为领导者内部的每一条日志都创建一个 Ballot 对象，既然是为每一条日志都创建 Ballot 对象，最直接最简单的做法是什么？**是不是就是在领导者将每一条客户端指令包装成日志的时候，直接为每一条日志创建 Ballot 对象**？因为那个时候日志刚刚生成，我们完全可以做到每生成一条日志就创建一个 Ballot 对象，然后把这个 Ballot 对象存放到 BallotBox 对象内部队列中。**这也就意味着我们应该当领导者内部日志刚刚生成的时候，就为每一条日志创建 Ballot 对象，创建成功之后，就可以直接把 Ballot 对象存放到 BallotBox 对象的内部队列中**。那么领导者内部的日志是怎么被创建的呢？具体实现就是在 NodeImpl 类的 executeApplyingTasks 方法中，这个是前几章的知识，大家可以回顾一下前几章的知识。**而现在 NodeImpl 又可以直接获得 BallotBox 对象了，所以就可以直接在 executeApplyingTasks 方法中为每一条新创建的日志创建一个 Ballot 对象，然后把 Ballot 对象交给 BallotBox 对象内部的队列**。具体实现请看下面代码块(下面代码块中 54-57 行的注释一定要重新看一下)。
```java
public class NodeImpl implements Node, RaftServerService {

    //省略其他内容


    //投票箱终于引入了
    private BallotBox ballotBox;



    //本类最最核心的方法，初始化节点的方法，jraft框架中的所有组件都是在这个方法中初始化的，但是我们这是第一版本代码
    //所以初始化的组件非常少，只有集群内部使用的客户端服务初始化了，还有元数据存储器初始化了
    @Override
    public boolean init(final NodeOptions opts) {

        //省略部分内容


         //初始化元数据存储器组件
        if (!initMetaStorage()) {
            LOG.error("Node {} initMetaStorage failed.", getNodeId());
            return false;
        }//初始化状态机组件，这个状态机组件暂且不必关注
        if (!initFSMCaller(new LogId(0, 0))) {
            LOG.error("Node {} initFSMCaller failed.", getNodeId());
            return false;
        }
        //初始化投票箱
        this.ballotBox = new BallotBox();

        
    }



    //批处理日志的方法，其实在这个方法中也只不过是做了一点点校验，然后就设置了一下日志的任期和类型
    private void executeApplyingTasks(final List<LogEntryAndClosure> tasks) {

        //加锁
        this.writeLock.lock();

        //省略部分内容

        try {//这个tasks集合中存放着32个LogEntryAndClosure对象
           //每个LogEntryAndClosure对象中存放着一个logEntry对象
           final int size = tasks.size();
           
            //在这里创建的这个集合专门存放每一条日志
            final List<LogEntry> entries = new ArrayList<>(size);
            
            for (int i = 0; i < size; i++) {
                
                
                //下面这段代码是为每一个日志条目对象创建了Ballot对象，并且把每一个日志条目对应的回调方法放到了
                //投票箱的回调队列中，投票箱使用的回调队列，实际上就是状态机组件持有的回调队列，这个要搞清楚
                //task.done 得到的就是一个回调对象，该对象中封装着一个回调方法，为什么这里要传进去一个回调方法呢？
                //这个回调方法又是什么呢？答案就在这个代码块得后半部分，请大家先看一下当前代码块得78-80行的注释
                if (!this.ballotBox.appendPendingTask(this.conf.getConf(),
                        this.conf.isStable() ? null : this.conf.getOldConf(), task.done)) {
                    //如果没有创建成功成功，则直接通知业务层操作失败
                    ThreadPoolsFactory.runClosureInThread(this.groupId, task.done, new Status(RaftError.EINTERNAL, "Fail to append task."));
                    task.reset();
                    continue;
                }
                
                
                
                //为每一个日志条目设置任期
                task.entry.getId().setTerm(this.currTerm);
                //设置日志条目类型，从这个方法中设置设置的日志条目对象都是业务日志
                task.entry.setType(EnumOutter.EntryType.ENTRY_TYPE_DATA);
                //得到了每一条日志，并且把日志添加到集合中
                entries.add(task.entry);
                //这里也是为了循环利用Event
                task.reset();
            }
            
            //将设置好的日志交给日志管理器处理，在日志管理器中，这些日志将会被设置任期，然后进行落盘的操作
            //这里的回调是当日志落盘成功后，直接判断是否可以在领导者提交了，这里判断提交的条件是看看每一条日志是否
            //得到了集群中过半节点的投票，这里大家肯定注意到了LeaderStableClosure这个回调对象
            //经过前面几章得学习，大家应该还记得，这个回调对象中封装的方法会在日志持久化成功后被回调
            //而这个回调对象的代码我也展示在下面了
            this.logManager.appendEntries(entries, new LeaderStableClosure(entries));
           
        } finally {
            this.writeLock.unlock();
        } 
        
    }






    //该回调方法中的逻辑是用来判断当前日志是否可以被提交，然后应用到状态机上了
    class LeaderStableClosure extends LogManager.StableClosure {

        public LeaderStableClosure(final List<LogEntry> entries) {
            super(entries);
        }

        @Override
        public void run(final Status status) {
            if (status.isOk()) {
                //在这里把日志应用到状态机上，注意，这里是领导者在调用这个方法，当领导者内部的日志持久化成功后，领导者就会回调这个方法
                //然后领导者的日志要应用到状态机上了，但是请大家仔细想想，领导者日志能应用到状态机上，实际上是要等到日志在集群各个节点中达成共识之后才行
                //领导者将日志落盘成功之后，未必就能接收到集群中其他各个节点对这条日志的成功响应，但这里仍然执行了BallotBox的commitAt方法
                //真正的目的其实就是领导者给自己的日志投一票而已，别忘了在commitAt方法中要先给日志投票，然后再判断日志是否拥有足够的票数，最后才决定是否应用日志
                //所以这里其实真正的作用就是领导者直接给自己的日志投了一票而已
                //到这里，大家应该就明白了领导者是怎么给自己的日志投票的吧？
                NodeImpl.this.ballotBox.commitAt(this.firstLogIndex, this.firstLogIndex + this.nEntries - 1,
                        NodeImpl.this.serverId);
                System.out.println("领导者自己把业务日志写入数据库成功！！！！！！！！！！！！！");
            } else {
                LOG.error("Node {} append [{}, {}] failed, status={}.", getNodeId(), this.firstLogIndex,
                        this.firstLogIndex + this.nEntries - 1, status);
            }
        }
    }


    //省略其他内容
}
```
在上面代码块的第 56 行，程序执行了 BallotBox 对象的 appendPendingTask 方法，在这个方法内部，就会为每一个日志创建一个 Ballot 对象。这里可能会有一些朋友感到奇怪，为什么在执行 appendPendingTask 方法的时候，还要传进去一个回调对象，task.done 得到的就是一个回调对象，该对象中封装着一个回调方法。为什么这里要传进去一个回调方法呢？这个回调方法又是什么呢？我相信每个朋友都有这个疑问。所以我就直接解释一下，**当客户端向领导者发送一条指令的时候，往往都会定义一个回调方法，这个回调方法也会一起传递给领导者，当客户端指令执行成功之后，这个方法就会被回调通知业务层操作成功了，这个知识大家应该还有印象，也是之前讲解过的了。也就是说，每一条客户端指令就会对应一个回调方法，而这个回调方法最终会在指令被应用之后回调。所谓指令被应用实际上就是包装指令的日志被应用，而包装指令的日志被应用的操作是在 BallotBox 对象中执行的。一旦日志被成功应用了，就可以直接回调每一条日志包装的指令的回调方法了，所以在为每一条日志创建 Ballot 对象的时候，也要把每一条日志对应的回调方法交给 BallotBox 对象保管，方便在日志被应用后直接回调。因此，这里把每一条指令对应的回调方法直接传递到 appendPendingTask 方法中，交给 BallotBox 对象来保管了**。<br />那么 BallotBox 对象怎么保管每一条指令对应的回调方法呢？实际上，在 BallotBox 对象中还定义了一个成员变量，那就是 ClosureQueue 类型的队列，这个队列内部有一个泛型为 Closure 的集合，这个集合专门存放每一条客户端指令对应的回调对象，回调对象中就封装着回调方法。因此，BallotBox 类又要简单重构一下。这次重构就要引入 ClosureQueue 类型的成员变量，以及实现完毕的 appendPendingTask 方法。请看下面代码块。
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


    //省略部分内容




    //该方法的作用很重要，但是逻辑很简单，就是给每一个日志条目创建一个Ballot对象，Ballot对象是投票计数器，同时把每一个日志条目的回调方法存放到closureQueue队列中
    //存放到这个队列中，就意味着状态机组件可以使用这个队列中的数据了
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

    

    
}
```
到此为止，上面提出来的 7 个问题中的第 1 个，第 2 个，第 3 个，第 5 个，第 7 个都已经解决了，并且在解决的过程中，还顺带解决了很多衍生出的新问题。现在我再把之前的 7 个问题展示在下面，大家可以看看这几个问题，是不是有些已经解决了。<br />之前我为大家提出的 7 个问题：<br />**1 为什么要给每一条日志都创建 Ballot 投票计数器对象，难道不能给每一批日志创建一个唯一的 Ballot 对象吗？**<br />**2 Ballot 对象究竟在哪里被创建呢？**<br />**3 BallotBox 对象究竟是在什么时候被创建的？**<br />**4 BallotBox 对象内部还应该定义什么成员变量和方法？**<br />**5 创建好的 Ballot 对象怎么存放到 BallotBox 对象的内部队列中呢？**<br />**6 BallotBox 对象要怎么应用已被提交的日志呢？肯定是把要被应用的日志交给状态机，但状态机在哪里呢？是怎么实现的呢？**<br />**7 BallotBox 对象内部队列中的 Ballot 投票计数器什么时候被清除呢？**<br />现在就剩下第 4 个和第 6 个问题没有解决，实际上第 4 个问题已经解决的差不多了。因为 BallotBox 类已经重构了很多次，引入了很多成员变量和方法，已经重构得差不多了，要说最后还差一个什么成员变量，这就要和第 6 个问题联系上了。第 6 个问题就是怎么应用日志，而经过上面得讲解，我们都知道日志应该交给状态机应用，应用日志的操作是在 BallotBox 对象中执行的，所以 BallotBox 对象势必得持有状态机的引用，因此肯定要把状态机也定义为 BallotBox 类的成员变量。如果这个功能实现了，那么 BallotBox 类就重构完整了。但这一章我不想再讲解这个知识，实现这个功能了，这一章得最终目标就是为大家把状态机引出来，至于怎么实现，怎么使用，就留到下一章讲解吧。状态机还要讲解很多知识，这一章肯定讲不完了。好了诸位，我们下一章见！
