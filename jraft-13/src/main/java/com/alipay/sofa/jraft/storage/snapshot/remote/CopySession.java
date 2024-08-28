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

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.Scheduler;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.CopyOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.rpc.RaftClientService;
import com.alipay.sofa.jraft.rpc.RpcRequests.GetFileRequest;
import com.alipay.sofa.jraft.rpc.RpcRequests.GetFileResponse;
import com.alipay.sofa.jraft.rpc.RpcResponseClosureAdapter;
import com.alipay.sofa.jraft.rpc.RpcUtils;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.util.*;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//复制领导者快照文件的会话器
@ThreadSafe
public class CopySession implements Session {

    private static final Logger LOG = LoggerFactory.getLogger(CopySession.class);

    private final Lock lock = new ReentrantLock();
    private final Status st = Status.OK();
    private final CountDownLatch finishLatch = new CountDownLatch(1);
    //直接在这里创建好跟随者节点发送了GetFileRequest请求之后，接收到GetFileResponse响应后要被回调的方法
    private final GetFileResponseClosure done = new GetFileResponseClosure();
    private final RaftClientService rpcService;
    private final GetFileRequest.Builder requestBuilder;
    private final Endpoint endpoint;
    private final Scheduler timerManager;
    private final SnapshotThrottle snapshotThrottle;
    private final RaftOptions raftOptions;
    //重试次数
    private int retryTimes = 0;
    private boolean finished;
    private ByteBufferCollector destBuf;
    private CopyOptions copyOptions = new CopyOptions();
    private OutputStream outputStream;
    private ScheduledFuture<?> timer;
    private String destPath;
    private Future<Message> rpcCall;


    private class GetFileResponseClosure extends RpcResponseClosureAdapter<GetFileResponse> {

        @Override
        public void run(final Status status) {
            //该方法会在跟随者节点接收到领导者回复的GetFileResponse响应后被回调
            //领导者的快照数据就封装在GetFileResponse响应中
            onRpcReturned(status, getResponse());
        }
    }

    public void setDestPath(final String destPath) {
        this.destPath = destPath;
    }


    GetFileResponseClosure getDone() {
        return this.done;
    }


    Future<Message> getRpcCall() {
        return this.rpcCall;
    }


    ScheduledFuture<?> getTimer() {
        return this.timer;
    }

    //关闭会话的方法，在关闭会话对象的时候，会关闭会话对象持有的outputStream输出流
    @Override
    public void close() throws IOException {
        this.lock.lock();
        try {
            if (!this.finished) {
                //在这里关闭outputStream输出流，注意，当会话对象接收到领导者GetFileResponse响应后
                //会把响应中的快照数据放到这个输出流中，输出流被关闭的时候，会把输出流中的数据写入到快照文件中
                Utils.closeQuietly(this.outputStream);
            }
            if (null != this.destBuf) {
                //这个destBuf是会话对象从领导者复制快照元数据时使用的存放数据的对象
                this.destBuf.recycle();
                this.destBuf = null;
            }
        } finally {
            this.lock.unlock();
        }
    }


    //构造方法
    public CopySession(final RaftClientService rpcService, final Scheduler timerManager,
                       final SnapshotThrottle snapshotThrottle, final RaftOptions raftOptions,
                       final GetFileRequest.Builder rb, final Endpoint ep) {
        super();
        this.snapshotThrottle = snapshotThrottle;
        this.raftOptions = raftOptions;
        this.timerManager = timerManager;
        this.rpcService = rpcService;
        this.requestBuilder = rb;
        this.endpoint = ep;
    }

    public void setDestBuf(final ByteBufferCollector bufRef) {
        this.destBuf = bufRef;
    }

    public void setCopyOptions(final CopyOptions copyOptions) {
        this.copyOptions = copyOptions;
    }

    public void setOutputStream(final OutputStream out) {
        this.outputStream = out;
    }

    //取消发送请求的操作
    @Override
    public void cancel() {
        this.lock.lock();
        try {
            if (this.finished) {
                return;
            }
            if (this.timer != null) {
                this.timer.cancel(true);
            }
            if (this.rpcCall != null) {
                this.rpcCall.cancel(true);
            }
            if (this.st.isOk()) {
                this.st.setError(RaftError.ECANCELED, RaftError.ECANCELED.name());
            }
            onFinished();
        } finally {
            this.lock.unlock();
        }
    }


    @Override
    public void join() throws InterruptedException {
        this.finishLatch.await();
    }

    @Override
    public Status status() {
        return this.st;
    }


    //本次会话结束时会调用该方法，代表会话完成了
    private void onFinished() {
        if (!this.finished) {
            if (!this.st.isOk()) {
                LOG.error("Fail to copy data, readerId={} fileName={} offset={} status={}",
                    this.requestBuilder.getReaderId(), this.requestBuilder.getFilename(),
                    this.requestBuilder.getOffset(), this.st);
            }
            if (this.outputStream != null) {
                Utils.closeQuietly(this.outputStream);
                this.outputStream = null;
            }
            if (this.destBuf != null) {
                final ByteBuffer buf = this.destBuf.getBuffer();
                if (buf != null) {
                    BufferUtils.flip(buf);
                }
                this.destBuf = null;
            }
            this.finished = true;
            this.finishLatch.countDown();
        }
    }


