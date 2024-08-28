
大家好，紧接着上一章的内容，这一章我将为大家实现日志组件的初始化。在上一章我已经为大家引入了 LogManager 接口和接口实现类 LogManagerImpl，以及 LogStorage 接口和接口的实现类 RocksDBLogStorage。LogManagerImpl 是日志管理器，RocksDBLogStorage 是日志存储器，也可以称为日志存储引擎。并且 RocksDBLogStorage 是 LogManagerImpl 类的一个成员变量。在上一章我也为大家展示了 LogManagerImpl 和 RocksDBLogStorage 这两个类的内容，当然，这两个类都还非常简单，内部的方法都还没有实现，各自的初始化方法也没有实现。这一章我们就从初始化方法开始讲起，先实现各自的初始化方法，然后再进一步实现其他功能。

<a name="KVPjF"></a>
## 剖析 LogManager 初始化流程

大家应该还有印象，在上一章我为大家展示了一下 NodeImpl 类的代码，因为引入日志组件之后，LogManager 日志管理器就被定义为了 NodeImpl 类的一个成员变量，并且我也跟大家讲到了，LogManager 会在 NodeImpl 的 init 方法中被初始化。具体逻辑请看下面代码块。
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
可以看到，在上面的代码块中，在 NodeImpl 的 init 方法中，执行了 initLogStorage 方法，这个方法就是用来初始化日志组件的。现在我可以跟大家分析分析这个 initLogStorage 方法的实现逻辑了。要**初始化日志组件，首先肯定得先创建日志组件，或者说先得到日志组件的对象，这里的日志组件指的就是 logManager 日志管理器。当然，创建日志管理器时肯定就是创建 LogManager 接口实现类的对象，也就是创建  LogManagerImpl 对象，然后再调用该对象的 init 方法**。这个逻辑还是比较简单的，但是在 sofajraft 框架中，在 initLogStorage 方法中做了更多的操作。**在 sofajraft 中不仅创建了 LogManagerImpl 对象，还创建了 RocksDBLogStorage 对象。实际上，在 sofajraft 框架中，日志存储器也被定义为了 NodeImpl 类中的成员变量。虽然 NodeImpl 对象本身并没有直接使用日志存储器的情况**。具体实现请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {


    //日志管理组件
    private LogManager logManager;

    //日志存储组件，这个接口的实现类就是RocksDBLogStorage对象
    private LogStorage logStorage;

    //为jraft框架提供各种服务的工厂，在第一版本中，这个工厂只提供了元数据存储器服务
    //在第二版本代码中，它就要再次发挥作用了
    private JRaftServiceFactory serviceFactory;


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
这时候大家肯定感到困惑，不明白为什么在 sofajraft 框架中，把日志存储器也定义为 NodeImpl 的成员变量。让我来给大家简单解释一下，实际上，在 sofajraft 框架中，日志存储器接口 LogStorage 有多个不同的实现类，RocksDBLogStorage 只是其中之一，也是逻辑比较简单的一个实现，除了 RocksDBLogStorage 实现类，还有其它 4 个不同的实现类，也就是说在 sofajraft 框架中可供选择使用的日志存储器有 5 个，我为大家展示的只是其中一个，就是这个 RocksDBLogStorage 类。其他的 4 个日志存储器我们先不必关心，大家感兴趣的话可以直接去源码中查看。现在我们先看看这个 RocksDBLogStorage 类的对象是怎么创建的。<br />在上面代码块中，我不止为大家展示了 logManager 和 logStorage 这两个成员变量，还展示了 serviceFactory 成员变量。如果大家对第 4 章的内容还有印象，肯定就能回忆起来，这个 serviceFactory 就是为框架提供各种服务组件的工厂。实际上，**日志存储器就是这个 serviceFactory 服务工厂提供的，这也就意味着 RocksDBLogStorage 对象是在 serviceFactory 服务工厂内部创建的**。具体实现请看下面代码块。
```java
//默认的JRaft服务工厂类，这个类上有SPI注解，在程序启动的时候，显然是要被SPI机制加载到内存中的
@SPI
public class DefaultJRaftServiceFactory implements JRaftServiceFactory {

    public static DefaultJRaftServiceFactory newInstance() {
        return new DefaultJRaftServiceFactory();
    }


    //创建日志存储器，实际上创建的就是一个RocksDBLogStorage对象
    //这个RocksDBLogStorage对象封装了RocksDB数据库，jraft框架运行过程中
    //产生的所有日志都会存放在这个RocksDB数据库中，这个数据库是C++开发的
    //这个uri就是用户定义的日志存储路径
    @Override
    public LogStorage createLogStorage(final String uri, final RaftOptions raftOptions) {
        Requires.requireTrue(StringUtils.isNotBlank(uri), "Blank log storage uri.");
        //在这里创建了日志存储器
        return new RocksDBLogStorage(uri, raftOptions);
    }

    //真正创建元数据存储器的方法
    //url是元数据文件的路径
    //raftOptions中封装着jRaft需要用到的一些配置参数，这些信息从RaftOptions对象创建完成的那一刻就初始化好的
    //因为这些信息都是RaftOptions对象的成员变量
    @Override
    public RaftMetaStorage createRaftMetaStorage(final String uri, final RaftOptions raftOptions) {
        Requires.requireTrue(!StringUtils.isBlank(uri), "Blank raft meta storage uri.");
        //在这里创建元数据存储器
        return new LocalRaftMetaStorage(uri, raftOptions);
    }

}
```
反正 NodeImpl 类持有者这个 serviceFactory 服务工厂，这样一来，在程序执行到 NodeImpl 对象的 init 方法时，开始在方法内部的 initLogStorage 方法中初始化日志组件，就可以直接使用 serviceFactory 服务工厂创建 RocksDBLogStorage 日志存储器，然后把日志存储器交给 LogManagerImpl 日志管理器使用，同时还能直接给 NodeImpl 类中的 logStorage 赋值。<br />好了，现在我们已经分析了几个要在 initLogStorage 方法中执行的操作：**1 使用 serviceFactory 服务工厂创建 RocksDBLogStorage 日志存储器对象；2 将日志存储器对象赋值给 NodeImpl 类的 logStorage 成员变量；3 创建 LogManagerImpl 日志管理器对象；4 调用 LogManagerImpl 日志管理器对象的 init 方法，将日志存储器对象交给日志管理器对象使用**。具体实现请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {


    //日志管理组件
    private LogManager logManager;

    //日志存储组件，这个接口的实现类就是RocksDBLogStorage对象
    private LogStorage logStorage;

    //为jraft框架提供各种服务的工厂，在第一版本中，这个工厂只提供了元数据存储器服务
    //在第二版本代码中，它就要再次发挥作用了
    private JRaftServiceFactory serviceFactory;


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
        
        //使用服务工厂创建日志存储器
        this.logStorage = this.serviceFactory.createLogStorage(this.options.getLogUri(), this.raftOptions);
        
        //创建日志管理器，日志管理器内部持有者日志存储器
        this.logManager = new LogManagerImpl();
        
        //创建封装日志管理器需要参数的对象
        final LogManagerOptions opts = new LogManagerOptions();
        
        //下面都是设置一些信息的操作了，设置集群Id
        opts.setGroupId(this.groupId);
        //设置日志存储器
        opts.setLogStorage(this.logStorage);
        //设置集群配置管理器
        opts.setConfigurationManager(this.configManager);
        opts.setRaftOptions(this.raftOptions);
        //在这里使用opts对象初始化日志管理器
        return this.logManager.init(opts);
    }


    //省略其他内容
}
```
本来讲到这里，我们就可以直接去实现 LogManagerImpl 类的 init 方法了。但是，在实现这个方法之前，我想先为大家把主题岔开一下，讲点不一样的东西。在上一章我为大家讲解了，**实际上在 sofajraft 框架中，每一条日志想要持久化到本地，都要通过 RocksDBLogStorage 日志存储器才能实现。而在 RocksDBLogStorage 存储器中，实际上是持有了 RocksDB 数据库对象，把日志存储到 RocksDB 数据库中，再由这个数据库完成持久化**。所以说，**一条日志完整的持久化流程是先被领导者生产出来，然后这条日志会交给 LogManagerImpl 日志管理器，调用日志管理器的 appendEntries 方法，将日志交给 RocksDBLogStorage 日志存储器，在日志存储器中，再将日志交给 RocksDB 数据库对象，存储到 RocksDB 数据库中，再由数据库完整本地持久化**。这个其实是下一章的内容了，下一章我要为大家实现日志落盘功能，但是现在有些知识点也需要围绕着日志落盘来简单讲解一下，所以就先为大家稍微扩展一下，下一章会讲解得更加详细。总之，一条日志持久化成功，最终是通过 RocksDB 数据库实现的，要想将日志持久化成功，就要先把日志存放到 RocksDB 数据库中。现在我要为大家继续拓展一个知识点，**那就是 RocksDB 数据库是一个以键值对方式来存放数据的数据库**。这就意味着在我们实现的框架中，如果一条日志想要存储到 RocksDB 数据库中，也需要以键值对的方法进行存储。这个好说，既然是键值对的方式，而且之前我们已经分析过了，**领导者内部生产的每一条日志都有唯一的日志索引，那么存储日志的时候，就以日志的索引为键。日志本身，也就是 LogEntry 对象为 value，**这样不就可以把日志顺利存储到这个键值对数据库中了吗？<br />很好，如果上面的知识大家都理解了，接下来让我们再思考另一个问题，那就是日志本身需要怎么存放到 RocksDB 数据库中呢？刚才我们已经明确了，日志需要以键值对的方式存放到 RocksDB 数据库中。但是，这只是为我们制定了一个大的方针而已，具体的细节都还没有明确。比如说，将数据存放到数据库的时候，是以字符串的方式存储呢？还是以另外的方式存储呢？如果是我的话，我就弄得直接一点，**反正这些数据最终都是要持久化到硬盘上的，索性从一开始我就把这些日志编码成二进制数据，然后直接以二进制的形式将日志存放到 RocksDB 数据库中，RocksDB 数据库再将这些数据持久化到硬盘即可**。<br />如果是这样的话，**显然我就要再为自己的程序定义一个编码器**，对要存放到 RocksDB 数据库中的日志进行编码，当然，**既然有日志编码器，自然也应该有日志解码器**，因为你肯定会在某些场景下，需要将日志从本地加载到内存，到时候就需要解码器登场了。所以，为我们的程序构建编解码器就成了目前的主要任务。<br />那么这个编码器应该怎么定义呢？我们的每一步思考都应该是有逻辑的，既然现在要定义日志编码器了，首先应该考虑的是什么？毫无疑问，应该考虑的是应该对数据的哪些内容进行编码，以及应该定义怎样的编码协议？说协议可能有些夸张了，那么，至少应该定义一个编解码的标准，或者是规范吧？**既然日志是以键值对的方式存放到 RocksDB 数据库中的**，然后持久化到本地，这就意味着键值对中的 key、value 都需要进行二进制编码，key 就是日志索引，value 也就是 LogEntry 对象本身。**如果对 key 进行编码，无非就是把 key 编码成二进制数据，这个很好说，毕竟 key 也就是一个整型数值，所以这个并不是我们要研究的重点，我们关注的重点是如何对 value，也就是 LogEntry 进行编码，换句话说，要对 LogEntry 定制什么样的编码规范**？<br />很好，现在目标已经有了，那么请大家想一想，根据我们对 RPC 的了解，当我们自己手写 RPC 框架的时候，通常都会自定义编码协议，自己定义编解码规范。在我们自己定义的编码规范中，可能会先定义魔数，版本号，数据长度，甚至还会预留一个扩展位，最后是数据本身。那么在我们自己实现的 raft 共识算法框架中，可以怎么定义编码规范呢？这时候简单点就可以了，没必要设计得非常复杂，**首先魔数是必须要有的，魔数的作用有很多，但最重要的作用就是可以作为一段二进制数据的起始标识，通过对魔术的检验，我们可以准确判断当前正在解码的数据是不是一个 LogEntry 的二进制数据；然后再定义一个协议版本号吧，预留一个扩展位字段也很有必要，谁知道以后会不会拓展别的什么功能呢？最后就是 LogEntry 数据本身了**。<br />总之，这么分析下来，我们要对 LogEntry 日志进行编码的规范也差不多明确了，接下来再确定一下协议中的具体字节即可。我是这么规划的，**魔术使用 2 个字节来表示，协议版本号使用 1 个字节来表示，预留字段使用 3 个字节来表示。这样算下来，这三个重要数据一共占据了 6 个字节**，就像下面代码块展示的这样。
```java
 *   0  1     2    3  4  5
 *  +-+-+-+-+-+-+-+-++-+-+-+
 *  |Magic|Version|Reserved|
 *  +-+-+-+-+-+-+-+-++-+-+-+
