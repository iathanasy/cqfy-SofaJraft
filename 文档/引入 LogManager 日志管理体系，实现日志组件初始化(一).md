大家好，从这一章开始，我就为大家正式讲解 sofajraft 框架日志模块的知识了。这一个模块的知识总共分为四个阶段，在第一阶段，我将为大家引入日志模块组件，这一阶段的知识非常简单，也正是本章我要为大家讲解的内容；在第二阶段，我会实现领导者生产日志，并将日志落盘的功能；在第三阶段，我将为大家实现领导者向跟随者复制日志的功能；这两个阶段的知识在逻辑上稍微有些复杂，但是并没有什么难度；在第四阶段，我将引入 Pipeline 模式，重构领导者向跟随者传输日志的功能，第四阶段的内容逻辑会更复杂一点。<br />当然，我要再解释一下，我刚才对日志模块的知识分阶段解释了一下，并不是想为大家强调这一块的知识有多难掌握，恰恰相反，我是想提前给大家灌输足够的信心，那就是这个模块的知识并没有多难。虽然在我刚才的解释中，我跟大家说随着课程的深入，功能的实现逻辑也会更复杂。**但是所谓的逻辑复杂，无非就是线性逻辑变成了异步逻辑，甚至是异步逻辑中又出现了另一个异步逻辑**，这种难度根本算不了什么**。因为我们的程序再怎么重构，类和接口的关系已经定型了，基本不会再有什么变化，类的职责也都已经明确了，只要大家明白每个类的作用，这些异步逻辑很容易就能看懂**。而且随着学习的深入，大家就会发现其实这个框架的所有模块都在 NodeImpl 类的 init 方法中被初始化了。也就是说，在学习 sofajraft 框架的时候，只要将 NodeImpl 类的 init 方法作为起点，就能顺藤摸瓜掌握各个模块的初始化，以及模块本身的类结构关系。至于具体的功能，就可以将我提供的测试类当作学习的入口。好了，话不多说，接下来就让我来为我们的程序引入日志组件吧。

<a name="rqXIb"></a>
## 引入 LogEntry，封装客户端写指令

