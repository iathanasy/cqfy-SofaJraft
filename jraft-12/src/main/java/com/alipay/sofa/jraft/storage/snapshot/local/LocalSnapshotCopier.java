package com.alipay.sofa.jraft.storage.snapshot.local;

import com.alipay.sofa.jraft.entity.LocalFileMetaOutter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.SnapshotCopierOptions;
import com.alipay.sofa.jraft.storage.SnapshotStorage;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.storage.snapshot.Snapshot;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotCopier;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.remote.RemoteFileCopier;
import com.alipay.sofa.jraft.storage.snapshot.remote.Session;
import com.alipay.sofa.jraft.util.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//快照文件复制器，作用就是把领导者的快照复制到本地
public class LocalSnapshotCopier extends SnapshotCopier {


    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotCopier.class);

    //所在集群Id
    private String groupId;
    private final Lock lock = new ReentrantLock();
    private volatile Future<?> future;
    //表示远程复制快照的操作是否取消
    private boolean cancelled;
    //快照写入器
    private LocalSnapshotWriter writer;
    //快照读取器
    private volatile LocalSnapshotReader reader;
    //快照存储器
    private LocalSnapshotStorage storage;
    //是否在复制快照文件之前过滤相同文件，这个其实也没有用，在sofajraft中默认不开启这个操作
    private boolean filterBeforeCopyRemote;
    //远程快照对象，大家别被这个成员变量的名字和类型搞混了，类型虽然为本地快照文件，变量名却为远程快照文件
    //这其实就是把远程快照文件下载过来之后放到了这个成员变量中，所以虽然该成员变量的类型为本地快照
    //实际上存放到是远程快照
    private LocalSnapshot remoteSnapshot;
    //远程文件复制器，这个是真正远程复制快照文件的对象
    private RemoteFileCopier copier;
    //当前会话
    private Session curSession;
    //快照传输限流器
    private SnapshotThrottle snapshotThrottle;

    //设置快照传输限流器，这个实际上用不到
    public void setSnapshotThrottle(final SnapshotThrottle snapshotThrottle) {
        this.snapshotThrottle = snapshotThrottle;
    }


    //开始远程复制快照文件的方法，注意，该方法一旦被执行，就是在异步线程中被执行的了
    private void startCopy() {
        try {
            //在下面这个方法中开始复制
            internalCopy();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final IOException e) {
            LOG.error("Fail to start copy job", e);
        }
    }


    //复制领导者节点快照的方法
    private void internalCopy() throws IOException, InterruptedException {
        do {
            //该方法会先把领导者内部快照的元数据文件复制到本地
            loadMetaTable();
            //校验快照复制器状态是否征程
            if (!isOk()) {
                break;
            }
            //现在快照元数据文件下载完毕了，元数据中封装了快照数据文件的名称，所以现在就可以根据快照数据文件
            //的名称直接从领导者复制真正的快照文件了，但是在复制之前还需要进行一个操作，那就是看看当前节点
            //本地是否存在和领导者相同的快照文件，存在的话，就不必让领导者传输过来的
            //说实话，我其实不太明白这个过滤的步骤，因为根据我对领导者生成快照的逻辑的掌握，我认为领导者每生成要给最新的快照
            //都会把存放旧版本的快照文件夹删除了， 这也就意味着旧的快照文件已经被删除了，跟随者节点从领导者节点安装快照文件只能安装最新的
            //那需要过滤什么文件呢？
            //这个步骤我没有深入探究，因为在sofajraft框中默认不过滤，也不使用快照传输限流器，所以这两个知识点我也没看
            //但我把相关的源码搬运到第九版本代码中了，感兴趣的话大家可以点金这个filter方法看一看
            filter();
            if (!isOk()) {
                break;
            }
            //这里就可以得到从领导者复制过来的快照文件的名称
            final Set<String> files = this.remoteSnapshot.listFiles();
            //遍历快照文件的名称，开始远程复制快照文件本身
            //这里使用了遍历，但是最新的快照文件只有一个呀
            for (final String file : files) {
                //在下面这个方法中真正开始远程复制领导者内部的快照文件了
                copyFile(file);
            }
        } while (false);
        if (!isOk() && this.writer != null && this.writer.isOk()) {
            this.writer.setError(getCode(), getErrorMsg());
        }
        //关闭快照写入器
        if (this.writer != null) {
            //这里有个很重要的操作，当跟随者节点复制了快照文件到本地后
            //一开始是存放在临时文件夹中的，当快照写入器关闭的时候，会把快照文件从临时文件夹中移动到正式文件夹里
            Utils.closeQuietly(this.writer);
            this.writer = null;
        }
        if (isOk()) {
            //在这里得到了快照读取器
            this.reader = (LocalSnapshotReader) this.storage.open();
        }
    }



    //在该方法中，开始真正去复制领导者内部的快照文件
    void copyFile(final String fileName) throws IOException, InterruptedException {
        //如果这个快照文件已经在本地存在了，就退出该方法，不必复制
        if (this.writer.getFileMeta(fileName) != null) {
            LOG.info("Skipped downloading {}", fileName);
            return;
        }
        //校验快照文件名是否合法
        if (!checkFile(fileName)) {
            return;
        }
        //得到存放快照文件的完整路径，这里得到的还是临时路径
        final String filePath = this.writer.getPath() + File.separator + fileName;
        final Path subPath = Paths.get(filePath);
        //确保父目录存在，其实就是保证快照文件要存放在文件夹中，文件夹路径就是 this.writer.getPath()提供的
        if (!subPath.equals(subPath.getParent()) && !subPath.getParent().getFileName().toString().equals(".")) {
            final File parentDir = subPath.getParent().toFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                LOG.error("Fail to create directory for {}", filePath);
                setError(RaftError.EIO, "Fail to create directory");
                return;
            }
        }
        //在这里得到快照文件对应的元数据文件，这个元数据文件一会要和快照文件名一起被快照写入器放到元数据表中，然后落盘
        final LocalFileMetaOutter.LocalFileMeta meta = (LocalFileMetaOutter.LocalFileMeta) this.remoteSnapshot.getFileMeta(fileName);
        Session session = null;
        try {
            this.lock.lock();
            try {
                //判断操作是否被取消了
                if (this.cancelled) {
                    if (isOk()) {
                        setError(RaftError.ECANCELED, "ECANCELED");
                    }
                    return;
                }
                //在这里开始真正从领导者复制快找文件了
                session = this.copier.startCopyToFile(fileName, filePath, null);
                if (session == null) {
                    LOG.error("Fail to copy {}", fileName);
                    setError(-1, "Fail to copy %s", fileName);
                    return;
                }//保存会话信息
                this.curSession = session;
            } finally {
                this.lock.unlock();
            }//等待本次会话结束，也就是快照文件复制成功
            session.join();
            this.lock.lock();
            try {
                this.curSession = null;
            } finally {
                this.lock.unlock();
            }
            if (!session.status().isOk() && isOk()) {
                setError(session.status().getCode(), session.status().getErrorMsg());
                return;
            }
            //把快照文件的名称和对应的元数据信息存放到元数据表中
            if (!this.writer.addFile(fileName, meta)) {
                setError(RaftError.EIO, "Fail to add file to writer");
                return;
            }
            //等待元数据表中的数据落盘成功
            if (!this.writer.sync()) {
                setError(RaftError.EIO, "Fail to sync writer");
            }
        } finally {
            if (session != null) {
                Utils.closeQuietly(session);
            }
        }
    }


    private boolean checkFile(final String fileName) {
        try {
            final String parentCanonicalPath = Paths.get(this.writer.getPath()).toFile().getCanonicalPath();
            final Path filePath = Paths.get(parentCanonicalPath, fileName);
            final File file = filePath.toFile();
            final String fileAbsolutePath = file.getAbsolutePath();
            final String fileCanonicalPath = file.getCanonicalPath();
            if (!fileAbsolutePath.equals(fileCanonicalPath)) {
                LOG.error("File[{}] are not allowed to be created outside of directory[{}].", fileAbsolutePath,
                        fileCanonicalPath);
                setError(RaftError.EIO, "File[%s] are not allowed to be created outside of directory.",
                        fileAbsolutePath, fileCanonicalPath);
                return false;
            }
        } catch (final IOException e) {
            LOG.error("Failed to check file: {}, writer path: {}.", fileName, this.writer.getPath(), e);
            setError(RaftError.EIO, "Failed to check file: {}, writer path: {}.", fileName, this.writer.getPath());
            return false;
        }
        return true;
    }



    //在该方法内部会远程下载领导者快照的元数据文件
    private void loadMetaTable() throws InterruptedException {
        //这个对象就用来存放领导者快照元数据文件的
        final ByteBufferCollector metaBuf = ByteBufferCollector.allocate(0);
        //定义一个会话，是这样的，这里引入了一个会话的概念，其实这个会话，我建议大家就把它当成一个future或者peomise就行
        //因为当跟随者节点远程复制领导者快照文件时，每一次复制的数据也是有限制的，如果领导者快照文件内容很多的话，跟随者节点一次
        //是复制不完的，所以可能会复制很多次，也就会发送多次rpc请求，但是就算发送了很多rpc请求，也是在复制一个快照文件
        //所以引入了一个会话的概念，不管发送几个rpc请求，只要复制的是同一个快照文件，就称这是一次会话，当文件被彻底复制完毕了
        //这次会话也就结束了
        Session session = null;
        try {
            //上锁
            this.lock.lock();
            try {
                //如果本次操作被取消，直接退出该方法
                if (this.cancelled) {
                    if (isOk()) {
                        setError(RaftError.ECANCELED, "ECANCELED");
                    }
                    return;
                }
                //在这里开始真正执行远程复制快照操作了，并且返回了一个会话对象
                session = this.copier.startCopy2IoBuffer(Snapshot.JRAFT_SNAPSHOT_META_FILE, metaBuf, null);
                //保存该会话对象
                this.curSession = session;
            } finally {
                //解锁
                this.lock.unlock();
            }
            //在这里等待本次会话结束，点进去这个方法大家就能看到，其实就是一个CountDownLatch在发挥作用
            //在CopySession中创建了一个CountDownLatch finishLatch = new CountDownLatch(1)对象
            //当快照复制器把快照文件都复制完毕后，就会调用 this.finishLatch.countDown()方法，然后程序就会继续向下执行了
            session.join();
            this.lock.lock();
            try {
                //走到这里意味着复制已经结束了，会话也就结束了，置为null即可
                this.curSession = null;
            } finally {
                this.lock.unlock();
            }
            if (!session.status().isOk() && isOk()) {
                LOG.warn("Fail to copy meta file: {}", session.status());
                setError(session.status().getCode(), session.status().getErrorMsg());
                return;
            }
            //从领导者复制过来的数据都放在metaBuf中了，这里复制的是快照元数据信息
            //所以把数据从metaBuf中放到元数据表中了
            if (!this.remoteSnapshot.getMetaTable().loadFromIoBufferAsRemote(metaBuf.getBuffer())) {
                LOG.warn("Bad meta_table format");
                setError(-1, "Bad meta_table format from remote");
                return;
            }
            Requires.requireTrue(this.remoteSnapshot.getMetaTable().hasMeta(), "Invalid remote snapshot meta:%s", this.remoteSnapshot.getMetaTable().getMeta());
        } finally {
            if (session != null) {
                Utils.closeQuietly(session);
            }
        }
    }


    boolean filterBeforeCopy(final LocalSnapshotWriter writer, final SnapshotReader lastSnapshot) throws IOException {
        final Set<String> existingFiles = writer.listFiles();
        final ArrayDeque<String> toRemove = new ArrayDeque<>();
        for (final String file : existingFiles) {
            if (this.remoteSnapshot.getFileMeta(file) == null) {
                toRemove.add(file);
                writer.removeFile(file);
            }
        }
        final Set<String> remoteFiles = this.remoteSnapshot.listFiles();
        for (final String fileName : remoteFiles) {
            final LocalFileMetaOutter.LocalFileMeta remoteMeta = (LocalFileMetaOutter.LocalFileMeta) this.remoteSnapshot.getFileMeta(fileName);
            Requires.requireNonNull(remoteMeta, "remoteMeta");
            if (!remoteMeta.hasChecksum()) {
                // Re-download file if this file doesn't have checksum
                writer.removeFile(fileName);
                toRemove.add(fileName);
                continue;
            }
            LocalFileMetaOutter.LocalFileMeta localMeta = (LocalFileMetaOutter.LocalFileMeta) writer.getFileMeta(fileName);
            if (localMeta != null) {
                if (localMeta.hasChecksum() && localMeta.getChecksum().equals(remoteMeta.getChecksum())) {
                    LOG.info("Keep file={} checksum={} in {}", fileName, remoteMeta.getChecksum(), writer.getPath());
                    continue;
                }
                // Remove files from writer so that the file is to be copied from
                // remote_snapshot or last_snapshot
                writer.removeFile(fileName);
                toRemove.add(fileName);
            }
            // Try find files in last_snapshot
            if (lastSnapshot == null) {
                continue;
            }
            if ((localMeta = (LocalFileMetaOutter.LocalFileMeta) lastSnapshot.getFileMeta(fileName)) == null) {
                continue;
            }
            if (!localMeta.hasChecksum() || !localMeta.getChecksum().equals(remoteMeta.getChecksum())) {
                continue;
            }
            LOG.info("Found the same file ={} checksum={} in lastSnapshot={}", fileName, remoteMeta.getChecksum(),
                    lastSnapshot.getPath());
            if (localMeta.getSource() == LocalFileMetaOutter.FileSource.FILE_SOURCE_LOCAL) {
                final String sourcePath = lastSnapshot.getPath() + File.separator + fileName;
                final String destPath = writer.getPath() + File.separator + fileName;
                FileUtils.deleteQuietly(new File(destPath));
                try {
                    Files.createLink(Paths.get(destPath), Paths.get(sourcePath));
                } catch (final IOException e) {
                    LOG.error("Fail to link {} to {}", sourcePath, destPath, e);
                    continue;
                }
                // Don't delete linked file
                if (!toRemove.isEmpty() && toRemove.peekLast().equals(fileName)) {
                    toRemove.pollLast();
                }
            }
            // Copy file from last_snapshot
            writer.addFile(fileName, localMeta);
        }
        if (!writer.sync()) {
            LOG.error("Fail to sync writer on path={}", writer.getPath());
            return false;
        }
        for (final String fileName : toRemove) {
            final String removePath = writer.getPath() + File.separator + fileName;
            FileUtils.deleteQuietly(new File(removePath));
            LOG.info("Deleted file: {}", removePath);
        }
        return true;
    }



    //这个方法中有个很重要的操作，那就是创建了快照写入器，并且把从领导者复制过来的快照元数据文件通过快照写入器设置到了元数据表中
    private void filter() throws IOException {
        this.writer = (LocalSnapshotWriter) this.storage.create(!this.filterBeforeCopyRemote);
        if (this.writer == null) {
            setError(RaftError.EIO, "Fail to create snapshot writer");
            return;
        }
        if (this.filterBeforeCopyRemote) {
            final SnapshotReader reader = this.storage.open();
            if (!filterBeforeCopy(this.writer, reader)) {
                LOG.warn("Fail to filter writer before copying, destroy and create a new writer.");
                this.writer.setError(-1, "Fail to filter");
                Utils.closeQuietly(this.writer);
                this.writer = (LocalSnapshotWriter) this.storage.create(true);
            }
            if (reader != null) {
                Utils.closeQuietly(reader);
            }
            if (this.writer == null) {
                setError(RaftError.EIO, "Fail to create snapshot writer");
                return;
            }
        }
        this.writer.saveMeta(this.remoteSnapshot.getMetaTable().getMeta());
        if (!this.writer.sync()) {
            LOG.error("Fail to sync snapshot writer path={}", this.writer.getPath());
            setError(RaftError.EIO, "Fail to sync snapshot writer");
        }
    }


    //初始化快照复制器的方法
    public boolean init(final String uri, final SnapshotCopierOptions opts) {
        //创建远程文件复制器，这个复制器对象是真正远程复制领导者快照的对象
        this.copier = new RemoteFileCopier();
        //设置任务是否取消的标志，默认为false
        this.cancelled = false;
        //设置集群Id
        this.groupId = opts.getGroupId();
        this.filterBeforeCopyRemote = opts.getNodeOptions().isFilterBeforeCopyRemote();
        this.remoteSnapshot = new LocalSnapshot(opts.getRaftOptions());
        //初始化远程文件复制其
        return this.copier.init(uri, this.snapshotThrottle, opts);
    }


    public SnapshotStorage getStorage() {
        return this.storage;
    }


    public void setStorage(final SnapshotStorage storage) {
        this.storage = (LocalSnapshotStorage) storage;
    }


    public boolean isFilterBeforeCopyRemote() {
        return this.filterBeforeCopyRemote;
    }


    public void setFilterBeforeCopyRemote(final boolean filterBeforeCopyRemote) {
        this.filterBeforeCopyRemote = filterBeforeCopyRemote;
    }

    //关闭快照复制器的方法
    @Override
    public void close() throws IOException {
        //取消拷贝快照的任务
        cancel();
        try {
            //等待拷贝快照的任务结束
            join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    //启动快照文件复制器
    @Override
    public void start() {
        //在另一个线程中，也就是异步执行任务，执行的是本类的startCopy方法
        this.future = ThreadPoolsFactory.runInThread(this.groupId, this::startCopy);
    }

    @Override
    public void cancel() {
        this.lock.lock();
        try {
            if (this.cancelled) {
                return;
            }
            if (isOk()) {
                setError(RaftError.ECANCELED, "Cancel the copier manually.");
            }
            this.cancelled = true;
            if (this.curSession != null) {
                this.curSession.cancel();
            }
            if (this.future != null) {
                this.future.cancel(true);
            }
        } finally {
            this.lock.unlock();
        }
    }

    //等待从领导者复制快照任务结束的方法
    @Override
    public void join() throws InterruptedException {
        //如果有future说明存在异步任务，当前正在异步复制领导者快照
        if (this.future != null) {
            try {//等待任务完成
                this.future.get();
            } catch (final InterruptedException e) {
                throw e;
            } catch (final CancellationException ignored) {
            } catch (final Exception e) {
                LOG.error("Fail to join on copier", e);
                throw new IllegalStateException(e);
            }
        }
    }


    @Override
    public SnapshotReader getReader() {
        return this.reader;
    }
}
