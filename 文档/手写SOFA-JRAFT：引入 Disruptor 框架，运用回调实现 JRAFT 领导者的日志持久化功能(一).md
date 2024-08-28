
大家好，在引入了日志组件，并且实现了日志组件初始化之后，从这一章开始我将为大家实现日志罗盘这一功能。实际上接下来我们要实现两个功能，一个是日志落盘，一个是将日志从领导者传输给跟随者节点。这两个功能其实是紧密连接在一起的，完整流程应该是：**客户端向领导者发送了指令，领导者将指令包装成一条日志，在领导者将日志落盘的同时，也会异步将日志传输给集群中的所有跟随者节点，跟随者节点再将接收到的日志成功落盘，落盘成功之后再回复给领导者一个成功响应**。当然，接下来肯定就是领导者根据跟随者节点回复的响应判断是否应该提交对应的日志，但这个不是本章要实现的功能，会在状态机的章节展开讲解。本章的主要内容就是实现在领导者内部，将日志成功落盘的功能。这里我还要再多说一句，**因为在 raft 集群中，不管是领导者还是跟随者，程序执行的都是相同的代码，所以，实现了领导者日志落盘功能，那么跟随者将日志落盘的功能也就实现了。大家理解这一点即可。说白了，这一章我为大家实现的就是日志落盘功能**。

<a name="ZZx6h"></a>
## 引入 apply 方法，重构 NodeImpl 类

