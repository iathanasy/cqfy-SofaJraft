package com.alipay.sofa.jraft.storage.impl;

import com.alipay.sofa.jraft.core.NodeImpl;
import com.alipay.sofa.jraft.core.NodeMetrics;
import com.alipay.sofa.jraft.entity.EnumOutter;
import com.alipay.sofa.jraft.entity.LocalStorageOutter;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.RaftMetaStorageOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.RaftMetaStorage;
import com.alipay.sofa.jraft.storage.io.ProtoBufFile;
import com.alipay.sofa.jraft.util.Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:元数据存储器
 */
public class LocalRaftMetaStorage implements RaftMetaStorage {

    private static final Logger LOG = LoggerFactory.getLogger(LocalRaftMetaStorage.class);
    //元数据文件的名称
    private static final String RAFT_META = "raft_meta";
    //判断元数据是否已经初始化了没有，所谓初始化，就是当前节点在启动的时候
    //是否已经存本地元数据文件中把之前的元数据加载到内存中了
    private boolean isInited;
    //元数据文件的路径
    private final String path;
    //初始化好的任期，也就是当前节点之前的任期
    private long term;
    //最后一次给哪个节点投票了，PeerId封装的就是一个节点的信息，就可以代表raft集群中的一个节点
    //该成员变量初始化的时候为空，PeerId.emptyPeer()方法返回一个PeerId对象，但这个对象中的信息为空
    //所以我就直接称为空节点了
    private PeerId votedFor = PeerId.emptyPeer();
    //raft协议用到的配置参数对象
    private final RaftOptions raftOptions;
    //这个成员变量是用来监控当前节点性能的，这个暂时不做讲解
    //因为这个成员变量功能的实现其实还依赖了com.codahale.metrics jar包的功能
    private NodeMetrics nodeMetrics;
    //当前节点的实现类，当前节点的所有功能几乎全集中在这个nodeImpl类里了
    //这个NodeImpl类的内容非常多
    private NodeImpl node;

