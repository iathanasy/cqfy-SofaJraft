package com.alipay.sofa.jraft.storage.impl;

import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.conf.ConfigurationEntry;
import com.alipay.sofa.jraft.conf.ConfigurationManager;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.LogId;
import com.alipay.sofa.jraft.entity.codec.LogEntryDecoder;
import com.alipay.sofa.jraft.entity.codec.LogEntryEncoder;
import com.alipay.sofa.jraft.option.LogStorageOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.LogStorage;
import com.alipay.sofa.jraft.util.*;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/1
 * @Description:日志存储器，存储日志的所有功能都是由这个类提供的，数据库存储日志是以键值对的形式存放发
 * 键就是日志索引，值就是日志条目本身
 */
public class RocksDBLogStorage implements LogStorage, Describer {

    private static final Logger LOG = LoggerFactory.getLogger(RocksDBLogStorage.class);

    static {
        //加载RocksDB依赖的jar包，其实就是加载一些C++代码文件
        //因为RocksDB是C++编写的，加载了库文件后，就可以调用C++的代码了
        RocksDB.loadLibrary();
    }

    private String groupId;
    //日志要存放的本地路径
    private final String path;
    //写操作后是否要立即进行持久化，所谓立即不就是同步的意思吗？
    //这个参数会在日志入库的时候使用
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

    //构造方法
    public RocksDBLogStorage(final String path, final RaftOptions raftOptions) {
        super();
        this.path = path;
        this.sync = raftOptions.isSync();
    }



    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:写操作的批处理器
     */
    private interface WriteBatchTemplate {

        void execute(WriteBatch batch) throws RocksDBException, IOException, InterruptedException;
    }



