大家好，在这一章，我将为大家实现 sofajraft 框架的领导者与跟随者之间的心跳检测功能。在上一章我已经为大家引入了 Replicator 复制器对象，**当 raft 集群中的一个节点成功当选为领导者之后，该领导者会将集群中的每一个节点都包装成 Replicator 复制器，也就是说，每一个 Replicator 复制器对象对应着集群中的一个跟随者节点，领导者通过每一个复制器对象，来跟集群中的其他跟随者节点通信。当然，所谓的通信也就是进行心跳检测和日志传输**。在上一章的最后一小节，我已经为大家展示了 Replicator 复制器对象的代码，代码还比较简陋，其中最重要的的两个方法 sendEmptyEntries 和 onHeartbeatReturned 方法都还没有为大家实现。这两个方法都和心跳检测有关，这一章我就先为大家实现这两个方法，为大家讲解完整的领导者心跳检测流程。

<a name="fH5Tl"></a>
## 重构Replicator，实现心跳检测

还是老规矩，在正式开始实现 sendEmptyEntries 和 onHeartbeatReturned 这两个方法之前，先把上一章为大家实现的 Replicator 类搬运到这里。大家先简单回顾一下，然后我就开始为大家实现这两个方法。请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator{


    private static final Logger LOG = LoggerFactory.getLogger(Replicator.class);

    //RPC客户端，大家可以顺着构造方法，看看这个成员变量被赋值的逻辑
    //因为要在该类的对象中执行心跳检测和日志复制等工作，肯定需要RPC客户端发送请求啊
    private final RaftClientService rpcService;

    
    //全局定时任务管理器，也就是定时任务调度器，这个定时任务管理器会提交发送心跳信息的定时任务
    private final Scheduler timerManager;

	//这个其实就可以看成一个句柄，复制器对象的引用被ThreadId对象持用
    //该对象会交给复制器组管理
    protected ThreadId id;
    
    //下面两个是配置参数对象，这个成员变量是用来创建Replicator对象的
    private final ReplicatorOptions options;
    
    private final RaftOptions raftOptions;

    //领导者最后一次发送信息的时间
    private volatile long lastRpcSendTimestamp;
    
    //心跳计数器，代表发送了多少次心跳请求
    private volatile long heartbeatCounter = 0;
    
    //心跳定时器
    private ScheduledFuture<?> heartbeatTimer;



    //构造方法
    public Replicator(final ReplicatorOptions replicatorOptions, final RaftOptions raftOptions) {
        this.options = replicatorOptions;
        this.timerManager = replicatorOptions.getTimerManager();
        this.raftOptions = raftOptions;
        this.rpcService = replicatorOptions.getRaftRpcService();
    }


    //该类的核心方法，在该方法中创建了复制器对象
    public static ThreadId start(final ReplicatorOptions opts, final RaftOptions raftOptions) {
        
        //创建复制器对象，这时候这两个配置参数对象就派上用场了
        final Replicator r = new Replicator(opts, raftOptions);
        
        //在这里远程连接一下这个复制器对象对应的节点，连接失败返回null
        if (!r.rpcService.connect(opts.getPeerId().getEndpoint())) {
            LOG.error("Fail to init sending channel to {}.", opts.getPeerId());
            return null;
        }
        
        //创建ThreadId句柄对象，这个对象持有了刚才创建的复制器对象
        //ThreadId类马上就会展示给大家
        r.id = new ThreadId(r, r);
        
        //这里的代码很有意思，直接解释的话就是给当前复制器对象上了一个锁
        //上锁期间，其他任何线程都不能操纵当前的复制器对象
        //这个上锁和释放锁的操作在复制器对象中非常常见，比如说发送心跳消息呢
        //肯定不能多个线程都在操纵这个复制器对象，来向集群中对应的节点发送心跳消息
        //所以一会大家就会看到，在发送心跳消息的时候是怎么上锁和释放锁的
        //这里上锁之后，会在发送完探针消息之后把锁释放了
        r.id.lock();
        
        //记录一条日志
        LOG.info("Replicator={}@{} is started", r.id, r.options.getPeerId());
        
        //记录最后一次发送信息的时间
        r.lastRpcSendTimestamp = Utils.monotonicMs();
        
        //启动心跳定时器，这个定时器会定时向其他节点发送心跳消息
        //这里就启动了心跳任务定时器，开始向对应的跟随者节点发送心跳消息了
        r.startHeartbeatTimer(Utils.nowMs());

        //这里还省略了一个发送探针消息的操纵
        
        //返回ThreadId对象
        return r.id;
    }



    //启动心跳定时器的方法
    private void startHeartbeatTimer(final long startMs) {
        final long dueTime = startMs + this.options.getDynamicHeartBeatTimeoutMs();
        try {//使用全局定时任务管理器提交一个定时任务，在这个任务中执行发送心跳消息的请求
            //可以看到，这里其实就是用timerManager提交了一个定时任务
            //具体细节，大家可以去看我提供的第一版本代码，总之，在这个定时任务中，会调用sendEmptyEntries方法
            //发送心跳消息给对应的跟随者节点
            this.heartbeatTimer = this.timerManager.schedule(() -> onTimeout(this.id), dueTime - Utils.nowMs(),
                    TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            LOG.error("Fail to schedule heartbeat timer", e);
            onTimeout(this.id);
        }
    }
    

    //发送心跳消息的方法
    private void sendEmptyEntries(){


        
    }


    

    //收到心跳消息的响应之后，会调用这个方法来处理响应，这个方法
    static void onHeartbeatReturned(){

        
    }
      
}
```
现在，让我们先把注意力集中在 sendEmptyEntries 方法上，也就是发送心跳消息的方法。请大家想一想，心跳消息应该负责什么？换句话说，什么信息应该封装到领导者的心跳消息中，然后发送给集群中的其他所有节点呢？**我首先想到的是领导者节点本身的信息，也就是领导者节点的 IP 地址和端口号这些信息应该封装在心跳信息中，因为领导者当选之后，需要将自己节点信息告诉集群中的其他节点，其他节点才能知道集群中现在的领导者是谁**。所以，领导者节点的信息应该随着心跳信息一起发送给集群中的其他节点。<br />第二个要封装的信息，就是领导者的任期。也许有朋友会对这个感到疑惑，为什么领导者的任期还需要封装到心跳消息中，因为当集群中的一个跟随者触发了超时选举后，会将自己的身份更新为候选者，将自己的任期自增 1，然后发送投票请求给集群中的其他节点。集群中的其他节点在接收到候选者发送过来的索要投票请求后，也会将自己的任期更新为候选者的任期。这样一来，集群中其他节点的任期实际上在领导者选举的过程中已经更新过了，如果一个候选者将自己的任期更新为 2，然后向集群中的其他跟随者节点发送了索要投票的请求，集群中的其他跟随者节点的任期也会更新为 2，当这个候选者当选为领导者后，其任期就为 2，而集群中其他跟随者节点的任期也为 2，新当选的领导者似乎没必要再发送自己的任期给集群中的跟随者节点了。看起来是这样的，对吧？<br />但是，请大家思考一个场景，集群中共有 5 个节点，1 个成为了候选者，其他 4 个为跟随者节点。假如这个候选者节点在选举领导者的时候，因为网络波动，只给 3 个跟随者节点发送投票请求成功了，剩下的那一个跟随者节点根本就没有接收到投票请求，也就不会把自己的任期更新为候选者的任期。如果是这种情况，那么这一个跟随者节点只能等待领导者发送心跳消息，让领导者在心跳信息中封装自己的任期值，然后让集群中的跟随者跟新。<br />当然，还有另一种情况，那就是集群中发生了网络分区，出现了两个领导者，当网络分区结束之后，这两个领导者肯定会收到彼此的心跳消息，如果心跳消息中封装着各自的任期，这时候就可以通过对比这两个领导者的任期，谁的任期小，谁就应该执行身份降级方法，让自己成为跟随者节点。总之，将领导者的任期封装在心跳消息中是很有必要的。<br />除此之外呢？还应该把什么信息封装到心跳请求中呢？**考虑到一个服务器可以部署多个 raft 节点，这些 raft 节点可能属于不同的集群，所以集群 Id 也应该封装到心跳请求中，目标节点，也就是要把心跳信息发送给对应的跟随者节点的信息也要封装在心跳信息中**，除此之外，在第一版本代码中，似乎不用再封装什么信息了，但是在 sofajraft 框架的完整实现中，还另外封装了 3 个非常重要的参数。分别是领导者本地日志的 prevLogIndex、PrevLogTerm、CommittedInde。**prevLogIndex 代表的是领导者本地日志的倒数第二条日志的索引，PrevLogTerm 代表的是领导者本地日志倒数第二条日志的任期，CommittedInde 代表的是领导者最新应用的日志索引**。<br />我相信有的朋友可能已经明白上面三个重要参数的作用和意义，但第一次接触 raft 共识算法的朋友可能还不太明白，所以接下来让我来为大家讲解一下。首先来看 CommittedInde 的重要作用，这个很好说，在本套课程的前三章我就为大家分析过，跟随者节点要等待领导者提交了日志之后，才能去应用对应的日志，这就需要领导者把它最新应用的日志索引，说的严谨一点，就是把它最新提交的日志索引传输给跟随者，这样跟随者就可以根据这个索引来应用对应的日志了。在前三章的时候，我跟大家讲解的是在领导者每一次向跟随者传输日志的时候，把最新应用的日志索引传输给跟随者。在 sofajraft 框架中确实是这么做的。但是，只靠这种方式把领导者应用的最新日志索引传输给跟随者，**如果领导者一直没有接收到新的日志，岂不是就不能把应用日志的索引传输给跟随者了？所以在 sofajraft 框架中，还做了这样一个操作，那就是当领导者给跟随者发送心跳消息的时候，会把领导者最新应用的日志索引封装到心跳请求中发送给跟随者，这样一来，跟随者就知道自己可以应用的最新日志索引了，然后直接应用对应日志即可**。这就是在心跳请求中封装 CommittedInde 的作用。<br />接下来，我们再来看看 prevLogIndex 和 PrevLogTerm 的作用，**相对于领导者本地最新要发送给跟随者的日志，注意，是最新要发送给跟随者的日志，这两个代表的是要发送的最新日志的前一条日志的索引和任期**。日志的索引和任期可以用来确定唯一一条日志，也可以这么说，在一个确定的任期中，这条日志是唯一的。那么，将这两个参数信息传递给跟随者节点的作用是什么呢？很简单，**既然 prevLogIndex 和 PrevLogTerm 可以确定唯一一条日志，当跟随者节点接收到心跳请求后，就会根据请求中的这两个值判断自己本地有没有相应的日志**。那跟随者节点为什么要判断有没有对应的日志呢？请让我给大家举一个很简单的例子，**假如领导者现在的任期是 3，最新要发送给跟随者的日志的索引为 25，也可以说下一条要发送给跟随者的日志的索引为 25**，当领导者向跟随者发送心跳消息的时候，就会把要发送的下一条日志的前一条日志的索引和任期发送给跟随者，也就是索引为 24，任期为 3 的日志信息发送给跟随者。跟随者节点在接收到心跳消息之后，会从心跳消息中获得索引 24，和任期 3，然后判断自己本地有没有任期为 3，索引为 24 的日志，如果存在，**就说明现在的跟随者复制日志的进度是紧紧跟随着领导者的。领导者要发送的下一条日志索引为 25，跟随者要接受的下一条日志索引也恰好为 25，这是我们最希望出现的情况**。如果跟随者本地并不存在对应的日志，这就意味着日志有丢失，或者是跟随者的日志和领导者的日志有冲突，需要和领导者重新对接。这里的具体处理方式我就不为大家展开了，这些都是日志传输模块的知识，后面总会讲到，实际上，prevLogIndex 和 PrevLogTerm 这两个参数的作用也没必要在本章为大家讲解，等到第 11 章为大家实现日志传输操作的时候，大家就全明白了。<br />总之，现在大家可以先对心跳消息中额外封装的三个参数混个眼熟，等日志传输和日志应用的时候还会用到，到此为止，发送心跳消息的流程也就分析得差不多了，封装好心跳请求其实就可以直接把心跳消息发送给跟随者节点了。当然，**领导者要给跟随者发送的心跳消息实际上也是一个请求，我就把它定义为了 AppendEntriesRequest，有了请求之后，还要将刚才我们分析的一些数据封装到请求中，所以我为此有定义了一个方法，就叫做 fillCommonFields，该方法的作用就是将一些公共数据封装到请求中**。好了，接下来就让我们看看 Replicator 的 sendEmptyEntries 方法具体怎么实现吧。请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator{


    private static final Logger LOG = LoggerFactory.getLogger(Replicator.class);

    //RPC客户端，大家可以顺着构造方法，看看这个成员变量被赋值的逻辑
    //因为要在该类的对象中执行心跳检测和日志复制等工作，肯定需要RPC客户端发送请求啊
    private final RaftClientService rpcService;

    
    //全局定时任务管理器，也就是定时任务调度器，这个定时任务管理器会提交发送心跳信息的定时任务
    private final Scheduler timerManager;

	//这个其实就可以看成一个句柄，复制器对象的引用被ThreadId对象持用
    //该对象会交给复制器组管理
    protected ThreadId id;
    
    //下面两个是配置参数对象，这个成员变量是用来创建Replicator对象的
    private final ReplicatorOptions options;
    
    private final RaftOptions raftOptions;

    //领导者最后一次发送信息的时间
    private volatile long lastRpcSendTimestamp;
    
    //心跳计数器，代表发送了多少次心跳请求
    private volatile long heartbeatCounter = 0;
    
    //心跳定时器
    private ScheduledFuture<?> heartbeatTimer;



    //构造方法
    public Replicator(final ReplicatorOptions replicatorOptions, final RaftOptions raftOptions) {
        this.options = replicatorOptions;
        this.timerManager = replicatorOptions.getTimerManager();
        this.raftOptions = raftOptions;
        this.rpcService = replicatorOptions.getRaftRpcService();
    }


    //该类的核心方法，在该方法中创建了复制器对象
    public static ThreadId start(final ReplicatorOptions opts, final RaftOptions raftOptions) {
        
        //创建复制器对象，这时候这两个配置参数对象就派上用场了
        final Replicator r = new Replicator(opts, raftOptions);
        
        //在这里远程连接一下这个复制器对象对应的节点，连接失败返回null
        if (!r.rpcService.connect(opts.getPeerId().getEndpoint())) {
            LOG.error("Fail to init sending channel to {}.", opts.getPeerId());
            return null;
        }
        
        //创建ThreadId句柄对象，这个对象持有了刚才创建的复制器对象
        //ThreadId类马上就会展示给大家
        r.id = new ThreadId(r, r);
        
        //这里的代码很有意思，直接解释的话就是给当前复制器对象上了一个锁
        //上锁期间，其他任何线程都不能操纵当前的复制器对象
        //这个上锁和释放锁的操作在复制器对象中非常常见，比如说发送心跳消息呢
        //肯定不能多个线程都在操纵这个复制器对象，来向集群中对应的节点发送心跳消息
        //所以一会大家就会看到，在发送心跳消息的时候是怎么上锁和释放锁的
        //这里上锁之后，会在发送完探针消息之后把锁释放了
        r.id.lock();
        
        //记录一条日志
        LOG.info("Replicator={}@{} is started", r.id, r.options.getPeerId());
        
        //记录最后一次发送信息的时间
        r.lastRpcSendTimestamp = Utils.monotonicMs();
        
        //启动心跳定时器，这个定时器会定时向其他节点发送心跳消息
        //这里就启动了心跳任务定时器，开始向对应的跟随者节点发送心跳消息了
        r.startHeartbeatTimer(Utils.nowMs());

        //这里还省略了一个发送探针消息的操纵
        
        //返回ThreadId对象
        return r.id;
    }



    //启动心跳定时器的方法
    private void startHeartbeatTimer(final long startMs) {
        final long dueTime = startMs + this.options.getDynamicHeartBeatTimeoutMs();
        try {//使用全局定时任务管理器提交一个定时任务，在这个任务中执行发送心跳消息的请求
            //可以看到，这里其实就是用timerManager提交了一个定时任务
            //具体细节，大家可以去看我提供的第一版本代码，总之，在这个定时任务中，会调用sendEmptyEntries方法
            //发送心跳消息给对应的跟随者节点
            this.heartbeatTimer = this.timerManager.schedule(() -> onTimeout(this.id), dueTime - Utils.nowMs(),
                    TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            LOG.error("Fail to schedule heartbeat timer", e);
            onTimeout(this.id);
        }
    }



     //填充AppendEntriesRequest请求中公共字段的方法，这里面也有一些写死的数据，后面会进行重构
    private boolean fillCommonFields(final RpcRequests.AppendEntriesRequest.Builder rb, long prevLogIndex, final boolean isHeartbeat) {
        //设置领导者的任期
        rb.setTerm(this.options.getTerm());
        //设置集群ID
        rb.setGroupId(this.options.getGroupId());
        //设置领导者的节点信息
        rb.setServerId(this.options.getServerId().toString());
        //设置要发送请求的目标节点，也就是跟随者节点的信息
        rb.setPeerId(this.options.getPeerId().toString());
        //设置领导者前一条日式的索引
        rb.setPrevLogIndex(prevLogIndex);
        //下面这两个是写死了，后面版本会重构
        //设置前一条日志的任期
        rb.setPrevLogTerm(0);
        //设置领导者已提交的最新日志的索引
        rb.setCommittedIndex(0);
        return true;
    }
    

    
    //发送心跳或者探针请求的方法，根据方法中的第一个参数判断是否发送心跳还是探针消息，第二个是收到响应后的回调方法
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private void sendEmptyEntries(final boolean isHeartbeat, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> heartBeatClosure) {
        
        //构建AppendEntriesRequest请求，该请求是共用的，日志传输，心跳消息，探针消息都会使用这个请求封装消息内容
        //得到心跳请求构建器
        final RpcRequests.AppendEntriesRequest.Builder rb = RpcRequests.AppendEntriesRequest.newBuilder();
        
        //填充请求中的公共属性，这里的0是写死的，在第四版本就会把这里重构成和源码一致
        //0代表的就是prevLogIndex，本来是要根据this.nextIndex - 1来获得的，this.nextIndex就是当前的Replicator
        //对象的一个成员变量，记录的就是领导者要发送给对应的跟随者对象的下一条日志的索引，但是我还没有为大家引入日志组件
        //所以这里就写成0了，第四版本代码会重构得和源码一致
        fillCommonFields(rb, 0, isHeartbeat);
        
        try {
            //当前发送请求的时间
            final long monotonicSendTimeMs = Utils.monotonicMs();
            
            //根据方法中的参数判断是否为心跳请求
            if (isHeartbeat) {
                //这行代码是我自己添加的
                LOG.info("LEADER发送了心跳消息！");
                
                //构建AppendEntriesRequest对象封装消息内容
                final RpcRequests.AppendEntriesRequest request = rb.build();
                
                //心跳计数器自增
                this.heartbeatCounter++;
                //定义一个回调方法，该方法会在接收到心跳响应后被回调
                RpcResponseClosure<RpcRequests.AppendEntriesResponse> heartbeatDone;
                //判断参数中的回调方法是否为空
                if (heartBeatClosure != null) {
                    heartbeatDone = heartBeatClosure;
                } else {//如果为空就创建一个新的
                    heartbeatDone = new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                        @Override
                        public void run(final Status status) {
                            //在创建的新的回调方法中，收到心跳响应之后，会回调这个方法，在这个方法中会提交下一个心跳消息任务给全局定时器
                            onHeartbeatReturned(Replicator.this.id, status, request, getResponse(), monotonicSendTimeMs);
                        }
                    };
                }
                //发送心跳消息
                this.heartbeatInFly = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(), request,
                        this.options.getElectionTimeoutMs() / 2, heartbeatDone);
            } else {
                //这里关于探针请求的发送逻辑暂时省略了，第一版本中虽然也提供了探针请求的发送逻辑，但是并不完善，在日志传输模块，我会为大家详细展示这一部分的逻辑
                //探针请求也会放在第11章进行讲解，大家可以暂时忽略这一部分的逻辑

            }
        } finally {
            //这里可以直接去第一版本代码中查看注释和具体流程
            unlockId();
        }
    }


    

    //收到心跳消息的响应之后，会调用这个方法来处理响应，这个方法
    static void onHeartbeatReturned(){

        
    }
      
}
```
在上面的代码块中可以看到，发送心跳请求的流程非常简单，就是在 sendEmptyEntries 方法中创建了一个 AppendEntriesRequest 请求对象，然后把一些重要的公共字段封装到心跳请求中，然后就直接使用客户端发送给对应的跟随者节点了。这个逻辑十分清晰吧。在这里我想多提一句，那就是结合上一章的内容来看，当 raft 集群中的一个节点成为领导者之后，这个领导者会将集群中的其他节点都包装成一个个 Replicator 复制器对象，在创建每一个复制器对象的时候，就已经启动了心跳任务，开始向每一个跟随者节点发送心跳消息。当然，**这个过程中我省略了一个非常关键的步骤，那就是在发送心跳消息之前，领导者会通过每一个 Replicator 复制器对象向每一个对应的节点发送一个探针请求，这个探针请求的具体作用就是用来定位需要传输的下一条日志索引**，但在本章还未引入日志组件，所以我就省略了。后面会为大家重构完整。<br />好了，现在心跳消息也发送完毕了，在 sendEmptyEntries 方法中，还可以看到在上面代码块的第 160 行，创建了一个 RpcResponseClosureAdapter 对象，**这个对象的 run 方法中调用了这个 onHeartbeatReturned 方法，该方法会在领导者接收到每一个心跳响应后被回调，处理心跳响应**。onHeartbeatReturned 方法在我们目前的代码中还是一个空方法，但接下来我们还不急着实现这个 onHeartbeatReturned 方法。现在有一个更重要的操作需要我们分析，那就是当集群中的其他节点接收到领导者发送过来的心跳请求后，会怎么处理这个心跳请求呢？这个是我们接下来要分析的重点。<br />首先我想请大家思考一下，当 raft 集群中某一个节点当选为领导者之后，这时候，集群中的其他节点可能是什么状态？我目前想到的有三种状态：<br />**1 其他节点中大多数都为跟随者节点，这些跟随者节点在接收到某个候选者节点索要投票的请求后，会回复投票的成功响应，让某个候选者成为领导者，而自己仍然是跟随者状态。这时候跟随者节点接收到来自领导者的请求后，就可以知道集群中的新任领导者是谁了。**<br />**2 其他节点中可能存在候选者节点，这种情况也是可能出现的，可能集群中出现了两个候选者节点，只不过其中一个候选者节点的投票请求先得到大部分节点的投票响应，所以当选为领导者，另一个候选者节点就无法成为领导者了，所以就只有等待选举出来的领导者给它发送心跳请求，让这个候选者节点知道集群中已经有了新的领导者，自己可以身份降级为跟随者节点了。很好，已经分析完两种情况了，总之，不管集群中的其他节点是跟随者还是候选者，跟随者和候选者任期基本上都是和领导者相同的，这个应该没有疑问了，因为在候选者申请成为领导者的过程中，会将自己的任期自增 1，跟随者节点接收到投票请求后会用请求中的任期更新自己的任期。如果同时出现了两个候选者，那么肯定是两个跟随者将自己的任期都自增了 1，然后都成为了候选者，这两个候选者的任期也应该是相等的。这时候，我想问大家一句，在跟随者和候选者的任期都和领导者相同的前提下，集群中的其他节点会不会出现比新任领导者任期小的情况呢？接下来，请跟我一起分析第三种情况。**<br />**3 其他节点中可能存在另一个领导者，如果考虑的谨慎一些，这种情况也是可能出现的。当然，如果网络一直保持着健康的状态，可能 raft 集群中就不会出现这种情况，可一旦发生网络分区，就有可能出现这种情况。比如集群中有 5 个节点，节点 1 是领导者，某个时间段节点 1 和 节点 2 发生了网络分区，这两个节点可以互相通信，但是无法和节点 3、4、5 通信，而节点 3、4、5 可以互相通信。这样一来，节点 3、4、5 就可以选举出新的领导者，这三个节点网络情况良好，如果有一个节点率先进行领导者选举，肯定会先自增自己的任期，然后发送投票请求，最终获得 3 票，这就超过了集群过半节点的数量，成功当选为领导者了。假如说节点 4 成为了新的领导者之后，网络分区结束了，这 5 个节点又可以互相通信了，节点 4 和节点 1 都是领导者，但是节点 4 的任期比节点 1 的任期高，所以节点 4 发送给节点 1 心跳请求后，节点 1 就应该执行身份降级方法，让自己成为跟随者。我想这一点也是很合理的。这时候，我知道大家肯定会有一个疑问，如果真的发生了网络分区了，能不能让旧的领导者自己感受到自己出了故障，然后自动降级为跟随者，这样一来，客户端就不会再向这个旧的领导者发送写操作指令了。这当然是很合理，甚至是很有必要的一个功能，实际上，sofajraft 框架已经为我们实现了这个功能，只不过我现在还没有讲解，在 sofajraft 框架的 NodeImpl 类中还定义了一个成员变量，这个成员变量也是 RepeatedTimer 类型的，叫做 stepDownTimer，翻译过来就是身份降级定时器，这个定时器会在节点当选为领导者后启动，在该定时器中，会定期检测领导者管理的所有 Replicator 复制器对象中是否有死对象，所谓死对象就是超过规定时间没有和领导者通信的对象。这时候，大家应该也就明白了 Replicator 对象中 lastRpcSendTimestamp 成员变量的作用了吧？这个成员变量记录的就是对应的跟随者节点最后一次跟领导者的通信时间。如果 stepDownTimer 定时器检测到当前领导者管理的复制器对象中的死对象超过了集群节点数的一半，就会让当前领导者执行 stepDown 方法进行身份降级，成为跟随者。这里拓展得有点多了，这个功能会在课程即将结尾的时候为大家实现。**<br />好了，现在这三种情况都为大家分析完毕了，文字分析写了一大堆，接下来，我们从代码层面上来实现一下刚才分析的这几种情况。在 sofajraft 框架中，节点处理领导者心跳请求的方法定义在了 NodeImpl 类中，方法名称定义为了 handleAppendEntriesRequest，这里我仍然要多说一句，这个方法不仅仅是用来处理领导者心跳请求的，也是用来处理领导者向跟随者节点复制日志请求的。在本章我只会为大家实现该方法的部分逻辑，在后续章节会渐渐重构完整。话不多说，请大家直接看下面的代码块吧。
```java
public class NodeImpl implements Node{

    private static final Logger LOG = LoggerFactory.getLogger(NodeImpl.class);



    //省略其他内容


    //当前节点接收到领导者发送过来的消息时，不管消息是日志复制还是心跳消息，还是探针消息，都会调用该方法进行处理
    //换句话说，其实领导者要发送的日志复制消息，以及心跳消息探针消息，使用的都是同一个RPC请求，也就是该方法中的第一个参数对象
    @Override
    public Message handleAppendEntriesRequest(final RpcRequests.AppendEntriesRequest request, final RpcRequestClosure done) {
        //这些重复逻辑就不添加注释了
        boolean doUnlock = true;
        this.writeLock.lock();
        
        //从请求中获得领导者传送过来的日志的条数
        final int entriesCount = request.getEntriesCount();
        try {//判断当前节点是否活跃
            if (!this.state.isActive()) {
                LOG.warn("Node {} is not in active state, currTerm={}.", getNodeId(), this.currTerm);
                return RpcFactoryHelper
                        .responseFactory()
                        .newResponse(RpcRequests.AppendEntriesResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Node %s is not in active state, state %s.", getNodeId(), this.state.name());
            }//解析领导者节点的信息
            final PeerId serverId = new PeerId();
            if (!serverId.parse(request.getServerId())) {
                LOG.warn("Node {} received AppendEntriesRequest from {} serverId bad format.", getNodeId(),
                        request.getServerId());
                return RpcFactoryHelper
                        .responseFactory()
                        .newResponse(RpcRequests.AppendEntriesResponse.getDefaultInstance(), RaftError.EINVAL,
                                "Parse serverId failed: %s.", request.getServerId());
            }
            
            //如果领导者的任期小于当前节点的任期，则直接忽略这条请求
            //这种情况也不是没可能出现，如果出现网络分区了，分区故障解决之后，另一个分区的领导者任期很大
            //把当前节点的任期修改了，修改的比当前节点原来的领导者的任期还要大，就会出现这种情况
            //现在就是旧领导者给分区后的节点发送心跳请求了，分区后的节点选举过新的领导者了，任期肯定比旧的领导者大
            //所以直接就可以忽略旧领导者的心跳消息
            if (request.getTerm() < this.currTerm) {
                LOG.warn("Node {} ignore stale AppendEntriesRequest from {}, term={}, currTerm={}.", getNodeId(),
                        request.getServerId(), request.getTerm(), this.currTerm);
                return RpcRequests.AppendEntriesResponse.newBuilder()
                        .setSuccess(false)
                        .setTerm(this.currTerm)
                        .build();
            }
            //更具领导者的任期来判断当前节点是否需要进行身份降级，也就是变更身份为跟随者
            //在下面这个方法中很可能会把当前节点的领导者id设置为请求中传递过来的节点的id
            //这个方法是目前handleAppendEntriesRequest方法中最核心的方法，一定要仔细看一看
            checkStepDown(request.getTerm(), serverId);
            
            //更新最后一次接收到领导者消息的时间
            updateLastLeaderTimestamp(Utils.monotonicMs());
            
            //这是我自己添加的代码
            LOG.info("jraft1FOLLOWER接收到了心跳消息！");
            
            //下面一大段代码都是和日志有关的，因为当前的请求消息可能就是日志复制消息，消息对象中封装着传入过来的日志
            //如果当前节点要复制主节点的日志，肯定要对比一下当前节点和请求中日志是否符合要求
            //第一版本中把这些代码删除了，这个方法中还有日志复制的逻辑，以及一些耗时的统计信息等等，这些代码在第一版本全部删除了
            //后面再补充完整
            //如果请求中的日志条目为0，说明当前的请求是心跳或者是探针消息
            if (entriesCount == 0) {
                //在处理探针消息的逻辑中也删除了一些和日志有干的代码，后面会加上
                //创建响应对象
                final RpcRequests.AppendEntriesResponse.Builder respBuilder = RpcRequests.AppendEntriesResponse.newBuilder()
                        .setSuccess(true)
                        .setTerm(this.currTerm)
                        //这里也写死了，本来是要从日志组件中获得的
                        //给领导者回复当前节点最后一条日志的索引
                        .setLastLogIndex(0);
                doUnlock = false;
                this.writeLock.unlock();
                return respBuilder.build();
            }
            return null;
        } finally {
            if (doUnlock) {
                this.writeLock.unlock();
            }
        }
    }



    //根据传进该方法中的参数，对当前节点的身份进行变更，说的直接一点就是身份降级为跟随者并且在第一版本中，该方法只会在handleAppendEntriesRequest方法中被调用
    //而handleAppendEntriesRequest方法是处理来自领导者的信息的，不管来自领导者信息是日志复制还是心跳，总是这条信息肯定是来自领导者
    //这也就意味着集群中有了新的领导者
    private void checkStepDown(final long requestTerm, final PeerId serverId) {
        //该对象用来封装当前节点身份变更的重要信息
        //其实就是一个状态值，大家现在还不用关注这个对象的作用，后面的文章会针对这个对象展开讲解
        final Status status = new Status();
        if (requestTerm > this.currTerm) {
            //如果传进来的任期大于当前节点的任期，那么当前节点就要变成跟随者，这个情况可能就是集群中出现了另一个新的领导者，网络分区结束后
            //旧的领导者接收到新的领导者的消息了
            status.setError(RaftError.ENEWLEADER, "Raft node receives message from new leader with higher term.");
            //就在下面的方法中，将当前节点的状态设置为跟随者
            stepDown(requestTerm, false, status);
            //如果走到下面的两个分支，不管是哪一个，都意味着领导者的任期和当前节点相等
        } else if (this.state != State.STATE_FOLLOWER) {
            //如果走到这里，意味着当前节点是候选者节点，并且收到了和自己任期相同的领导者的信息
            status.setError(RaftError.ENEWLEADER, "Candidate receives message from new leader with the same term.");
            //让自己降级为跟随者，集群中有多个候选者时可能会出现这种情况
            stepDown(requestTerm, false, status);
        } else if (this.leaderId.isEmpty()) {
            //走到这里说明当前节点是跟随者，并且自己记录的领导者信息为空，但现在收到到了领导者发送过来的信息
            //说明领导者是刚刚选举出来，这时候就仍然让自己保持跟随者状态即可
            status.setError(RaftError.ENEWLEADER, "Follower receives message from new leader with the same term.");
            stepDown(requestTerm, false, status);
        }//走到这里就把当前节点记录的领导者信息复制为新领导者的信息
        if (this.leaderId == null || this.leaderId.isEmpty()) {
            resetLeaderId(serverId, status);
        }
    }

    
    
	//省略其他方法

}

```
代码中的注释十分详细，所以我就不再重复讲解了，上面代码块中的 checkStepDown 方法非常重要，这个方法中的操作对应就是我在前面为大家分析的三种情况。大家可以仔细看看。好了，现在处理领导者心跳响应的方法也展示给大家了。在该方法的最后，给领导者回复了一个心跳成功的响应，接下来就该为大家展示领导者处理心跳响应方法的具体实现了。所以，我们的注意力又要回到 Replicator 复制器对象中，实现该对象内部的 onHeartbeatReturned 方法。讲到这里，我也可以稍微放松一下了，因为这个 onHeartbeatReturned 方法其实没什么可说的，至少在第一版本代码中，它的实现逻辑非常简单，就是在该方法中判断接收到心跳响应是否为成功响应，如果响应成功就向心跳定时器提交下一个心跳定时任务即可。具体逻辑我也展示在下面的代码块中了，大家简单看看这个方法即可，我就不再详细讲解了。请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator{


    private static final Logger LOG = LoggerFactory.getLogger(Replicator.class);

    //RPC客户端，大家可以顺着构造方法，看看这个成员变量被赋值的逻辑
    //因为要在该类的对象中执行心跳检测和日志复制等工作，肯定需要RPC客户端发送请求啊
    private final RaftClientService rpcService;

    
    //全局定时任务管理器，也就是定时任务调度器，这个定时任务管理器会提交发送心跳信息的定时任务
    private final Scheduler timerManager;

	//这个其实就可以看成一个句柄，复制器对象的引用被ThreadId对象持用
    //该对象会交给复制器组管理
    protected ThreadId id;
    
    //下面两个是配置参数对象，这个成员变量是用来创建Replicator对象的
    private final ReplicatorOptions options;
    
    private final RaftOptions raftOptions;

    //领导者最后一次发送信息的时间
    private volatile long lastRpcSendTimestamp;
    
    //心跳计数器，代表发送了多少次心跳请求
    private volatile long heartbeatCounter = 0;
    
    //心跳定时器
    private ScheduledFuture<?> heartbeatTimer;


    //启动心跳定时器的方法
    private void startHeartbeatTimer(final long startMs) {
        final long dueTime = startMs + this.options.getDynamicHeartBeatTimeoutMs();
        try {//使用全局定时任务管理器提交一个定时任务，在这个任务中执行发送心跳消息的请求
            //可以看到，这里其实就是用timerManager提交了一个定时任务
            //具体细节，大家可以去看我提供的第一版本代码，总之，在这个定时任务中，会调用sendEmptyEntries方法
            //发送心跳消息给对应的跟随者节点
            this.heartbeatTimer = this.timerManager.schedule(() -> onTimeout(this.id), dueTime - Utils.nowMs(),
                    TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            LOG.error("Fail to schedule heartbeat timer", e);
            onTimeout(this.id);
        }
    }

    
    //发送心跳或者探针请求的方法，根据方法中的第一个参数判断是否发送心跳还是探针消息，第二个是收到响应后的回调方法
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private void sendEmptyEntries(final boolean isHeartbeat, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> heartBeatClosure) {
        
        //省略部分

        
        //判断参数中的回调方法是否为空
        if (heartBeatClosure != null) {
            heartbeatDone = heartBeatClosure;
        } else {//如果为空就创建一个新的
            heartbeatDone = new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>()
                @Override
                public void run(final Status status) {
                    //在创建的新的回调方法中，收到心跳响应之后，会回调这个方法，在这个方法中会提交下一个心跳消息任务给全局定时器
                    onHeartbeatReturned(Replicator.this.id, status, request, getResponse(), monotonicSendTimeMs);
                }
            };
        }
        //发送心跳消息
        this.heartbeatInFly = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(), request,
                this.options.getElectionTimeoutMs() / 2, heartbeatDone);


		//省略部分内容
       
    }


    

    //收到心跳消息的响应之后，会调用这个方法来处理响应，这个方法
    static void onHeartbeatReturned(){

        //如果ThreadId为null，这个threadId对象可能已经被销毁了
        if (id == null) {
            return;
        }
        final long startTimeMs = Utils.nowMs();
        Replicator r;
        //先上锁
        if ((r = (Replicator) id.lock()) == null) {
            return;
        }
        
        boolean doUnlock = true;
        try {
        	//心跳响应中的任期更大
            if (response.getTerm() > r.options.getTerm()) {
                if (isLogDebugEnabled) {
                    sb.append(" fail, greater term ")
                            .append(response.getTerm())
                            .append(" expect term ")
                            .append(r.options.getTerm());
                    LOG.debug(sb.toString());
                }
                final NodeImpl node = r.options.getNode();
                //这里我删减了部分代码，后面会重构完整，其实这里就是集群中出现了两个领导者的情况
                //当前领导者发现集群中有节点任期比自己大，就应该让自己身份降级了，这个在后面版本再实现
                return;
            }
            
            //响应状态为不成功
            if (!response.getSuccess() && response.hasLastLogIndex()) {
                if (isLogDebugEnabled) {
                    sb.append(" fail, response term ")
                            .append(response.getTerm())
                            .append(" lastLogIndex ")
                            .append(response.getLastLogIndex());
                    LOG.debug(sb.toString());
                }
                LOG.warn("Heartbeat to peer {} failure, try to send a probe request.", r.options.getPeerId());
                doUnlock = false;
                //发送探针消息，这个逻辑可以暂时忽略，后面会展开讲解
                r.sendProbeRequest();
                //启动心跳定时器
                r.startHeartbeatTimer(startTimeMs);
                return;
            }
            //更新lastRpcSendTimestamp时间戳
            if (rpcSendTime > r.lastRpcSendTimestamp) {
                r.lastRpcSendTimestamp = rpcSendTime;
            }//走到这里不管前面结果怎样，都会启动心跳定时器
            //在这个方法中又会重新提交一个心跳定时任务给调度器
            //这里大家应该就能意识到了，领导者的心跳消息就是这样发送的，每一次收到心跳之后
            //在处理心跳的响应时，开启下一次心跳定时任务
            r.startHeartbeatTimer(startTimeMs);
        } finally {
            if (doUnlock) {
                //在这里把锁释放了
                id.unlock();
            }
        }
    }
      
}
```
到此为止，心跳检测的完整流程我就为大家实现了，总的来说，心跳检测的逻辑还是非常简单的。大家在看完文章后，就可以直接去看我为大家提供的第一版本代码了。第一版本代码的所有重要知识，以及代码的迭代过程，我都为大家讲解完毕了，在第一版本技术之后，我希望大家对 NodeImpl 和 Replicator 这两个类有着非常清晰的认识，在我看来，这两个类是整个 sofajraft 框架中最核心的两个类了，当然还有状态机组件和快照组件，但是我可以负责任地告诉大家，这门课程的复杂逻辑几乎都在前半部分，尤其是日志复制这一块，当我为大家展示完毕在 Pipeline 模式下，领导者是怎样将日志复制给跟随者之后，剩下的部分内容几乎就没什么难度了。为什么我这么说呢？举个很直接的例子，假如让我现在就为大家讲解日志压缩，安装快照的知识，我几乎不用怎么思考，就能很顺畅地写完一篇一万五千字的文章，因为这一块的知识很简单，流程图在我脑海里十分清晰，我只需要按照脑海里的流程图一点点为大家在文章中迭代代码即可；但让我现在讲解日志传输的知识，并且是结合 Pipeline 模式讲解，我就会觉得思绪杂乱，不知道切入点在哪里，需要静心构思个一两天才行。好了，就先不扯这么远了，第一版本代码的知识虽然讲得差不多了，但这一章还不能结束，因为我还有一点知识要为大家补充。

<a name="yjsVO"></a>
## 分析节点内部线程的职责

**当 raft 集群中的一个节点成功当选领导者之后，这个领导者会把集群中的其他节点包装成一个个 Replicator 对象，领导者对跟随者节点的心跳检测和日志传输都由每一个 Replicator 对象负责，Replicator 对象中持有者领导者的客户端，心跳请求就通过客户端发送给对应的跟随者节点**。这句话我已经为大家重复了无数次了，也许大家都听烦了。但是，请允许我再说一句，其实我们对客户端发送心跳请求的理解还浮于表面，在上一小节的代码中，我在 Replicator 类的 sendEmptyEntries 方法中展示了客户端发送心跳消息的代码，就像下面这样，请看下面代码块。
```java
//发送心跳消息
this.heartbeatInFly = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(), request,
        this.options.getElectionTimeoutMs() / 2, heartbeatDone);