```
如果要用代码来表示，可以写成下面这样，请看下面代码块。
```java

//魔数，占据2个字节
public static final byte[] MAGIC_BYTES = new byte[]{(byte) 0xBB, (byte) 0xD2};

//版本号，占据1个字节
public static final byte VERSION = 1;

//预留字段，占据3个字节
public static final byte[] RESERVED = new byte[3];

//上面三个属性加起来的长度，总数为6个字节
public static final int HEADER_SIZE = MAGIC_BYTES.length + 1 + RESERVED.length;
```
好了，到此为止，这些前置知识都讲解得差不多了，现在得来点真格的了。假如说现在有一个 LogEntry 对象，这个日志对象的索引为 1，要把这个日志持久化到本地，首先应该对这条日志的键值对进行编码，然后存放到 RocksDB 数据库中，最终持久化到本地硬盘。在编码的过程中应该怎么做呢？先不考虑怎么对键值对的 key 如何编码，先让我们关注一下怎么对键值对的 value，也就是 LogEntry 对象如何编码。这就很简单了，**肯定是把 LogEntry 中的全部数据都转化为二进制数据，存放到一个字节数组中**。所以，这时候就要看看 LogEntry 对象中究竟封装着什么数据了，我把上一章的 LogEntry 类搬运到这里了，请大家简单回顾一下。请看下面代码块。
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
结合上面的代码块，现在我想为大家简单总结一下，**如果想把一个 LogEntry 对象进行二进制编码，那么就应该把日志对象的任期、索引、日志类型、日志有效数据以及集群中的节点信息都进行编码，然后把编码后的数据存放到一个字节数组中即可**。如果这个逻辑理解了，那么接下来我就来为大家正式展示一下我早已经定义好的 LogEntry 编码器和解码器。<br />在展示具体的编解码器之前，我还要先展示一下我定义好的编解码器工厂，也就是 LogEntryV2CodecFactory 类，编解码器就是由这个 LogEntryV2CodecFactory 编解码器工厂创建的。请看下面代码块。
```java
//编解码器工厂，专门提供日志的编解码器，上面是源码中的注释，可以看到编解码遵照的协议，魔数，版本号，预留字段
public class LogEntryV2CodecFactory implements LogEntryCodecFactory {

