/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.jraft.storage.snapshot.remote;

import com.alipay.sofa.jraft.core.Scheduler;
import com.alipay.sofa.jraft.option.CopyOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.SnapshotCopierOptions;
import com.alipay.sofa.jraft.rpc.RaftClientService;
import com.alipay.sofa.jraft.rpc.RpcRequests.GetFileRequest;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.storage.snapshot.Snapshot;
import com.alipay.sofa.jraft.util.ByteBufferCollector;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;



//远程快照复制器
public class RemoteFileCopier {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCopier.class);

    private long readId;
    private RaftClientService rpcService;
    private Endpoint endpoint;
    private RaftOptions raftOptions;
    private Scheduler timerManager;
    private SnapshotThrottle snapshotThrottle;


    long getReaderId() {
        return this.readId;
    }


    Endpoint getEndpoint() {
        return this.endpoint;
    }



    //初始化远程快照复制器的方法，在这个方法中会解析出要访问的领导者的网络地址，readId也会被解析出来
    //这样就知道在领导者内部，让哪一个快照文件读取器来读取快照文件了，详细的注释我就不添加了，这些逻辑都很简单
    public boolean init(String uri, final SnapshotThrottle snapshotThrottle, final SnapshotCopierOptions opts) {
        this.rpcService = opts.getRaftClientService();
        this.timerManager = opts.getTimerManager();
        this.raftOptions = opts.getRaftOptions();
        this.snapshotThrottle = snapshotThrottle;
        final int prefixSize = Snapshot.REMOTE_SNAPSHOT_URI_SCHEME.length();
        if (uri == null || !uri.startsWith(Snapshot.REMOTE_SNAPSHOT_URI_SCHEME)) {
            LOG.error("Invalid uri {}.", uri);
            return false;
        }
        uri = uri.substring(prefixSize);
        final int slasPos = uri.indexOf('/');
        final String ipAndPort = uri.substring(0, slasPos);
        uri = uri.substring(slasPos + 1);
        try {
            this.readId = Long.parseLong(uri);
            final String[] ipAndPortStrs = ipAndPort.split(":");
            this.endpoint = new Endpoint(ipAndPortStrs[0], Integer.parseInt(ipAndPortStrs[1]));
        } catch (final Exception e) {
            LOG.error("Fail to parse readerId or endpoint.", e);
            return false;
        }
        if (!this.rpcService.connect(this.endpoint)) {
            LOG.error("Fail to init channel to {}.", this.endpoint);
            return false;
        }

        return true;
    }


    public boolean copyToFile(final String source, final String destPath, final CopyOptions opts) throws IOException,
                                                                                                 InterruptedException {
        final Session session = startCopyToFile(source, destPath, opts);
        if (session == null) {
            return false;
        }
        try {
            session.join();
            return session.status().isOk();
        } finally {
            Utils.closeQuietly(session);
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/3/14
     * @方法描述：开始从领导者复制快照文件
     */
    public Session startCopyToFile(final String source, final String destPath, final CopyOptions opts) throws IOException {
        final File file = new File(destPath);

        //判断文件是否存在，正常应该是不存在的
        if (file.exists()) {
            //预删除一下，如果有文件正好在这里删除了
            if (!file.delete()) {
                LOG.error("Fail to delete destPath: {}.", destPath);
                return null;
            }
        }//创建一个输出流
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(file, false) {

            @Override
            public void close() throws IOException {
                //注意，这行代码很关键，因为这个out实际上是包装了一个文件输出流，在并且这个out会交给session对象使用
                //当session对象从领导者复制了快照数据后，会把快照数据存放到out中，当把领导者的快照文件复制完毕后
                //最终也会关闭上面这个out，释放资源，在释放资源之前会调用out的close方法，把输出流中的快照文件数据刷新到file文件中
                getFD().sync();
                super.close();
            }
        });//创建一个会话，实际上在会话中构建了一个申请得到领导者快照文件的GetFileRequest请求
        final CopySession session = newCopySession(source);
        session.setOutputStream(out);
        session.setDestPath(destPath);
        session.setDestBuf(null);
        if (opts != null) {
            session.setCopyOptions(opts);
        }//在这里调用快照的方法，把GetFileRequest请求发送给领导者
        //这时候大家应该也就明白了，最终将复制快照的请求发送给领导者的操作
        //其实是这个会话对象执行的
        session.sendNextRpc();
        return session;
    }

    //创建一个会话对象的方法
    private CopySession newCopySession(final String source) {
        final GetFileRequest.Builder reqBuilder = GetFileRequest.newBuilder()
            .setFilename(source)
            .setReaderId(this.readId);
        return new CopySession(this.rpcService, this.timerManager, this.snapshotThrottle, this.raftOptions, reqBuilder,
            this.endpoint);
    }


    public boolean copy2IoBuffer(final String source, final ByteBufferCollector destBuf, final CopyOptions opt) throws InterruptedException {
        final Session session = startCopy2IoBuffer(source, destBuf, opt);
        if (session == null) {
            return false;
        }
        try {
            session.join();
            return session.status().isOk();
        } finally {
            Utils.closeQuietly(session);
        }
    }


    //这个方法也是从领导者复制快照数据，只不过这个方法复制的是快照元数据
    public Session startCopy2IoBuffer(final String source, final ByteBufferCollector destBuf, final CopyOptions opts) {
        final CopySession session = newCopySession(source);
        session.setOutputStream(null);
        session.setDestBuf(destBuf);
        if (opts != null) {
            session.setCopyOptions(opts);
        }//发送GetFileRequest请求给领导者，申请复制快照元数据文件
        session.sendNextRpc();
        return session;
    }
}