课程进行到这里，我相信我已经不用再为大家解释，为什么我们的框架要引入日志组件了。如果非要解释一句，原因也很简单，那就是领导者需要使用日志传输这个功能，让集群中各个节点对数据的备份和状态保持一致。**在 raft 集群中，一个集群只应该存在一个领导者，客户端的写指令只能发送给集群中的领导者，然后领导者将这条写指令包装成一条日志，将这条日志传输给各个跟随者节点**。这是前三章的内容了。并且，每一条日志都有对应的任期和索引，在前面几章，我为大家实现预投票和正式投票功能的时候，其实还为大家展示过一个 LogId 类，不知道大家还有没有印象，这个 LogId 类的对象封装了一条日志的任期和索引，具体实现请看下面代码块。
```java
//该类的对象就是日志ID
public class LogId implements Comparable<LogId>, Copiable<LogId>, Serializable, Checksum {

    private static final long serialVersionUID = -6680425579347357313L;

    //日志索引
    private long index;
    //日志任期
    private long term;

    @Override
    public LogId copy() {
        return new LogId(this.index, this.term);
    }

    @Override
    public long checksum() {
        byte[] bs = new byte[16];
        Bits.putLong(bs, 0, this.index);
        Bits.putLong(bs, 8, this.term);
        return CrcUtil.crc64(bs);
    }

    public LogId() {
        this(0, 0);
    }

    public LogId(final long index, final long term) {
        super();
        setIndex(index);
        setTerm(term);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.index ^ (this.index >>> 32));
        result = prime * result + (int) (this.term ^ (this.term >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LogId other = (LogId) obj;
        if (this.index != other.index) {
            return false;
        }
        // noinspection RedundantIfStatement
        if (this.term != other.term) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(final LogId o) {
        // Compare term at first
        final int c = Long.compare(getTerm(), o.getTerm());
        if (c == 0) {
            return Long.compare(getIndex(), o.getIndex());
        } else {
            return c;
        }
    }

    public long getTerm() {
        return this.term;
    }

    public void setTerm(final long term) {
        this.term = term;
    }

    public long getIndex() {
        return this.index;
    }

    public void setIndex(final long index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "LogId [index=" + this.index + ", term=" + this.term + "]";
    }

}
```
刚才我提到，在 raft 集群中，客户端的写操纵指令只能发送给领导者，领导者将这个指令包装成一条日志，并且这条日志的索引是自增的，任期就是当前领导者的任期，然后领导者需要把这条日志传输给跟随者，以此来实现集群中的数据备份。这个流程是很清晰的，很好，现在问题来了，我们应该怎么把一个客户端写指令包装成一条日志呢？或者把问题描述得更准确一点，该怎么定义这个日志类呢？当然，就目前来说成果还不错，我们已经拥有了一个 LogId 来包装每一条日志的任期和索引。**但只有一个 LogId 还是不够的，因为我们最想要的那一部分数据并没有被包装成日志，那就是客户端的写指令，我通常会把这客户端的写指令成为日志的有效数据**。<br />接下来，让我们进一步讨论一下，假如我现在就先给我们的 raft 共识算法框架定义一个日志类，这个日志类的作用就是将客户端的写操作指令包装成日志对象。因为客户端会将很多写指令发送给领导者，所以在领导者这里就会生成很多条日志，**索性我就把这个日志类定义为 LogEntry，也就是日志条目的意思。LogEntry 对象就是一个日志条目，对应一条客户端写指令，也就是说，客户端写指令会被封装在 LogEntry 对象中**。而每一条日志都有对应的索引和任期，而 LogId 对象恰好封装了一条日志的索引和任期，那我为什么不直接把 LogId 定义为 LogEntry 类的一个成员变量呢？这样 LogEntry 对象就拥有了自己的索引和任期。具体实现请看下面代码块。
```java
//该类的对象就是一个日志条目
public class LogEntry {


    //日志ID，注意，日志ID中封装着日志的索引和任期
    private LogId id = new LogId(0, 0);


    public LogId getId() {
        return this.id;
    }

    public void setId(final LogId id) {
        this.id = id;
    }
    
}
```
上看的 LogEntry，就是一个最简单的用来封装客户端指令的日志类，该类的对象就代表一条日志。当然，这时候肯定有朋友会继续追问，哪里封装客户端指令了？看不到啊。没错，我也看不到，我担心有人抢我台词，所以我就先把这个问题问出来了。**假如我现在创建一个 LogEntry 对象，并且设置这个对象的 LogId 成员变量，那么创建的这个 LogEntry 日志对象只拥有对应的索引和任期，并没有包装的客户端写指令，也就是所谓的有效数据**。我们接下来的工作肯定是继续完善这个 LogEntry 类，要做的工作也很简单，**那就是在这个类中再定义一个新的成员变量，用这个成员变量来封装客户端写指令**。那么现在问题来了，该定义什么类型的成员变量来封装客户端写指令呢？这时候就让我为大家再分析一下。<br />请大家想一想，假如现在有一个分布式 kv 数据库用到了我们正在开发的 raft 共识算法框架，并且我也为这个 kv 数据库开发好了一个客户端。当客户端发送写指令的时候，它会将指令直接发送给 raft 集群中的领导者。这时候大家肯定还不太明白这一切内部的流程是怎么运转的，没关系，后面在更新 Naco 的时候，大家就全清楚了，现在大家只需要知道，客户端即将向领导者发送写指令即可。<br />因为是 kv 数据库，我也定义了两条和操作键值对相关的最基本的客户端写指令，**分别是 put 和 remove**。这两个指令完全可以用枚举对象来表示，比如我们就定义一个 CommandType 枚举类，这个枚举类中有两个枚举对象，分别是 PUT 和 REOMVE，这样就可以直接根据枚举对象来判断当前的写指令要执行的是什么操作了。当然，既然是写操作指令，肯定也要定义操作数，也就是要操作的 key 和 value 分别是多少。如果是 remove 指令，只需要一个 key 就可以了，这时候只需要把对应的 value 设置为 null 即可。好了，现在写指令和操作数也定义好了。那么，客户端怎么把写指令发送给领导者呢？肯定不能直接发送这三个数据，**按照常规流程，肯定是定义一个新的类，用这个类的对象封装这三个数据，然后将对象序列化，网络传输给领导者**。所以我们就不防再定义一个 WriteOperation 类，翻译过来就是写操作的意思，在这个类中定义三个成员变量，具体实现如下，请看下面代码块。
```java
public class WriteOperation<K,V> {


    //本次操作
    private CommandType command;

    private K key;

    private V value;

    public WriteOperation(CommandType command, K key, V value) {
        this.command = command;
        this.key = key;
        this.value = value;
    }

   
    public CommandType getCommand() {
        return command;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }


    public void setCommand(CommandType command) {
        this.command = command;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }
    
}
```
当 WriteOperation 类定义好之后，客户端完全可以把一次操作的指令和操作数都封装在 WriteOperation 对象中，然后在客户端请求中把这个对象发送给 raft 集群的领导者。领导者接收到客户端的请求之后，就可以从请求中获得这个 WriteOperation 对象，这样一来，领导者不就得到了写指令了吗？至于领导者之后怎么应用写指令，这就是状态机模块的知识了，并且是状态机暴露给用户的业务层接口中的逻辑，是需要用户根据不同的业务逻辑自己实现的，这里我就不为大家进一步展开了。当然，我刚才描述的客户端向领导者发送指令的流程也过于简单，大家只需要理解这个意思即可。总之，现在领导者确实可以从客户端接收到写指令本身了。<br />那么，领导者获取到写指令之后呢？该怎么把这个指令封装到我们上面定义好的 LogEntry 对象中呢？还是刚才那个问题，要在 LogEntry 类中定义一个什么成员变量来接收这个客户端的写指令呢？现在我为大家定义的客户端写指令是 WriteOperation 类型的，难道就要在 LogEntry 类中也定义一个 WriteOperation 类型的成员变量吗？这么做显然不太合适，这时候一定要理清楚，**我们目前开发的是一个 raft 共识算法框架，LogEntry 是这个 raft 框架中的一个类，这个框架可能被任何分布式程序使用，可能有各种各样的客户端发送各种各样的写指令给集群中的领导者，如果在 LogEntry 中定义 WriteOperation 成员变量，这不就把领导者能接收并且包装成日志的写指令限定死了吗？没有任何扩展性，显然不符合开发框架的原则**。<br />那我们应该怎么做呢？既然 LogEntry 中的类型不能写死，索性就弄的直接点吧，比如，**我直接在 LogEntry 类中定义一个 ByteBuffer 类型的 data 成员变量，这样一来，不管客户端发过来的是什么类型的写指令，都可以把它们变成字节放到字节数组中，然后包装在 ByteBuffer 对象中赋值给 LogEntry 对象中的 data 成员变量。等到要应用日志的时候，再从 LogEntry 中获得对应的字节数组，然后还原为对象本身，然后解析这个对象中的内容，判断是什么指令，操作数是多少，这样一来，我们开发的这个 raft 共识算法程序不就具备通用性了吗**？当然，这其中的具体操作是每个业务程序员自己的工作，只要保证能将写操作指令成功赋值给 LogEntry 对象中的 data 成员变量即可。为了验证我刚才分析的正确性，我特意从 Nacos 中截取了一段关键代码，这段代码就是当客户端把操作指令发送给集群中的领导者之后，在领导者这边将指令本身包转成了一个 ByteBuffer** **对象，具体方法请看下面代码块。
```java
//该类是nacos中非常重要的一个类，这个类的作用是将nacos程序包装成raft集群中节点
//里面定义了很多和sofajraft集群操作有关的方法
public class JRaftServer {


    //省略其他内容


    //这个方法就是在客户端发送指令给当前节点之后，并且当前节点恰好是nacos集群中的领导者
    //当前节点就会调用下面这个方法，将方法中Message类型的参数包装成日志
    //方法中的Message类型的参数其实就包装了操作指令
    //public class Task implements Serializable {
    //    private static final long serialVersionUID = 2971309899898274575L;
    //    private ByteBuffer data;
    //    private Closure done;
    //}
    //以上就是Task类的内容，其内部有一个ByteBuffer类型的成员变量，一个回调对象成员变量
    public void applyOperation(Node node, Message data, FailoverClosure closure) {
        //创建一个用于任务，该任务中会携带被包装成ByteBuffer的写操作指令
        //以及一个通知客户端指令是否应用成功的回调方法
        //当task中的指令应用成功了，其内部包装的回调方法将会被回调
        final Task task = new Task();
        //设置task内部的回调方法
        task.setDone(new NacosClosure(data, status -> {
            NacosClosure.NacosStatus nacosStatus = (NacosClosure.NacosStatus) status;
            closure.setThrowable(nacosStatus.getThrowable());
            //可以看到，在这里设置了返回客户端的响应
            closure.setResponse(nacosStatus.getResponse());
            closure.run(nacosStatus);
        }));
        
        //创建一个字节数组，这个字节数组中，用不同的字节表示本次客户端发送过来的请求类型
        byte[] requestTypeFieldBytes = new byte[2];
        //设置请求类型标志位
        requestTypeFieldBytes[0] = ProtoMessageUtil.REQUEST_TYPE_FIELD_TAG;
        //判断本次请求是读请求还是写请求
        if (data instanceof ReadRequest) {
            //走到这里以为这是读请求，设置读请求标志
            requestTypeFieldBytes[1] = ProtoMessageUtil.REQUEST_TYPE_READ;
        } else {//走到这里意味着是写请求，设置写请求标志
            requestTypeFieldBytes[1] = ProtoMessageUtil.REQUEST_TYPE_WRITE;
        }
        //将请求本身转化为字节数组
        byte[] dataBytes = data.toByteArray();
        //根据requestTypeFieldBytes.length + dataBytes.length长度，也就是刚才创建的请求类型字节数组
        //以及请求本身字节长度分配一个ByteBuffer缓冲区，把这两个字节数组都放到缓冲区中
        //到此为止，请求类型以及请求内容本身都被ByteBuffer包装了
        //然后把这个ByteBuffer设置到task中
        task.setData((ByteBuffer) ByteBuffer.allocate(requestTypeFieldBytes.length + dataBytes.length)
                .put(requestTypeFieldBytes).put(dataBytes).position(0));
        //在这里将task传递到领导者内部，在领导者内部，经过一系列方法调用
        //task中的ByteBuffer成员变量会交给领导者中的LogEntry对象来保管
        node.apply(task);
    }



    //省略其他内容
}
```
可能现在大家都还看不懂上面的代码块，感兴趣的朋友们可以多看几遍，大家也可以只混个眼熟，等后面我更新 Nacos 的时候再为大家详细讲解。总之，现在我们已经清楚了，**确实需要在 LogEntry 中定义一个新的成员变量，这个成员变量应该定义为 Bytebuffer 类型的。客户端向集群领导者发送的每一条写指令都应该在领导者内部被 LogEntry 对象来封装**。好了，下面就让我们看一下 LogEntry 类的具体实现，请看下面代码块。
```java
//该类的对象就是一个日志条目
public class LogEntry {


    public static final ByteBuffer EMPTY_DATA = ByteBuffer.wrap(new byte[0]);

    //日志ID，注意，日志ID中封装着日志的索引和任期
    private LogId id = new LogId(0, 0);

    //真正的日志信息，初始化时为空
    private ByteBuffer data = EMPTY_DATA;


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
    
}
```
很好，现在终于给 LogEntry 添加了 ByteBuffer 成员变量了，**我们也知道了这个 ByteBuffer 成员变量就是日志对象中的有效数据，因为这部分数据就是客户端的操作指令本身，至于日志的任期和索引，就封装在了 LogId 这个成员变量中**。那么，到此为止，这个 LogEntry 就实现完毕了吗？当我提出这个问题的时候，大家应该就知道了，显然，这个 LogEntry 类还没有重构完整。究竟哪里没有重构完整，也许大家一时间没有头绪，因为这涉及到之后章节的知识，比如说配置变更。现在我还没有为大家讲解，大家没有头绪也是很正常的。<br />接下来，让我们思考一下，目前我们实现的这个 LogEntry 只是能够包装一条条客户端指令，当然，按照 raft 共识算法，能把客户端指令包装成日志已经足够了。领导者把日志复制给客户端，就可以实现集群中各个节点的数据备份和状态一致。如果简单概括一下，实际上我们可以这么认为，**那就是领导者产生了一条日志的同时，就意味着领导者相对于集群中其他节点的数据状态发生了变化，领导者需要把这条日志传输给集群中其他的节点，才能让集群中所有节点的状态保持一致**。那么，请让我再提一个问题，在 raft 集群中，除了领导者产生日志的时候，还有什么场景，会让领导者的数据状态和集群中其他节点不一样吗？这时候答案就显而易见了，那就是集群配置变更场景。**在 raft 集群中，配置变更的操作也是在领导者中执行的，比如说整个集群要添加新的节点，或者是移除节点，这就意味着集群中的节点数量发生了变化，也就意味着集群的配置信息发生了变化。只在领导者中执行集群配置变更的操作，显然只有领导者知道集群配置发生了什么变化，集群中的其他节点并不知道。要想要集群中的其他节点也知道集群配置信息发生了变化，那么领导者就需要把集群配置变更的信息也传输给各个跟随者节点。当然，也是通过日志传输的**。<br />**实际上，在 sofajraft 框架中，领导者也会把每一次集群变更的信息也包装成一条日志，将这条日志传递给集群中的各个跟随者节点。集群变更的信息同样也是用 LogEntry 对象来封装的，所谓集群信息，不就是整个集群中有多少个节点，以及各个节点的信息吗？所以，每一次集群变更之后，领导者只需要把集群中当下的节点信息都封装到一个 LogEntry 对象中，然后把这个对象传输给集群中的各个跟随者节点**，这样一来，不就实现了集群配置信息的传输吗？所以，在 LogEntry 类中还应该定义一些新的成员变量，比如定义一个成员变量表示当前日志对象封装的是业务日志还是集群配置变更日志。还要再定义一些集合，封装当前集群中最新的节点信息。具体实现请看下面代码块。
```java
//该类的对象就是一个日志条目
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
到此为止，这个 LogEntry 才真正被重构完整。当然，在第二版本代码中，这个类中的内容比文章中展示得更详细，大家可以在学习第二版本代码的时候再对比一下。

<a name="MqXBe"></a>
## 掌握日志组件内部的类结构

很好，随着 LogEntry 的实现，我们终于也可以步入本章正题了，那就是引入日志组件，并且实现日志组件初始化。为什么现在才为大家引入日志组件呢？等大家继续向下看就会发现，LogEntry 对象在日志组件初始化的过程中发挥了关键作用，所以我必须要先为大家把 LogEntry 实现完毕。好了，接下来就让我们先一起来看看，所谓的日志组件该怎么定义。<br />不知道大家还有没有印象，在前面章节，我为大家实现 raft 框架中预投票和正式投票操作时，会在请求中封装候选者节点本地最新的日志索引，还有日志对应的任期，在获得日志索引的时候，我提供的并不是正式代码，而是下面代码块中的简单代码。请看下面代码块。
```java
    //比如说就在正式发起选取的方法，在该方法中，候选者节点会向集群中的其他各个节点
    //发送
    private void electSelf() {
 
        //记录开始正式投票的日志
        LOG.info("Node {} start vote and grant vote self, term={}.", getNodeId(), this.currTerm);
        //根据当前节点的ID去配置信息中查看当前节点是为集群中的节点，如果不是集群中的节点就直接退出
        if (!this.conf.contains(this.serverId)) {
            LOG.warn("Node {} can't do electSelf as it is not in {}.", getNodeId(), this.conf);
            return;
        }
        //如果当前节点目前是跟随者，就停止超时选举定时器工作，超时选举定时器是用来进行选举的，但是当前的节点正在进行选举工作
        //就没必要让这个定时器工作了，等选举工作结束之后再启动，如果选举迟迟没有结果，会有另一个定时器处理的，也就是选举超时定时器
        if (this.state == State.STATE_FOLLOWER) {
            LOG.debug("Node {} stop election timer, term={}.", getNodeId(), this.currTerm);
            //在这里停止超时选举定时器工作
            this.electionTimer.stop();
        }
        //既然正在选举，肯定是没有领导者了，所以在进行选举时把当前节点的领导者节点设置为空
        //这里的PeerId.emptyPeer()就是一个无效的节点ID，会被赋值给当前节点的领导者ID
        resetLeaderId(PeerId.emptyPeer(), new Status(RaftError.ERAFTTIMEDOUT,
                "A follower's leader_id is reset to NULL as it begins to request_vote."));
        
        //将当前节点状态更改为候选者
        this.state = State.STATE_CANDIDATE;
        
        //递增当前节点的任期
        this.currTerm++;
        
        //当前节点要为自己投票，所以把为谁投票的ID设置为自己的服务器ID
        this.votedId = this.serverId.copy();
        
        //这里还会记录一条日志，表示要启动投票超时计时器了
        LOG.debug("Node {} start vote timer, term={} .", getNodeId(), this.currTerm);

        //启动选举超时定时器
        this.voteTimer.start();
        
        //初始投票计数器，这里初始化的是正式投票的计数器
        this.voteCtx.init(this.conf.getConf(), this.conf.isStable() ? null : this.conf.getOldConf());
        
        //这里仍然写死了，获得当前节点最后一个日志的ID
        //源码是这样的：final LogId lastLogId = this.logManager.getLastLogId(true);使用日志管理器来获得最后一条日志信息
        final LogId lastLogId = new LogId(0,0);

        //省略其他内容
        
    }