    private static final LogEntryV2CodecFactory INSTANCE = new LogEntryV2CodecFactory();

    //单例，把工厂对象暴露出去
    public static LogEntryV2CodecFactory getInstance() {
        return INSTANCE;
    }

    
    //魔数
    public static final byte[] MAGIC_BYTES = new byte[]{(byte) 0xBB, (byte) 0xD2};
    
    //版本号
    public static final byte VERSION = 1;
    
    //预留字段
    public static final byte[] RESERVED = new byte[3];
    
    //上面三个属性加起来的长度
    public static final int HEADER_SIZE = MAGIC_BYTES.length + 1 + RESERVED.length;

    //下面两个方法提供了编码器和解码器
    //创建编码器
    @Override
    public LogEntryEncoder encoder() {
        return V2Encoder.INSTANCE;
    }

    //创建解码器
    @Override
    public LogEntryDecoder decoder() {
        //这里的AutoDetectDecoder的具体内容就在后面
        return AutoDetectDecoder.INSTANCE;
    }

    //私有构造方法
    private LogEntryV2CodecFactory() {
    }
}




public class AutoDetectDecoder implements LogEntryDecoder {

    private AutoDetectDecoder() {

    }

    public static final AutoDetectDecoder INSTANCE = new AutoDetectDecoder();

    @Override
    public LogEntry decode(final byte[] bs) {
        if (bs == null || bs.length < 1) {
            return null;
        }//这个V2Decoder就是真正的解码器
        return V2Decoder.INSTANCE.decode(bs);
    }

}
```
 在上面代码块中可以看到，接下来我要给大家展示的编码器名称为 V2Encoder，解码器的名称为 V2Decoder。接下来我先为大家展示编码器的具体内容，请看下面代码块。
```java
//日志的编码器对象
public class V2Encoder implements LogEntryEncoder {

    public static final V2Encoder INSTANCE = new V2Encoder();

    private static boolean hasPeers(final Collection<PeerId> peers) {
        return peers != null && !peers.isEmpty();
    }

    private void encodePeers(final LogOutter.PBLogEntry.Builder builder, final List<PeerId> peers) {
        final int size = peers.size();
        for (int i = 0; i < size; i++) {
            builder.addPeers(ZeroByteStringHelper.wrap(AsciiStringUtil.unsafeEncode(peers.get(i).toString())));
        }
    }

    private void encodeOldPeers(final LogOutter.PBLogEntry.Builder builder, final List<PeerId> peers) {
        final int size = peers.size();
        for (int i = 0; i < size; i++) {
            builder.addOldPeers(ZeroByteStringHelper.wrap(AsciiStringUtil.unsafeEncode(peers.get(i).toString())));
        }
    }