在我们目前构建的程序中，日志组件已经初始化完毕了，并且我们也知道了客户端指令是被 LogEntry 对象来包装的，当领导者每接收到一条来自客户端的指令，就会把这条指令包装成一个 LogEntry 对象，也就是说，这条指令被包装成一条日志了。接下来，领导者要做的就是把这条日志持久化到本地，当然，在持久化到本地的同时，还要将这条日志异步发送给集群中的各个跟随者节点。在本章我们先不考虑怎么把日志发送给集群中的各个跟随者节点，让我们先来关注一下，在领导者内部生成的一条日志，究竟该如何进行持久化。这时候，如果要给我们本章的内容寻找一个合理的起点，想必大家已经心中有数了，**肯定就是先找到日志持久化的源头，也就是将客户端指令包装成一条日志，这是我们首先应该实现的操作**。但这个操作其实在上一章，我多少也为大家提到了一点，我稍微为大家展示了一些 Nacos 服务端的源码，在 Nacos 的源码中，将接收到的客户端指令包装成了一条日志。如果有些朋友对上一章的例子感到困惑，接下来，我将为大家提供一个更简单直接的例子，为大家再次实现领导者将客户端指令包装为日志的功能。当然，这个功能的实现离不开 LogEntry 对象，所以在这里，我又一次把 LogEntry 类搬运过来了，请大家简单回顾一下。请看下面代码块。
```java
public class LogEntry {


    public static final ByteBuffer EMPTY_DATA = ByteBuffer.wrap(new byte[0]);

    //日志ID，注意，日志ID中封装着日志的索引和任期
    private LogId id = new LogId(0, 0);

    //真正的日志信息，初始化时为空
    private ByteBuffer data = EMPTY_DATA;

    //日志类型，可能是业务日志，也可能是配置变更日志
    private EnumOutter.EntryType type;

    //当前集群中的节点，这里也许有朋友会感到困惑，日志条目不是只包含日志锁云和任期以及日志内容吗？
    //为什么还要包含集群中的节点信息，这里解释一下，在jraft框架中除了业务日志，还有配置变更日志
    //配置变更日志也是使用当前类的对象来封装的，因此会有以下几个成员变量
    //并且配置日志也是会传输给跟随者的
    private List<PeerId> peers;
    
    //旧配置中的节点
    private List<PeerId> oldPeers;

    //校验和，这个成员变量很重要，在日志传输之后，会在跟随者节点判断校验和是否和之前的相等，以此来判断日志条目是否有损坏
    //这个成员变量可以暂时忽略，等用到的时候我会为大家讲解
    private long checksum;
    
    //是否有校验和
    private boolean hasChecksum;


    public LogId getId() {
        return this.id;
    }

    public void setId(final LogId id) {
        this.id = id;
    }

    public ByteBuffer getData() {
        return this.data;
    }

    public void setData(final ByteBuffer data) {
        this.data = data;
    }

    public List<PeerId> getPeers() {
        return this.peers;
    }

    public void setPeers(final List<PeerId> peers) {
        this.peers = peers;
    }

    public List<PeerId> getOldPeers() {
        return this.oldPeers;
    }

    public void setOldPeers(final List<PeerId> oldPeers) {
        this.oldPeers = oldPeers;
    }
    
}
```
好了，LogEntry 类已经展示完毕了，接下来，我就可以先来模拟一下领导者接收到一条客户端指令的情况了。当然，不同公司经营的业务并不相同，所以每个公司开发的业务层程序肯定各不相同，那么根据业务层封装的客户端指令或者是请求体肯定也各不相同。为了统一领导者包装客户端指令的方式，我记得在第二版本代码中，为大家引入了 ByteBuffer 来包装客户端指令。现在我模拟的客户端指令非常简单，就是一个简单的字符串。比说客户端向领导者发送了一个 “hello” 字符串，那么这个字符串就可以直接被 ByteBuffer 对象包装。就像下面这样。请看下面代码块。
```java
//客户端指令为hello，所以日志的有效内容也就为hello
ByteBuffer data = ByteBuffer.wrap(("hello").getBytes());
```
客户端指令已经被包装为一个 ByteBuffer 对象了，而 LogEntry 对象中也有一个 ByteBuffer 成员变量，**按理说现在就可以把包装好的客户端指令直接赋值给 LogEntry 对象中的 ByteBuffer 成员变量了**，这样一来，LogEntry 就拥有了有效数据，成为一条真正的日志，可以被持久化以及传输给集群中的跟随者节点了。但是，让我们再多思考一下，**领导者毕竟是要和客户端进行交互的，当客户端发送给领导者一条指令之后，这条指令在领导者会被包装成一条日志，然后进行一系列操作，如果领导者提交了这条日志，最终就要通知客户端这条指令已经成功提交了，如果指令没能成功提交，也要通知客户端。显然，根据我们以往的编程经验，当客户端发送给领导者一条客户端指令的时候，我们最好再为这条指令定义一个回调方法。这样一来，当指令成功提交或者提交失败了，程序都可以调用该回调方法通知客户端(这里我一直把回调方法要通知的目标称为客户端，实际上我更愿意把这个目标成为业务层，这里还无法展开讲解，因为我们实现的 raft 框架还没有和其他业务框架对接，等实现 Nacos 的时候，大家就会明白为什么我称其为业务层了)**。本章我先为大家定义一个简单点的回调方法，比如我就定义一个名为 ExpectClosure 的类，该类中定义一个回调方法，该方法会在日志被成功提交或者提交失败后被回调。具体实现请看下面代码块。
```java
public class ExpectClosure implements Closure {


    //当日志经过处理之后，就调用下面这个方法，根据日志被处理的情况通知客户端指令
    //指令成功或失败
    @Override
    public void run(final Status status) {
        
        if (status.isOk()) {
            System.out.println("通知客户端操作执行成功了！");
        }
        
        System.out.println("通知客户端操作执行失败了！");
    }

}
```
回调的方法也定义好了，那么这个回调方法该怎么交给领导者呢？从逻辑上来说，**我们肯定是要在程序中创建一个 ExpectClosure 对象，然后把该对象和客户端发送过来的指令一起提交给领导者**。**如果我现在为领导者专门定义一个接收客户端指令的方法，就把这个方法定义为 apply，在该方法中将接收到的客户端指令包装成日志，同时管理随着指令一起提交过来的回调方法**，那么这个 apply 方法就可以定义成下面这样。请看下面代码块，<br />这个 apply 方法也是属于节点本身的方法，所以首先应该在 Node 接口中新添加一个 apply 方法。
```java
public interface Node extends Lifecycle<NodeOptions> {


    PeerId getLeaderId();

    NodeId getNodeId();

    String getGroupId();

    RaftOptions getRaftOptions();

    //这个就是新添加的方法，方法参数一个为客户端指令，一个为回调方法对象
    void apply(ByteBuffer buteffer, Closure closure);

    
    State getNodeState();

    //得到最后一条日志的索引，记住这个方法，后面就会用到
    long getLastLogIndex();

    LogStorage getLogStorage();


}
```
接下来就要在 NodeImpl 实现类中实现这个 apply 方法。
```java
public class NodeImpl implements Node, RaftServerService {

    //其他内容省略

    
    @Override
    public void apply(ByteBuffer buteffer, Closure closure) {

        //暂且不做实现
    }


    //其他内容省略
}
```
现在领导者接收客户端指令的方法也定义好了，如果领导者接收到一条客户端指令，就可以用下面的代码来实现这个流程。请看下面代码块。
```java
//用ByteBuffer包装客户端指令
ByteBuffer data = ByteBuffer.wrap(("hello").getBytes());
//调用NodeImpl对象的apply方法接收客户端指令
node.apply(data,new ExpectClosure());
```
在上面的代码块中，就把领导者接收客户端指令的操作实现了。接下来，我就可以为大家深入一些，带领大家真正实现 NodeImpl 类的 apply 方法，在 apply 方法中，领导者会将客户端指令包装成一个 LogEntry 对象。但是，在实现 apply 方法之前，我不得不为大家再对目前的程序做一次重构。**实际上，在 sofajraft 框架中，还引入了一个 Task 类，这个类中有一个 ByteBuffer 成员变量，还有一个 Closure 成员变量。当领导者在接收客户端指令和回调方法对象时，实际上 apply 方法接收的是一个 Task 参数对象，这个 Task 对象封装了客户端指令和要被回调的方法**。我们先看看这个 Task 类的具体实现，一会我再来为大家解释为什么要引入这个 Task 类。请看下面代码块。
```java
//日志条目对象和响应的回调方法就封装在这个类的对象中，可以从第三版本代码，我提供的测试类中学习该类的具体用法
public class Task implements Serializable {

    private static final long serialVersionUID = 2971309899898274575L;

    //客户端指令会赋值给这个成员变量
    private ByteBuffer data = LogEntry.EMPTY_DATA;

    //回调方法对象会赋值给这个成员变量
    private Closure done;

    //这个成员变量的作用也比较重要，提供了一个初始化的任期值，也就是-1
    //下面我就会为大家解释这个成员变量的作用
    private long expectedTerm = -1;

    public Task() {
        super();
    }


    public Task(final ByteBuffer data, final Closure done) {
        super();
        this.data = data;
        this.done = done;
    }


    public Task(final ByteBuffer data, final Closure done, final long expectedTerm) {
        super();
        this.data = data;
        this.done = done;
        this.expectedTerm = expectedTerm;
    }

    public ByteBuffer getData() {
        return this.data;
    }

    public void setData(final ByteBuffer data) {
        Requires.requireNonNull(data, "data should not be null, you can use LogEntry.EMPTY_DATA instead.");
        this.data = data;
    }

    public Closure getDone() {
        return this.done;
    }

    public void setDone(final Closure done) {
        this.done = done;
    }

    public long getExpectedTerm() {
        return this.expectedTerm;
    }

    public void setExpectedTerm(final long expectedTerm) {
        this.expectedTerm = expectedTerm;
    }

}
```
在引入 Task 类之后，现在领导者接收客户端指令的 apply 方法就要重构成下面这样了。
```java
public class NodeImpl implements Node, RaftServerService {

    //其他内容省略


    //向jraft框架中提交日志的方法，这些日志就是从业务层，也可以说客户端传递到核心算法层的，日志以Task的形式在
    //jraft内部传输，每一个Task对象中都包装了一条客户端指令，当然，这个指令已经被包装成ByteBuffer了
    @Override
    public void apply(Task task) {

        //暂且不做实现
    }


    //其他内容省略
}
```
领导者具体接收客户端指令的操作也重构成了下面这样，请看下面代码块。
```java
//用ByteBuffer包装客户端指令
ByteBuffer data = ByteBuffer.wrap(("hello").getBytes());
//创建Task对象，封装客户端指令和回调方法对象
final Task task = new Task(data, new ExpectClosure());
//领导者接收客户端指令
node.apply(task);
```
我知道这一条流程重构下来之后，肯定会有一些朋友好奇为什么引入这个 Task 类，这里我简单解释一下。实际上也没有什么特别的原因，**无非就是围绕着一条客户端指令发挥重要作用的变量越来越多，所以用一个对象来封装这些变量而已**。当然，现在我只为大家提到了封装客户端指令的 ByteBuffer 和根据日志提交情况进行回调的回调方法对象 ExpectClosure，除此之外还有一个很重要变量，那就是 Task 类中的 expectedTerm 成员变量。<br />请大家先想一想，**当领导者接收到客户端的一条指令时，要将这条指令包装成一个日志条目，这时候包装好的这个日志条目肯定还没有对应的索引和任期，领导者需要给这个日志条目设置了对应的索引和任期之后，才能将这个日志条目进行持久化**。这里就涉及到一个日志条目默认任期的情况，**所谓日志条目的默认任期，也就是说当领导者将一条客户端指令包装成日志条目之后，这个刚刚被创建出来的日志条目应该有一个默认的任期，这个默认任期的值就是 Task 类中的 expectedTerm 成员变量的值，也就是 -1。这样一来，等到领导者根据自己的任期给新的日志条目进行任期赋值时，会对新日志的默认任期进行判断(如何判断后面会具体讲解)，如果新日志的默认任期不为 -1 就直接认为客户端指令应用失败，执行回调方法，通知客户端即可**。这时候大家应该明白了，为什么需要在 Task 类中定义一个 expectedTerm 成员变量，当然，我们也可以认为需要的定义的变量太多了，索性就定义一个新的 Task 类，用这个类的对象来封装这些变量而已。好了，到此为止，我就为大家把领导者日志持久化的前置知识讲解完毕了，接下来，就让我来带领着大家实现 NodeImpl 类的 apply 方法吧，**apply 方法就是领导者将日志持久化的入口方法**。<br />当我们要实现 NodeImpl 类的 apply 方法的时候，先让我们整理一下思路，首先应该在这个方法中执行什么操作？从大的方向上来讲，似乎就 4 个步骤。<br />**1 创建一个 LogEntry 对象，用来封装客户端指令**<br />**2 将 apply 的方法参数 Task 对象中的客户端指令交给刚刚创建的 LogEntry 对象(回调方法对象暂且不用关心)**<br />**3 给 LogEntry 对象的索引和任期赋值，还有日志条目对象的类型，通过 apply 方法接收的日志都是业务日志，因为配置变更日志是领导者内部产生的，并不会从客户端接收到。所以，在 apply 方法中会将新创建的日志对象的类型设置成业务日志**<br />**4 将 LogEntry 对象交给日志管理器，也就是 logManager 对象，调用 logManager 对象的 appendEntries 方法，对这个日志条目进行持久化操作。至于 appendEntries 方法内部的操作，下一小节我会为大家实现(appendEntries 方法大家应该都有印象吧，这是第 10 章的内容，该方法会将一批日志进行本地持久化)。**<br />我知道，看到这里肯定也会有不少人感到疑惑，不是说应该对新创建的 LogEntry 对象的默认任期进行判断吗？怎么上面 4 个步骤中并不包含这个操作？还是那句话，大家先别急，慢慢往后看🐶。<br />如果大家都认同以上 4 个步骤，那么接下来，就让我为大家直接实现 NodeImpl 类的 apply 方法。请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {

    //其他内容省略


    //向jraft框架中提交日志的方法，这些日志就是从业务层，也可以说客户端传递到核心算法层的，日志以Task的形式在
    //jraft内部传输，每一个Task对象中都包装了一条客户端指令，当然，这个指令已经被包装成ByteBuffer了
    @Override
    public void apply(Task task) {
        //对task进行判空
        Requires.requireNonNull(task, "Null task");
        
        //1.创建日志对象
        final LogEntry entry = new LogEntry();
    
        //2.把客户端传来的指令包装成一个日志条目，到这里一个业务指令就变成一条日志了
        //只不过现在的日志还没有索引，任期等等
        entry.setData(task.getData());

        //3.设置日志的任期和索引
        //得到日志条目的日志Id，设置日志索引
        entry.getId().setIndex(getLastLogIndex());
        //设置日志任期
        entry.getId().setTerm(this.currTerm);
        //设置日志类型，类行为业务日志
        entry.setType(EnumOutter.EntryType.ENTRY_TYPE_DATA);

        //4.把日志交给日志管理器组件，继续进行持久化操作
        //该方法暂时写成伪代码，因为真正的appendEntries方法并不是这样定义的
        //方法参数并不是只有一个LogEntry对象，下一小节就会进行重构
        //这里大家理解意思即可
        this.logManager.appendEntries(entrie);
    }


    //其他内容省略
}
```
以上就是我为大家初步实现的 NodeImpl 类的 apply 方法。不用大家说我也知道，这个方法目前存在一些缺陷。至于是什么缺陷，我想也许大家一时间也说不上来，或者说不到重点上。因为我已经看过源码了，所以就请大家听我慢慢道来即可。<br />好了，现在我想请大家暂且忘掉上面代码块中展示的代码，回想曾经学习 Netty 的时候，我们在搭建 Netty 最基础的骨架时，我为大家引入了单线程执行器。引入单线程执行器的目的很简单，那就是当服务端 Channel 不断接收到客户端连接时，为了进一步提升服务端 Channel 接收客户端连接的工作效率，我为程序引入了一个单线程执行器。这样一来，当服务端 Channel 接收到客户端连接之后，就可以立刻把这个客户端连接提交给单线程执行器，让单线程执行器异步将客户端 Channel 注册到多路复用器上。大家可以回顾回顾以前的知识，看是否还能想起来。<br />如果大家对以前的知识还有些印象，接下来，大家可以用 Netty 的场景简单类比一下目前领导者接收客户端指令的场景。在 raft 集群中领导者只有一个，在业务非常繁忙的时候，这个领导者可能会一直接收客户端的各种写指令，如果是这样，那么领导者肯定会非常频繁地在 apply 方法中将客户端写指令包装成日志，然后将日志进行本地持久化。让我们来想一想，这么做有什么缺陷，每当 raft 集群中的领导者接收到一个客户端的写指令，实际上，这个操作的本质就是集群领导者接收到一个客户端请求。当然，这中间要经过一些列方法的调用，要让业务框架和 raft 共识算法框架协同工作。但现在我还没有为大家讲解过业务框架如何和 raft 共识算法框架衔接，等到更新 Nacos 的时候，才会为大家具体讲解。所以，现在大家可能还无法理解领导者中的 apply 方法会在什么情况下被调用。从**最直接的逻辑上来说，每当 raft 集群中的领导者接收到一条客户端写指令，就会调用一次 apply 方法；但是从更深层次的逻辑来说，肯定是 raft 集群的领导者接收到客户端发送过来的一个请求，请求中封装着写操作指令，领导者就会调用一次 apply 方法。如果在某个时刻领导者接收到很多客户端请求，按照我们目前编写的程序，领导者就会多次调用 apply 方法来处理客户端指令**。这不就和 Netty 的场景差不多吗？**如果我们想加快领导者处理客户端请求的速度，就要保证程序能尽快执行完 apply 方法，这样，领导者就能尽快处理下一个客户端发送过来的请求**。<br />现在让我们回头看看上面的代码块中，也就是目前实现的 apply 方法存在的问题。这时候大家应该都意识到了，**如果我们直接就在 apply 方法中进行日志持久化的操作，这个 IO 操作会让 apply 方法耗费很多时间。显然会拖累领导者处理客户端写指令的速度。所以，我们最好也仿照 Netty，在 apply 方法中创建新的线程，或者是创建一个单线程执行器，只要领导者接收到了客户端的写指令，就可以在 apply 方法中将接收到的客户端指令提交给单线程执行器，这样一来，apply 方法就直接执行结束了，领导者就可以处理下一个客户端指令**。当然，这时候也许会有朋友说，就算在 apply 方法中进行异步持久化，也不会对日志持久化的效率有什么提升。这一点我也赞同，线程都是一样的，一个线程将一些数据持久化到本地耗费了 5 秒，换另一个线程来执行持久化操作，好肥的时间不可能就突然变成 1 秒了。这显然是不合常理的。我希望大家能多多思考，**在我们创建多线程，使用并发时一定要弄清楚，我们使用多线程提升的是哪部分程序的执行效率。就像很多框架使用 Netty 作为通信模块，总要定义一个业务线程池，原因很简单，定义业务线程池并不会让原本的业务流程快多少，但可以让 Netty 处理 IO 时间的速度大大提升**。这个道理想必大家也早就理解了。<br />好了，分析了一个缺陷之后，请大家再想想，再上面代码块中的 apply 方法中，还有什么缺陷？这个缺陷可能不太容易想到，所以我就直接跟大家说明了。在上面代码块中的 apply 方法中，我最后写了 this.logManager.appendEntries(entrie) 这样一行伪代码，这个方法每次只能持久化一个日志条目。如果我最后真的把 logManager 的 appendEntries 方法定义成每次只能持久化一个日志条目，那我就是个蠢货。**原因也很简单，频繁的 IO 操作会显著降低程序的执行效率，如果我有 10 条日志要持久化，与其每次持久化 1 条日志，和硬盘交互 10 次，不如只和硬盘交互 1 次，1 次持久化 10 条日志。总之，我的思路很直接，那就是领导者接收到客户端指令后并不会立刻就持久化到硬盘中，而是先被包装成日志，将日志存放到一个容器中，也许是个数组，也许是个集合，然后分批处理这些日志，比如一次处理 32 条日志。处理完 32 条之后再处理 32 条。这样一次处理一批日志，显然比一条一条处理效率高一些**。

<a name="V3CSj"></a>
## 引入 **Disruptor** 框架，重构 NodeImpl 类

分析了以上两个缺陷之后，我们该如何重构程序呢？重构之后的程序即可异步持久化日志，也可以对日志条目进行批处理。答案已经不言而喻了吧，显然，我们可以引入上一门课程讲解的 Disruptor 框架。这个框架中拥有一个批处理器，可以批量消费环形数组中的数据。这个大家应该多少还有点印象，批处理器就是 BatchEventProcessor，如果没印象了，大家可以再回顾一下 Disruptor 的知识。如果引入了 Disruptor 框架，那我们就可以把领导者创建的业务日志对象都放到 Disruptor 的环形数组中，然后让框架内部的批处理器去消费。这里我要强调一下，**Disruptor 框架的批处理器是单线程的，如果用户只定义了一个 EventHandler，那么在程序中只会有一个批处理器不断消费环形数组中的数据，也就是说，只会有一个消费者线程消费数据，这样一来就会对日志持久化提供一个非常有力的顺序保障。日志索引是严格按照顺序递增的，日志持久化以及后续被应用也是按照索引顺序来进行的，而单线程的批处理器正好保证了日志也是按照索引的顺序被处理的**。如果大家不理解这句话，可以再去看看 Disruptor。<br />既然要引入 Disruptor 框架，就要想想这个框架的关键对象应该定义在哪里。根据我们以前学习 Disruptor 框架的经验，我们可以知道，如果要启动 Disruptor 框架，显然应该先把 disruptor 启动器对象和环形数组 RingBuffer 对象定义出来，这两个都好说，因为 apply 是 NodeImpl 类中的方法，所以这两个对象都可以直接定义在 NodeImpl 类中。重构之后的 NodeImpl 类就变成了下面这样，请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {

    //其他内容省略

    //终于见到这个Disruptor对象了
    //这个Disruptor就是用来将日志落盘的入口
    private Disruptor<LogEntryAndClosure> applyDisruptor;
    
    //Disruptor框架要使用的任务队列，也就是环形数组
    private RingBuffer<LogEntryAndClosure> applyQueue;


    //向jraft框架中提交日志的方法，这些日志就是从业务层，也可以说客户端传递到核心算法层的，日志以Task的形式在
    //jraft内部传输，每一个Task对象中都包装了一条客户端指令，当然，这个指令已经被包装成ByteBuffer了
    @Override
    public void apply(Task task) {

        //暂且不做实现
    }


    //其他内容省略
}
```
在上面的代码块中可以看到，applyQueue 的泛型是 LogEntryAndClosure，这个泛型对象其实就是 Disruptor 框架环形数组中存放的数据，当然，这个数据并不是直接被消费者线程使用批处理器消费，**LogEntry 才是真正要被消费的数据，所以这个 LogEntryAndClosure 对象显然要封装 LogEntry 对象。LogEntry 对象就这样被放到了环形数组中，然后被批处理器消费即可。同时我们也别忘了，跟随者客户端指令提交给领导者的 apply 方法的还有事先定义好的回调方法，以及 LogEntry 日志对象的默认任期值，它们当然也要和日志一起被管理起来，所以在 LogEntryAndClosure 对象中，还应该封装回调方法对象与默认任期值**。大家可以结合 DIsruptor 方法再品味一下 LogEntryAndClosure 类的编写思路。我把 LogEntryAndClosure 类的代码展示在下面的代码块中了，大家可以简单看看。请看下面代码块。
```java
//Disruptor框架中的生产者生产的数据，在Disruptor中，会创建一个事件工厂，然后专门用来生产生产者生产的数据
//这个LogEntryAndClosure对象会被放到环形数组中，当作一个个要被消费者消费的数据，当然，真正要消费的数据还是这个类的对象
//持有的LogEntry对象，那怎么把真正的要消费的数据放到这个类的对象中呢？这就要创建一个数据传输器对象了，很快我们就会见到了
private static class LogEntryAndClosure {
    //日志对象
    LogEntry  entry;
    //回调通知客户端的方法
    Closure   done;
    //日志对象的初始任期值
    long      expectedTerm;

    //重置LogEntryAndClosure对象的方法
    //因为这个LogEntryAndClosure对象是存放在环形数组中的，重置该对象就可以一直复用，减少垃圾回收
    public void reset() {
        this.entry = null;
        this.done = null;
        this.expectedTerm = 0;
    }
}
```
环形数组中存放的数据已经定义好了，接下来还要做什么？如果我们只关注重点的话，接下来要做的肯定就是分析在什么时候、在哪里创建 applyDisruptor 和 applyQueue 对象。但我们还没必要那么着急，先把一些小组件定义好就行。比如继续把事件工厂定义出来，事件工厂定义完毕之后，还可以把数据传输器定义出来。事件工厂就是用来创建环形数组中的消费对象的，也就是用来创建一个个 LogEntryAndClosure 对象的。而数据传输器的作用就是给 LogEntryAndClosure 对象内部的成员变量赋值，也就是把真正的 LogEntry 这些消费数据填充到 LogEntryAndClosure 对象中。事件工厂和数据传输器我也定义好了，具体实现请看下面代码块。
```java
//事件工厂，需要在Disruptor框架中使用
private static class LogEntryAndClosureFactory implements EventFactory<LogEntryAndClosure> {
    @Override
    public LogEntryAndClosure newInstance() {
        return new LogEntryAndClosure();
    }
}




//创建一个Disruptor框架的生产者需要使用的数据传输器，这个数据传输器的作用就是把日志条目对象
//存放到LogEntryAndClosure对象中，而LogEntryAndClosure对象就会放到环形数组中
final EventTranslator<LogEntryAndClosure> translator = (event, sequence) -> {
    //从task中获得回调对象
    event.done = task.getDone();
    event.entry = entry;
    //默认任期值，该值为-1
    event.expectedTerm = task.getExpectedTerm();
};


```
这些都定义好之后，最后还差一个组件，那就是消费者处理器，**这个消费者处理器就是真正的消费者逻辑，这个消费处理器会交在批处理器中被执行，并且是在一个循环中一直被执行，直到没有消费数据了，消费者线程才会阻塞，有数据之后会继续消费环形数组中的数据**。这个消费处理器的名称我也定义好了，就叫做 LogEntryAndClosureHandler，但是这个处理器的具体实现我先不讲解。现在，让我们先回过头看看，已经定义好的 Disruptor 对象和环形数组对象应该在哪里创建出来？<br />按照常规逻辑，Disruptor 和环形数组对象肯定也是在 NodeImpl 对象初始化的过程中被创建出来的，因为之前我已经为大家分析过了，sofjraft 框架中有各种各样的组件，这些组件都是在 NodeImpl 对象 init 的过程中被初始化的。所以，我也就在 NodeImpl 的 init 方法中把 Disruptor 对象和环形数组对象创建出来了。当然，这时候围绕着 Disruptor 框架的组件基本上都定义完整了，只有一个 LogEntryAndClosureHandler 处理器还没实现。但这没什么关系，总之，我们也可以进一步重构 NodeImpl 的 apply 方法了。我这次重构这个 apply 方法的思路也很简单，**无非就是将客户端指令包装成日志，然后通过数据传输器将日志提交到 Disruptor 的环形数组中**。在下面的代码块中，我就为大家展示了重构之后的 NodeImpl 类和 apply 方法，请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {

    //其他内容省略

    //终于见到这个Disruptor对象了
    //这个Disruptor就是用来将日志落盘的入口
    private Disruptor<LogEntryAndClosure> applyDisruptor;
    
    //Disruptor框架要使用的任务队列，也就是环形数组
    private RingBuffer<LogEntryAndClosure> applyQueue;




    //Disruptor框架中的生产者生产的数据，在Disruptor中，会创建一个事件工厂，然后专门用来生产生产者生产的数据
    //这个LogEntryAndClosure对象会被放到环形数组中，当作一个个要被消费者消费的数据，当然，真正要消费的数据还是这个类的对象
    //持有的LogEntry对象，那怎么把真正的要消费的数据放到这个类的对象中呢？这就要创建一个数据传输器对象了，很快我们就会见到了
    private static class LogEntryAndClosure {
        //日志对象
        LogEntry  entry;
        //回调通知客户端的方法
        Closure   done;
        //日志对象的初始任期值
        long      expectedTerm;
    
        //重置LogEntryAndClosure对象的方法
        //因为这个LogEntryAndClosure对象是存放在环形数组中的，重置该对象就可以一直复用，减少垃圾回收
        public void reset() {
            this.entry = null;
            this.done = null;
            this.expectedTerm = 0;
        }
    }


    
    //事件工厂，需要在Disruptor框架中使用
    private static class LogEntryAndClosureFactory implements EventFactory<LogEntryAndClosure> {

        @Override
        public LogEntryAndClosure newInstance() {
            return new LogEntryAndClosure();
        }
    }

    

     @Override
    public boolean init(final NodeOptions opts) {


        //省略部分代码


        
        //初始化一个disruptor队列，这里采用的是多生产者模式
        this.applyDisruptor = DisruptorBuilder.<LogEntryAndClosure> newInstance()
                //设置disruptor大小，默认16384
                .setRingBufferSize(this.raftOptions.getDisruptorBufferSize())
                //设置事件工厂
                .setEventFactory(new LogEntryAndClosureFactory())
                //设置线程工厂
                .setThreadFactory(new NamedThreadFactory("JRaft-NodeImpl-Disruptor-", true))
                //采用多生产者模式，也就是并发向环形队列中填充数据，这就意味着在领导者内部可以并发向环形队列中提交日志
                //可以进一步提高日志被处理的效率
                .setProducerType(ProducerType.MULTI)
                //阻塞策略
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        
        //设置消费者处理器，使用的还是批处理器，因为这里只创建了一个LogEntryAndClosureHandler消费处理器
        //这个消费者处理器会被disruptor框架内部的BatchEventProcessor批处理器持有，然后开始处理任务
        //如果还有朋友不熟悉Disruptor框架，我强烈建议先去把Disruptor框架学习一下，否则你不明白日志处理的回调逻辑
        this.applyDisruptor.handleEventsWith(new LogEntryAndClosureHandler());
        
        //设置异常处理器
        this.applyDisruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(getClass().getSimpleName()));
        
        //启动disruptor框架的消费者，启动之后消费者会阻塞，因为现在生产者还没有向环形数组中生产数据呢
        this.applyQueue = this.applyDisruptor.start();


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

        //省略部分代码

        
    }
    


    


    //重构之后的apply方法
    @Override
    public void apply(Task task) {

        Requires.requireNonNull(task, "Null task");
        
        //创建日志对象
        final LogEntry entry = new LogEntry();
        
        //把业务层传来的指令包装成一个日志条目，到这里一个业务指令就变成一条日志了
        //只不过现在的日志还没有索引，任期等等
        entry.setData(task.getData());
        
        //创建一个Disruptor框架的生产者需要使用的数据传输器，这个数据传输器的作用就是把日志条目对象
        //存放到LogEntryAndClosure对象中，而LogEntryAndClosure对象就会放到环形数组中
        final EventTranslator<LogEntryAndClosure> translator = (event, sequence) -> {
            event.reset();
            event.done = task.getDone();
            event.entry = entry;
            event.expectedTerm = task.getExpectedTerm();
        };

        //下面就是发布生产数据的操作了
        //默认提交日志到Disruptor框架中是采用非阻塞模式
        switch(this.options.getApplyTaskMode()) {
            case Blocking:
                //这里阻塞的意思是，消费速度跟不上生产者速度时，当前发布生产者数据的线程就直接阻塞了
                //直到发布成功才结束阻塞
                this.applyQueue.publishEvent(translator);
                break;
            case NonBlocking:
            default:
                //这里非阻塞的意思是，当消费速度跟不上生产速度时，尝试向队列中发布生产者数据
                //如果发布不成功，就直接回复给客户端当前节点非常忙碌，并没有让线程阻塞
                if (!this.applyQueue.tryPublishEvent(translator)) {
                    String errorMsg = "Node is busy, has too many tasks, queue is full and bufferSize="+ this.applyQueue.getBufferSize();
                    //用另外的线程执行task对象中的回调方法，通知客户端操作失败
                    ThreadPoolsFactory.runClosureInThread(this.groupId, task.getDone(),
                            new Status(RaftError.EBUSY, errorMsg));
                    LOG.warn("Node {} applyQueue is overload.", getNodeId());
                    if(task.getDone() == null) {
                        throw new OverloadException(errorMsg);
                    }
                }
                break;
        }
    }


    //其他内容省略
}
```
上面代码块的注释非常详细，大家可以多看几遍。总之，现在客户端的指令发送给领导者之后，在领导者的 apply 方法中，这条指令就会被包装成一个 LogEntry 对象，接着把这个日志对象提交到 Disruptor 的环形数组中了。接下来要做的就是从环形数组中消费数据了。**这时候我要强调一下，目前的 LogEntry 对象还没有索引，任期为默认值 -1。并且当 Disruptor 从环形数组中消费数据时，并不是直接就能获得 LogEntry 对象，而是先获得 LogEntryAndClosure 对象，再从 LogEntryAndClosure 对象中获得 LogEntry 对象**。<br />消费数据的逻辑肯定被定义在了 LogEntryAndClosureHandler 处理器中，但这个处理器我还没有未大家实现，接下来就让我来为大家实现这个处理器吧。

<a name="boVZD"></a>
## 实现 LogEntryAndClosureHandler 处理器

想必大家还记得之前我在分析 apply 方法的执行流程时，提出了 4 个操作步骤。就是下面这 4 个。<br />**1 创建一个 LogEntry 对象，用来封装客户端指令**<br />**2 将 apply 的方法参数 Task 对象中的客户端指令交给刚刚创建的 LogEntry 对象(回调方法对象暂且不用关心)**<br />**3 给 LogEntry 对象的索引和任期赋值，还有日志条目对象的类型，通过 apply 方法接收的日志都是业务日志，因为配置变更日志是领导者内部产生的，并不会从客户端接收到。所以，在 apply 方法中会将新创建的日志对象的类型设置成业务日志**<br />**4 将 LogEntry 对象交给日志管理器，也就是 logManager 对象，调用 logManager 对象的 appendEntries 方法，对这个日志条目进行持久化操作。至于 appendEntries 方法内部的操作，下一小节我会为大家实现(appendEntries 方法大家应该都有印象吧，这是第 10 章的内容，该方法会将一批日志进行本地持久化)。**<br />现在我重构之后的 apply 方法其实已经执行了上面第 1 和第 2 个步骤。剩下第 3、第 4 还没有执行。所以，接下来这两个步骤就要在 LogEntryAndClosureHandler 消费处理器中实现，让 Disruptor 的批处理器异步执行这些操作。当然，我们也不能忘记之前补充的一点，那就是要对日志进行批处理，不能一条一条地处理。就像我之前说的那样，我们可以一次性处理 32 条日志，在 LogEntryAndClosureHandler 处理器中分批次对 32 条日志进行索引和任期的赋值，以及交给日志管理器进行持久化操作。<br />分析到这里，其实 LogEntryAndClosureHandler 处理器中的操作也已经很清晰了。可以总结为以下 3 个步骤。<br />**1 从环形数组中获得 32 个 LogEntry 日志条目对象，把这些对象放到一个集合中。**<br />**2 判断每一个日志条目对象的初始化任期是否为 -1，如果为 -1 则给日志条目对象进行任期赋值。**<br />**3 将这一批日志提交给 logManager 日志管理器组件，调用它的 appendEntries 方法将日志持久化到本地。**<br />理解了这三个步骤之后，接下来就请大家看看 LogEntryAndClosureHandler 的具体实现，请看下面代码块。<br />LogEntryAndClosureHandler 也是 NodeImpl 的一个内部类。
```java
public class NodeImpl implements Node, RaftServerService {