```
在上面代码块的第 44 行，可以看到我写了 final LogId lastLogId = new LogId(0,0) 这样一行代码，这里本来要获得当前节点最新日志的索引和任期，得到了 LogId 对象，就可以从 LogId 对象中获得日志的索引和任期了。本来当前节点最新日志的索引和任期应该从节点的日志组件中获得，但是在第一版本代码中我还没有为我们的程序引入日志组件，所以就把代码写死了，自己创建了一个 LogId 对象，默认本地最新日志的索引和任期都为 0，让大家先将就着看。但现在到了第二版本代码，要引入日志组件了，这里就不能这么写了。其实在这行代码上面的注释中，我也给大家列出了 sofajraft 框架源码中的写法，**那就是 this.logManager.getLastLogId(true) 这行代码，调用 logManager 对象的 getLastLogId 方法，直接获得本地最新日志的 LogId ，然后从 LogId 中得到本地最新日志的索引和任期即可**。**所以，这个 logManager 就是我接下来要为大家定义的日志组件**。<br />我们一直说要定义日志组件，说到底无非是定义一个新的类，在这个类中定义各种成员变量和方法，让这个类的对象可以提供各种和日志相关的功能。所以，我们也可以把这个日志组件直接称为日志管理器，如果要用一个类来定义，就可以把这个类定义为 LogManager，也就是日志管理器的意思。我们先不考虑这个 LogManager 内部要怎么实现，要在哪里创建，我想先请大家想一想，这个 LogManager 要被谁使用呢？这个问题的答案几乎没有正义，肯定是要被 NodeImpl 使用，对吧？**因为 NodeImpl 是对节点本身的抽象，NodeImpl 类中定义了很多方法，都是节点在集群中要进行的相关操作，这些操作有的和日志有关，有的和状态机有关，有的和快照有关，总之，现在我可以为大家总结一句，NodeImpl 这个类持有了整个框架中所有的组件，当节点本身需要执行什么操作，就直接从内部使用什么组件执行即可**。所以，我刚才引入的这个 LogManager 类肯定也应该被定义成 NodeImpl 类的成员变量，当然，LogManager 初始化，也就是日志管理器初始化也应该在 NodeImpl 类的 init 方法中执行了。现在先让我把 LogManager 这个类定义出来，然后把 NodeImpl 类简单重构一下。请看下面两个代码块。<br />我刚才定义的这个 LogManager 实际上是一个接口，实现类为 LogManagerImpl，实现类才是真正的日志管理器。下面代码块展示的是 LogManager 接口，目前接口中什么都没有。 
```java
//日志管理器要实现的接口
public interface LogManager{

    
}
```
接下来是 LogManagerImpl 实现类，这个实现类就是日志管理器。
```java
//日志管理组件，这个类的对象是用来管理日志的，提供了根据索引得到日志，得到日志的任期
//把日志存放到数据库中等等方法，日志存储系统其实分三个层次，一个是快照，严格来说快照不应该算作日志
//但是快照生成后对应的日志就被删除了，所以就把快照暂时列在日志存储系统中了。一个是内存，最后就是硬盘
//第三个版本会实现完整的日志落盘功能，第二版本只实现了日志组件初始化
public class LogManagerImpl implements LogManager{

