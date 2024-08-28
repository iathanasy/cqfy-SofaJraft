package com.alipay.sofa.jraft.storage.io;

import com.alipay.sofa.jraft.error.RetryAgainException;
import com.alipay.sofa.jraft.util.ByteBufferCollector;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


//本地的目录读取器
public class LocalDirReader implements FileReader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDirReader.class);

    //这个就是存储快照文件的文件夹路径，这个path是从该类的子类SnapshotFileReader中传递过来的
    private final String path;


    public LocalDirReader(String path) {
        super();
        this.path = path;
    }


    @Override
    public String getPath() {
        return path;
    }


    //该方法的作用就是把快照文件的内容读取到ByteBufferCollector这个方法参数中，实际上就是把快照文件的内容读取到一个ByteBuffer中
    @Override
    public int readFile(final ByteBufferCollector buf, final String fileName, final long offset, final long maxCount)
            throws IOException,
            RetryAgainException {
        return readFileWithMeta(buf, fileName, null, offset, maxCount);
    }



    //该方法就是把快照文件数据读取到ByteBufferCollector中的真正方法，当FileService在处理GetFileRequest请求时，会调用handleGetFile方法
    //在handleGetFile方法中会调用SnapshotFileReader的readFile方法，如果读取的是快照文件数据，而不是快照元文件数据
    //就会在SnapshotFileReader的readFile方法中调用父类的readFileWithMeta方法，也就是当前方法，将快照文件数据读取到buf中
    @SuppressWarnings("unused")
    protected int readFileWithMeta(final ByteBufferCollector buf, final String fileName, final Message fileMeta,
                                   long offset, final long maxCount) throws IOException, RetryAgainException {

        //这个ByteBufferCollector对象在我为大家实现日志传输功能的时候，大家应该就见过了
        //这个对象内部实际上是有一个可以一直扩容的ByteBuffer成员变量，这里就是先看看这个ByteBufferCollector中ByteBuffer成员变量是否需要扩容
        //因为数据最终是存放到ByteBufferCollector的ByteBuffer成员变量中
        buf.expandIfNecessary();
        //得到快照文件本身的路径
        final String filePath = this.path + File.separator + fileName;
        //根据路径创建文件对象
        final File file = new File(filePath);
        //下面就是读取快照文件的常规操作了
        try (final FileInputStream input = new FileInputStream(file);
             final FileChannel fc = input.getChannel()) {
            //这里定义一个变量，记录从快照文件中读取了多少字节
            int totalRead = 0;
            while (true) {//在循环中读取快照文件
                //这里的这个offset代表的是文件偏移量，也就是从文件的哪个位置开始读取文件，这个offset是从GetFileRequest请求中得到的
                //因为快照文件可能比较大，跟随者节点一次并不能读取完整，要分几次读取，所以每一次读取都要记录读取到的位置，也就是这个文件偏移量
                //下一次读取的时候直接从偏移量的位置读取即可
                final int nread = fc.read(buf.getBuffer(), offset);
                //读取到的字节为0，则意味着读取到文件末尾了
                if (nread <= 0) {
                    return EOF;
                }//累加读取到的字节
                totalRead += nread;
                //判断读取到的总字节数是否小于本次允许读取的最大字节数
                if (totalRead < maxCount) {
                    //如果没有超出限制并且buf中还有剩余空间，说明就是快照文件读完了，并且读取完毕之后，这个buf也没有用完
                    if (buf.hasRemaining()) {
                        return EOF;
                    } else {//走到这里意味着buf没有空间了，但是本次读取还没有超出限制，这就意味着快照文件有可能没有读完，因为buf没有空间了才停下来
                        //所以这里开始扩容，当然即便扩容了，也只能按照最多能读取的字节数扩容
                        buf.expandAtMost((int) (maxCount - totalRead));
                        //更新偏移量，表示最新读取到的位置，然后进入下一次循环
                        offset += nread;
                    }
                } else {//走到这这里就意味着本次读取的快照文件的字节数超过或者等于限制了
                    //在这里得到快照文件本身的字节长度
                    final long fsize = file.length();
                    //校验文件长度合法性
                    if (fsize < 0) {
                        LOG.warn("Invalid file length {}", filePath);
                        return EOF;
                    }//如果上一次读取到的文件的位置加上本次读取到的字节数等于文件长度，那就意味着文件读取到头了
                    if (fsize == offset + nread) {
                        return EOF;
                    } else {//走到这里意味着文件还没有读取完，还有剩余内容，但是已经到达限制了，要等到下一次才能读取完
                        //所以这里就把本次读取到的总字数返回就行
                        return totalRead;
                    }
                }
            }
        }
    }
}