```
当程序调用 this.rpcService.appendEntries 方法的时候，客户端就会把心跳消息发送给对应的跟随者节点。但是究竟怎么发送的，我还没有为大家分析过。当然，发送的逻辑并不难以理解，但是也不是一两句话可以讲清楚的，因为这个逻辑需要结合 bolt 框架来理解，bolt 就是 sofajraft 内部使用的 rpc 通信框架，这个框架也是对 netty 做了一层封装，底层使用的仍然是 netty。<br />现在，我想让大家先思考一个问题，raft 集群中只有一个领导者，一个领导者管理着多个 Replicator 复制器对象，每一个 Replicator 复制器对象都对应着一个跟随者节点。当领导者要和集群中的所有节点进行心跳检测，或者是领导者要把日志传输给集群中的其他所有节点时，都会通过领导者管理的 Replicator 对象们来完成这些操作。我想问一下，这些操作应该由几个线程来执行呢？假如集群中有 7 个节点，1 个领导者需要同时给 6 个跟随者节点发送心跳消息和日志传输消息，如果领导者只用一个线程来执行这些操作，效率肯定不会高到哪去。那么，如果领导者创建了 6 个线程，给每一个 Replicator 对象分配了一个线程，实际上就是每一个跟随者在领导者内部都有一个对应的线程来执行相关操作。这样一来，各个跟随者节点的操作互不干涉，每个线程执行好各自 Replicator 对象中的方法，更新各自 Replicator 对象内的属性即可。不仅提高了效率，还在一定程度上解决了并发问题。这也算是一种不错的优化手段吧？<br />实际上在 sofajraft 框架的领导者中也正是这么做的，接下来请大家跟我一起看一些代码片段，逻辑可能有点碎，这里我只是简单介绍一下，算是给大家一个指引，更具体的逻辑我会放在视频中，结合 bolt 框架的流程为大家详细讲解。首先，我们先看看 this.rpcService.appendEntries 这个方法内部的具体流程。请看下面代码块。
```java
//JRaft框架内部的客户端，这个客户端是用来在集群中访问其他节点，进行各种raft活动的，比如选主，投票，预投票，发送日志等等
public class DefaultRaftClientService extends AbstractClientService implements RaftClientService {