    //目前也是空的
}
```
接下来就是重构之后的 NodeImpl 类。
```java
public class NodeImpl implements Node, RaftServerService {


    //日志管理组件
    private LogManager logManager;


    //省略其他内容


    @Override
    public boolean init(final NodeOptions opts) {

        //省略其他内容

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

        //省略其他内容
    }


    //初始化日志组件的方法
    private boolean initLogStorage() {
        //该方法暂时不实现
    }


    //省略其他内容
}
```
可以看到，**在重构之后的 NodeImpl 类中，我已经为我们的程序引入了 logManager 日志管理器，并且这个日志管理器在 NodeImpl 类的 init 方法中初始化了，就在元数据存储器组件初始化之前初始化完毕**。当然，现在初始化 logManager 日志管理器的 initLogStorage 方法还没有实现，因为我还没有为大家具体分析这个日志管理器究竟该怎么实现。<br />其实在上面我已经为这个 logManager 日志管理器定义了一个方法了，那就是 getLastLogId 方法，在正式发送投票请求的方法中，可以调用 logManager 的 getLastLogId 方法，直接获得本地最新日志的 LogId。所以，这个 getLastLogId 方法肯定要定义在 LogManager 日志管理器中。那么，除了这个方法，还应该定义什么方法呢？按照常规的思路，**这个日志管理器中定义的肯定是和日志相关的方法，比如得到日志，或者是添加日志，或者是根据某个索引得到对应日志等等。简单来说，更像是一些功能扩充后的 get、set 方法**。实际上我也已经在 logManager 接口中定义好了一些操纵日志的方法，请大家看看下面的代码块。
```java
//日志管理器要实现的接口
public interface LogManager{


