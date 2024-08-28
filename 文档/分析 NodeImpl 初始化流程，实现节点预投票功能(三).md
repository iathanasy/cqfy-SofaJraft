<a name="NOq0C"></a>
## 实现 handleElectionTimeout 方法，引出预投票概念

在上一章结尾我说要为大家实现这个 handleElectionTimeout 方法，为了让大家的思路保持连贯，我们就直接开始对这个方法的实现进行分析。<br />好了，现在请大家跟着我继续思考这样一个问题，还记得在上一章，我为大家引入节点状态的时候，跟大家说过，**节点会根据自身的状态来判断自己要执行什么操作。**这个应该还有印象吧？那么现在节点是什么状态呢？显然是刚刚被创建，在创建的时候给 NodeImpl 的 state 赋值为了 State.STATE_UNINITIALIZED 状态，也就是未初始化状态。但现在节点马上就要进行领导者选举了，可是节点的状态还是未初始化状态，这显然有点不太合适，所以，我们要做的，首先就是把节点状态更新一下**，根据 raft 共识算法，只有候选者节点才能进行领导者选举，那候选者节点是怎么来的呢？就是由跟随者节点变更过来的。实际上，在 raft 算法中，集群中的各个节点一开始都是以跟随者的身份参与进来的，当这个跟随者节点触发了超时选举，正式进入领导者选举阶段之后，会将自己的状态再更新为候选者。所以，接下来，我们就先实现第一步，将当前节点的状态更新为跟随者。**<br />那么，在哪里将当前节点的状态更新为跟随者呢？这个我早就想好了，就在 NodeImpl 的 init 方法中，在 init 方法将要结束的时候，也就意味着当前节点将要初始化完毕了。既然都要初始化完毕了，那就让节点状态更新为跟随者，然后开始进行领导者选举吧。那怎么就能进行领导者选举了呢？肯定就是启动超时选举定时器呀！所以，现在思路已经很明确了，**要在 NodeImpl 的 init 方法尾部，将当前节点的 state 更新为跟随者状态，然后启动超时选举定时器。**这样一来，当前节点，就只需要静静等待触发超时选举即可，触发了超市选举之后，超时选举定时器会执行超时选举的操作，也就是执行 handleElectionTimeout 方法。<br />很好，目前为止，思路仍然是一如既往的清晰，那么，接下来我们似乎就可以首先按照刚才分析的思路，重构一下 NodeImpl 的 init 方法了。但是，没错，我还是要说一句但是，我们目前还不能直接就这么做。因为我还有一个知识点要为大家补充。我们刚才分析的思路是没问题的，实际上，在 sofajraft 框架中也是按照这种思路来编写程序的，但是 sofajraft 框架多做了一点工作。**那就是它定义了一个名为 stepDown 的方法，在这个方法中将节点状态更新为跟随者，然后启动了超时选举定时器。并且，这个 stepDown 方法就是在 NodeImpl 对象的 init 方法中执行的。**为什么要这么做呢？大家可以先想一想，可以从方法的名称来思考。<br />方法的名字为 stepDown，可能一时之间，大家还不知道这个名字代表什么含义，或者说这个名字该怎么翻译。这时候，我想用我自己的经验为大家解释一下，**我喜欢称这个 stepDown 方法为身份降级方法，所谓身份降级，也就是将一个节点的状态更新为跟随者状态**。**当然，这个方法也有另外一层含义，假如当前节点的状态是候选者状态，或者是领导者状态，如果集群中已经选举出领导者了，那么候选者节点就应该将自己的状态更新为跟随者状态，如果集群中有新旧领导者更替，旧的领导者感知到新的领导者之后，也需要执行 stepDown 方法，将自己的状态更新为跟随者状态。由此可见，这个 stepDown 方法实际上会被广泛使用，并不只是会在节点初始化时在 init 方法中被调用，只要当前节点的状态需要降级为跟随者，就会调用这个方法。**<br />但是，讲解到这里，我不知道大家会不会有另外一个疑问，stepDown 方法会被广泛地使用，但是在这个方法中，还有另外一个操作，那就是将节点状态更新为跟随者之后，紧接着就会启动超时选举定时器，请大家思考思考，这个操作合理吗？首先我需要表明我自己的观点，我认为这个操作是非常合理的。请让我来解释一下为什么合理：**我们必须明确一点，那就是超时选举定时器的职责就是用来选举领导者的，既然是这样，那么我们就应该对这一点大达成共识，那就是如果一个节点的状态已经是领导者状态了，那么这个节点根本就不必启动自己的超时选举定时器。**我想这个也是可以理解的。如果理解了这一点，让我们再进一步思考一下，实际上，候选者也不必启动超时选举定时器，原因也很简单，如果大家对 raft 共识算法稍微有一些了解，就会知道，**候选者是由跟随者变更过来的，而正是因为启动了超时选举定时器，触发了超时选举之后，跟随者节点才将自己的状态更新为候选者，然后开始进行领导者选举了。这样一来，我是不是可以认为，候选者节点本身已经启动了超时选举定时器，正在进行领导者选举了，所以就可以让超时选举定时器停止工作了呢？也就是说超时选举定时任务只触发一次就够了，不必再向时间轮提交下一个超时选举定时任务了，因为说到底，这个超时选举定时器是用来判断领导者发送的心跳消息是否超过了心跳间隔时间，如果正在进行领导者选举，肯定就意味着没有领导者，也就不必再向时间轮提交下一次的超时任务选举定时任务**。这个逻辑，我希望大家可以仔细品味品味。分析了这么多，总之我就是想告诉大家，**在 raft 集群中，领导者和候选者是不必启动超时选举定时器的，只有跟随者需要启动超时选举定时器。**所以，在每一次调用 stepDown 方法的时候，都会把节点的状态更新为跟随者状态，既然是跟随者状态，就应该启动超时选举定时器。我想，这个逻辑到此也就很清晰了。<br />当然，还有一点必须要补充，那就是在 stepDown 方法中启动超时选举定时器时，并不是直接调用 RepeatedTimer 的 start 方法，而是调用了该对象的 restart 方法，也就是重新启动超时选举定时器的意思。好了，接下来，让我来为大家展示一下重构后的 NodeImpl 类，以及 stepDown 方法本身吧。请看下面代码块。
```java
public class NodeImpl implements Node{

    private static final Logger LOG = LoggerFactory.getLogger(NodeImpl.class);


    //当前节点所在集群的Id
    private final String groupId;

    //当前节点的Id
    private NodeId nodeId;

    //当前节点的状态
    private volatile State state;

    //节点的当前任期
    private long currTerm;

    //当前节点投过票的节点的PeerId，这个成员变量可以记录下来，当前节点为哪个节点投过票
    private PeerId votedId;

    //当前节点记录的领导者的PeerId
    private PeerId leaderId = new PeerId();

    //当前节点自己的节点信息
    private final PeerId serverId;

    //集群当前生效的配置信息，包括旧的配置信息
    private ConfigurationEntry conf;

    //为jraft框架提供各种服务的工厂，在第一版本中，这个工厂只提供了元数据存储器服务
    private JRaftServiceFactory serviceFactory;

    //注意，这里多出来一个NodeOptions成员变量，节点需要的配置参数对象
    private NodeOptions options;

    //超时选举定时器
    private RepeatedTimer electionTimer;


    //当Nodeimpl调用它的init方法时，这时候方法参数NodeOptions
    //已经创建了DefaultJRaftServiceFactory对象了
    @Override
    public boolean init(final NodeOptions opts) {
        //省略部分内容
        

        //这里会判断一下配置信息是否为空，现在当然不可能为空
        //因为上面已经初始化好了，false取反就会进入下面的分之，最重要的一个操作就来了
        if (!this.conf.isEmpty()) {
            //新启动的节点需要进行选举
            //就是在这里，当前节点开始执行超时选举任务，具体逻辑就在下面这个方法中
            //在下面这个方法中，会启动刚才创建好的超时选举定时器，在定时器任务中执行选举操作
            stepDown(this.currTerm);
        }
        //把当前节点添加到节点管理器中
        if (!NodeManager.getInstance().add(this)) {
            LOG.error("NodeManager add {} failed.", getNodeId());
            return false;
        }
    }



    //检测当前节点是否需要进行身份降级的操作，学习这个方法时候，不要只看这个方法，而要看看
    //这个方法会在哪些方法中被调用，结合外层方法一起学习该方法的功能和逻辑，否则你虽然看懂了这个方法的每一行代码
    //但根本不明白每一行代码背后的具体作用，当然在第一版本中，这个方法我也做了大量删减
    private void stepDown(final long term) {
        //检查当前节点的状态是否活跃。其实就是看看是否正在关闭或者有没有异常
        if (!this.state.isActive()) {
            return;
        }
        //这里我删减了很多代码，在后面版本的代码中，会逐渐重构完整
        //重置当前节点的领导者信息，因为正要开始进行领导者选举，集群中还没有领导者呢
        //所以先把当前节点记录的领导者信息重置为空
        resetLeaderId(PeerId.emptyPeer());
        //将当前节点的状态变更为跟随者
        this.state = State.STATE_FOLLOWER;
        //跟新最后一次收到领导者信息的时间，大家可能对这一行代码有点困惑，不明白为什么在这里
        //再次更新最后一次收到来自领导者的时间戳，这里我解释一下，刚才我已经给大家讲解了，只有跟随者需要
        //启动超时选举定时器，集群中可能有某个节点率先进行超时选举了，这个节点就会变成候选者
        //候选者在和其他节点通信的时候，其他节点发现自己的任期低于接收到的候选者的任期
        //就会调用当前方法将自己的状态更新为跟随者，同时更新接收到消息的时间戳，这是很合理的
        //请大家想想，如果其他跟随者一直不更新这个时间戳，那么其他跟随者不就也要触发超时选举，然后进行领导者选举了吗？
        //只有更新了这个时间戳，其他跟随者才会发现还没有出发选举超时，就不会进入超时选举阶段
        //当然，我很清楚，大家现在还没有完全理解这个stepDown方法会被哪里被调用，所以暂且先对这段话混个眼熟吧
        //后面代码补充完整了，大家再来回顾这行代码，肯定会豁然开朗
        updateLastLeaderTimestamp(Utils.monotonicMs());
        //如果方法参数中的任期必当前节点的任期大，这时候就意味着当前节点正在进行投票工作
        //这个stepDown方法可能正在被handleRequestVoteRequest方法调用
        //这里大家暂且不必关心，后面代码补充完整就会明白具体含义了
        if (term > this.currTerm) {
            //修改当前节点任期
            this.currTerm = term;
            //这里先把当前节点的投票记录设置为空，因为到了一个新的任期了，一个任期只能投一票
            //但是这个新的任期中还没有投呢，所以先设置为空，在外层的handleRequestVoteRequest方法中会投票
            //然后给当前节点的投票记录复制
            this.votedId = PeerId.emptyPeer();
            //持久化元数据信息
            this.metaStorage.setTermAndVotedFor(term, this.votedId);
        }
            //启动超时选举定时器，走到这里，就意味着当前的stepDown方法，也可能是在NodeImpl类的init方法中被调用的
            //大家可以仔细研究研究，一定要理清楚是哪个外层方法在调用当前的stepDown方法
            //当然即便不是init方法中调用的，也需要重新启动超时选举定时器，因为该节点马上就要投完票了，这意味着新的领导者即将产生
            //如果没产生，那么就根据超时时间继续进行新一轮选举，这正是超时选举定时器的工作
            this.electionTimer.restart();
        }
    }



    //这个方法会将当前节点记录的领导者信息重制
    private void resetLeaderId(final PeerId newLeaderId) {
        //判断当前传进来的节点是否为空
        if (newLeaderId.isEmpty()) {
            //如果为空，可能意味着正要进行选举操作，进行选举操作则意味着
            //集群还没有产生领导者，所以把当前节点记录的领导者信息设置为空即可
            this.leaderId = PeerId.emptyPeer();
        } else {
            //如果走到这里说明传进该方法中的节点并不为空，这可能是集群已经选举出了新的领导者
            //所以把当前节点记录的领导者信息设置为新的节点
            this.leaderId = newLeaderId.copy();
        }
    }

    
    
	//省略其他方法

}

```
好了，stepDown 方法已经为大家讲解完毕了，这顶多算一道开胃小菜，别忘了我们这一小节的真正目标，那就是实现 handleElectionTimeout 方法。我们之所以先讲解 stepDown 方法，是为了让大家知道超时选举定时器是在哪里启动的，以及节点状态是什么时候被更新为跟随者的。现在这一切都讲解完毕了，节点已经被更新为了跟随者状态，超时选举定时器也重新启动了，接下来当前节点就可以静静等待触发超时选举，然后执行 handleElectionTimeout 方法了，这个方法的作用就是进行真正的超时选举操作。所以，接下来，我就要为大家实现这个 handleElectionTimeout 方法。<br />其实在上一小节，我已经为大家简单实现了 handleElectionTimeout 方法的部分逻辑，请看下面代码块。
```java
public class NodeImpl implements Node{

	//其他内容省略


    //超时选举处理处理方法，这个方法就是超时选举定时器要调用的方法
    private void handleElectionTimeout() {
        //这里的这个判断非常重要，超时选举定时器启动之后，只要节点状态是跟随着，就会一直工作
        //每过一段时间就会触发一次，但是触发了并不意味着就要真的去进行选举，会在这里进行判断
        //看看距离当前节点最后一次收到领导者信息过去的多少时间，如果这个时间超过了超时选举时间，才会进入预投票阶段
        //否则直接退出，这里返回true就代表还没超时呢
        if (isCurrentLeaderValid()) {
            return;
        }

        //其他逻辑暂时不做实现
    }


    //判断是否触发选举超时了，如果触发了就可以让选举定时器进行工作了，在选举定时器中就可以
    //预投票，然后正式投票选举出新的领导者了，该方法返回false，就意味着当前领导者无效了
    //也就可以让跟随者进行领导者选举了
    private boolean isCurrentLeaderValid() {
        //下面这行代码的意思就是用当前时间减去当前节点最后一次收到领导者信息的时间
        //如果这个时间差超过了用户设置的超时选举的时间，那就可以在超时选举定时器中进行投票选举领导者的活动了
        return Utils.monotonicMs() - this.lastLeaderTimestamp < this.options.getElectionTimeoutMs();
    }

    
    //其他方法省略

    
}
```
在初步实现的 handleElectionTimeout 方法中，会先判断当前节点是否达到了触发超时选举的条件了，如果没有达到条件，就不会进入超时选举阶段。那如果达到了可以触发超时选举的条件呢？这部分逻辑就是接下来我们要实现的。还是按照老规矩，实现之前先分析，先分析实现逻辑，也就是代码要执行的每一个步骤。当然，我觉得也没必要分析得特别仔细了，因为之前已经把这一部分的逻辑分析过了，要进行的操作肯定三个：**将当前节点状态更新为候选者，停止超时选举定时器工作，之前已经解释过了，候选者和领导者是不用启动自己的超时选举定时器的，这里我要多说一句，假如当前节点更新为候选者状态，并且之后选举领导者成功了，因为在更新为候选者状态时就停止超时选举定时器工作了，所以成为领导者之后，领导者也没有启动自己的超时选举定时器。假如当前节点成为候选者后没有成功当选为领导者，那一定是集群中的另外节点当选成功了，当集群中的领导者通知当前节点有了领导者时，当前节点会调用 stepDown 方法将自己进行身份降级，更新节点状态为跟随者，并且在 stepDown 方法中会重新启动自己的超时选举定时器。第三个要执行的操作肯定就是向其他节点发送请求，等待其他节点投票。**<br />好了，我不得不又一次把主题岔开了，因为我为大家引入了这个投票的概念，所以我想简单解释几句。投票这个概念是 raft 共识算法中的，具体过程是这样的：**当前节点的状态更新为候选者之后，会将自己的任期加 1，然后找到自己本地最新的一条日志，因为引入了任期，所以用当前节点的日志和其他跟随者节点本地最新的日志比较时，就不能只比较日志索引了，应该比较日志的任期和日志的索引。也就是说，raft 集群中的日志并不单单拥有索引，还拥有索引对应的任期，其意义就是表明该条日志是在哪个任期中生成的索引为几的日志。这样一来，假如集群中有两条日志，这两条日志位于不同的节点上，一条日志的索引为 15，日志任期为 3，而另一条日志的索引也为 15，但是任期为 4，这样两条日志比较的时候，我们就认为第二条日志比第一条日志更新，因为第二条日志的任期更大。拥有第二条日志的节点如果接收到拥有第一条日志节点的申请成为领导者的请求后，肯定不会给它回复成功响应。当两条日志的任期相同时，就可以直接比较索引，谁的索引大谁就比较新。因此，一个候选者节点想要申请成为领导者，向其他跟随者节点发送请求时，会将自增过的任期，以及本地最新日志的索引和任期都封装到请求体中，发送给集群中的其他节点。其他节点在收到请求后会先对比请求中的任期和自己任期的大小，如果请求中的任期大于自己的任期(任期相等的情况，我们下一章再分析)，那就先把自己的任期更新为请求中的任期，然后对比日志，如果本地的日志没有比候选者发送过来的日志新，并且当前节点还没有给其他节点回复过成功响应，那么当前节点就可以给发送请求的候选者节点回复一个成功响应。这个成功响应，在 raft 共识算法中就叫做投票，跟随者给候选者投票了。**<br />这就是对投票这个概念的解释，可以看出，其实这个流程在第三章已经讲解过了，就是从节点选举主节点的流程，只不过在本章给这个选举主节点的流程命名为投票了。总结下来其实就一句话：**如果当前节点发现候选者发送的任期比自己的小，就可以直接忽略候选者发送的请求，然后回复一个响应，在响应中把自己的任期告诉候选者，这样候选者知道已经有任期比自己大了，就会让自己身份降级，变成跟随者；如果候选者的任期比自己的大，那就先更新自己的任期，然后再对比日志，接着再判断自己是否已经给其他节点投过票了，当所有条件都满足时，才会给发送请求过来的候选者投票。**整个流程概括下来，就这点东西，可以说逻辑是非常清晰的。<br />但是，请大家再仔细想一想，这么做有什么弊端吗？是想一下，当一个节点触发了超时选举，成为了候选者，然后向集群中的其他节点申请投票，假如这个候选者没有成功当选为领导者，那该怎么办呢？这时候这个候选者已经将自己的超时选举定时器终止了，如果它没有当选领导者，看起来只能等待真正当选的领导者向它发送消息，然后该节点调用 stepDown 方法，将自己身份降级，从候选者状态更新为跟随者状态(这时候大家可以再回头看看 stepDown 方法的逻辑，应该就会明白很多了)。但是，会不会有另外一种情况呢？那就是当前候选者节点发现自己一直没有接收到足够的票数，也就是它收到的票数始终没有超过集群中过半节点的数量，也许它会不甘心，在等待了一段时间之后，会主动增加自己的任期，开始进入下一轮领导者选举。这种情况也是可能的，并且在 sofajraft 框架中专门为候选者节点定义了一个选举超时定时器，注意，之前我们引入的叫超时选举定时器，现在定义的这个叫选举超时定时器，大家不要弄混了。选举超时定时器的作用就是当发现当前节点为候选者节点，并且等待投票数量超过节点一半时超时了，就会让当前候选者节点进入下一轮选举。这也就意味着当前节点会再次自增自己的任期，然后给其他节点发送申请投票的请求。这里我就不详细讲解这个超时选举定时器了，下一小节再为大家详细展开。<br />现在，让我们的注意力再回到刚才的问题，那就是整个投票选举流程有没有什么弊端？弊端肯定是有的，比如说，假如集群中有 5 个节点，1 个领导者 4 个跟随者，集群正常工作了一段时间，忽然有一个跟随者和其他 4 个节点发生了网络分区，就比如说节点 2 吧，它就是一个跟随者，它发生了网络分区，和其他节点无法取得联系了。显然，过一段时间它肯定会触发超时选举，这是毋庸置疑的。当它发触发了超时选举时会怎么样呢？肯定会把自己的状态更新为候选者，然后把自己的任期自增 1，找到本地最新日志的索引和任期，将这些信息封装到请求体中，发送给集群中其他节点。但是别忘了，它发生了网络分区，无法和其他节点取得联系。所以它发出去的消息注定会石沉大海，收不到一个投票回复。这样一来，**候选者节点的选举超时定时器就会发挥作用了**，这个定时器会将当前节点的任期再次自增 1，进入下一轮领导者选举，当然，这么做仍然是徒劳的，无法收到回复。但是这个节点又不知道为什么收不到回复，它只会死板地执行程序员设定好的程序。所以，最后可能会出现这样一种结果：**那就是在当前节点发生网络分区期间，这个节点在选举超时定时器的作用下，将自己的任期自增到了一个非常高的数值，这个数值比集群中正常领导者的任期要高出很多。比如当前节点的任期是 15，领导者的任期是 5。当网络分区恢复之后，领导者会把每一条日志复制给当前节点，但是根据 raft 共识算法，当前节点在接收到领导者发送过来的复制日志的请求时，会发现领导者的任期比自己小很多，所以就会忽略领导者发送过来的请求，然后给领导者回复一个响应，在响应中把自己的任期告诉领导者。当领导者接收到当前节点的响应后，发现集群中有一个节点的任期竟然比自己高出很多，这他妈不邪门了吗？本来领导者正认真工作呢，现在有一个家伙告诉自己你的任期太低了，快执行 stepDown 方法进行身份降级吧，老老实实成为跟随者。**请诸位想一想，这样一来，集群不就出现问题了吗？说了这么多，无非就是一点，那就是如果一个跟随者发生了网络分区，不能放任这个节点无线自增自己的任期，否则网络分区恢复之后，这个节点的任期会干扰正常领导者工作。<br />很好，费了我老大劲，到此为止，就终于把这个问题给大家解释清楚了，我希望大家能自己仔细分析分析，结合我的文章，彻底理解问题是怎么产生的，不要走马观花，只在脑海里留下一个模糊的印象，就觉得自己已经完全了解这个问题的前因后果了。当然，问题已经分析清楚了，接下来就该探讨如何解决这个问题了。<br />其实解决问题的方法非常简单，那就是在当前节点成为候选者之后，开始正式进行领导者选举之前，先执行一个预投票的操作。如果预选举没有成功，那么当前节点就不会进入正式选举领导者阶段，任期也就不会自增了。如果预选举成功了，再让当前节点开始正式进入领导者选举阶段。我知道，这时候大家都期待着我立刻解释预选举的概念，想知道预选举的操作究竟是什么。但是还不太急，现在，别忘了我们这一小节的主题，那就是实现 handleElectionTimeout 方法。所以，在这一小节的结尾，让我先为大家展示一下 handleElectionTimeout 方法，然后再具体解释预选举的概念。请看下面代码块。
```java
public class NodeImpl implements Node{

	//其他内容省略


    //超时选举处理处理方法，这个方法就是超时选举定时器要调用的方法
    private void handleElectionTimeout() {
        
        //判断当前节点的状态，如果当前节点不是跟随者的状态，就不能进行投票选举
        //因为当节点正式开始进行选举领导者操作时，会将自己的状态从跟随者更新为候选者
        //也就是说候选者是从跟随着转换过去的，如果连跟随者都不是，怎么转换成候选者呢？
        if (this.state != State.STATE_FOLLOWER) {
            return;
        }
        //这里的这个判断非常重要，超时选举定时器启动之后，只要节点状态是跟随着，就会一直工作
        //每过一段时间就会触发一次，但是触发了并不意味着就要真的去进行选举，会在这里进行判断
        //看看距离当前节点最后一次收到领导者信息过去的多少时间，如果这个时间超过了超时选举时间，才会进入预投票阶段
        //否则直接退出，这里返回true就代表还没超时呢
        if (isCurrentLeaderValid()) {
            return;
        }
        //既然是要超时选举了，说明领导者已经没有了，在这里对当前节点存放的领导者信息进行重置
        //这里我想再为大家解释一下，可能有朋友觉得已经在stepDown方法中执行了下面这个方法了
        //为什么在这里还要再执行一次呢？我想说的是，大家不要主观代入，不要很主观地就认为stepDown方法
        //和handleElectionTimeout方法存在强因果关系
        //请大家思考一下，假如一个跟随者节点一直在集群中正常工作，超时选举定时器也一直在启动着
        //过了一段时间没有收到领导者的信息，触发了超时选举，这时候超时选举定时器就会直接执行这个
        //handleElectionTimeout方法了，所以就要在这里重置当前节点记录的领导者信息
        resetLeaderId(PeerId.emptyPeer());
        //开始预投票
        preVote();
    
    }


    //判断是否触发选举超时了，如果触发了就可以让选举定时器进行工作了，在选举定时器中就可以
    //预投票，然后正式投票选举出新的领导者了，该方法返回false，就意味着当前领导者无效了
    //也就可以让跟随者进行领导者选举了
    private boolean isCurrentLeaderValid() {
        //下面这行代码的意思就是用当前时间减去当前节点最后一次收到领导者信息的时间
        //如果这个时间差超过了用户设置的超时选举的时间，那就可以在超时选举定时器中进行投票选举领导者的活动了
        return Utils.monotonicMs() - this.lastLeaderTimestamp < this.options.getElectionTimeoutMs();
    }

    
    //其他方法省略

    
}
```
代码已经展示完毕了，可以看到，在 handleElectionTimeout 方法中进行的是预投票操作，接下来我就来为大家解释一下什么是预投票。