    //省略其他内容




    //Disruptor框架要使用的事件处理器，这个事件处理器会被Disruptor框架的批处理器持有
    //然后就开始批量处理任务了
    private class LogEntryAndClosureHandler implements EventHandler<LogEntryAndClosure> {
        //创建一个用于批量处理日志的集合，这个集合的容量默认是32
        private final List<LogEntryAndClosure> tasks = new ArrayList<>(NodeImpl.this.raftOptions.getApplyBatch());

        //该方法其实会在disruptor的BatchEventProcessor任务批处理器中被循环调用
        //该方法就是Disruptor的批处理器要调用的真正消费数据的方法，这个方法就是将日志落盘的入口方法
        @Override
        public void onEvent(final LogEntryAndClosure event, final long sequence, final boolean endOfBatch)throws Exception {
    
            //一直向这个集合中添加事件，每一个事件中都包含一条要存储到本地的日志
            //注意，这里直接把事件从环形数组中获得了，并不是直接获得的LogEntry对象
            this.tasks.add(event);
            
            //直到满足32条日志后，就把32条日志成批处理，endOfBatch表示是否为Disruptor环形数组中最后一个数据，当集合中的数据等于32条或者已经没有
            //更多数据可消费的时候，就要开始进行批量写入本地了
            if (this.tasks.size() >= NodeImpl.this.raftOptions.getApplyBatch() || endOfBatch) {
                //在这个方法中开始处理业务日志
                executeApplyingTasks(this.tasks);
                //清空tasks集合，并且重置每一个event对象
                reset();
            }
        }

        
        //重置可循环利用对象
        private void reset() {
            for (final LogEntryAndClosure task : this.tasks) {
                //这里重置event对象是为了循环利用这个对象
                task.reset();
            }//这里清空集合，是为了下一次处理32个日志做准备
            this.tasks.clear();
        }
    }