    //将一批日志持久化的方法
    void appendEntries(final List<LogEntry> entries, StableClosure done);


    //根据索引获得指定日志条目
    LogEntry getEntry(final long index);

    
    //根据日志索引获得对应任期的方法
    long getTerm(final long index);

    
    //得到第一条日志索引的方法
    long getFirstLogIndex();

    
    //得到最后一条日志索引的方法
    long getLastLogIndex();

    
    //得到最后一条日志的索引，这个方法和上面方法的区别是
    //当调用这个方法的时候，如果方法参数isFlush为true
    //就会将内存中的日志刷新到硬盘中，然后返回最新的最后一条日志的索引
    //这个方法会在第三版本代码中得到真正实现
    long getLastLogIndex(final boolean isFlush);

    
    //得到最后一条日志ID
    LogId getLastLogId(final boolean isFlush);

    
    //根据索引得到指定的配置日志条目
    ConfigurationEntry getConfiguration(final long index);
    
}
```
可以看到，在上面的代码块中定义的 8 个方法都是和日志有关的，其中有的是将日志持久化的方法，有的是从日志管理器中得到对应的日志。这个还是很容易理解的。如果大家都理解了这几个方法，现在我又有了一个新的问题，那就是不管是领导者还是跟随者，都要将日志本地持久化，并且在 LogManager 接口中，也定义了将日志持久化到本地的方法，那么，日志究竟要怎么持久化，或者说以什么形式持久化呢？为什么我要提出这个问题？请大家结合上面的代码块思考一下，**不管我们要存放什么数据，最终都要使用对应的数据结构将数据保存起来，就算是要持久化到本地，也要考虑合适的编解码方式，以便日志持久化到本地和从本地读取到内存中**。也许是一个 Map、也许是一个集合、也许是一个数组，也许就直接是本地硬盘，总之肯定要有一个地方存放数据才行，现在我已经在 LogManager 日志管理器中定义了几个方法了，假如我要在日志管理器中存放一些数据，要通过什么来执行这个操作呢？肯定不用在 LogManager 中定义什么 Map 或者数组了，因为日志是要持久化到本地的，这里我就直接讲解了，**实际上在 sofajraft 框架中，在 LogManager 接口的实现类 LogManagerImpl 中定义了一个成员变量，那就是 LogStorage 类型的 logStorage 成员变量。我把这个成员变量称为日志存储器，LogManagerImpl 日志管理器就是通过其内部的 logStorage 日志存储器，才能将日志持久化到本地的**。<br />当然这个 LogStorage** **日志管理器实际上也是一个接口，其实现类为 RocksDBLogStorage，而这个 RocksDBLogStorage，实际上就是对 RocksDB 数据库做了一层封装，其内部持有者 RocksDB 数据库对象，最终将日志存放在 RocksDB 数据库中，然后在数据库中持久化到本地(这个 RocksDB 是一个 C++ 开发的数据库，在文章中我就不详细展开了，第二版本代码注释中对这个数据库解释得很详细，大家可以在第二版本代码中再研究这个数据库本身)。<br />好了，文字分析了这么多，终究不如直接展示代码来的直接，这一次我们就直接展示日志管理器本身了，也就是 LogManagerImpl 这个类。在展示之前，还有一点小细节需要继续和大家讨论一下，那就是在这个 LogManagerImpl 日志管理器中，究竟应该定义哪些成员变量？刚才已经为这个 LogManagerImpl 引入一个成员变量了，那就是 RocksDBLogStorage，也就是日志存储器，除此之外呢？还需要定义什么成员变量呢？我自己能想到的确实不多，**考虑到某些日志的特殊性，比如本地日志中第一条日志的索引，还有最后一条日志的索引可能会被经常使用，所以，将这两个索引定义为 LogManagerImpl 类中的成员变量似乎挺合适的**。这样一来，当 LogManagerImpl 执行了某些操纵日志的方法后，直接更新这两个成员变量，其他组件就可以直接通过访问这两个成员变量得到相应的数据了。也许有朋友会觉得困惑，为什么要把本地日志中的第一条日志索引单独定义为成员变量，第一条日志索引难道不为 1 吗？在第二版本代码中确实为 1，但是在引入快照组件之后，会将变成快照的日志全部删除，这时候就有必要把本地中的第一条日志索引单独记录下来了，并且这个值可能会随着快照的不断生成而不断更新。<br />那么，**除了本地第一条日志和最后一条日志的索引，以及 LogStorage 日志存储器之外**，还应该在 LogManagerImpl 中定义什么成员变量呢？这里就直接参照 sofajraft 框架的源码吧，其实在 sofajraft 框架的 LogManagerImpl 类中还定义了最新被应用到状态机的日志 Id，持久化到硬盘上的最后一条日志的 Id，日志管理器是否正在工作的标志变量、解决并发问题的读写锁等等。可以看到，后面列出来的这些成员变量在第二版本代码几乎都用上，所以我就直接列出来让大家混个眼熟，后面再详细讲解，同步锁的作用大家也可以自己去注释中看看。LogManagerImpl 类的具体实现请看下面代码块。
```java
//日志管理组件，这个类的对象是用来管理日志的，提供了根据索引得到日志，得到日志的任期
//把日志存放到数据库中等等方法，日志存储系统其实分三个层次，一个是快照，严格来说快照不应该算作日志
//但是快照生成后对应的日志就被删除了，所以就把快照暂时列在日志存储系统中了。一个是内存，最后就是硬盘
//第三个版本会实现完整的日志落盘功能，第二版本只实现了日志组件初始化
public class LogManagerImpl implements LogManager{