    //编码方法，这个是和解码方法一一对应的，就不再一一解释了，逻辑非常简单
    //当需要把日志存放到数据库时，就会调用这个编码方法，将LogEntry对象进行编码，然后落盘
    @Override
    public byte[] encode(final LogEntry log) {
        Requires.requireNonNull(log, "Null log");
        final LogId logId = log.getId();
        //日志中的各种信息最终都会先编码，然后设置到PBLogEntry中
        //这里创建的是PBLogEntry的构建器
        final LogOutter.PBLogEntry.Builder builder = LogOutter.PBLogEntry.newBuilder()
                //设置日志类型到构建器中
                .setType(log.getType())
                //设置日志索引到构建器中
                .setIndex(logId.getIndex())
                //设置日志任期到构建器中
                .setTerm(logId.getTerm());
        //得到集群的节点信息
        final List<PeerId> peers = log.getPeers();
        //判断集群节点信息是否为空
        if (hasPeers(peers)) {
            //不为空则进行编码
            encodePeers(builder, peers);
        }
        final List<PeerId> oldPeers = log.getOldPeers();
        if (hasPeers(oldPeers)) {
            encodeOldPeers(builder, oldPeers);
        }
        //设置日志校验和
        if (log.hasChecksum()) {
            builder.setChecksum(log.getChecksum());
        }
        //在这里把日志的有效数据设置到构建器中了
        //这个有效数据就是包装的客户端指令，这个千万别忘了
        builder.setData(log.getData() != null ? ZeroByteStringHelper.wrap(log.getData()) : ByteString.EMPTY);
        //在这里终于创建出了PBLogEntry对象，这时候这个PBLogEntry对象就包含了日志条目的所有数据
        final LogOutter.PBLogEntry pbLogEntry = builder.build();
        //获取该对象序列化后的大小
        final int bodyLen = pbLogEntry.getSerializedSize();
        //根据bodyLen的值创建字节数组，这个字节数组最终会存放日志对象的所有数据
        //LogEntryV2CodecFactory.HEADER_SIZE 这个得到的是编码协议中魔术，版本号，预留字段的总长度，也就是6
        final byte[] ret = new byte[LogEntryV2CodecFactory.HEADER_SIZE + bodyLen];
        //设置魔术
        int i = 0;
        for (; i < LogEntryV2CodecFactory.MAGIC_BYTES.length; i++) {
            ret[i] = LogEntryV2CodecFactory.MAGIC_BYTES[i];
        }//设置版本号
        ret[i++] = LogEntryV2CodecFactory.VERSION;
        //设置预留字段
        for (; i < LogEntryV2CodecFactory.HEADER_SIZE; i++) {
            ret[i] = LogEntryV2CodecFactory.RESERVED[i - LogEntryV2CodecFactory.MAGIC_BYTES.length - 1];
        }
        //把pbLogEntry对象写入到ret字节数组中
        writeToByteArray(pbLogEntry, ret, i, bodyLen);
        //返回编码完成的字节数组
        return ret;
    }

    //把pbLogEntry对象写入到ret字节数组的方法
    private void writeToByteArray(final LogOutter.PBLogEntry pbLogEntry, final byte[] array, final int offset, final int len) {
        final CodedOutputStream output = CodedOutputStream.newInstance(array, offset, len);
        try {
            pbLogEntry.writeTo(output);
            output.checkNoSpaceLeft();
        } catch (final IOException e) {
            throw new LogEntryCorruptedException(
                    "Serializing PBLogEntry to a byte array threw an IOException (should never happen).", e);
        }
    }

    private V2Encoder() {
    }
}

```
代码中的注释非常详细，V2Encoder 类中的核心方法就是 encode 方法，大家看看就行。接下来就是 V2Decoder 解码器的具体实现，请看下面代码块。
```java
//日志的解码器
public class V2Decoder implements LogEntryDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(V2Decoder.class);

    public static final V2Decoder INSTANCE = new V2Decoder();

    //在这个方法内部，会把方法参这一堆字节解析成一个PBLogEntry对象
    //将日志从数据库中读取出来时，会调用这个解码器对日志进行解码，把日志数据解析为一个LogEntry对象
    @Override
    public LogEntry decode(final byte[] bs) {
        //判断字节数组的长度是否小于6个字节，因为魔数、版本号、预留字段的总长度为6字节
        //如果比6字节还小就直接退出当前方法
        if (bs == null || bs.length < LogEntryV2CodecFactory.HEADER_SIZE) {
            return null;
        }

        int i = 0;
        //校验魔数
        for (; i < LogEntryV2CodecFactory.MAGIC_BYTES.length; i++) {
            if (bs[i] != LogEntryV2CodecFactory.MAGIC_BYTES[i]) {
                return null;
            }
        }
        //校验版本号
        if (bs[i++] != LogEntryV2CodecFactory.VERSION) {
            return null;
        }
        //忽略预留字段
        i += LogEntryV2CodecFactory.RESERVED.length;
        
        try {//就在这里，把字节解析成PBLogEntry对象，字节就是从数据库中获得的
            final LogOutter.PBLogEntry entry = LogOutter.PBLogEntry.parseFrom(ZeroByteStringHelper.wrap(bs, i, bs.length - i));
            //创建一个LogEntry对象，用来封装解析好的数据
            final LogEntry log = new LogEntry();
            
            //设置条目类型，看看是配置信息日志还是业务日志
            log.setType(entry.getType());
            
            //设置日志索引
            log.getId().setIndex(entry.getIndex());
            
            //设置任期
            log.getId().setTerm(entry.getTerm());
            
            //设置校验和
            if (entry.hasChecksum()) {
                log.setChecksum(entry.getChecksum());
            }
            //下面的我就不再重复了，大家根据LogEntry内部结构，就知道解析的是个成员变量了
            //在第二版本，LogEntry内部的成员变量还无法解析完整，下面的这些都没有数据，等到更新到配置变更的时候
            //下面这些才能被完整解析
            if (entry.getPeersCount() > 0) {
                final List<PeerId> peers = new ArrayList<>(entry.getPeersCount());
                for (final ByteString bstring : entry.getPeersList()) {
                    peers.add(JRaftUtils.getPeerId(AsciiStringUtil.unsafeDecode(bstring)));
                }
                log.setPeers(peers);
            }
            if (entry.getOldPeersCount() > 0) {
                final List<PeerId> peers = new ArrayList<>(entry.getOldPeersCount());
                for (final ByteString bstring : entry.getOldPeersList()) {
                    peers.add(JRaftUtils.getPeerId(AsciiStringUtil.unsafeDecode(bstring)));
                }
                log.setOldPeers(peers);
            }

            //设置日志有效数据，这个有效数据就是包装的客户端指令，这个千万别忘了
            final ByteString data = entry.getData();
            if (!data.isEmpty()) {
                log.setData(ByteBuffer.wrap(ZeroByteStringHelper.getByteArray(data)));
            }

            return log;
        } catch (final InvalidProtocolBufferException e) {
            LOG.error("Fail to decode pb log entry", e);
            return null;
        }
    }

    private V2Decoder() {
    }
}