<a name="Rw9A8"></a>
## 引入投票计数器，实现预投票操作


实际上，预投票的概念非常简单，我可以负责任地告诉大家，预投票的流程和正式进行领导者选举的流程没什么大区别。**所谓预投票，就是让当前的候选者节点向集群中的其他节点发送请求，在请求中将自己的任期自增 1，注意，这里并不是更新自己的本地任期了，只是把请求中的任期自增了 1，如果给本地任期是 3，那么再给其他节点发送预投票请求的时候，会把任期 4 封装在请求中，自己本地的任期还是 3，然后找到本地最新日志的索引和任期，随着请求一起发送给其他节点。如果收到的票数超过了集群半数节点，就意味着当前候选者节点预投票操作成功了，接下来就可以正式进行领导者选举操作了。**<br />这时候大家可以思考思考这个预投票操作的必要性了，如果当前候选者节点发生了网络分区，**在选举超时之后，注意，是选举超时之后**，**选举超时定时器就可以先执行预投票操作，当没有收到足够的票数之后，当前节点就不会进入领导者正式选举阶段，而自己的本地任期也不会增加**。这样，当网络分区恢复之后，集群的领导者仍然可以正常工作。这个逻辑应该很清晰了吧？当然，到此为止，其实我也已经给大家把预投票的操作讲解完了，完全可以直接为大家展示 preVote 这个预投票操作的代码了。但是，还有最后一个问题，需要跟大家讨论，那就是当前节点怎么就知道自己收到了足够的票数呢？<br />这里我就不再大费周章地讲解了，因为这个功能实现起来非常简单，在 sofajraft 框架中，有一个名为 Ballot 的类，我一般把这个类成为投票计数器。这个类也是 NodeImpl 类的成员变量，并且会在 NodeImpl 对象被创建的时候就创建好了。具体逻辑是这样的：**因为 NodeImpl 在执行完 init 方法后，已经能够得到集群的配置信息了，也就是已经知道集群中一共有多少节点了，并且 Ballot 对象早就创建完成了。那么，当节点执行预投票操作的时候，会在 preVote 方法中调用 Ballot 对象的 init 方法，并且用集群配置信息来初始化这个 Ballot 对象。这样 Ballot 对象就知道集群中一共有多少节点了，也就可以在内部计算出收到多少投票就超过集群半数节点的数量了，也就意味着预投票操作成功了**。比如说当前节点的 Ballot 投票计数器计算出来，至少收到 3 个投票，预投票操作才算成功，那么节点没收到一次投票时，都会告诉 Ballot 对象，Ballot 对象就会用 3 减去 1，直到结果剪到 0，就意味着当前节点收到 3 个投票了，也就意味预投票操作成功了。<br />接下来，我就为大家展示一下这个 Ballot 类，然后再展示一下重构之后的 NodeImpl 类，这次重构把 Ballot 成员变量加上了，也展示了具体的 preVote 方法。请看下面代码块。<br />首先是 Ballot 类。
```java
//计算投票结果的类，整个类的原理实际上是做减法，就是得到集群中所有参与选举的节点
//然后计算出最少需要的票数，每得到一票就从需要得到的票数中减一，直到减为0，就说明获得足够的票数了
//比如说一个集群中有5个节点，那么当前节点至少需要3票就可以成功当选领导者了，所以没得到1票就让3减1，直到结果为0，就说明可以成功当选了
public class Ballot {

    //该内部类对象是用来表示peerId在集合中未知的，方便快速查找
    public static final class PosHint {
        //这个成员变量表示节点在peers集合中的位置
        //初始化为-1
        int pos0 = -1;
        //如果一个节点是旧配置中的，那么就用下面这个成员变量代表
        //其在oldPeers中的位置
        int pos1 = -1;
    }

    //这个内部类的对象会封装一个个节点，并且给这个节点添加一个未找到的标记
    //然后把这个节点存放到集合中，如果节点是属于当前配置中的就存放奥peers集合中
    //否则存放到oldPeers集合中
    //等当前节点收到集合中存放的节点的投票时，会去集合中查找，看看有没有对应的节点
    //如果有就把该节点标记为已找到，并且把投票数计入结果，说明成功收到投票了
    public static class UnfoundPeerId {
        //节点信息
        PeerId peerId;
        //节点是否被找到，UnfoundPeerId对象刚创建的时候
        //默认这个成员变量为false，代表着该节点对象还没被找到
        boolean found;
        //节点索引
        int index;

        //构造方法
        public UnfoundPeerId(PeerId peerId, int index, boolean found) {
            super();
            this.peerId = peerId;
            this.index = index;
            this.found = found;
        }
    }

    //存放当前配置中的节点信息
    private final List<UnfoundPeerId> peers = new ArrayList<>();
    //当前配置中需要收到的最小票数，才能成功当选领导者或者进入正式投票节点
    private int quorum;
    //存放旧配置中的节点信息
    private final List<UnfoundPeerId> oldPeers = new ArrayList<>();
    //旧配置中需要收到的最小票数，才能成功当选领导者或者进入正式投票节点
    private int oldQuorum;


    //该类最核心的方法，初始化该类的对象时，会把集群中的节点信息封装到该类中，并且根据集群中
    //节点数量计算出当前节点至少需要收到多少票数，才能成功当选领导者或者进入正式投票阶段
    public boolean init(final Configuration conf, final Configuration oldConf) {
        //先清空两个集合
        this.peers.clear();
        this.oldPeers.clear();
        //需要的最小票数也清零
        this.quorum = this.oldQuorum = 0;
        //定义节点索引，该索引会在创建UnfoundPeerId对象时一直递增，当作每一个节点的索引
        int index = 0;
        //将当前配置中的节点信息存放到集合中
        if (conf != null) {
            for (final PeerId peer : conf) {
                this.peers.add(new UnfoundPeerId(peer, index++, false));
            }
        }
        //计算出最少需要的票数才能成功当选或者进入正式投票阶段
        //这里的成员变量是针对当前配置的
        this.quorum = this.peers.size() / 2 + 1;
        //下面判断旧配置是否为空
        if (oldConf == null) {
            return true;
        }
        index = 0;
        //如果旧配置不为空，则执行和上面相同的逻辑
        for (final PeerId peer : oldConf) {
            this.oldPeers.add(new UnfoundPeerId(peer, index++, false));
        }
        this.oldQuorum = this.oldPeers.size() / 2 + 1;
        return true;
    }


    //根据给订的PeerId，判断集合中是否存在对应节点
    private UnfoundPeerId findPeer(final PeerId peerId, final List<UnfoundPeerId> peers, final int posHint) {
        //这里先判断了一下代表节点在集合中的位置是否是正确的，当一个节点被包装成UnfoundPeerId对象的时候，它在集合中的位置是-1
        //显然是不合理的，这种情况下应该遍历集合，根据对应的peerId找出对应节点，然后根据节点的index来更新自己在集合中的位置
        //也就是更新PosHint对象中的索引
        if (posHint < 0 || posHint >= peers.size() || !peers.get(posHint).peerId.equals(peerId)) {
            for (final UnfoundPeerId ufp : peers) {
                if (ufp.peerId.equals(peerId)) {
                    return ufp;
                }
            }
            return null;
        }
        //如果走到这里说明节点在集合中的位置已经更新了，直接根据位置获得节点即可
        return peers.get(posHint);
    }


    //给当前节点投票的方法
    public PosHint grant(final PeerId peerId, final PosHint hint) {
        //在peers集合中查找对应的节点
        UnfoundPeerId peer = findPeer(peerId, this.peers, hint.pos0);
        if (peer != null) {
            //找到的话意味着方法参数中的那个peerId节点的投票是有效的
            //这个peerId在集群中
            if (!peer.found) {
                //将这个节点标记为已找到，或者说是被发现
                peer.found = true;
                //需要的票数减1，减到0则意味着票数够了
                this.quorum--;
            }
            //更新节点在集合中的位置
            hint.pos0 = peer.index;
        } else {
            //如果没找到，就把位置设置为-1
            hint.pos0 = -1;
        }
        //判断一下旧配置的节点集合是否为空
        if (this.oldPeers.isEmpty()) {
            hint.pos1 = -1;
            return hint;
        }
        //如果不为空，就执行和上面相同的逻辑，这里的当前配置和旧配置先不必关心
        //后面将配置变更的时候，会在文章里详细讲解
        peer = findPeer(peerId, this.oldPeers, hint.pos1);
        if (peer != null) {
            if (!peer.found) {
                peer.found = true;
                this.oldQuorum--;
            }
            hint.pos1 = peer.index;
        } else {
            hint.pos1 = -1;
        }
        return hint;
    }


    //给当前节点投票的方法，方法中传进来的这个peerId，就是给当前节点投票的节点
    public void grant(final PeerId peerId) {
        //在下面这个方法中，真正执行投票的逻辑
        //注意，这里创建了一个PosHint对象，这个对象的作用
        //是用来表示peerId在集合中未知的，方便快速查找
        grant(peerId, new PosHint());
    }


    //该方法就是用来判断是否收到足够的票数了，因为没收到一票quorum就会减1，直到为0则意味着收到足够票数了
    public boolean isGranted() {
        return this.quorum <= 0 && this.oldQuorum <= 0;
    }

```
该类的逻辑非常简单，注释也十分详细，我就不再重复讲解了。接下来，请大家看看重构之后的 NodeImpl 类。
```java
public class NodeImpl implements Node{

    //计算预投票结果的计数器
    private final Ballot prevVoteCtx = new Ballot();

	//其他内容省略


    //超时选举处理处理方法，这个方法就是超时选举定时器要调用的方法
    private void handleElectionTimeout() {
        
        //判断当前节点的状态，如果当前节点不是跟随者的状态，就不能进行投票选举
        //因为当节点正式开始进行选举领导者操作时，会将自己的状态从跟随者更新为候选者
        //也就是说候选者是从跟随着转换过去的，如果连跟随者都不是，怎么转换成候选者呢？
        if (this.state != State.STATE_FOLLOWER) {
            return;
        }
        //这里的这个判断非常重要，超时选举定时器启动之后，只要节点状态是跟随着，就会一直工作
        //每过一段时间就会触发一次，但是触发了并不意味着就要真的去进行选举，会在这里进行判断
        //看看距离当前节点最后一次收到领导者信息过去的多少时间，如果这个时间超过了超时选举时间，才会进入预投票阶段
        //否则直接退出，这里返回true就代表还没超时呢
        if (isCurrentLeaderValid()) {
            return;
        }
        //既然是要超时选举了，说明领导者已经没有了，在这里对当前节点存放的领导者信息进行重置
        //这里我想再为大家解释一下，可能有朋友觉得已经在stepDown方法中执行了下面这个方法了
        //为什么在这里还要再执行一次呢？我想说的是，大家不要主观代入，不要很主观地就认为stepDown方法
        //和handleElectionTimeout方法存在强因果关系
        //请大家思考一下，假如一个跟随者节点一直在集群中正常工作，超时选举定时器也一直在启动着
        //过了一段时间没有收到领导者的信息，触发了超时选举，这时候超时选举定时器就会直接执行这个
        //handleElectionTimeout方法了，所以就要在这里重置当前节点记录的领导者信息
        resetLeaderId(PeerId.emptyPeer());
        //开始预投票
        preVote();
    
    }


    //判断是否触发选举超时了，如果触发了就可以让选举定时器进行工作了，在选举定时器中就可以
    //预投票，然后正式投票选举出新的领导者了，该方法返回false，就意味着当前领导者无效了
    //也就可以让跟随者进行领导者选举了
    private boolean isCurrentLeaderValid() {
        //下面这行代码的意思就是用当前时间减去当前节点最后一次收到领导者信息的时间
        //如果这个时间差超过了用户设置的超时选举的时间，那就可以在超时选举定时器中进行投票选举领导者的活动了
        return Utils.monotonicMs() - this.lastLeaderTimestamp < this.options.getElectionTimeoutMs();
    }


    //预投票的方法，raft协议中好像触发了超时选举任务时，就会直接进入预投票阶段了
	private void preVote() {
        
        //记录开始预投票的日志
        LOG.info("Node {} term {} start preVote.", getNodeId(), this.currTerm);
        //根据当前节点的ID去配置信息中查看当前节点是否为集群中的节点，如果不是集群中的节点就直接退出
        if (!this.conf.contains(this.serverId)) {
            LOG.warn("Node {} can't do preVote as it is not in conf <{}>.", getNodeId(), this.conf);
            return;
        }
        //定义一个变量记录当前节点任期
        long oldTerm; = this.currTerm;

        
        //这里的代码本来是获得当前节点最后一条日志，源码是下面这样的
        //final LogId lastLogId = this.logManager.getLastLogId(true);使用日志管理器来获得最后一条日志信息
        //但是第一版本并没有引入日志管理器，所以我就直接写死了，写成了下面这样，在第一版本的其他方法中还会见到下面这种写死的代码
        //其实源码都是final LogId lastLogId = this.logManager.getLastLogId(true)这样的，后面我会为大家重构完整的
        //在这里得到了本地日志的任期和索引，任期和索引都封装在了LogId对象中，因为这里我们模拟的实际上是第一次启动集群的情况
        //第一次启动集群时，集群中还没有日志，节点的任期也为0，所以这里就把最新日志的索引和任期写死了
        final LogId lastLogId = new LogId(0,0);
 
        //初始化预投票计数器，当前节点收到的投票结果都会被prevVoteCtx这个成员变量收集，在该成员变量中计算
        //如果收到的预投票的票数大于一半，就会进入正式投票状态
        //这里初始化预投票计算器其实就是把配置类中的节点信息交给prevVoteCtx对象
        //这样这个prevVoteCtx预投票计算器就知道集群中有多少节点了，也知道得到的预投票的票数超过多少就可以进入正式投票状态了
        this.prevVoteCtx.init(this.conf.getConf(), this.conf.isStable() ? null : this.conf.getOldConf());
        
        //遍历配置中的节点
        for (final PeerId peer : this.conf.listPeers()) {
            //如果发现这个节点就是当前节点，就跳过这次循环
            if (peer.equals(this.serverId)) {
                continue;
            }
            //连接遍历到的这个节点，如果不能成功连接，就记录日志
            if (!this.rpcService.connect(peer.getEndpoint())) {
                LOG.warn("Node {} channel init failed, address={}.", getNodeId(), peer.getEndpoint());
                continue;
            }
            //创建一个回调对象，这个对象中实现了一个回调函数，该回调函数会在当前接收到预投票请求的响应后被回调
            //构造方法中的peer代表要把消息送到的目标节点，第二个则是当前节点发送请求消息前的任期
            //这里大家先不必关心这个封装了回调方法的对象
            final OnPreVoteRpcDone done = new OnPreVoteRpcDone(peer, this.currTerm);
            
            //向被遍历到的这个节点发送一个预投票请求
            //下面就是构建预投票请求的内容
            done.request = RpcRequests.RequestVoteRequest.newBuilder()
                    //设置为true意味着是预投票请求
                    .setPreVote(true)
                    //设置节点所属集群的id
                    .setGroupId(this.groupId)
                    //设置节点当前的IP地址和端口号
                    .setServerId(this.serverId.toString())
                    .setPeerId(peer.toString())
                    //在发送请求时发送的任期值为当前节点的任期值加1
                    .setTerm(this.currTerm + 1)
                    //当前节点最后一条日志的索引
                    .setLastLogIndex(lastLogId.getIndex())
                    //最后一条日志的任期
                    .setLastLogTerm(lastLogId.getTerm())
                    .build();
            //发送预投票请求
            this.rpcService.preVote(peer.getEndpoint(), done.request, done);
        }
        //给自己投一票，虽然节点当前还没有把自己更新为候选者状态，在预投票通过后，正式进行领导者选举时才会把自己更新为候选者状态，然后投自己一票
        //但是预投票流程和正式选举流程差不多，所以也投自己一票
        this.prevVoteCtx.grant(this.serverId);
        //这里判断了一下，看是否得到了超过集群中半数节点的投票
        if (this.prevVoteCtx.isGranted()) {
            //如果票数超过了集群半数节点，意味着预投票成功了，开始正式进行投票即可
            //这个方法就是正式进行领导者选举的方法，在下一章再讲解
            electSelf();
        }
    }
    


    
    
    //其他方法省略

    
}
```
好了，预投票方法已经展示给完毕了，这个方法是由率先触发了超时选举的节点调用的，在这个方法中，当前节点会把所有预投票的请求发送给集群中的其他节点。那么，其他节点在接收到请求后该怎么处理呢？这里我就没必要卖关子了，方法也早就定义好了，就叫 handlePreVoteRequest，翻译过来就是处理预投票请求。该方法也在 NodeImpl 类中，因为都是节点本身的功能，都是节点自己的操作，所以就都定义在 NodeImpl 类中了。当集群中的一个节点接收到了来自其他节点的索要预投票的请求，这个节点就会调用 handlePreVoteRequest 方法来处理这个请求。具体实现请看下面代码块。
```java
public class NodeImpl implements Node{

    //其他内容省略


    //该方法就是专门用来处理预投票请求的，当前节点接收到另一个节点预投票的请求时，就会通过这个方法来处理
    //这个方法的参数就是集群中的候选者节点发送过来的RPC预投票请求
    @Override
    public Message handlePreVoteRequest(final RpcRequests.RequestVoteRequest request) {
       
        //检查当前节点是否处于活跃状态
        if (!this.state.isActive()) {
            LOG.warn("Node {} is not in active state, currTerm={}.", getNodeId(), this.currTerm);
            return RpcFactoryHelper.responseFactory()
                    .newResponse(RpcRequests.RequestVoteResponse.getDefaultInstance(), RaftError.EINVAL,
                            "Node %s is not in active state, state %s.", getNodeId(), this.state.name());
        }
        
        //创建一个PeerId对象，这个对象是用来解析候选者节点信息的
        final PeerId candidateId = new PeerId();
        //先从请求中获取候选者节点信息，也就是发送预投票请求的节点信息
        if (!candidateId.parse(request.getServerId())) {
            LOG.warn("Node {} received PreVoteRequest from {} serverId bad format.", getNodeId(),
                    request.getServerId());
            //解析失败则发送失败响应
            return RpcFactoryHelper.responseFactory()
                    .newResponse(RpcRequests.RequestVoteResponse.getDefaultInstance(), RaftError.EINVAL,
                            "Parse candidateId failed: %s.", request.getServerId());
        }
        
        //定义一个投票变量，false表示当前节点还没有给候选者投票
        boolean granted = false;
        do {
            
            //现在就进入循环了，但是这里我想强调一下，这个循环是个伪循环，因为while中的条件为false，意味着这个循环只循环一次
            //这里定义这个循环只是为了能使用berak，以此来处理不同的情况
            //首先先判断一下候选者节点是否在当前的集群配置中
            if (!this.conf.contains(candidateId)) {
                LOG.warn("Node {} ignore PreVoteRequest from {} as it is not in conf <{}>.", getNodeId(),
                        request.getServerId(), this.conf);
                //不在则退出循环，不必往下进行了
                break;
            }
            
            //这里又进行了一步判断，先判断当前节点记录的领导者信息是否为空，如果不为空说明在当前节点看来领导者还在呢，然后
            //又判断了一下最后一次接收领导者信息是否超过选举超时时间了，如果没超过，当前节点记录的领导者显然还是有效领导者，这种情况也忽略候选者节点的预投票请求
            //出现这种情况，可能是因为网络延迟，有的跟随者没有及时接收到领导者的心跳信息，因此就转变成候选者开始进行预投票活动了
            //注意，这里条件判断是&&，也就是说下面三个条件都成立才会直接退出方法，忽略本次收到的预投票请求
            if (this.leaderId != null && !this.leaderId.isEmpty() && isCurrentLeaderValid()) {
                LOG.info(
                        "Node {} ignore PreVoteRequest from {}, term={}, currTerm={}, because the leader {}'s lease is still valid.",
                        getNodeId(), request.getServerId(), request.getTerm(), this.currTerm, this.leaderId);
                break;
            }
            
            //判断候选者节点的任期是否小于当前节点任期，小于的话也忽略预投票请求
            if (request.getTerm() < this.currTerm) {
                LOG.info("Node {} ignore PreVoteRequest from {}, term={}, currTerm={}.", getNodeId(),
                        request.getServerId(), request.getTerm(), this.currTerm);
                //这个方法在第一版本不用关注，因为我根本就没做实现
                checkReplicator(candidateId);
                break;
            }
            
            //可以看到，这里又进行了一次复制器检查，其实在这个方法内部会判断当前接收到预投票请求的节点是不是领导者
            //如果是领导者，并且领导者并没有出故障，所以就根据候选者节点信息去复制器组中检查一下，如果复制器组中没有与候选者节点
            //对应的复制器对象，就为这个候选者节点创建一个复制器节点，创建完毕后，也会给候选者发送心跳消息，让候选者重新成为跟随者
            //同样先不必关心该方法
            checkReplicator(candidateId);
            
            //获取当前节点的最后一条日志的ID，这里我把源码注释掉了
            //final LogId lastLogId = this.logManager.getLastLogId(true);
            //从请求中获得候选者节点的最后一条日志的ID
            final LogId requestLastLogId = new LogId(request.getLastLogIndex(), request.getLastLogTerm());
            
            //如果候选者节点的最后一条日志的ID和任期大于当前节点，当前节点就给候选者投票
            //这里我把源码注释掉了，因为日志组件引入的并不完整
            //granted = requestLastLogId.compareTo(lastLogId) >= 0;
            //这行代码是我自己添加的，改成默认投票了
            //预投票的步骤中并没有对当前节点做了只能投一次票的限制
            granted = true;
            LOG.info("Node {} received PreVoteRequest from {}, term={}, currTerm={}, granted={}, requestLastLogId={}.",
                    getNodeId(), request.getServerId(), request.getTerm(), this.currTerm, granted, requestLastLogId);
            //直接退出循环
        } while (false);
        
        //构建给候选者节点返回的响应
        return RpcRequests.RequestVoteResponse.newBuilder()
                .setTerm(this.currTerm)
                //这里参数为true，意味着当前节点给候选者投票了
                .setGranted(granted)
                .build();
        } 
    }



    //其他内容省略
    
}
```
现在已经把预投票的发起方法，以及预投票请求的处理方法都给大家实现了，就差一个预投票响应的处理方法了。简而言之，就是当发起预投票操作的节点接收到集群中其他节点回复的关于预投票请求的响应时，该怎么处理这个响应呢？我把具体的实现方法就定义在下面的代码块中了，该方法就定义为了 handlePreVoteResponse。请看下面代码块。
```java
public class NodeImpl implements Node{

    //其他内容省略


    //如果当前节点向其他节点发送了索要预投票的请求，收到其他节点的响应后就会调用这个方法
    //处理响应，这个方法中term参数代表的是当前候选者节点发送索要投票请求之前的任期，这个逻辑可以从OnPreVoteRpcDone类的run方法中查看
    public void handlePreVoteResponse(final PeerId peerId, final long term, final RpcRequests.RequestVoteResponse response) {
        
        //如果当前节点的身份不再是跟随者，就直接退出该方法
        //这行代码会在什么情况下出现呢？发起预投票的节点在收到预投票之后不是跟随者了？
        //一般情况下好像不可能出现这种情况，这个节点不可能成为候选者，只有进行正式投票时才会将自己身份转换为候选者
        //但是进行正式投票的入口方法在该方法中呀，还没有执行到呢，这个节点更不可能成为领导者了
        //除非是有网络延迟，这个节点已经成为领导者或者候选者了，它之前发送给某个节点的预投票请求才刚刚收到响应
        if (this.state != State.STATE_FOLLOWER) {
            LOG.warn("Node {} received invalid PreVoteResponse from {}, state not in STATE_FOLLOWER but {}.",
                    getNodeId(), peerId, this.state);
            return;
        }
        
        //判断当前节点的前后任期是否一致，不一致则直接退出该方法
        if (term != this.currTerm) {
            LOG.warn("Node {} received invalid PreVoteResponse from {}, term={}, currTerm={}.", getNodeId(),
                    peerId, term, this.currTerm);
            return;
        }
        
        //如果接收到响应后发现请求中的任期，也就是回复响应的节点的任期比自己大
        //也直接退出该方法，并且让自己成为跟随者，处理预请求响应，正常情况下，没有发生分区故障的话，响应的节点应该和当前发起预投票的任期一样
        if (response.getTerm() > this.currTerm) {
            LOG.warn("Node {} received invalid PreVoteResponse from {}, term {}, expect={}.", getNodeId(), peerId,
                    response.getTerm(), this.currTerm);
            //执行了身份降级方法
            stepDown(response.getTerm());
            return;
        }
        LOG.info("Node {} received PreVoteResponse from {}, term={}, granted={}.", getNodeId(), peerId,
                response.getTerm(), response.getGranted());
        
        //从响应中判断回复响应的节点是否给当前节点投票了
        if (response.getGranted()) {
            //如果投票了，就在这里收集票数到计票器中
            this.prevVoteCtx.grant(peerId);
            //如果获得票数超过集群节点一半，就进入正式选举阶段
            if (this.prevVoteCtx.isGranted()) {
                //开始正式投票，下面这个方法还没有实现
                electSelf();
            }
        }
    } 

    


    //其他内容省略
    
}
```
处理响应的方法也实现了，那么在什么时候调用这个处理响应的方法呢？当然是在收到响应之后就调用该方法。如果大家对之前的代码块还有印象，肯定还记得在 preVote 方法中有这样一段代码。请看下面代码块。
```java
//创建一个回调对象，这个对象中实现了一个回调函数，该回调函数会在当前接收到预投票请求的响应后被回调
//构造方法中的peer代表要把消息送到的目标节点，第二个则是当前节点发送请求消息前的任期
//这里大家先不必关心这个封装了回调方法的对象
final OnPreVoteRpcDone done = new OnPreVoteRpcDone(peer, this.currTerm);

//向被遍历到的这个节点发送一个预投票请求
//下面就是构建预投票请求的内容
done.request = RpcRequests.RequestVoteRequest.newBuilder()
        //设置为true意味着是预投票请求
        .setPreVote(true)
        //设置节点所属集群的id
        .setGroupId(this.groupId)
        //设置节点当前的IP地址和端口号
        .setServerId(this.serverId.toString())
        .setPeerId(peer.toString())
        //在发送请求时发送的任期值为当前节点的任期值加1
        .setTerm(this.currTerm + 1)
        //当前节点最后一条日志的索引
        .setLastLogIndex(lastLogId.getIndex())
        //最后一条日志的任期
        .setLastLogTerm(lastLogId.getTerm())
        .build();
//发送预投票请求，这里把封装回调方法的done也穿进发送请求的方法中了
this.rpcService.preVote(peer.getEndpoint(), done.request, done);
```
这时候逻辑就清楚一些了，在 sofajraft 框架中，rpc 通信机制采用的都是异步发送，并不会同步等待响应回复，然后再处理响应，而是异步发送，等接收到响应后，再执行之前定义的回调方法。当然，rpc 的具体细节还没必要现在就讲解，大家只要理解这个机制就行了。所以，接下来我们看看这个 OnPreVoteRpcDone 对象究竟是什么就行了。这个 OnPreVoteRpcDone 实际上就是 NdeImpl 的一个内部类，具体逻辑请看下面代码块。
```java
public class NodeImpl implements Node{

  

	//其他内容省略


	//这个类的对象中封装着一个回调方法，该方法会在候选者节点接收到预投票的响应之后被回调
    private class OnPreVoteRpcDone extends RpcResponseClosureAdapter<RpcRequests.RequestVoteResponse> {

        final long         startMs;
        final PeerId       peer;
        final long         term;
        RpcRequests.RequestVoteRequest request;

        public OnPreVoteRpcDone(final PeerId peer, final long term) {
            super();
            this.startMs = Utils.monotonicMs();
            this.peer = peer;
            this.term = term;
        }

        //该方法会被回调
        @Override
        public void run(final Status status) {
            NodeImpl.this.metrics.recordLatency("pre-vote", Utils.monotonicMs() - this.startMs);
            if (!status.isOk()) {
                LOG.warn("Node {} PreVote to {} error: {}.", getNodeId(), this.peer, status);
            } else {//在这里handlePreVoteResponse方法就被调用了，处理收到的响应
                handlePreVoteResponse(this.peer, this.term, getResponse());
            }
        }
    }
    
    
    //其他方法省略

    
}
```
到此为止，预投票的全部流程我就为大家讲解完毕了，后面这一部分代码块比较多，注释也非常多。我希望大家能够反复观看这些代码，直接去看我提供的第一版本源码也可以，就看 NodeImpl 类就可以了，这时候，这个类的大部分方法大家应该都能看懂了。大家一定要多看几遍，然后自己总结一下文章中展示的每个方法的执行流程，把细节弄清楚，理解每一行代码的用意。我相信通过这种方式，大家会对知识掌握得更牢固。当然，本来梳理每一个方法的流程细节应该是我的任务，但是代码中注释已经特别详细了，每一行代码的用意我都解释清楚了，而且文章的篇幅已经很长了，我再梳理流程细节，也无非是对注释的重复。所以，这个任务就交给大家自己去完成吧。一定要思考，认真研究一下预投票流程中的细节。这一章的内容彻底掌握了之后，后面两章学起来会非常容易，因为正式选举领导者的流程和预投票流程没什么差别。等第一版本讲完之后，我会为大家呈现一个第一版本程序完整的运行流程图。<br />好了各位，我们下一章见！