    private static final Logger LOG = LoggerFactory.getLogger(LogManagerImpl.class);

    //日志存储器，这个类虽然持有日志管理器，但是并不能意味着
    //这个LogStorage接口的实现类就是刚才提到的RocksDBLogStorage
    private LogStorage logStorage;

    //配置信息管理器
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
    private String groupId;



    
    @Override
    public boolean init(final LogManagerOptions opts) {

        //该方法暂时不做实现
    }
    

    //将一批日志持久化的方法
    void appendEntries(final List<LogEntry> entries, StableClosure done){
        
        //暂时不做实现
    }


    //根据索引获得指定日志条目
    LogEntry getEntry(final long index){

         //暂时不做实现
    }

    
    //根据日志索引获得对应任期的方法
    long getTerm(final long index){

         //暂时不做实现
    }

    
    //得到第一条日志索引的方法
    long getFirstLogIndex(){

         //暂时不做实现
    }

    
    //得到最后一条日志索引的方法
    long getLastLogIndex(){

         //暂时不做实现
    }

    
    //得到最后一条日志的索引，这个方法和上面方法的区别是
    //当调用这个方法的时候，如果方法参数isFlush为true
    //就会将内存中的日志刷新到硬盘中，然后返回最新的最后一条日志的索引
    //这个方法会在第三版本代码中得到真正实现
    long getLastLogIndex(final boolean isFlush){

         //暂时不做实现
    }

    
    //得到最后一条日志ID
    LogId getLastLogId(final boolean isFlush){

         //暂时不做实现
    }

    
    //根据索引得到指定的配置日志条目
    ConfigurationEntry getConfiguration(final long index){

         //暂时不做实现
    }
    
}
```
LogManagerImpl 类展示完毕之后，接下来我为大家简单展示一下 RocksDBLogStorage 类，也就是日志存储器。当然，之前我跟大家说了，RocksDBLogStorage 类其实就是 LogStorage 接口的实现类。所以在展示 RocksDBLogStorage 类之前，我先为大家把 LogStorage 接口展示一下，跟大家梳理一下这个接口中定义的方法。其实讲到这里，我相信大家也能意识到，**所谓的日志管理器持有了 RocksDBLogStorage 日志存储器，无非就是对 RocksDBLogStorage 日志存储器做了一层封装，真正的存储日志的操作都是 RocksDBLogStorage 对象来执行的，把日志存储在 RocksDBLogStorage 对象内部持有的 RocksDB 数据库中。如果是这样的话，当 LogManagerImpl 日志管理器想要获得本地最后一条日志的索引，实际上最终也是通过 RocksDBLogStorage 日志存储器来获得的，因为 RocksDBLogStorage 才是真正直接操纵日志的对象**。所以，假如 LogManagerImpl 类中定义了一个 getFirstLogIndex 方法，那么在 RocksDBLogStorage 类中也要定义一个同名方法。这个想必也是容易理解的。其他的方法都是一个道理，**RocksDBLogStorage 中确实需要定义一些 LogManagerImpl 中的同名方法**。<br />好了，明白了这个，接下来请大家先看看 RocksDBLogStorage 的接口 LogStorage 中要定义的方法。请看下面代码块。
```java
//日志存储器要实现的接口
public interface LogStorage extends Lifecycle<LogStorageOptions>, Storage {