```
到此为止，我就为大家把 LogEntry 对象的编解码器实现完毕了。当然，我们的任务还没有完成，现在我们也只是实现了键值对中的 value 的编解码功能，那么键值对中的 key 该怎么编码呢？这个很简单，在 sofajraft 框架中制定了一个规范，**那就是日志索引统一编码成为 8 个字节长度的二进制数据**，使用以下方式对键值对的 key 进行编码，请看下面代码块。
```java
//创建了长度为8的字节数组，是用来封装索引的值的
byte[] vs = new byte[8];
//把firstLogIndex索引编码为二进制数据
Bits.putLong(vs, 0, firstLogIndex);
```
现在，我才终于为大家把日志编解码的功能全都实现了。好了，此刻我终于可以解释一下为什么我在前面忽然把主题岔开了，没有继续实现 LogManagerImpl 类的 init 方法，而是讲起了日志的编解码器。这是因为在 LogManagerImpl 初始化的过程中，用到了日志编解码器，而编解码器都是由 LogEntryV2CodecFactory 工厂创建的，但是 LogEntryV2CodecFactory 工厂是由 DefaultJRaftServiceFactory 服务工厂创建的，具体实现请看下面代码块。
```java
//默认的JRaft服务工厂类，这个类上有SPI注解，在程序启动的时候，显然是要被SPI机制加载到内存中的
@SPI
public class DefaultJRaftServiceFactory implements JRaftServiceFactory {

    public static DefaultJRaftServiceFactory newInstance() {
        return new DefaultJRaftServiceFactory();
    }


    //创建日志存储器，实际上创建的就是一个RocksDBLogStorage对象
    //这个RocksDBLogStorage对象封装了RocksDB数据库，jraft框架运行过程中
    //产生的所有日志都会存放在这个RocksDB数据库中，这个数据库是C++开发的
    @Override
    public LogStorage createLogStorage(final String uri, final RaftOptions raftOptions) {
        Requires.requireTrue(StringUtils.isNotBlank(uri), "Blank log storage uri.");
        //在这里创建了日志存储器
        return new RocksDBLogStorage(uri, raftOptions);
    }

    //创建编解码工厂
    @Override
    public LogEntryCodecFactory createLogEntryCodecFactory() {
        return LogEntryV2CodecFactory.getInstance();
    }

}
```
而我们在 NodeImpl 类的 initLogStorage 方法中已经使用 DefaultJRaftServiceFactory 服务工厂创建了日志存储器，也就是 RocksDBLogStorage 对象，接下来其实就应该紧接着把编解码器工厂创建出来，交给 LogManagerImpl，让它在初始化的时候能够使用。所以，NodeImpl 类的 initLogStorage 方法的部分代码应该重构成下面这样。请看下面代码块。
```java
public class NodeImpl implements Node, RaftServerService {


    //日志管理组件
    private LogManager logManager;

    //日志存储组件，这个接口的实现类就是RocksDBLogStorage对象
    private LogStorage logStorage;

    //为jraft框架提供各种服务的工厂，在第一版本中，这个工厂只提供了元数据存储器服务
    //在第二版本代码中，它就要再次发挥作用了
    private JRaftServiceFactory serviceFactory;


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
        
        //使用服务工厂创建日志存储器
        this.logStorage = this.serviceFactory.createLogStorage(this.options.getLogUri(), this.raftOptions);
        
        //创建日志管理器，日志管理器内部持有者日志存储器
        this.logManager = new LogManagerImpl();
        
        //创建封装日志管理器需要参数的对象
        final LogManagerOptions opts = new LogManagerOptions();
        