    //单线程执行器组，和netty中的单线程执行器组差不多，大家可以去第一版本代码中看看这个类
    //看看这里的这个成员变量就是怎么被赋值的，从nodeimpl类的init方法中看就行
    private final FixedThreadsExecutorGroup appendEntriesExecutors;

    private final ConcurrentMap<Endpoint, Executor> appendEntriesExecutorMap = new ConcurrentHashMap<>();

    private NodeOptions nodeOptions;
    private final ReplicatorGroup rgGroup;



	//省略其他内容


    //客户端发送预投票请求的方法
    @Override
    public Future<Message> preVote(final Endpoint endpoint, final RpcRequests.RequestVoteRequest request,
                                   final RpcResponseClosure<RpcRequests.RequestVoteResponse> done) {
        if (!checkConnection(endpoint, true)) {
            return onConnectionFail(endpoint, request, done, this.rpcExecutor);
        }

        return invokeWithDone(endpoint, request, done, this.nodeOptions.getElectionTimeoutMs());
    }



	//发送正式投票请求的方法
    @Override
    public Future<Message> requestVote(final Endpoint endpoint, final RpcRequests.RequestVoteRequest request,
                                       final RpcResponseClosure<RpcRequests.RequestVoteResponse> done) {
        if (!checkConnection(endpoint, true)) {
            return onConnectionFail(endpoint, request, done, this.rpcExecutor);
        }

        return invokeWithDone(endpoint, request, done, this.nodeOptions.getElectionTimeoutMs());
    }
    

