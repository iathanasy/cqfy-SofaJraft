package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.SnapshotStorage;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.storage.snapshot.Snapshot;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/21
 * @方法描述：快照存储器
 */
public class LocalSnapshotStorage implements SnapshotStorage {


    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotStorage.class);

    //存放快照文件的临时路径
    private static final String TEMP_PATH = "temp";
    //快照引用计数表
    private final ConcurrentMap<Long, AtomicInteger> refMap = new ConcurrentHashMap<>();
    //用户定义的快照真正的存放路径
    private final String path;
    //当前节点的IP地址
    private Endpoint addr;
    //远程复制快照文件之前是否过滤
    private boolean filterBeforeCopyRemote;
    //快照文件记录的最后应用的日志的索引
    private long lastSnapshotIndex;
    private final Lock lock;
    private final RaftOptions raftOptions;
    //快照传输限流器
    private SnapshotThrottle snapshotThrottle;

    //设置快照传输限流器的方法
    @Override
    public void setSnapshotThrottle(SnapshotThrottle snapshotThrottle) {
        this.snapshotThrottle = snapshotThrottle;
    }

    //判断当前节点是否有快照服务器地址
    public boolean hasServerAddr() {
        return this.addr != null;
    }

    //设置快照服务器地址
    public void setServerAddr(Endpoint addr) {
        this.addr = addr;
    }

    //构造方法
    public LocalSnapshotStorage(String path, RaftOptions raftOptions) {
        super();
        this.path = path;
        //快照文件记录的最后日志索引初始值为0
        this.lastSnapshotIndex = 0;
        this.raftOptions = raftOptions;
        this.lock = new ReentrantLock();
    }

    //获得快照文件记录的最后应用的日志索引
    public long getLastSnapshotIndex() {
        this.lock.lock();
        try {
            return this.lastSnapshotIndex;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：初始化快照存储器的方法
     */
    @Override
    public boolean init(final Void v) {
        //根据用户配置的参数，创建存放快照文件的具体路径，在测试类中，我设置的路径就是/User/chenqingyang/code/jraft-log/server1
        //当然，这只是一个目录，在这个目录中会再次进行细分，存放日志文件夹叫做log，存放节点元数据的文件夹叫做raft_meta，存放快照的文件夹叫做snapshot
        final File dir = new File(this.path);
        try { //这里传进来的path就是/User/chenqingyang/code/jraft-log/server1/snapshot，所以就在这里创建了这个snapshot文件夹
            FileUtils.forceMkdir(dir);
        } catch (final IOException e) {
            LOG.error("Fail to create directory {}.", this.path, e);
            return false;
        }
        //下面是删除临时快照文件的操作，这里要解释一下，在每一次生成快照的时候，都会先把快照写入到一个临时文件夹中，临时文件夹就叫做temp
        if (!this.filterBeforeCopyRemote) {
            //得到临时快照的路径
            final String tempSnapshotPath = this.path + File.separator + TEMP_PATH;
            //得到临时快照文件夹
            final File tempFile = new File(tempSnapshotPath);
            //判断临时文件夹是否存在
            if (tempFile.exists()) {
                try {//如果存在就强制删除临时快照文件夹
                    FileUtils.forceDelete(tempFile);
                } catch (final IOException e) {
                    LOG.error("Fail to delete temp snapshot path {}.", tempSnapshotPath, e);
                    return false;
                }
            }
        }
        //除了删除临时快照文件，还会删除旧的快照文件，因为在程序运行期间，状态机会不断地应用日志，所以也会持续生成快照文件
        //在sofajraft框架中，每一个真正的快照文件名称为snapshot_X，这个X就是每一个快照文件记录的应用的最后一条日志的索引
        //如果第一个快照生成的时候，状态机应用到索引为31的日志了，那么第一个快照文件的名称就为snapshot_31，如果第二个快照文件生成的时候
        //状态机最新应用的日志是108，那么第二个快照名称就为snapshot_108，注意，这里的snapshot_X仍然为文件夹的名称，文件夹中才是真正的快照元数据文件和快照文件
                                                                        //snapshot_31/__raft_snapshot_meta(快照元数据文件)     data(快照文件)

        //就像这样：/User/chenqingyang/code/jraft-log/server1/snapshot/
                                                                        //snapshot_108/__raft_snapshot_meta(快照元数据文件)     data(快照文件)

        final List<Long> snapshots = new ArrayList<>();
        //查找/User/chenqingyang/code/jraft-log/server1/snapshot目录下的所有快照文件
        //这些都是旧的快照文件夹
        final File[] oldFiles = dir.listFiles();
        if (oldFiles != null) {
            for (final File sFile : oldFiles) {
                //遍历这些快照文件夹
                final String name = sFile.getName();
                //如果文件夹名称不是以snapshot_开头的，就说明不是快照文件夹，就跳过这个文件夹
                if (!name.startsWith(Snapshot.JRAFT_SNAPSHOT_PREFIX)) {
                    continue;
                }
                //得到每一个快照文件夹对应的索引，也就是snapshot_后面的那个数值
                final long index = Long.parseLong(name.substring(Snapshot.JRAFT_SNAPSHOT_PREFIX.length()));
                //把这些索引添加到上面创价的集合中
                snapshots.add(index);
            }
        }
        //刚才已经把所有快照的索引都添加到集合中了
        if (!snapshots.isEmpty()) {
            //给集合中的索引排序
            Collections.sort(snapshots);
            //得到集合的长度
            final int snapshotCount = snapshots.size();
            //接下来就是删除旧快照的操作，已经给快照的索引排好序了，所以只留下索引最大的快照文件夹就行了，索引最大的快照肯定就是最新的快照
            //其他的都是旧快照，都可以被删除了，这里减1是避免把最后一个索引数据删除了
            for (int i = 0; i < snapshotCount - 1; i++) {
                final long index = snapshots.get(i);
                //根据快照索引得到快照文件路径
                final String snapshotPath = getSnapshotPath(index);
                //在这里删除旧的快照文件夹
                if (!destroySnapshot(snapshotPath)) {
                    return false;
                }
            }
            //得到最新的快照记录的最后应用的日志的索引，主意啊，这个实在初始化方法中被赋值了，如果接下来程序内部还要生成快照，那么这个lastSnapshotIndex肯定就不是最新值了
            this.lastSnapshotIndex = snapshots.get(snapshotCount - 1);
            //对该索引的引用计数加1，这里有一个规则，就是引用计数为0的快照索引对应的快照文件夹可以被删除
            //但是只要快照索引对应的引用计数不为0，就不可以被删除
            ref(this.lastSnapshotIndex);
        }
        return true;
    }

    //根据索引得到快照文件夹全路径
    private String getSnapshotPath(final long index) {
        return this.path + File.separator + Snapshot.JRAFT_SNAPSHOT_PREFIX + index;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：给快照文件夹索引增加引用的方法
     */
    void ref(final long index) {
        final AtomicInteger refs = getRefs(index);
        refs.incrementAndGet();
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：删除快照文件夹的方法
     */
    private boolean destroySnapshot(final String path) {
        LOG.info("Deleting snapshot {}.", path);
        final File file = new File(path);
        try {
            FileUtils.deleteDirectory(file);
            return true;
        } catch (final IOException e) {
            LOG.error("Fail to destroy snapshot {}.", path, e);
            return false;
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：减少快照索引的引用计数
     */
    void unref(final long index) {
        final AtomicInteger refs = getRefs(index);
        if (refs.decrementAndGet() == 0) {
            //这里会判断一下，如果引用计数为0了
            if (this.refMap.remove(index, refs)) {
                //就可以在这里把对应的快照文件删除了
                destroySnapshot(getSnapshotPath(index));
            }
        }
    }

    //得到快照文件索引的引用计数
    AtomicInteger getRefs(final long index) {
        AtomicInteger refs = this.refMap.get(index);
        if (refs == null) {
            refs = new AtomicInteger(0);
            final AtomicInteger eRefs = this.refMap.putIfAbsent(index, refs);
            if (eRefs != null) {
                refs = eRefs;
            }
        }
        return refs;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：关闭快照存储器的方法，这个方法非常重要，该方法会被LocalSnapshotWriter调用，而LocalSnapshotWriter的close方法会在快照生成之后被调用
     * 在这个方法中，会把快照临时快照文件移动到正式的快照文件中
     */
    void close(final LocalSnapshotWriter writer, final boolean keepDataOnError) throws IOException {
        //得到快照写入器最后的执行状态码
        int ret = writer.getCode();
        IOException ioe = null;
        do {//状态码为0时就代表成功，只要不为0，就代表出错了，直接退出循环即可
            if (ret != 0) {
                break;
            }
            try {//在这里把本次生成快照的元数据写入到硬盘文件中，这里是同步写入
                if (!writer.sync()) {
                    ret = RaftError.EIO.getNumber();
                    break;
                }
            } catch (final IOException e) {
                LOG.error("Fail to sync writer {}.", writer.getPath(), e);
                ret = RaftError.EIO.getNumber();
                ioe = e;
                break;
            }//得到之前记录的旧的快照文件夹的日志索引
            final long oldIndex = getLastSnapshotIndex();
            //得到本次生成的快照记录的最新应用日志索引
            final long newIndex = writer.getSnapshotIndex();
            //既然是新快照生成了，肯定应用的最新日志索引不同了，如果相同则记录错误码
            if (oldIndex == newIndex) {
                ret = RaftError.EEXISTS.getNumber();
                break;
            }
            //现在得到的就是临时快照目录
            final String tempPath = this.path + File.separator + TEMP_PATH;
            //这里得到的就是正式的快照文件夹路径
            final String newPath = getSnapshotPath(newIndex);
            //先把正是路径的文件预判性删除一下
            if (!destroySnapshot(newPath)) {
                LOG.warn("Delete new snapshot path failed, path is {}.", newPath);
                ret = RaftError.EIO.getNumber();
                ioe = new IOException("Fail to delete new snapshot path: " + newPath);
                break;
            }
            LOG.info("Renaming {} to {}.", tempPath, newPath);
            //在这里把快照从临时路径移动到正式文件夹中，这里也是同步移动的
            if (!Utils.atomicMoveFile(new File(tempPath), new File(newPath), true)) {
                LOG.error("Renamed temp snapshot failed, from path {} to path {}.", tempPath, newPath);
                ret = RaftError.EIO.getNumber();
                ioe = new IOException("Fail to rename temp snapshot from: " + tempPath + " to: " + newPath);
                break;
            }
            //给最新的快照记录的应用日志的索引增加引用
            ref(newIndex);
            //这里上锁是为了保证领导者节点给跟随者节点安装快照时lastSnapshotIndex的正确性
            this.lock.lock();
            try {//校验一下旧的快照记录的日志索引是不是当前快照存储器之前记录的那个
                Requires.requireTrue(oldIndex == this.lastSnapshotIndex);
                //如果是就说明快照生成没问题，然后给lastSnapshotIndex赋最新值
                this.lastSnapshotIndex = newIndex;
            } finally {
                this.lock.unlock();
            }
            //减少旧的快照文件记录日志索引的引用，如果引用为0了，就可以直接删除该快照文件了
            unref(oldIndex);
        } while (false);
        if (ret != 0) {
            //走到这里意味着上面有操作没执行成功
            LOG.warn("Close snapshot writer {} with exit code: {}.", writer.getPath(), ret);
            if (!keepDataOnError) {
                //不管是哪没有成功，都要把快照文件删除了，keepDataOnError就是是否删除错误快照文件的意思
                //keepDataOnError这个值是从LocalSnapshotWriter类的close方法中传递过来的，传过来的值为false
                //所以这里会把错误数据删除
                destroySnapshot(writer.getPath());
            }
        }
        if (ioe != null) {
            //如果异常不为空，抛出异常
            throw ioe;
        }

    }

    @Override
    public void shutdown() {
        // ignore
    }


    @Override
    public boolean setFilterBeforeCopyRemote() {
        this.filterBeforeCopyRemote = true;
        return true;
    }


    //创建快照写入器的入口方法
    @Override
    public SnapshotWriter create() {
        return create(true);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：真正创建快照写入器的方法
     */
    public SnapshotWriter create(final boolean fromEmpty) {
        LocalSnapshotWriter writer = null;
        do {
            //首先得到存储快照的临时路径，这里就可以看出来，每一次生成快照的时候，会先把快照存放到临时目录中
            //等快照生成完毕之后，才会把快照移动到正式的文件中，原因也很简单，就是不想让正在生成的快照对当前程序中的最新快照造成干扰
            //如果生成快照失败了，那么失败的快照也是在临时目录中，并没有在正式目录中
            final String snapshotPath = this.path + File.separator + TEMP_PATH;
            //上面已经得到了临时目录，然后就是判断临时目录是否存在，如果存在就删除
            //这里就是在每一次生成新快照之前，把旧的临时快照删除了的操作
            if (new File(snapshotPath).exists() && fromEmpty) {
                //在这里删除临时快照文件夹
                if (!destroySnapshot(snapshotPath)) {
                    break;
                }
            }
            //创建快照写入器，快照写入器持有的路径是临时路径
            writer = new LocalSnapshotWriter(snapshotPath, this, this.raftOptions);
            //初始化快照写入器
            if (!writer.init(null)) {
                LOG.error("Fail to init snapshot writer.");
                writer = null;
                break;
            }
        } while (false);
        //返回快照写入器
        return writer;
    }




//    @Override
//    public SnapshotReader open() {
//        long lsIndex = 0;
//        this.lock.lock();
//        try {
//            if (this.lastSnapshotIndex != 0) {
//                lsIndex = this.lastSnapshotIndex;
//                ref(lsIndex);
//            }
//        } finally {
//            this.lock.unlock();
//        }
//        if (lsIndex == 0) {
//            LOG.warn("No data for snapshot reader {}.", this.path);
//            return null;
//        }
//        final String snapshotPath = getSnapshotPath(lsIndex);
//        final SnapshotReader reader = new LocalSnapshotReader(this, this.snapshotThrottle, this.addr, this.raftOptions,
//                snapshotPath);
//        if (!reader.init(null)) {
//            LOG.error("Fail to init reader for path {}.", snapshotPath);
//            unref(lsIndex);
//            return null;
//        }
//        return reader;
//    }
//
//    @Override
//    public SnapshotReader copyFrom(final String uri, final SnapshotCopierOptions opts) {
//        final SnapshotCopier copier = startToCopyFrom(uri, opts);
//        if (copier == null) {
//            return null;
//        }
//        try {
//            copier.join();
//        } catch (final InterruptedException e) {
//            Thread.currentThread().interrupt();
//            LOG.error("Join on snapshot copier was interrupted.");
//            return null;
//        }
//        final SnapshotReader reader = copier.getReader();
//        Utils.closeQuietly(copier);
//        return reader;
//    }
//
//    @Override
//    public SnapshotCopier startToCopyFrom(final String uri, final SnapshotCopierOptions opts) {
//        final LocalSnapshotCopier copier = new LocalSnapshotCopier();
//        copier.setStorage(this);
//        copier.setSnapshotThrottle(this.snapshotThrottle);
//        copier.setFilterBeforeCopyRemote(this.filterBeforeCopyRemote);
//        if (!copier.init(uri, opts)) {
//            LOG.error("Fail to init copier to {}.", uri);
//            return null;
//        }
//        copier.start();
//        return copier;
//    }

}
