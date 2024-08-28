package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.entity.LocalFileMetaOutter;
import com.alipay.sofa.jraft.error.RetryAgainException;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.storage.io.LocalDirReader;
import com.alipay.sofa.jraft.storage.snapshot.Snapshot;
import com.alipay.sofa.jraft.util.BufferUtils;
import com.alipay.sofa.jraft.util.ByteBufferCollector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

//真正的快照文件读取器
public class SnapshotFileReader extends LocalDirReader {

    //快照传输限流器
    private final SnapshotThrottle snapshotThrottle;
    //快照文件的元数据表中
    private LocalSnapshotMetaTable metaTable;



    //构造方法
    public SnapshotFileReader(String path, SnapshotThrottle snapshotThrottle) {
        //在这里把存储快照文件的路径传递给了父类LocalDirReader使用
        super(path);
        this.snapshotThrottle = snapshotThrottle;
    }



    public LocalSnapshotMetaTable getMetaTable() {
        return this.metaTable;
    }


    public void setMetaTable(LocalSnapshotMetaTable metaTable) {
        this.metaTable = metaTable;
    }

    //打开快照文件读取器的方法
    public boolean open() {
        //根据快照文件存放路径创建文件对象
        final File file = new File(getPath());
        //判断文件是否存在
        return file.exists();
    }


    //从快照文件中读取快照数据
    @Override
    public int readFile(final ByteBufferCollector metaBufferCollector, final String fileName, final long offset,
                        final long maxCount) throws IOException, RetryAgainException {
        //这里判断出要读取的数据是元数据文件中的数据
        if (fileName.equals(Snapshot.JRAFT_SNAPSHOT_META_FILE)) {
            //把元数据表中的数据全存放在metaBuf这个ByteBuffer中
            final ByteBuffer metaBuf = this.metaTable.saveToByteBufferAsRemote();
            BufferUtils.position(metaBuf, metaBuf.limit());
            //将上面得到的metaBuf设置到metaBufferCollector中，这个大家应该有印象吧，在传输日志的时候，也是这么封装日志数据的
            //都是放到ByteBuffer中
            metaBufferCollector.setBuffer(metaBuf);
            //返回EOF，代表读取到文件末尾了
            return EOF;
        }

        //程序执行到这里，说明要读取的并不是快照元数据文件，而是快照文件本身
        //每一个快照文件都对应着一个LocalFileMeta元数据对象，以键值对的方式存放到快照元数据表中，键是快照文件的文件名，value就是对用的元数据
        //这里就是根据快照文件名称，获得了快照文件对应的元数据对象
        final LocalFileMetaOutter.LocalFileMeta fileMeta = this.metaTable.getFileMeta(fileName);
        if (fileMeta == null) {
            throw new FileNotFoundException("LocalFileMeta not found for " + fileName);
        }
        //因为在sofajraft框架中默认不使用快照传输限流器，所以下面的内容就先不讲解了
        //大家感兴趣可以自己看看源码，或者直接看我提供的第9版本代码也行，还是先赶赶进度吧。。
        long newMaxCount = maxCount;
        if (this.snapshotThrottle != null) {
            newMaxCount = this.snapshotThrottle.throttledByThroughput(maxCount);
            if (newMaxCount < maxCount) {
                if (newMaxCount == 0) {
                    throw new RetryAgainException("readFile throttled by throughput");
                }
            }
        }
        //读取快照文件本身的数据到metaBufferCollector中，返回读取到的快照文件的字节数
        return readFileWithMeta(metaBufferCollector, fileName, fileMeta, offset, newMaxCount);
    }
}