    //这里强调一下，发送预投票和正式投票请求不需要使用特意为复制器对象分配的单线程执行器，只需要使用客户端的业务线程池即可
    //发送心跳和日志给跟随者的方法，endpoint其实就是要发送过去消息的目标节点，也就是跟随者节点
    //一个跟随者节点对应一个Replicator对象，在下面的方法中就为一个跟随者节点确定了一个单线程执行器
	@Override
	public Future<Message> appendEntries(final Endpoint endpoint, final RpcRequests.AppendEntriesRequest request,
	                                     final int timeoutMs, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> done) {
	    //这里可以发现，发送日志和心跳信息之后，在收到响应执行器回调方法时使用的是这个appendEntriesExecutors执行器组中的一个执行器
	    //这里有点Netty中从执行器组中获得一个单线程执行器的感觉，实际上也正是如此，这个appendEntriesExecutors执行器组中管理的就是一个个单线程执行器
	    //也就是一个SingleThreadExecutor对象，这里的关系也很明确，通过一个Map，以及Map的computeIfAbsent方法
	    //让appendEntriesExecutors执行器组中的每一个单线程执行器都对应一个endpoint。这样一来，每一个单线程执行器都负责一个专属节点的响应
	    //为什么这么设计呢？原因说简单很简单，说复杂真的也有点复杂，心跳消息响应的回调到没什么，关键是领导者向每一个跟随者节点发送日志请求后收到响应的回调
	    //也就是收到响应之后要做哪些工作，这是后面的内容，要做的内容还是很多的，如果是在多线程情况下处理，还会出现并发问题，所以就给每一个跟随者复制器对象
	    //分配了一个单线程执行器，具体处理什么工作讲到日志复制的知识时再说。这里有点像xxljob执行器执行定时任务一样，给每一个定时任务分配一个线程
	    //关于这个单线程执行器的被调用的逻辑，我录制了一个小视频为大家讲解。因为这涉及到和bolt这个基于Netty的RPC框架的执行流程，这个bolt框架的源码我也是刚看
	    //流程也看了个大概，明白回调方法是在哪里被执行的了，为了让大家对这个框架的工作流程更清晰，我有必要为大家借着讲解回调方法的回调时机，为大家梳理一下
	    //这个bolt框架的工作流程
        //这里我还要再强调一下，这个appendEntriesExecutors单线程执行器组内部的单线程数量的默认值最小为16
        //这个值也是可以由用户配置的，如果领导者管理着20个Replicator对象，但是用户只配置了16个线程，这种情况下，还是
        //会出现一个线程负责多个Replicator复制器对象工作的情况
	    final Executor executor = this.appendEntriesExecutorMap.computeIfAbsent(endpoint, k -> appendEntriesExecutors.next());
	    if (!checkConnection(endpoint, true)) {
	        return onConnectionFail(endpoint, request, done, executor);
	    }//下面这个方法就要和bolt框架的流程结合起来了，这里把done和executor传进方法中了
        //executor就是当前复制器对象刚刚得到的单线程执行器，done就是接收到心跳响应后要被回调的方法
	    return invokeWithDone(endpoint, request, done, timeoutMs, executor);
	}