    //批处理日志的方法，其实在这个方法中也只不过是做了一点点校验，然后就设置了一下日志的任期和类型
    private void executeApplyingTasks(final List<LogEntryAndClosure> tasks) {


        //虽然该方法是在批处理器中执行的，批处理器就是一个线程，不会有并发问题
        //但是在整个集群中，除了当前的线程，可能还会有生成快照线程，日志传输线程
        //同时访问日志组件，对日志组件中的某些属性进行修改，所以，这里要直接获得写锁
        this.writeLock.lock();
        
        try {//这个tasks集合中存放着32个LogEntryAndClosure对象
            //每个LogEntryAndClosure对象中存放着一个logEntry对象
            final int size = tasks.size();

            //这里多了一个判断，判断当前节点是否为领导者，只有领导者才能对日志进行索引和任期赋值操作
            //判断当前节点是不是领导者，如果不是领导者不能处理日志
            if (this.state != State.STATE_LEADER) {
                //这个就是获得每一个LogEntryAndClosure对象中包装每一个Task中的回调方法
                //这个回调方法是用来通知业务层日志处理是否成功的，这个回调方法会在日志被顺利提交或者提交失败之后才会被回调
                final List<Closure> dones = tasks.stream().map(ele -> ele.done)
                        .filter(Objects::nonNull).collect(Collectors.toList());
                //回调每一个方法，通知业务层日志处理失败，其实也就是指令操作失败，因为访问的节点已经不是领导者了
                ThreadPoolsFactory.runInThread(this.groupId, () -> {
                    for (final Closure done : dones) {
                        done.run(st);
                    }
                });
                return;
            }
            
            
            //在这里创建的这个集合专门存放每一条日志
            final List<LogEntry> entries = new ArrayList<>(size);
            
            for (int i = 0; i < size; i++) {
                //获得每一个包装日志的对象
                final LogEntryAndClosure task = tasks.get(i);
                //当领导者在处理日志的时候，这个时候刚初始化好的日志对象中的任期是-1，这个是在Task创建的时候初始化好的
                //这时候expectedTerm肯定等于-1，并且不等于当前节点的任期
                //expectedTerm的值不等于-1，就报错
                if (task.expectedTerm != -1 && task.expectedTerm != this.currTerm) {
                    LOG.debug("Node {} can't apply task whose expectedTerm={} doesn't match currTerm={}.", getNodeId(),
                            task.expectedTerm, this.currTerm);
                    if (task.done != null) {
                        //执行回调方法，通知业务层操作失败了
                        final Status st = new Status(RaftError.EPERM, "expected_term=%d doesn't match current_term=%d",
                                task.expectedTerm, this.currTerm);
                        //在这里执行回调方法
                        ThreadPoolsFactory.runClosureInThread(this.groupId, task.done, st);
                        task.reset();
                    }
                    continue;
                }
                
                //为每一个日志条目设置任期，这里之所以没有设置索引，是因为还要在logManager中对日志索引做一次判断
                //看看当前日志是领导者生成的，还是从领导者传输过来的，因为领导者和跟随者节点的代码是一样的，所以要在
                //一些细节判断上将两者的操作区分开，判断完毕之后，会在logManager组件中对LogEntry的索引赋值
                task.entry.getId().setTerm(this.currTerm);
                //设置日志条目类型，从这个方法中设置设置的日志条目对象都是业务日志
                task.entry.setType(EnumOutter.EntryType.ENTRY_TYPE_DATA);
                //得到了每一条日志，并且把日志添加到集合中
                entries.add(task.entry);
                //这里也是为了循环利用Event
                task.reset();
            }

            //将设置好的日志交给日志管理器处理，在日志管理器中，这些日志将会被设置索引，然后进行落盘的操作
            //这里大家可能会困惑，最初提交给apply方法的是一个task对象，目前为止，task对象中的客户端指令已经被包装成日志了，日志的初始任期值也被使用了
            //但是与客户端指令对应的回调方法呢？好像到这里就没有再使用了。确实是这样，因为我在当前executeApplyingTasks方法中省略了部分代码
            //这部分代码就是操作与客户端指令对应的回调方法的，但这里需要和状态机结合，而状态机我还没有为大家引入，所以就先省略了
            //后面会为大家重构完整
            //可以看到，在这里对这一整批日志entries又定义了一个回调方法对象，就是这个LeaderStableClosure对象
            //这里的这个回调大家可以暂时忽略，等第七版本大家就明白这个回调方法的作用了
            //这里的回调是当日志落盘成功后，直接判断是否可以在领导者提交了，这里判断提交的条件是看看每一条日志是否
            //得到了集群中过半节点的投票，第三版本还不用关心这个回调函数，因为这个回调方法也要和状态机结合
            //而我们还没有引入状态机，也没有引入日志传输的功能
            this.logManager.appendEntries(entries, new LeaderStableClosure(entries));
        } finally {
            this.writeLock.unlock();
        }

        
    }



