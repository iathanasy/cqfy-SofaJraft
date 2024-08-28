package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.entity.LocalFileMetaOutter;
import com.alipay.sofa.jraft.entity.LocalStorageOutter;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.storage.io.ProtoBufFile;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ZeroByteStringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/22
 * @方法描述：快照元数据表
 */
public class LocalSnapshotMetaTable {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotMetaTable.class);

    private final Map<String, LocalFileMetaOutter.LocalFileMeta> fileMap;
    private final RaftOptions raftOptions;
    private RaftOutter.SnapshotMeta meta;

    public LocalSnapshotMetaTable(RaftOptions raftOptions) {
        super();
        this.fileMap = new HashMap<>();
        this.raftOptions = raftOptions;
    }


    public ByteBuffer saveToByteBufferAsRemote() {
        final LocalStorageOutter.LocalSnapshotPbMeta.Builder pbMetaBuilder = LocalStorageOutter.LocalSnapshotPbMeta.newBuilder();
        if (hasMeta()) {
            pbMetaBuilder.setMeta(this.meta);
        }
        for (final Map.Entry<String, LocalFileMetaOutter.LocalFileMeta> entry : this.fileMap.entrySet()) {
            final LocalStorageOutter.LocalSnapshotPbMeta.File.Builder fb = LocalStorageOutter.LocalSnapshotPbMeta.File.newBuilder()
                    .setName(entry.getKey())
                    .setMeta(entry.getValue());
            pbMetaBuilder.addFiles(fb.build());
        }
        return ByteBuffer.wrap(pbMetaBuilder.build().toByteArray());
    }


    public boolean loadFromIoBufferAsRemote(final ByteBuffer buf) {
        if (buf == null) {
            LOG.error("Null buf to load.");
            return false;
        }
        try {
            final LocalStorageOutter.LocalSnapshotPbMeta pbMeta = LocalStorageOutter.LocalSnapshotPbMeta.parseFrom(ZeroByteStringHelper.wrap(buf));
            if (pbMeta == null) {
                LOG.error("Fail to load meta from buffer.");
                return false;
            }
            return loadFromPbMeta(pbMeta);
        } catch (final InvalidProtocolBufferException e) {
            LOG.error("Fail to parse LocalSnapshotPbMeta from byte buffer", e);
            return false;
        }
    }


    public boolean addFile(final String fileName, final LocalFileMetaOutter.LocalFileMeta meta) {
        //这里就是把快照文件的名称和对应的元数据
        return this.fileMap.putIfAbsent(fileName, meta) == null;
    }


    public boolean removeFile(final String fileName) {
        return this.fileMap.remove(fileName) != null;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2023/12/22
     * @方法描述：把快照文件表写入到硬盘文件中
     */
    public boolean saveToFile(String path) throws IOException {
        LocalStorageOutter.LocalSnapshotPbMeta.Builder pbMeta = LocalStorageOutter.LocalSnapshotPbMeta.newBuilder();
        if (hasMeta()) {
            pbMeta.setMeta(this.meta);
        }
        for (Map.Entry<String, LocalFileMetaOutter.LocalFileMeta> entry : this.fileMap.entrySet()) {
            LocalStorageOutter.LocalSnapshotPbMeta.File f = LocalStorageOutter.LocalSnapshotPbMeta.File.newBuilder().setName(entry.getKey()).setMeta(entry.getValue()).build();
            pbMeta.addFiles(f);
        }
        ProtoBufFile pbFile = new ProtoBufFile(path);
        return pbFile.save(pbMeta.build(), this.raftOptions.isSyncMeta());
    }


    public boolean hasMeta() {
        return this.meta != null && this.meta.isInitialized();
    }


    public LocalFileMetaOutter.LocalFileMeta getFileMeta(String fileName) {
        return this.fileMap.get(fileName);
    }


    public Set<String> listFiles() {
        return this.fileMap.keySet();
    }


    public void setMeta(RaftOutter.SnapshotMeta meta) {
        this.meta = meta;
    }


    public RaftOutter.SnapshotMeta getMeta() {
        return this.meta;
    }


    public boolean loadFromFile(String path) throws IOException {
        ProtoBufFile pbFile = new ProtoBufFile(path);
        LocalStorageOutter.LocalSnapshotPbMeta pbMeta = pbFile.load();
        if (pbMeta == null) {
            LOG.error("Fail to load meta from {}.", path);
            return false;
        }
        return loadFromPbMeta(pbMeta);
    }

    private boolean loadFromPbMeta(final LocalStorageOutter.LocalSnapshotPbMeta pbMeta) {
        if (pbMeta.hasMeta()) {
            this.meta = pbMeta.getMeta();
        } else {
            this.meta = null;
        }
        this.fileMap.clear();
        for (final LocalStorageOutter.LocalSnapshotPbMeta.File f : pbMeta.getFilesList()) {
            this.fileMap.put(f.getName(), f.getMeta());
        }
        return true;
    }
}