    //省略其他内容
}

```
上面代码为大家展示的是客户端的部分内容，我想只展示上面的代码块，大家现在肯定是一头雾水，不知道在哪里会把 Replicator 对象的工作交给这个单线程执行器，先不要急，接下来我们还可以继续深入研究，那就是当程序在 appendEntries 方法内部为对应的跟随者节找到了一个单线程执行器之后，调用了 invokeWithDone 方法，下面，就请大家跟我一起看看这个 invokeWithDone 方法的具体逻辑。请看下面代码块。
```java
//客户端服务抽象类，这个类中提供的都是真正向服务端发送请求消息的方法
//上面展示的DefaultRaftClientService类就是该类的子类
public abstract class AbstractClientService implements ClientService {


    //加载Protobuf描述文件，并且注册ProtobufSerializer序列化器给RPC组件使用
    static {
        ProtobufMsgFactory.load();
    }

    //RPC通信的客户端，这个客户端实际上就是一个BoltRpcClient对象
    protected volatile RpcClient rpcClient;
    
    //业务线程池
    protected ThreadPoolExecutor rpcExecutor;
    
    //RPC远程调用需要的配置参数对象
    protected RpcOptions rpcOptions;


    //初始化方法
    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        if (this.rpcClient != null) {
            return true;
        }
        this.rpcOptions = rpcOptions;
        return initRpcClient(this.rpcOptions.getRpcProcessorThreadPoolSize());
    }



	
    //核心初始化方法，客户端的业务线程池就在下面这个方法中初始化成功了
    protected boolean initRpcClient(final int rpcProcessorThreadPoolSize) {
        //获得创建RPC客户端和服务端的工厂
        final RaftRpcFactory factory = RpcFactoryHelper.rpcFactory();
        //在这里使用RPC工厂创建了一个RPC客户端，也就是一个BoltRpcClient对象
        //这个BoltRpcClient对象内部持有者bolt框架中的客户端，真正使用客户端功能的时候
        //其实是bolt框架中的核心组件在发挥功能
        this.rpcClient = factory.createRpcClient(factory.defaultJRaftClientConfigHelper(this.rpcOptions));
        //配置客户端，这个方法源码中没做实现
        configRpcClient(this.rpcClient);
        //根据配置参数对象初始化客户端
        this.rpcClient.init(this.rpcOptions);
        //创建业务线程池，这里我要跟大家多解释一句，在一般的RPC框架中，使用Netty发送了请求或者是接收到响应之后
        //要处理响应的业务逻辑时都是在业务线程池中处理的，而在sofajraft框架中，发送请求之后收到的响应，一般都是通过一些回调方法来处理
        //这里，这个线程池就是专门执行这些回调方法的，所以这也可以看作是业务线程池
        this.rpcExecutor = ThreadPoolUtil.newBuilder()
                //设置名字
                .poolName("JRaft-RPC-Processor")
                //是否开启性能检测
                .enableMetric(true)
                //设置核心线程数
                .coreThreads(rpcProcessorThreadPoolSize / 3)
                //设置最大线程数量
                .maximumThreads(rpcProcessorThreadPoolSize)
                //空闲线程存活时间
                .keepAliveSeconds(60L)
                //任务队列
                .workQueue(new ArrayBlockingQueue<>(10000))
                //线程工厂
                .threadFactory(new NamedThreadFactory("JRaft-RPC-Processor-", true))
                .build();
        //暂时不必关注下面这行代码
        if (this.rpcOptions.getMetricRegistry() != null) {
            this.rpcOptions.getMetricRegistry().register("raft-rpc-client-thread-pool",
                    new ThreadPoolMetricSet(this.rpcExecutor));
        }
        return true;
    }
    

    
    //这个是RPC客户端发送请求的入口方法，从名字就能看出来是异步的，因为要通过回调函数来处理响应
    //不管是发送预投票请求，还是正式投票请求，还是日志传输，或者是心跳检测，都会通过这个方法把请求发送出去
    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        //得到客户端
        final RpcClient rc = this.rpcClient;
        
        //创建一个future对象，这个就可以当作Netty中的promise
        
        final FutureImpl<Message> future = new FutureImpl<>();
        
        //得到业务线程池，这个线程池的作用就是在当前节点收到响应后，执行一些回调方法
        //其实这里就可以挑明了说，这里之所以弄了一个三元运算符，是因为在客户端中初始化好的线程池，也就是在本类的initRpcClient方法中创建的这个线程池
        //主要是执行和投票请求相关的响应回调的，而当前方法的参数中也可能会传进来一个线程池，如果是方法中传入的线程池
        //就肯定是一个单线程执行器，专门用来执行和复制日志以及心跳消息请求相关的响应回调的，这个逻辑可以从复制器对象中查看，就从复制器对象的sendEmptyEntries、sendEntries方法中
        //但很遗憾，第一版本还没有sendEntries方法。。所以等到后面的版本大家进一步学习这里的知识吧
        final Executor currExecutor = rpcExecutor != null ? rpcExecutor : this.rpcExecutor;
        
        try {//判断客户端是否为空，为空则意味着客户端还没初始化好
            if (rc == null) {
                future.failure(new IllegalStateException("Client service is uninitialized."));
                RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                        "Client service is uninitialized."));
                return future;
            }
            //开始异步发送请求了，再往下调用，逻辑就会陷入bolt框架中了，这部分的逻辑讲解，我录制到视频中了
            //这里的异步指的是发送了请求不必同步等待响应，当响应到达时，直接执行回调方法即可
            //回调方法就是该方法中的第三个参数，创建了一个InvokeCallback对象，这个对象内部持有者上面得到的
            //currExecutor，所以，这个currExecutor就是用来执行回调方法的，并且方法参数中的done也会在InvokeCallback对象
            //中被回调，这样一来，就是当接收到跟随者节点的响应之后，会回调InvokeCallback对象中的complete方法，然后在该方法中回调done方法
            //InvokeCallback对象中要被回调的就是complete方法，具体回调的时机和逻辑都在视频中讲解
            //大家把第一版本代码知识掌握清楚了，就可以去视频中专门看看RPC的流程了
            rc.invokeAsync(endpoint, request, ctx, new InvokeCallback() {

                @SuppressWarnings({ "unchecked", "ConstantConditions" })
                @Override
                public void complete(final Object result, final Throwable err) {
                    //这里逻辑太长，就先不展示了
                }
                //InvokeCallback对象内部持有着执行器
                //下面这个方法非常重要
                @Override
                public Executor executor() {
                    return currExecutor;
                }
            }, timeoutMs <= 0 ? this.rpcOptions.getRpcDefaultTimeout() : timeoutMs);
        } catch (final InterruptedException e) {
            //这里的逻辑也都省略了

        }

        return future;
    }
}
```
在上面代码块中的第 119 行，是一个 executor() 方法，该方法会返回一个执行器，**如果本次发送的请求是预投票请求，或者是正式投票请求，那么在接收到请求的响应后，这个执行器就是客户端的业务线程池(参考上面代码块第 93 行，三元运算符的逻辑)，业务线程池接着就会执行早就创建好的回调方法，在回调方法中处理响应。如果本次发送的请求是心跳请求或者是日志传输请求，那么这个 executor() 方法返回的就是特意为当前 Replicator 对象分配的单线程执行器，然后单线程执行器执行对应的回调方法，在回调方法中会再次发送心跳响应，或者是继续传输日志。这样一来，这个单线程执行器就正式接管了当前的 Replicator 复制器对象的工作。**到此为止，领导者内部线程的职责就分析得差不多了，实际上就是客户端的线程职责分析完毕了。<br />跟随者节点内部的线程职责分析起来比较麻烦，也就是服务端的线程职责。这个就留到视频中为大家分析吧，如果放在文章中讲解，整篇文章就得超过 2 万字了。大家可以去我更新的第一版本相关视频中进一步学习服务端线程的职责和工作流程，比如服务器怎么根据请求中的集群 id 分发请求的等等。<br />好了朋友们，这时候我终于可以说一句，第一版本代码的知识已经讲解完毕了，重要代码的迭代过程几乎都展示在文章中了，大家可以直接去看第一版本代码了。下一章，我会正式为我们的程序引入日志组件，之后就会开始为大家讲解日志复制的复杂逻辑。好了，我们下一章见！
