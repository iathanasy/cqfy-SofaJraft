package com.alipay.sofa.jraft.storage;

import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RetryAgainException;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.storage.io.FileReader;
import com.alipay.sofa.jraft.util.BufferUtils;
import com.alipay.sofa.jraft.util.ByteBufferCollector;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;
import com.alipay.sofa.jraft.util.Utils;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.ZeroByteStringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


//该类的作用非常重要，当跟随者节点需要远程安装快照的时候，会向领导者发送一个GetFileRequest请求，这个请求就会被当前类的handleGetFile
//方法处理，在handleGetFile方法中将本地快照文件的数据读取到一个bytebuffer中，然后返回给跟随者节点
public final class FileService {


    private static final Logger LOG = LoggerFactory.getLogger(FileService.class);

    //单例模式
    private static final FileService INSTANCE = new FileService();

    //这个map存放的就是每一个跟随者节点对应的真正的快照文件读取器
    private final ConcurrentMap<Long, FileReader> fileReaderMap = new ConcurrentHashMap<>();
    //该成员变量就是用来生成快照文件读取器的唯一ID的
    private final AtomicLong nextId = new AtomicLong();


    public static FileService getInstance() {
        return INSTANCE;
    }



    void clear() {
        this.fileReaderMap.clear();
    }

    //构造方法，在该构造方法中会给nextId成员变量生成一个初始值
    private FileService() {
        final long processId = Utils.getProcessId(ThreadLocalRandom.current().nextLong(10000, Integer.MAX_VALUE));
        final long initialValue = Math.abs(processId << 45 | System.nanoTime() << 17 >> 17);
        this.nextId.set(initialValue);
        LOG.info("Initial file reader id in FileService is {}", initialValue);
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/3/13
     * @方法描述：本类最核心的方法，在该方法中处理了GetFileRequest请求，并且返回给跟随者节点一个响应，在响应中封装了快照文件数据
     */
    public Message handleGetFile(final RpcRequests.GetFileRequest request, final RpcRequestClosure done) {
        //校验跟随者节点发送过来的请求是否正确，因为跟随者节点要从领导者节点读取快照数据，request.getCount()得到的就是本次可以读取的最大数据字节数
        //request.getOffset()得到的就是读取文件的偏移量，这两个数据都不可能是小于0的。
        if (request.getCount() <= 0 || request.getOffset() < 0) {
            //请求不正确则返回失败响应
            return RpcFactoryHelper
                    .responseFactory()
                    .newResponse(RpcRequests.GetFileResponse.getDefaultInstance(), RaftError.EREQUEST, "Invalid request: %s", request);
        }
        //从请求中得到跟随者节点对应的快照文件读取器的Id，然后根据Id得到快照文件读取器
        final FileReader reader = this.fileReaderMap.get(request.getReaderId());
        //校验快照文件读取器是否存在，不存在则返回错误响应
        if (reader == null) {
            return RpcFactoryHelper
                    .responseFactory()
                    .newResponse(RpcRequests.GetFileResponse.getDefaultInstance(), RaftError.ENOENT, "Fail to find reader=%d",
                            request.getReaderId());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("GetFile from {} path={} filename={} offset={} count={}", done.getRpcCtx().getRemoteAddress(),
                    reader.getPath(), request.getFilename(), request.getOffset(), request.getCount());
        }
        //创建一个ByteBufferCollector对象，在实现日志传输功能的时候，大家应该都见过这个ByteBufferCollector对象了，其实就是用来存放读取到的快照数据的
        final ByteBufferCollector dataBuffer = ByteBufferCollector.allocate();
        //创建一个GetFileResponse响应构建器
        final RpcRequests.GetFileResponse.Builder responseBuilder = RpcRequests.GetFileResponse.newBuilder();
        try {
            //在这里把快照文件的数据读取到dataBuffer中了，这里返回的是本次读取到的字节的总量，当然，也有可能返回的是文件读取完毕的标志，也就是-1
            final int read = reader.readFile(dataBuffer, request.getFilename(), request.getOffset(), request.getCount());
            //将本次读取到的字节总量封装到响应中
            responseBuilder.setReadSize(read);
            //判断是否已经读取到快照文件末尾了，并且把结果封装到响应中
            responseBuilder.setEof(read == FileReader.EOF);
            //得到存放着快照数据的bytebuffer
            final ByteBuffer buf = dataBuffer.getBuffer();
            //进去读模式
            BufferUtils.flip(buf);
            //判断buf是否存在数据
            if (!buf.hasRemaining()) {
                //没有数据直接设置响应内容为空
                responseBuilder.setData(ByteString.EMPTY);
            } else {
                //存在数据则把快照数据封装到响应中
                responseBuilder.setData(ZeroByteStringHelper.wrap(buf));
            }
            //创建响应并把响应返回给跟随者节点
            return responseBuilder.build();
        } catch (final RetryAgainException e) {
            return RpcFactoryHelper
                    .responseFactory()
                    .newResponse(RpcRequests.GetFileResponse.getDefaultInstance(), RaftError.EAGAIN,
                            "Fail to read from path=%s filename=%s with error: %s", reader.getPath(), request.getFilename(),
                            e.getMessage());
        } catch (final IOException e) {
            LOG.error("Fail to read file path={} filename={}", reader.getPath(), request.getFilename(), e);
            return RpcFactoryHelper
                    .responseFactory()
                    .newResponse(RpcRequests.GetFileResponse.getDefaultInstance(), RaftError.EIO,
                            "Fail to read from path=%s filename=%s", reader.getPath(), request.getFilename());
        }
    }




    //将一个快照文件读取器添加到fileReaderMap中
    public long addReader(final FileReader reader) {
        //在这里生成快照文件读取器对应的唯一ID
        final long readerId = this.nextId.getAndIncrement();
        //将快照文件读取器放到fileReaderMap中
        if (this.fileReaderMap.putIfAbsent(readerId, reader) == null) {
            return readerId;
        } else {
            return -1L;
        }
    }


    //根据readerId将一个快照文件读取器从fileReaderMap中移除
    public boolean removeReader(final long readerId) {
        return this.fileReaderMap.remove(readerId) != null;
    }
}