     @Override
    public void apply(Task task) {

        //省略代码内容
    }


    //省略其他内容



    
}





//这里给大家展示一下logManager的appendEntries方法真正的样子，就是下面这样。
//将一批日志持久化的方法
void appendEntries(final List<LogEntry> entries, StableClosure done);
```
到此为止，我就为大家把 NodeImpl 类又一次重构完毕了，也实现了日志落盘的部分流程。现在我们的程序已经执行到 logManager.appendEntries 这个方法了，也就意味着这一批日志应该交给 logManager 来进一步操作了。我要说的是，通过前面几章的学习，我们都知道，实际上 logManager 内部持有了日志存储器，logManager 的大部分操作实际上最终都是交给了日志存储器来实现的。说得再直接一点，**日志持久化最终肯定也是日志存储器来执行的，也就是 RocksDBLogStorage 对象来执行的。当 logManager 通过 appendEntries 方法获得了一批日志之后，肯定会执行一些操作，然后将这批日志交给 RocksDBLogStorage 日志存储器，让日志存储器将日志落盘**。那么在 logManager 和 RocksDBLogStorage 之间又会对日志执行什么操作呢？RocksDBLogStorage 日志存储器又是怎么将日志落盘的呢？这一章显然是讲不完了，就留到下一章讲解吧。朋友们，我们下一章见！