    //重新发送复制快照请求的方法
    private void onTimer() {
        RpcUtils.runInThread(this::sendNextRpc);
    }

    //该方法会在跟随者节点接收到GetFileResponse响应后被回调
    void onRpcReturned(final Status status, final GetFileResponse response) {
        this.lock.lock();
        try {//判断复制快照的操作是否结束了
            if (this.finished) {
                return;
            }//响应失败
            if (!status.isOk()) {
                this.requestBuilder.setCount(0);
                //如果错误码是取消，那么复制快照的操作就不必执行了
                if (status.getCode() == RaftError.ECANCELED.getNumber()) {
                    if (this.st.isOk()) {
                        this.st.setError(status.getCode(), status.getErrorMsg());
                        //结束本次会话，退出该方法即可
                        onFinished();
                        return;
                    }
                }
                //如果错误码是重试的意思，并且重试的次数超过最大限制了，那就取消本次操作，直接退出方法即可
                if (status.getCode() != RaftError.EAGAIN.getNumber()
                        && ++this.retryTimes >= this.copyOptions.getMaxRetry()) {
                    if (this.st.isOk()) {
                        this.st.setError(status.getCode(), status.getErrorMsg());
                        onFinished();
                        return;
                    }
                }//在这里让定时任务管理器执行充实的操作
                this.timer = this.timerManager.schedule(this::onTimer, this.copyOptions.getRetryIntervalMs(),
                    TimeUnit.MILLISECONDS);
                return;
            }
            this.retryTimes = 0;
            Requires.requireNonNull(response, "response");
            //判断复制领导者的快照文件是否结束了
            if (!response.getEof()) {
                //没有结束则设置一下本次读取到的字节数，这个字节数就作为下一次读取的偏移量
                this.requestBuilder.setCount(response.getReadSize());
            }
            if (this.outputStream != null) {
                try {//走到这里说明复制的是快站文件本身
                    response.getData().writeTo(this.outputStream);
                } catch (final IOException e) {
                    LOG.error("Fail to write into file {}", this.destPath, e);
                    this.st.setError(RaftError.EIO, RaftError.EIO.name());
                    onFinished();
                    return;
                }
            } else {//走到这里意味着复制的是快照元数据文件
                this.destBuf.put(response.getData().asReadOnlyByteBuffer());
            }
            if (response.getEof()) {
                //走到这里意味着快照文件复制结束了，退出该方法即可
                onFinished();
                return;
            }
        } finally {
            this.lock.unlock();
        }//走到这里意味着上一次没有复制完快照文件，需要接着发送请求，接着复制
        sendNextRpc();
    }

    //发送复制快照请求的方法
    void sendNextRpc() {
        this.lock.lock();
        try {
            this.timer = null;
            //得到读取领导者快照文件时的偏移量，一开始偏移量肯定是0
            final long offset = this.requestBuilder.getOffset() + this.requestBuilder.getCount();
            //得到本次复制快照文件可以复制的最大字节数
            final long maxCount = this.destBuf == null ? this.raftOptions.getMaxByteCountPerRpc() : Integer.MAX_VALUE;
            //把偏移量和最大限制数设置到请求构建器中
            this.requestBuilder.setOffset(offset).setCount(maxCount).setReadPartly(true);
            //判断发送请求的操作是否被取消了
            if (this.finished) {
                return;
            }
            long newMaxCount = maxCount;
            //下面这个快照传输限流器没有用到，所以逻辑就不看了，感兴趣的话大家可以自己去snapshotThrottle类中看看
            //这个snapshotThrottle类的内容很简单，除了构造方法，就定义了两个方法
            if (this.snapshotThrottle != null) {
                newMaxCount = this.snapshotThrottle.throttledByThroughput(maxCount);
                if (newMaxCount == 0) {
                    this.requestBuilder.setCount(0);
                    this.timer = this.timerManager.schedule(this::onTimer, this.copyOptions.getRetryIntervalMs(),
                        TimeUnit.MILLISECONDS);
                    return;
                }
            }
            this.requestBuilder.setCount(newMaxCount);
            //在这里构建请求
            final GetFileRequest request = this.requestBuilder.build();
            LOG.debug("Send get file request {} to peer {}", request, this.endpoint);
            //把请求发送给领导者
            //注意方法中的done这个参数，这个对象就封装着回调方法，接收到响应后该对象中的run方法会被回调
            this.rpcCall = this.rpcService.getFile(this.endpoint, request, this.copyOptions.getTimeoutMs(), this.done);
        } finally {
            this.lock.unlock();
        }
    }
}
