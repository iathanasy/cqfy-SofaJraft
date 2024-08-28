package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.FileService;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.Utils;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2024/3/12
 * @方法描述：快照文件读取器
 */
public class LocalSnapshotReader extends SnapshotReader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotReader.class);


    //快照文件读取器Id，为什么每一快照读取器对象要有唯一Id呢？因为领导者可能会给多个跟随着节点同时传输快照
    //每个跟随者节点都会拥有一个快照读取器，也就要有唯一Id了
    private long readerId;

    //当前节点的Id地址，其实就是提供快照服务的服务器地址
    private final Endpoint addr;

    //快照文件的元数据表
    private final LocalSnapshotMetaTable metaTable;

    //存放快照文件的路径，注意，这个是快照文件全路径，也就是带索引的，可以直接定位到存放快照文件的文件夹
    private final String path;

    //快照文件存储器
    private final LocalSnapshotStorage snapshotStorage;

    //快照传输限流器
    private final SnapshotThrottle snapshotThrottle;

    //关闭快照文件读取器的方法
    @Override
    public void close() throws IOException {
        //在关闭快照文件读取器的时候，记得给快照文件解引用
        //因为在获得快照文件读取器的时候给快照文件增加引用了
        snapshotStorage.unref(this.getSnapshotIndex());
        //在这里销毁快照文件读取器，注意，这里销毁的是真正的快照文件读取器
        //也就是SnapshotFileReader对象
        this.destroyReaderInFileService();
    }


    //构造方法
    public LocalSnapshotReader(LocalSnapshotStorage snapshotStorage, SnapshotThrottle snapshotThrottle, Endpoint addr,
                               RaftOptions raftOptions, String path) {
        super();
        this.snapshotStorage = snapshotStorage;
        this.snapshotThrottle = snapshotThrottle;
        this.addr = addr;
        this.path = path;
        this.readerId = 0;
        //在这里创建一个快照元数据表对象
        this.metaTable = new LocalSnapshotMetaTable(raftOptions);
    }


    //得到快照文件读取器Id的方法
    long getReaderId() {
        return this.readerId;
    }



    //初始化快照读取器的方法
    @Override
    public boolean init(final Void v) {
        //根据快照文件存储路径创建文件对象
        final File dir = new File(this.path);
        //判断文件是否存在
        if (!dir.exists()) {
            LOG.error("No such path {} for snapshot reader.", this.path);
            setError(RaftError.ENOENT, "No such path %s for snapshot reader", this.path);
            return false;
        }
        //在这里得到快照元数据文件存储路径
        final String metaPath = this.path + File.separator + JRAFT_SNAPSHOT_META_FILE;
        try {
            //在这里使用元数据文件表把快照文件元数据加载到内存中了
            return this.metaTable.loadFromFile(metaPath);
        } catch (final IOException e) {
            LOG.error("Fail to load snapshot meta {}.", metaPath, e);
            setError(RaftError.EIO, "Fail to load snapshot meta from path %s", metaPath);
            return false;
        }
    }

    //得到快照文件索引的方法
    private long getSnapshotIndex() {
        //根据快照文件存储历经创建文件对象
        final File file = new File(this.path);
        //获取文件名称
        final String name = file.getName();
        //判断文件名称是不是以snapshot_开头
        if (!name.startsWith(JRAFT_SNAPSHOT_PREFIX)) {
            throw new IllegalStateException("Invalid snapshot path name:" + name);
        }
        //在这里解析出快照文件的索引，理清楚这个方法的逻辑需要大家掌握快照文件夹的结构，关于这个知识，已经在LocalSnapshotStorage类的init方法中讲解过了
        return Long.parseLong(name.substring(JRAFT_SNAPSHOT_PREFIX.length()));
    }


    @Override
    public void shutdown() {
        Utils.closeQuietly(this);
    }



    //得到快照文件对应的元数据信息的方法
    @Override
    public RaftOutter.SnapshotMeta load() {
        if (this.metaTable.hasMeta()) {
            return this.metaTable.getMeta();
        }
        return null;
    }


    //创建一个用于跟随者节点远程安装快照文件的URL的方法
    @Override
    public String generateURIForCopy() {
        //校验当前节点地址非空
        if (this.addr == null || this.addr.equals(new Endpoint(Utils.IP_ANY, 0))) {
            LOG.error("Address is not specified");
            return null;
        }
        //校验readerId是否为0，为0说明快照读取器刚被创建，还没有创建真正的快照文件读取器
        if (this.readerId == 0) {
            //在这里创建了一个真正的快照文件读取器
            final SnapshotFileReader reader = new SnapshotFileReader(this.path, this.snapshotThrottle);
            //把快照文件源数据表设置到读取器中
            reader.setMetaTable(this.metaTable);
            //打开快照文件读取器，实际上就是判断了一下快照文件夹是否存在
            if (!reader.open()) {
                LOG.error("Open snapshot {} failed.", this.path);
                return null;
            }
            //把创建出来的真正的快照文件读取器存放到文件服务中
            //实际上是存放到文件服务类内部的一个map成员变量中了
            //同时返回当前快照文件读取器的Id
            this.readerId = FileService.getInstance().addReader(reader);
            //校验快照读取器的Id是否合法
            if (this.readerId < 0) {
                LOG.error("Fail to add reader to file_service.");
                return null;
            }
        }
        //返回一个让跟随者节点安装快照的URL
        return String.format(REMOTE_SNAPSHOT_URI_SCHEME + "%s/%d", this.addr, this.readerId);
    }



    //从文件服务中销毁快照文件读取器的方法
    private void destroyReaderInFileService() {
        //校验快照读取器的Id合法性
        if (this.readerId > 0) {
            //通过Id从文件服务中删除真正的快照文件读取器
            FileService.getInstance().removeReader(this.readerId);
            //重置当前快照读取器的Id
            this.readerId = 0;
        } else {
            //走到这里意味着快照读取器的Id不大于0，但也不等于0，于是认定这个Id是非法的，记录错误日志
            if (this.readerId != 0) {
                LOG.warn("Ignore destroy invalid readerId: {}", this.readerId);
            }
        }
    }


    //得到快照文件路径的方法
    @Override
    public String getPath() {
        return this.path;
    }


    //得到快照文件元数据表中的所有文件
    @Override
    public Set<String> listFiles() {
        return this.metaTable.listFiles();
    }

    @Override
    public Message getFileMeta(final String fileName) {
        //根据快照文件名称返回快照文件对应的元数据，注意，这个元数据信息并不是SnapshotMeta
        //而是LocalFileMeta
        return this.metaTable.getFileMeta(fileName);
    }
}