        //下面都是设置一些信息的操作了，设置集群Id
        opts.setGroupId(this.groupId);
        //设置日志编解码工厂
        opts.setLogEntryCodecFactory(this.serviceFactory.createLogEntryCodecFactory());
        //设置日志存储器
        opts.setLogStorage(this.logStorage);
        //设置集群配置管理器
        opts.setConfigurationManager(this.configManager);
        opts.setRaftOptions(this.raftOptions);
        //在这里使用opts对象初始化日志管理器
        return this.logManager.init(opts);
    }


    //省略其他内容
}
```
好了，编解码器讲完之后，本章的核心内容其实已经讲完了，剩下的日志组件初始化的流程都比较常规。我为大家简单讲讲即可。当程序在上面代码块的 initLogStorage 方法中执行了 logManager.init(opts) 代码时，程序就会来到 LogManagerImpl 类中，执行 LogManagerImpl 对象的 init 方法。而在 LogManagerImpl 对象的 init 方法中，所做的操作很简单，**无非就是对 LogManagerImpl 类中的成员变量赋值，然后初始化日志存储器即可，也就是调用 RocksDBLogStorage 对象的 init 方法**。具体实现请看下面代码块。
```java
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
        //上锁
        this.writeLock.lock();
        try {//对日志存储器进行判空操作
            if (opts.getLogStorage() == null) {
                LOG.error("Fail to init log manager, log storage is null");
                return false;
            }//给集群Id赋值
            this.groupId = opts.getGroupId();
            this.raftOptions = opts.getRaftOptions();
            //为日志存储器赋值
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
            //这里就是从日志存储器中得到第一条日志索引的操作
            this.firstLogIndex = this.logStorage.getFirstLogIndex();
            //从日志存储器中得到最后一条日志索引的操作
            this.lastLogIndex = this.logStorage.getLastLogIndex();
            //从硬盘中得到最后一条日志的ID，getTermFromLogStorage方法是根据日志的索引得到相对应的任期
            this.diskId = new LogId(this.lastLogIndex, getTermFromLogStorage(this.lastLogIndex));
        } finally {
            this.writeLock.unlock();
        }
        return true;
    }
    

    //其他方法省略，在第二版本代码中，LogManagerImpl类中的所有方法的实现逻辑都非常简单
    //第二版本代码中都有详细注释，大家可以自己看看代码和注释
    
}
```
接下来，程序的执行流程就来到了日志存储器的 init 方法中，也就是 RocksDBLogStorage 类的 init 方法中。在这个方法中做了很多操作。这些操作还是很有必要为大家梳理一下的。要想正确认识 RocksDBLogStorage 类 init 方法中的诸多操作，仍然要先讲解一些前置知识，或者说对现有的 RocksDBLogStorage 类进行一些扩充。首先请大家看看上一章的 RocksDBLogStorage 类的简单实现，请看下面代码块。
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
在上面的代码块中，RocksDBLogStorage 类中只有几个简单的成员变量，其中最重要的就是 RocksDB 类型的 db 成员变量，这个就是用来存储数据的 RocksDB 数据库本身。但是仅仅有这一个成员变量，还不足以把日志存放到 RocksDB 数据库中，这就要从 RocksDB 数据库存储数据的方式开始讲起了。当我们使用 Mysql 数据库的时候，将数据存放到数据库时，肯定需要明确这些数据要存放到哪张表中，向 RocksDB 数据库中存放数据也是同样的道理，虽然是以键值对的方式存储数据，但也要知道存放的数据究竟存到哪张表中了。**而在 RocksDB 数据库中，一张表其实就可以用 ColumnFamilyHandle 对象来表示。如果两个数据要分别存放到两张表里，就可以创建两个 ColumnFamilyHandle 对象，存放不同的数据**。<br />在我们开发的框架中，日志对象其实有两种类型，**一种是业务日志，也就是客户端指令被包装后产生的日志，一种是配置变更日志**。**为了区分这两种日志，在 RocksDBLogStorage 类中还额外定义了两个新的成员变量，这两个成员变量都是 ColumnFamilyHandle 类型，分别为 defaultHandle 成员变量和 confHandle 成员变量。其中 confHandle 就是用来存放配置变更日志的，而 defaultHandle 成员变量就是用来存放业务日志的**。当然，除了这两个重要的成员变量，另外还有一些成员变量也需要定义在 RocksDBLogStorage 类中，这些成员变量都和 RocksDB 数据库有关，封装着数据库相关操作的配置参数。除此之外，还有我之前为大家引入的编解码器。接下来，我就把重构好的 RocksDBLogStorage 类展示给大家，请看下面代码块。
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

    //集群Id
    private String groupId;
    
    //日志要存放的本地路径
    private final String path;
    
    //写操作后是否要立即进行持久化，所谓立即不就是同步的意思吗？
    //这个参数会在日志入库的时候使用，sync的默认值为true
    //当日志存入到RocksDB数据库后，会直接同步持久化到本地
    //并不会异步持久化
    private final boolean sync;
    
    //RocksDB数据库对象，存储日志到数据库或者是从数据库获取日志，都是通过这个对象来进行的
    private RocksDB db;
    
    //在RocksDB对象的时候，需要一些配置参数，这个成员变量就封装了配置参数的信息
    private DBOptions dbOptions;
    
    //该成员变量封装了向数据库写入数据时需要的配置参数
    private WriteOptions writeOptions;
    
    //ColumnFamilyOptions对象中封装的是列族需要的配置参数
    private final List<ColumnFamilyOptions> cfOptions = new ArrayList<>();
    
    //列族暴露给用户的句柄
    private ColumnFamilyHandle defaultHandle;
    
    //这个也是列族暴露给用户的句柄
    //好了，现在可以来给大家简单聊聊列族是什么了。在RocksDB数据库中，是以列族来存放数据的
    //一个列族对应一个单独的文件，类比一下，一个列族就相当于一张表
    //在jraft集群中，有集群变更产生的日志条目，也有业务产生的日志条目
    //这两种日志条目各自对应一个列族。配置变更的日志条目就存放在这个confHandle句柄对应的列族中
    //而业务日志条目就存放在上面defaultHandle对应的列族文件中
    //通过defaultHandle和confHandle，向数据库中存放数据
    private ColumnFamilyHandle confHandle;
    
    //该成员变量封装了从数据库读取数据时需要的配置参数
    private ReadOptions totalOrderReadOptions;
    
    //读写锁
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = this.readWriteLock.readLock();
    private final Lock writeLock = this.readWriteLock.writeLock();
    
    //第一条日志索引默认为1
    private volatile long firstLogIndex = 1;
    
    //是否加载了第一条日志索引，这里指的是从硬盘加载第一条日志索引
    private volatile boolean hasLoadFirstLogIndex;
    
    //日志编码器
    private LogEntryEncoder logEntryEncoder;
    
    //日志解码器
    private LogEntryDecoder logEntryDecoder;


    @Override
    public boolean init(final LogStorageOptions opts) {

        //该方法暂时不实现
    }


    //其他方法省略
}
```
好了，现在 RocksDBLogStorage 类的成员变量已经重构完整了，我知道大家现在对其中某些成员变量还很困惑，不知道它们的作用。这很正常，因为我当初看源码的时候也是看了后面的逻辑，也就是 RocksDBLogStorage 类的 init 方法的实现逻辑之后，才完全清楚了那些成员变量的作用。所以，接下来我会为大家分析一下 RocksDBLogStorage 类的 init 方法的实现过程。其实要在 init 方法中执行的操作很简单，虽然代码看起来很多，但是逻辑真的很简单，**无非就是建立对 RocksDB 数据库的连接，以便通过 RocksDB 数据库对象直接操纵数据库，向里面存放数据，当然，也需要给 RocksDBLogStorage 类中的各个成员变量赋值。最后，还有一条最重要的逻辑，那就是从本地加载第一条日志的索引，将本地的配置变更日志加载到内存中。因为如果当前节点是故障重启，并不是第一次启动的话，那么在本地肯定有很多数据，这时候肯定是需要把本地的数据加载到内存中，使节点尽快恢复到宕机之前的状态**。<br />RocksDBLogStorage 类的 init 方法我也早就为大家实现了，接下来就请大家看看下面的代码块。
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

    //集群Id
    private String groupId;
    
    //日志要存放的本地路径
    private final String path;
    
    //写操作后是否要立即进行持久化，所谓立即不就是同步的意思吗？
    //这个参数会在日志入库的时候使用，sync的默认值为true
    //当日志存入到RocksDB数据库后，会直接同步持久化到本地
    //并不会异步持久化
    private final boolean sync;
    
    //RocksDB数据库对象，存储日志到数据库或者是从数据库获取日志，都是通过这个对象来进行的
    private RocksDB db;
    
    //在RocksDB对象的时候，需要一些配置参数，这个成员变量就封装了配置参数的信息
    private DBOptions dbOptions;
    
    //该成员变量封装了向数据库写入数据时需要的配置参数
    private WriteOptions writeOptions;
    
    //ColumnFamilyOptions对象中封装的是列族需要的配置参数
    private final List<ColumnFamilyOptions> cfOptions = new ArrayList<>();
    
    //列族暴露给用户的句柄
    private ColumnFamilyHandle defaultHandle;
    
    //这个也是列族暴露给用户的句柄
    //好了，现在可以来给大家简单聊聊列族是什么了。在RocksDB数据库中，是以列族来存放数据的
    //一个列族对应一个单独的文件，类比一下，一个列族就相当于一张表
    //在jraft集群中，有集群变更产生的日志条目，也有业务产生的日志条目
    //这两种日志条目各自对应一个列族。配置变更的日志条目就存放在这个confHandle句柄对应的列族中
    //而业务日志条目就存放在上面defaultHandle对应的列族文件中
    //通过defaultHandle和confHandle，向数据库中存放数据
    private ColumnFamilyHandle confHandle;
    
    //该成员变量封装了从数据库读取数据时需要的配置参数
    private ReadOptions totalOrderReadOptions;
    
    //读写锁
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = this.readWriteLock.readLock();
    private final Lock writeLock = this.readWriteLock.writeLock();
    
    //第一条日志索引默认为1
    private volatile long firstLogIndex = 1;
    
    //是否加载了第一条日志索引，这里指的是从硬盘加载第一条日志索引
    private volatile boolean hasLoadFirstLogIndex;
    
    //日志编码器
    private LogEntryEncoder logEntryEncoder;
    
    //日志解码器
    private LogEntryDecoder logEntryDecoder;


    //初始化方法，在这个方法中数据库就被创建了，并且如果集群中已经有日志落盘了
    //还会从硬盘中加载第一条日志的索引，赋值给本类的成员变量
    @Override
    public boolean init(final LogStorageOptions opts) {
        Requires.requireNonNull(opts.getConfigurationManager(), "Null conf manager");
        Requires.requireNonNull(opts.getLogEntryCodecFactory(), "Null log entry codec factory");
        this.groupId = opts.getGroupId();
        //上锁
        this.writeLock.lock();
        try {//判断是否初始化过了
            if (this.db != null) {
                LOG.warn("RocksDBLogStorage init() in {} already.", this.path);
                return true;
            }   
            
            //得到日志解码器
            this.logEntryDecoder = opts.getLogEntryCodecFactory().decoder();
            
            //得到日志编码器
            this.logEntryEncoder = opts.getLogEntryCodecFactory().encoder();
            Requires.requireNonNull(this.logEntryDecoder, "Null log entry decoder");
            Requires.requireNonNull(this.logEntryEncoder, "Null log entry encoder");
            
            //得到数据库配置参数对象
            this.dbOptions = createDBOptions();
            
            //得到写数据配置参数对象
            this.writeOptions = new WriteOptions();
            
            //这里设置同步写，默认是同步的，也就是当有数据要写入到数据库时
            //每次写操作都会立刻把数据同步刷新到硬盘，而不是先写到内存，再异步刷盘
            this.writeOptions.setSync(this.sync);
            
            //得到读取数据时的配置参数对象
            this.totalOrderReadOptions = new ReadOptions();
            
            //这里设置为true，意味着在读取数据时，是按照键的顺序来读取数据的
            //因为jraft集群中，日志以键值对的方式存放到数据库中，而键是每一条日志的索引
            //这个索引又是递增的，所以在读取数据的时候就采用了这种策略
            this.totalOrderReadOptions.setTotalOrderSeek(true);
            
            //初始化数据库并且加载配置日志条目到配置管理器中
            return initAndLoad(opts.getConfigurationManager());
        } catch (final RocksDBException e) {
            LOG.error("Fail to init RocksDBLogStorage, path={}.", this.path, e);
            return false;
        } finally {
            this.writeLock.unlock();
        }
    }


     //创建数据库需要的配置参数对象
    public static DBOptions createDBOptions() {
        //如果大家感兴趣，可以点进去StorageOptionsFactory类中，去该类的getDefaultRocksDBOptions方法中
        //看看为RocksDB数据库设置的默认参数，里面有一些英文注释，可以翻译了看一看，和列族，日志，打开文件的数量，并行刷新数据到硬盘的最大线程数等等有关
        return StorageOptionsFactory.getRocksDBOptions(RocksDBLogStorage.class);
    }


    
    //创建数据库的方法
    private boolean initAndLoad(final ConfigurationManager confManager) throws RocksDBException {
        //设置该成员变量为false，意味着还没有从硬盘加载第一条日志的索引
        //因为这时候集群刚刚启动，数据库还没有初始化呢
        this.hasLoadFirstLogIndex = false;
        
        //默认的第一条日志的索引为1
        this.firstLogIndex = 1;
        
        final List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
        //得到列族的配置参数对象
        final ColumnFamilyOptions cfOption = createColumnFamilyOptions();
        this.cfOptions.add(cfOption);
        
        //这里创建了专门存放配置变更日志条目的列族描述符
        columnFamilyDescriptors.add(new ColumnFamilyDescriptor("Configuration".getBytes(), cfOption));
        //这里创建的是专门用来存放业务日志条目的列族描述符，然后把这两个描述符都放进了上面创建的集合中
        columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOption));
        
        //下面这个方法就会打开数据库，然后返回给用户操作数据库的句柄
        openDB(columnFamilyDescriptors);
        //加载数据到配置管理器中
        
        load(confManager);
        
        //下面这个方法中返回true，表示数据库初始化和数据已经加载完成了
        return true;
    }
    

    
    //创建ColumnFamilyOptions对象，这个配置参数的默认设置可以去StorageOptionsFactory类的getDefaultRocksDBColumnFamilyOptions方法中查看
    public static ColumnFamilyOptions createColumnFamilyOptions() {
        final BlockBasedTableConfig tConfig = StorageOptionsFactory
                .getRocksDBTableFormatConfig(RocksDBLogStorage.class);
        return StorageOptionsFactory.getRocksDBColumnFamilyOptions(RocksDBLogStorage.class)
                .useFixedLengthPrefixExtractor(8)
                .setTableFormatConfig(tConfig)
                .setMergeOperator(new StringAppendOperator());
    }


    
    //打开数据库的方法
    private void openDB(final List<ColumnFamilyDescriptor> columnFamilyDescriptors) throws RocksDBException {
        //这里定义的这个集合，是用来存放数据库返回给用户的，可以操作具体列族的句柄
        final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
        
        //根据用户配置的路径，创建文件，向数据库中存放的数据都会存放到这个文件中，在我为大家提供的测试类中
        //我的数据最终存放在了jraft-log/serverx/log中了
        final File dir = new File(this.path);
        
        //判断路径，校验是否为目录
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalStateException("Invalid log path, it's a regular file: " + this.path);
        }
        
        //打开数据库，返回操作数据库的句柄
        this.db = RocksDB.open(this.dbOptions, this.path, columnFamilyDescriptors, columnFamilyHandles);
        
        //校验columnFamilyHandles是不是存放了两个列族句柄，因为我们定义的就是配置日志列族和业务日志列族
        //所以当然也应该返回这两个句柄
        assert (columnFamilyHandles.size() == 2);
        
        //获得操纵配置日志列族的句柄
        this.confHandle = columnFamilyHandles.get(0);
        
        //获得操纵业务日志列族的句柄
        this.defaultHandle = columnFamilyHandles.get(1);
    }



    //从数据库中加载数据到配置管理，这个方法的作用是让当前节点快速获得集群中的所有配置
    //我在该方法中略去了很多逻辑，在第二版本代码中逻辑非常详细，只展示了日志解码器的使用方法
    //以及日志索引编码后长度为8个字节的具体体现。其他的逻辑大家直接去第二版本代码中查看即可
    //逻辑非常简单，而且注释比文章中更加详细，大家肯定可以看明白
    private void load(final ConfigurationManager confManager) {
        //校验数据库是否已经打开，就是判断成员变量db不为null
        checkState();
        //下面就是具体操作confHandle列族的方法，可以看到，向方法中穿进去了要操纵的列族的句柄，以及读取数据的配置参数
        //然后返回给用户一个迭代器对象，通过这个对象可以遍历存放在confHandle列族中的所有配置日志条目
        try (final RocksIterator it = this.db.newIterator(this.confHandle, this.totalOrderReadOptions)) {
            //迭代器定位到第一个日志条目
            it.seekToFirst();
            //开始遍历
            while (it.isValid()) {
                //下面就是得到日志的键值对了
                //得到键，键就是日志的索引
                final byte[] ks = it.key();
                //得到value，value就是日志本身
                final byte[] bs = it.value();
                //判断键的长度，按照sofajraft中的设定，写入数据库的日志的键，其键的长度为8个字节
                //这一点可以在saveFirstLogIndex方法中查看
                if (ks.length == 8) {
                    //走到这里，说明当前遍历的就是一个日志条目，使用日志解码器解码
                    //得到日志条目对象
                    final LogEntry entry = this.logEntryDecoder.decode(bs);
                    //判空
                    if (entry != null) {
                        //这里会再校验一下日志条目对象的类型，看看是不是配置变更日志类型的
                        if (entry.getType() == EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                           
                        }
                        //省略此处的逻辑
                    } 
                }
                
                //省略此处的逻辑，这里就是给当前节点第一条日志索引赋值的操作
                
                //获取下一个日志条目，进入下一次循环
                it.next();
            }
        }
    }

    

    //其他方法省略
}
```
好了朋友们，上面的代码块展示完毕之后，这一章的内容也就结束了。可以看到，最后一个代码块的内容非常多，我也没有多么仔细的分析。请允许我解释一下，在上面代码块中虽然出现了很多方法，但是这些方法的逻辑都很常规，就相当于 1+1 一定等于 2 的那种逻辑，只要顺着方法一直往下看，肯定可以看懂，所以我就不做过多解释了，况且代码中的注释已经非常详细了。当然，在这一章里，不管是 LogManagerImpl 类还是 RocksDBLogStorage 类，这两个类中都有一些方法还没有为大家实现。在第二版本代码中，这些方法都加上了非常详细的注释，实现得也都非常简单。所以，大家也可以直接去代码中查看。可能有的朋友也注意到了，本章我已经为大家引入了日志编码器，但是这个编码器并没有被我们真正使用。这不要紧，下一章我将为大家实现日志落盘，在这个过程中就会用到日志编码器了。这些知识就留到下一章为大家讲解吧，好了朋友们，我们下一章见