    //得到第一条日志索引的方法
    long getFirstLogIndex();

    //得到最后一条日志索引的方法
    long getLastLogIndex();

    //根据索引得到指定日志的方法
    LogEntry getEntry(final long index);

}
```
接下来是 LogStorage 接口的实现类，也就是真正的日志存储器 RocksDBLogStorage。请看下面代码块。
```java
//日志存储器，存储日志的所有功能都是由这个类提供的，数据库存储日志是以键值对的形式存放发
//键就是日志索引，值就是日志条目本身
public class RocksDBLogStorage implements LogStorage {

    private static final Logger LOG = LoggerFactory.getLogger(RocksDBLogStorage.class);

    static {
        //加载RocksDB依赖的jar包，其实就是加载一些C++代码文件
        //因为RocksDB是C++编写的，加载了库文件后，就可以调用C++的代码了
        RocksDB.loadLibrary();
    }


    //日志要存放的本地路径
    private final String path;
    
    //写操作后是否要立即进行持久化，所谓立即不就是同步的意思吗？
    //这个参数会在日志入库的时候使用
    private final boolean sync;
    
    //RocksDB数据库对象，存储日志到数据库或者是从数据库获取日志，都是通过这个对象来进行的
    private RocksDB db;


    @Override
    public boolean init(final LogStorageOptions opts) {

        //该方法暂时不实现
    }


    //得到第一条日志索引的方法
    long getFirstLogIndex(){
        
        //该方法暂时不实现
    }

    //得到最后一条日志索引的方法
    long getLastLogIndex(){

        //该方法暂时不实现
    }

    //根据索引得到指定日志的方法
    LogEntry getEntry(final long index){

        //该方法暂时不实现
    }

    
}
```
在上面的代码块中，我给 RocksDBLogStorage 定义了几个简单的成员变量，大家看看代码注释就能理解了。到此为止，我就把日志管理器和日志存储器简单给大家介绍完毕了，很多方法都没有实现。接下来，就该正式为大家实现这些方法，当然，在实现这些方法之前，还要先把日志组件初始化实现了。这一章显然是实现不了了，下一章再为大家实现吧。下一章要讲解的内容并没有很多，引入日志编解码器之后，就可以成功初始化日志组件了。好了朋友们，我们下一章见！
