package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.entity.LocalFileMetaOutter;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Utils;
import com.google.protobuf.Message;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/21
 * @方法描述：快照写入器
 */
public class LocalSnapshotWriter extends SnapshotWriter {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotWriter.class);

    //快照元数据表
    private final LocalSnapshotMetaTable metaTable;
    //快照文件要存储的路径
    private final String path;
    //快照存储器
    private final LocalSnapshotStorage snapshotStorage;

    //构造方法
    public LocalSnapshotWriter(String path, LocalSnapshotStorage snapshotStorage, RaftOptions raftOptions) {
        super();
        this.snapshotStorage = snapshotStorage;
        this.path = path;
        //在这里创建了快照元数据表
        this.metaTable = new LocalSnapshotMetaTable(raftOptions);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：初始化快照写入器的方法
     */
    @Override
    public boolean init(final Void v) {
        //创建快照要存储的文件目录，这里就是临时目录，逻辑已经在LocalSnapshotStorage类中讲过了
        final File dir = new File(this.path);
        try {//创建临时目录，创建的其实就是个文件夹
            FileUtils.forceMkdir(dir);
        } catch (final IOException e) {
            LOG.error("Fail to create directory {}.", this.path, e);
            setError(RaftError.EIO, "Fail to create directory  %s", this.path);
            return false;
        }
        //得到存放快照元数据文件的路径，快照元数据文件名为 "__raft_snapshot_meta
        final String metaPath = path + File.separator + JRAFT_SNAPSHOT_META_FILE;
        final File metaFile = new File(metaPath);
        try {
            if (metaFile.exists()) {
                //判断快照元数据文件是否存在，如果存在则加载快照元数据
                return metaTable.loadFromFile(metaPath);
            }
        } catch (final IOException e) {
            LOG.error("Fail to load snapshot meta from {}.", metaPath, e);
            setError(RaftError.EIO, "Fail to load snapshot meta from %s", metaPath);
            return false;
        }
        return true;
    }


    //从快照元数据表中得到本次生成的快照记录的最后应用的日志索引
    public long getSnapshotIndex() {
        //这里会先判断一下元数据表中是否有元数据信息，如果有则返回，没有则返回0
        return this.metaTable.hasMeta() ? this.metaTable.getMeta().getLastIncludedIndex() : 0;
    }

    @Override
    public void shutdown() {
        Utils.closeQuietly(this);
    }

    //关闭快照写入器的方法
    @Override
    public void close() throws IOException {
        close(false);
    }


    //在该方法中会调用快照存储器的关闭方法
    @Override
    public void close(final boolean keepDataOnError) throws IOException {
        //这里的keepDataOnError为false
        this.snapshotStorage.close(this, keepDataOnError);
    }


    //设置本次快照元数据到元数据表中
    @Override
    public boolean saveMeta(final RaftOutter.SnapshotMeta meta) {
        this.metaTable.setMeta(meta);
        return true;
    }


    //将快照元数据信息保存到硬盘文件中
    public boolean sync() throws IOException {
        return this.metaTable.saveToFile(this.path + File.separator + JRAFT_SNAPSHOT_META_FILE);
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/21
     * @方法描述：把一个快照元数据信息添加到元数据表中，这里我要多说一句，在源码中这个方法的第二个参数fileMeta其实是null
     * fileName对应的就是在临时快照文件夹中存放的临时快照的文件名，这个逻辑会从用户自己定义的状态机中体现出来
     */
    @Override
    public boolean addFile(final String fileName, final Message fileMeta) {
        //创建元数据构造器
        final LocalFileMetaOutter.LocalFileMeta.Builder metaBuilder = LocalFileMetaOutter.LocalFileMeta.newBuilder();
        if (fileMeta != null) {
            //这里不用合并，在第八版本中这里的fileMeta为null
            metaBuilder.mergeFrom(fileMeta);
        }
        //创建快照文件对应的元数据对象
        final LocalFileMetaOutter.LocalFileMeta meta = metaBuilder.build();
        //把快照文件以及其对应的元数据添加到元数据表中，在第八版本中快照文件对应的元数据对象中没有数据
        //这里的元信息指的是当前快照是本地快照还是远程安装的快照
        return this.metaTable.addFile(fileName, meta);
    }


    //下面这几个方法的逻辑很简单，就不再添加注释了
    @Override
    public boolean removeFile(final String fileName) {
        return this.metaTable.removeFile(fileName);
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public Set<String> listFiles() {
        return this.metaTable.listFiles();
    }

    @Override
    public Message getFileMeta(final String fileName) {
        return this.metaTable.getFileMeta(fileName);
    }
}