    //创建数据库需要的配置参数对象
    public static DBOptions createDBOptions() {
        //如果大家感兴趣，可以点进去StorageOptionsFactory类中，去该类的getDefaultRocksDBOptions方法中
        //看看为RocksDB数据库设置的默认参数，里面有一些英文注释，可以翻译了看一看，和列族，日志，打开文件的数量，并行刷新数据到硬盘的最大线程数等等有关
        return StorageOptionsFactory.getRocksDBOptions(RocksDBLogStorage.class);
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

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:初始化方法，在这个方法中数据库就被创建了，并且如果集群中已经有日志落盘了
     * 还会从硬盘中加载第一条日志的索引，赋值给本类的成员变量
     */
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
            }   //得到日志解码器
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


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:创建数据库的方法
     */
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
        return onInitLoaded();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:打开数据库的方法
     */
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


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:该成员变量就是第一条日志的键，实际上是这样的，当集群启动时会从硬盘中把第一条日志的信息读取出来
     * 获得第一条日志的索引，然后把这个索引当作value，把下面这个成员变量的值，也就是meta/firstLogIndex当作key
     * 把这个键值对存放到confHandle这个列族中，这样集群每次启动的时候，就可以直接去这个列族中得到第一条日志的索引
     * 而不必再去defaultHandle列族中获得了，因为业务日志本身是存放在defaultHandle列族中的，第一条日志的索引会被用于校验日志索引一致性
     * 所以必须获得第一条日志的索引，但是集群启动的时候，默认只去confHandle列族中加载数据，会把confHandle列族中的配置条目
     * 加载到配置管理器中，如果是这样，还要再接着去defaultHandle列族中加载第一条日志的索引。所以，这里就做了一个操作，就是在集群启动的时候把第一条日志的索引存放到confHandle列族中
     * 这样系统崩溃的时候，重新恢复节点运行，就可以直接从confHandle列族中获得第一条日志的索引和所有的配置信息了
     * 注意，当引入快照之后，第一条日志索引会随着快照生成而不断变化，也会不断被修改，这一点之后再将
     */
    public static final byte[] FIRST_LOG_IDX_KEY = Utils.getBytes("meta/firstLogIndex");

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:从数据库中加载数据到配置管理，这个方法的作用是让当前节点快速获得集群中的所有配置
     */
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
                //判断键的长度，按照sofajraft中的设定，写入数据库的日志的键制度，其键的长度为8个字节
                //这一点可以在saveFirstLogIndex方法中查看
                if (ks.length == 8) {
                    //走到这里，说明当前遍历的就是一个日志条目，使用日志解码器解码
                    //得到日志条目对象
                    final LogEntry entry = this.logEntryDecoder.decode(bs);
                    //判空
                    if (entry != null) {
                        //这里会再校验一下日志条目对象的类型，看看是不是配置变更日志类型的
                        if (entry.getType() == EnumOutter.EntryType.ENTRY_TYPE_CONFIGURATION) {
                            //如果是的话，接下来就要把日志信息填充到ConfigurationEntry对象中
                            //下面就是一些具体填充信息的操作，不再一一解释了
                            final ConfigurationEntry confEntry = new ConfigurationEntry();
                            confEntry.setId(new LogId(entry.getId().getIndex(), entry.getId().getTerm()));
                            confEntry.setConf(new Configuration(entry.getPeers(), entry.getLearners()));
                            if (entry.getOldPeers() != null) {
                                confEntry.setOldConf(new Configuration(entry.getOldPeers(), entry.getOldLearners()));
                            }
                            //走到这里再判断一下配置管理器是否不为空
                            if (confManager != null) {
                                //如果不为空，就把配置日志条目添加到配置管理器中
                                confManager.add(confEntry);
                            }
                        }
                    } else {
                        //走到这里意味着解码失败
                        LOG.warn("Fail to decode conf entry at index {}, the log data is: {}.", Bits.getLong(ks, 0),
                                BytesUtil.toHex(bs));
                    }
                } else {
                    //走到这里意味着从列族中获得的数据的键的长度并不是8个字节，如果不是8个字节，那就是刚才的FIRST_LOG_IDX_KEY成员变量对应的值了
                    //这样的话，这里得到的就是第一条日志索引的数据，得到的value就是具体的索引值
                    //这里大家可能会有些疑惑，如果是第一次启动节点，根本就不会执行到这里，因为还没有把第一条日志的索引设置到confHandle列族中
                    //确实是这样的，所以在LogManagerImpl类的init方法中，在执行了this.logStorage.init(lsOpts)这行代码后，也就是日志存储器初始化好后
                    //会紧接着执行this.firstLogIndex = this.logStorage.getFirstLogIndex()这个方法，在这个方法会从数据库的defaultHandle列族中
                    //获得第一条日志，然后获得索引，这时候，会将第一条日志的索引存放到confHandle列族中，之后启动节点时，就可以直接从confHandle中获得第一条日志索引了
                    //但是这里也有一个小问题，那就是第一次启动节点的时候，defaultHandle列族中也是没有数据的，所以仍然无法从defaultHandle中
                    //获得第一条日志索引，只有当程序运行了一会，有日志写到数据库中了，快照也生成了，这时候，才会把第一条日志的索引设置到confHandle列族中
                    //如果没有生成快照节点就下线了，当节点回复运行的时候，还是需要去defaultHandle列族中加载第一条日志索引的
                    //现在将这些逻辑还有点早，等后面引入快照后再理解这里就会容易很多
                    //当然，要说围绕着第一条日志的索引搞得这一对操作具体有什么作用，其实就是为了不必在节点每次启动的时候都去硬盘加载数据
                    //稍微提高一点点性能而已
                    if (Arrays.equals(FIRST_LOG_IDX_KEY, ks)) {
                        //得到具体的索引值，然后赋值给firstLogIndex成员变量
                        setFirstLogIndex(Bits.getLong(bs, 0));
                        //这里这个方法暂时还不用关注，因为还没有引入日志快照，加入有日志快照之后再剖析这个方法
                        //truncatePrefixInBackground(0L, this.firstLogIndex);
                    } else {
                        //走到这里说明是未知的数据
                        LOG.warn("Unknown entry in configuration storage key={}, value={}.", BytesUtil.toHex(ks),
                                BytesUtil.toHex(bs));
                    }
                }
                //获取下一个日志条目，进入下一次循环
                it.next();
            }
        }
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:将一批日志存储到数据库中的方法
     */
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


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:给第一条日志索引赋值
     */
    private void setFirstLogIndex(final long index) {
        this.firstLogIndex = index;
        this.hasLoadFirstLogIndex = true;
    }


    private void checkState() {
        Requires.requireNonNull(this.db, "DB not initialized or destroyed");
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:把数据批量写入数据库的方法
     */
    private boolean executeBatch(final WriteBatchTemplate template) {
        this.readLock.lock();
        if (this.db == null) {
            LOG.warn("DB not initialized or destroyed in data path: {}.", this.path);
            this.readLock.unlock();
            return false;
        }//创建WriteBatch对象，该对象可以封装一批等待写入数据库中的数据，是专门协助批处理的写操作的
        try (final WriteBatch batch = new WriteBatch()) {
            //在appendEntries的executeBatch方法中，已经把要刷新到数据库中的数据全放到WriteBatch对象中了
            template.execute(batch);
            //在这里就把WriteBatch对象中的数据批量写入到数据库中，也就持久化成功了
            //注意，这里是同步写，因为writeOptions对象中设置了同步写的配置参数
            this.db.write(this.writeOptions, batch);
        } catch (final RocksDBException e) {
            LOG.error("Execute batch failed with rocksdb exception.", e);
            return false;
        } catch (final IOException e) {
            LOG.error("Execute batch failed with io exception.", e);
            return false;
        } catch (final InterruptedException e) {
            LOG.error("Execute batch failed with interrupt.", e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            this.readLock.unlock();
        }
        return true;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:把日志添加到confHandle列族中的方法
     */
    private void addConfBatch(final LogEntry entry, final WriteBatch batch) throws RocksDBException {
        //得到日志的key
        final byte[] ks = getKeyBytes(entry.getId().getIndex());
        //得到value
        final byte[] content = this.logEntryEncoder.encode(entry);
        //这里我还要再解释一下，可能有些朋友觉得困惑，为什么明明是存放配置变更日志
        //却需要把这个日志也向业务日志的列族中存放一份，学完第三个版本代码之后
        //我相信大家都会有这种意识，那就是在jraft集群中，无论是配置变更日志还是业务日志，它们的索引是共享的
        //并不是给这两种类别的日志单独分配一套索引系统，可能索引为1的日志是业务日志，紧接着索引为2的日志就是配置变更日志了
        //如果是这样的话，我们把这两种日志分开存放，那么将来某个时刻，节点需要根据索引获得某条日志，在某个列族中找不到了还要去另一个列族中寻找
        //或者说需要获取全部日志了，还要去两个列族中获取，然后再合并，但是合并的过程中也许索引就被打乱了，日志就不是按照索引顺序合并的
        //所以，最好的方法是配置变更的日志存放两份，一份单独存放，另一份就按顺序和业务日志存放到一起，如果在知道查找的这条日志是配置变更日志
        //就直接去confHandle中查找即可，否则就直接去defaultHandle列族中查找，这样还能提高查找效率
        batch.put(this.defaultHandle, ks, content);
        batch.put(this.confHandle, ks, content);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/4
     * @Description:把日志添加到defaultHandle列族中的方法
     */
    private void addDataBatch(final LogEntry entry, final WriteBatch batch) throws RocksDBException, IOException, InterruptedException {
        final long logIndex = entry.getId().getIndex();
        final byte[] content = this.logEntryEncoder.encode(entry);
        batch.put(this.defaultHandle, getKeyBytes(logIndex),content);
    }




    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:将第一条日志索引保存到confHandle列族中
     */
    private boolean saveFirstLogIndex(final long firstLogIndex) {
        this.readLock.lock();
        try {//这里创建了长度为8的字节数组，是用来封装索引的值的
            final byte[] vs = new byte[8];
            Bits.putLong(vs, 0, firstLogIndex);
            checkState();
            //这里大家会发现，键值对中的键变成了FIRST_LOG_IDX_KEY，而value就是封装了索引的字节数组
            this.db.put(this.confHandle, this.writeOptions, FIRST_LOG_IDX_KEY, vs);
            return true;
        } catch (final RocksDBException e) {
            LOG.error("Fail to save first log index {} in {}.", firstLogIndex, this.path, e);
            return false;
        } finally {
            this.readLock.unlock();
        }
    }


    protected boolean onInitLoaded() {
        return true;
    }

//该方法暂时注释掉
//    private void truncatePrefixInBackground(final long startIndex, final long firstIndexKept) {
//        ThreadPoolsFactory.runInThread(this.groupId, () -> {
//            long startMs = Utils.monotonicMs();
//            this.readLock.lock();
//            try {
//                RocksDB db = this.db;
//                if (db == null) {
//                    LOG.warn(
//                            "DB is null while truncating prefixed logs in data path: {}, the range is: [{}, {})",
//                            this.path, startIndex, firstIndexKept);
//                    return;
//                }
//                final byte[] startKey = getKeyBytes(startIndex);
//                final byte[] endKey = getKeyBytes(firstIndexKept);
//                db.deleteRange(this.defaultHandle, startKey, endKey);
//                db.deleteRange(this.confHandle, startKey, endKey);
//                db.deleteFilesInRanges(this.defaultHandle, Arrays.asList(startKey, endKey), false);
//                db.deleteFilesInRanges(this.confHandle, Arrays.asList(startKey, endKey), false);
//            } catch (final RocksDBException e) {
//                LOG.error("Fail to truncatePrefix in data path: {}, firstIndexKept={}.", this.path, firstIndexKept, e);
//            } finally {
//                this.readLock.unlock();
//                LOG.info("Truncated prefix logs in data path: {} from log index {} to {}, cost {} ms.",
//                        this.path, startIndex, firstIndexKept, Utils.monotonicMs() - startMs);
//            }
//        });
//    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/13
     * @Description:从指定所以开始，移除该索引之后的日志
     */
    @Override
    public boolean truncateSuffix(final long lastIndexKept) {
        this.readLock.lock();
        try {
            try {
                //onTruncateSuffix(lastIndexKept);
            } finally {//这里使用的就是范围删除，删除索引在 getKeyBytes(lastIndexKept + 1)和 getKeyBytes(getLastLogIndex() + 1)之间
                //的所有日志，这里逻辑就很简单了，就是直接从重叠日志的起始位置到本地的最后一条日志，这个范围之间的日志全部删除，当然也包括最后一条日志
                this.db.deleteRange(this.defaultHandle, this.writeOptions, getKeyBytes(lastIndexKept + 1),
                        getKeyBytes(getLastLogIndex() + 1));
                this.db.deleteRange(this.confHandle, this.writeOptions, getKeyBytes(lastIndexKept + 1),
                        getKeyBytes(getLastLogIndex() + 1));
            }
            return true;
        } catch (final RocksDBException e) {
            LOG.error("Fail to truncateSuffix {} in data path: {}.", lastIndexKept, this.path, e);
        } finally {
            this.readLock.unlock();
        }
        return false;
    }


    //关闭数据库方法
    private void closeDB() {
        this.confHandle.close();
        this.defaultHandle.close();
        this.db.close();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:得到第一条日志的索引
     */
    @Override
    public long getFirstLogIndex() {
        this.readLock.lock();
        RocksIterator it = null;
        try {//判断第一条日志索引有没有被加载，如果已经加载过了，就直接返回
            if (this.hasLoadFirstLogIndex) {
                return this.firstLogIndex;
            }//检查数据库状态
            checkState();
            //返回迭代器对象
            it = this.db.newIterator(this.defaultHandle, this.totalOrderReadOptions);
            //定位第一条数据
            it.seekToFirst();
            //判断第一条数据是否有效
            if (it.isValid()) {
                //如果有效就得到第一条日志的索引
                final long ret = Bits.getLong(it.key(), 0);
                //把第一条日志的索引存放到confHandle列族中
                saveFirstLogIndex(ret);
                //给成员变量赋值，修改hasLoadFirstLogIndex状态
                setFirstLogIndex(ret);
                return ret;
            }//如果数据无效就返回1
            return 1L;
        } finally {
            if (it != null) {
                it.close();
            }
            this.readLock.unlock();
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:得到最后一条日志的索引，逻辑和上面方法饿逻辑类似，就不再重复了
     */
    @Override
    public long getLastLogIndex() {
        this.readLock.lock();
        checkState();
        try (final RocksIterator it = this.db.newIterator(this.defaultHandle, this.totalOrderReadOptions)) {
            it.seekToLast();
            if (it.isValid()) {
                return Bits.getLong(it.key(), 0);
            }
            return 0L;
        } finally {
            this.readLock.unlock();
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:根据索引获得指定日志条目的方法
     */
    @Override
    public LogEntry getEntry(final long index) {
        this.readLock.lock();
        try {//检验索引是否合规
            if (this.hasLoadFirstLogIndex && index < this.firstLogIndex) {
                return null;
            }//从数据库中获得对应日志
            return getEntryFromDB(index);
        } catch (final RocksDBException | IOException e) {
            LOG.error("Fail to get log entry at index {} in data path: {}.", index, this.path, e);
        } finally {
            this.readLock.unlock();
        }
        return null;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:根据索引从数据库中获得指定日志条目的方法
     */
    LogEntry getEntryFromDB(final long index) throws IOException, RocksDBException {
        //获得索引，把索引转换成8字节长度的字节数组，这就得到了key
        final byte[] keyBytes = getKeyBytes(index);
        //共数据库中获得value
        final byte[] bs =  getValueFromRocksDB(keyBytes);
        if (bs != null) {//将日志条目解码
            final LogEntry entry = this.logEntryDecoder.decode(bs);
            if (entry != null) {
                return entry;
            } else {
                LOG.error("Bad log entry format for index={}, the log data is: {}.", index, BytesUtil.toHex(bs));
                return null;
            }
        }
        return null;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/2
     * @Description:根据key从数据库中获得对应的value，方法中的字节数组其实就是key
     */
    protected byte[] getValueFromRocksDB(final byte[] keyBytes) throws RocksDBException {
        checkState();
        return this.db.get(this.defaultHandle, keyBytes);
    }

    //将索引变成字节数组的方法
    protected byte[] getKeyBytes(final long index) {
        final byte[] ks = new byte[8];
        Bits.putLong(ks, 0, index);
        return ks;
    }


    //根据日志索引获得对应任期的方法
    @Override
    public long getTerm(final long index) {
        final LogEntry entry = getEntry(index);
        if (entry != null) {
            return entry.getId().getTerm();
        }
        return 0;
    }

    @Override
    public void shutdown() {
        //暂时不做实现
    }

    @Override
    public void describe(final Printer out) {
        this.readLock.lock();
        try {
            if (this.db != null) {
                out.println(this.db.getProperty("rocksdb.stats"));
            }
            out.println("");
        } catch (final RocksDBException e) {
            out.println(e);
        } finally {
            this.readLock.unlock();
        }
    }
}