    //构造方法
    public LocalRaftMetaStorage(final String path, final RaftOptions raftOptions) {
        super();
        this.path = path;
        this.raftOptions = raftOptions;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:初始化方法，这个方法会在NodeImpl类的init方法中被调用，相当于节点初始化的时候
     * 这个节点的元数据就已经从本地元数据文件中加载到程序中了，因为在下面这个方法中，会加载本地元数据文件
     * 到内存中
     */
    @Override
    public boolean init(final RaftMetaStorageOptions opts) {
        //如果元数据已经初始化了，就直接返回
        if (this.isInited) {
            LOG.warn("Raft meta storage is already inited.");
            return true;
        }
        //从RaftMetaStorageOptions对象中获得当前正在初始化的NodeImpl节点
        this.node = opts.getNode();
        //这里就要从NodeImpl对象中获得nodeMetrics对象了，但是我在第一版本中
        //把NodeImpl中很多方法都删掉了，所以下面这行代码就注释掉了，因为现在的NodeImpl
        //对象中没有getNodeMetrics方法
        //this.nodeMetrics = this.node.getNodeMetrics();
        try {
            //根据用户配置的路径创建用来存储元数据的元文件夹，如果元文件已经存在了，就不再重复创建
            FileUtils.forceMkdir(new File(this.path));
        } catch (final IOException e) {
            //创建过程中发生异常就打印日志
            LOG.error("Fail to mkdir {}", this.path, e);
            return false;
        }
        //在这里执行加载元数据文件到内存的操作
        if (load()) {
            this.isInited = true;
            return true;
        } else {
            //加载失败则直接返回false
            return false;
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:加载本地的元数据文件到内存的方法
     */
    private boolean load() {
        //创建一个ProtoBufFile对象，该对象用来加载本地的元数据文件
        final ProtoBufFile pbFile = newPbFile();
        try {
            //加载元数据文件，这里加载完毕之后，会根据Protobuf协议把元数据文件中的数据解析成一个StablePBMeta
            //元数据文件中的数据都会封装带这个StablePBMeta中
            final LocalStorageOutter.StablePBMeta meta = pbFile.load();
            //判空，然后根据meta中的数据对成员变量term和votedFor赋值
            if (meta != null) {
                //将当前节点之前的任期赋值
                this.term = meta.getTerm();
                //获得当前节点最后一次给哪个节点投过票
                return this.votedFor.parse(meta.getVotedfor());
            }
            //为null直接返回true，由此可见，在这个jraft框架中，程序加载本地元信息文件失败了，也不会报错，而是直接返回true
            //大不了节点初始化任期就是0，反正可以和其他节点通信，修改自己的任期值
            return true;
        } catch (final FileNotFoundException e) {
            //这里跑出的是找不到文件的异常，就算抛这个异常，也返回true
            return true;
        } catch (final IOException e) {
            //报出其他异常才返回false
            LOG.error("Fail to load raft meta storage", e);
            return false;
        }
    }

    //根据元数据文件的本地路径和元数据文件名称创建一个用来加载元数据文件的
    //ProtoBufFile对象
    private ProtoBufFile newPbFile() {
        return new ProtoBufFile(this.path + File.separator + RAFT_META);
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:这个方法的作用是持久化元数据，也就是元数据落盘
     * 当程序在执行的时候，当前节点的任期可能会发生变化，也可能会给其他节点投票，只要出现了这些动作，那么这些数据就要持久化到硬盘中
     * 在NodeImpl这个类中，就会看到下面这些方法被调用了
     */
    private boolean save() {
        //记录持久化开始时间
        final long start = Utils.monotonicMs();
        //创建序列化数据，还是通过Protobuf协议进行落盘，所以要创建一个StablePBMeta对象
        final LocalStorageOutter.StablePBMeta meta = LocalStorageOutter.StablePBMeta.newBuilder()
                //在这里把当前节点任期穿进去
                .setTerm(this.term)
                //为哪个节点投票也传进去
                .setVotedfor(this.votedFor.toString())
                .build();
        //根据数据要存放的本地文件的路径和文件名获得ProtoBufFile对象
        final ProtoBufFile pbFile = newPbFile();
        try {//开始持久化操作
            if (!pbFile.save(meta, this.raftOptions.isSyncMeta())) {
                reportIOError();
                return false;
            }
            return true;
        } catch (final Exception e) {
            LOG.error("Fail to save raft meta", e);
            reportIOError();
            return false;
        } finally {//执行完毕后获得该操作耗费的时间
            final long cost = Utils.monotonicMs() - start;
            if (this.nodeMetrics != null) {
                this.nodeMetrics.recordLatency("save-raft-meta", cost);
            }//记录日志
            LOG.info("Save raft meta, path={}, term={}, votedFor={}, cost time={} ms", this.path, this.term,
                    this.votedFor, cost);
        }
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:下面这几个方法都很简单了，就不再添加注释了
     */
    private void reportIOError() {
        this.node.onError(new RaftException(EnumOutter.ErrorType.ERROR_TYPE_META, RaftError.EIO,
                "Fail to save raft meta, path=%s", this.path));
    }

    @Override
    public void shutdown() {
        if (!this.isInited) {
            return;
        }
        save();
        this.isInited = false;
    }

    private void checkState() {
        if (!this.isInited) {
            throw new IllegalStateException("LocalRaftMetaStorage not initialized");
        }
    }

    @Override
    public boolean setTerm(final long term) {
        checkState();
        this.term = term;
        return save();
    }

    @Override
    public long getTerm() {
        checkState();
        return this.term;
    }

    @Override
    public boolean setVotedFor(final PeerId peerId) {
        checkState();
        this.votedFor = peerId;
        return save();
    }

    @Override
    public PeerId getVotedFor() {
        checkState();
        return this.votedFor;
    }

    //持久化当前节点任期和投票记录的方法
    @Override
    public boolean setTermAndVotedFor(final long term, final PeerId peerId) {
        checkState();
        this.votedFor = peerId;
        this.term = term;
        return save();
    }

    @Override
    public String toString() {
        return "RaftMetaStorageImpl [path=" + this.path + ", term=" + this.term + ", votedFor=" + this.votedFor + "]";
    }
}
