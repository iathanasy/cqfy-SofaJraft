package com.alipay.sofa.jraft.rpc.impl;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.InvokeTimeoutException;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.util.*;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:客户端服务抽象类，这个类中提供的都是真正向服务端发送请求消息的方法
 */
public abstract class AbstractClientService implements ClientService {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractClientService.class);

    //加载Protobuf描述文件，并且注册ProtobufSerializer序列化器给RPC组件使用
    static {
        ProtobufMsgFactory.load();
    }

    //RPC通信的客户端，这个客户端实际上就是一个BoltRpcClient对象
    protected volatile RpcClient rpcClient;
    //业务线程池
    protected ThreadPoolExecutor rpcExecutor;
    //RPC远程调用需要的配置参数对象
    protected RpcOptions rpcOptions;

    public RpcClient getRpcClient() {
        return this.rpcClient;
    }


    //下面三个方法都是检查当前RPC是否与方法参数中的节点建立了连接
    @Override
    public boolean isConnected(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        return rc != null && isConnected(rc, endpoint);
    }

    private static boolean isConnected(final RpcClient rpcClient, final Endpoint endpoint) {
        return rpcClient.checkConnection(endpoint);
    }


    @Override
    public boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            throw new IllegalStateException("Client service is uninitialized.");
        }
        return rc.checkConnection(endpoint, createIfAbsent);
    }


    //初始化方法
    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        if (this.rpcClient != null) {
            return true;
        }
        this.rpcOptions = rpcOptions;
        return initRpcClient(this.rpcOptions.getRpcProcessorThreadPoolSize());
    }

    protected void configRpcClient(final RpcClient rpcClient) {
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:核心初始化方法
     */
    protected boolean initRpcClient(final int rpcProcessorThreadPoolSize) {
        //获得创建RPC客户端和服务端的工厂
        final RaftRpcFactory factory = RpcFactoryHelper.rpcFactory();
        //在这里使用RPC工厂创建了一个RPC客户端，也就是一个BoltRpcClient对象
        //这个BoltRpcClient对象内部持有者bolt框架中的客户端，真正使用客户端功能的时候
        //其实是bolt框架中的核心组件在发挥功能
        this.rpcClient = factory.createRpcClient(factory.defaultJRaftClientConfigHelper(this.rpcOptions));
        //配置客户端，这个方法源码中没做实现
        configRpcClient(this.rpcClient);
        //根据配置参数对象初始化客户端
        this.rpcClient.init(this.rpcOptions);
        //创建业务线程池，这里我要跟大家多解释一句，在一般的RPC框架中，使用Netty发送了请求或者是接收到响应之后
        //要处理响应的业务逻辑时都是在业务线程池中处理的，而在sofajraft框架中，发送请求之后收到的响应，一般都是通过一些回调方法来处理
        //这里，这个线程池就是专门执行这些回调方法的，所以这也可以看作是业务线程池
        this.rpcExecutor = ThreadPoolUtil.newBuilder()
                //设置名字
                .poolName("JRaft-RPC-Processor")
                //是否开启性能检测
                .enableMetric(true)
                //设置核心线程数
                .coreThreads(rpcProcessorThreadPoolSize / 3)
                //设置最大线程数量
                .maximumThreads(rpcProcessorThreadPoolSize)
                //空闲线程存活时间
                .keepAliveSeconds(60L)
                //任务队列
                .workQueue(new ArrayBlockingQueue<>(10000))
                //线程工厂
                .threadFactory(new NamedThreadFactory("JRaft-RPC-Processor-", true))
                .build();
        //暂时不必关注下面这行代码
        if (this.rpcOptions.getMetricRegistry() != null) {
            this.rpcOptions.getMetricRegistry().register("raft-rpc-client-thread-pool",
                    new ThreadPoolMetricSet(this.rpcExecutor));
        }
        return true;
    }


    @Override
    public synchronized void shutdown() {
        if (this.rpcClient != null) {
            this.rpcClient.shutdown();
            this.rpcClient = null;
            this.rpcExecutor.shutdown();
        }
    }

    //和指定的节点建立连接
    @Override
    public boolean connect(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            throw new IllegalStateException("Client service is uninitialized.");
        }
        if (isConnected(rc, endpoint)) {
            return true;
        }
        try {
            final RpcRequests.PingRequest req = RpcRequests.PingRequest.newBuilder()
                    .setSendTimestamp(System.currentTimeMillis())
                    .build();
            //注意，这里可是同步等待响应，因为调用的是invokeSync方法
            final RpcRequests.ErrorResponse resp = (RpcRequests.ErrorResponse) rc.invokeSync(endpoint, req,
                    this.rpcOptions.getRpcConnectTimeoutMs());
            return resp.getErrorCode() == 0;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final RemotingException e) {
            LOG.error("Fail to connect {}, remoting exception: {}.", endpoint, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean disconnect(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            return true;
        }
        LOG.info("Disconnect from {}.", endpoint);
        rc.closeConnection(endpoint);
        return true;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:下面这些都是使用客户端向指定的节点发送请求的，但是都是异步的，所谓异步就是发送的请求之后
     * 不必同步等待响应，而是可以去做别的事，等响应到大的时候，调用传进方法中的回调函数即可，大家可以想想RPC框架的
     * 原理，RPC框架在处理请求和响应时，都会支持同步和异步
     */
    @Override
    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(endpoint, request, done, timeoutMs, this.rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        return invokeWithDone(endpoint, request, null, done, timeoutMs, rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(endpoint, request, ctx, done, timeoutMs, this.rpcExecutor);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:这个是RPC客户端发送请求的入口方法，从名字就能看出来是异步的，因为要通过回调函数来处理响应
     */
    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        //得到客户端
        final RpcClient rc = this.rpcClient;
        //创建一个future对象，这个就可以当作Netty中的promise
        final FutureImpl<Message> future = new FutureImpl<>();
        //得到业务线程池，这个线程池的作用就是在当前节点收到响应后，执行一些回调方法
        //其实这里就可以挑明了说，这里之所以弄了一个三元运算符，是因为在客户端中初始化好的线程池，也就是在本类的initRpcClient方法中创建的这个线程池
        //主要是执行和投票请求相关的响应回调的，而当前方法的参数中也可能会传进来一个线程池，如果是方法中传入的线程池
        //就肯定是一个单线程执行器，专门用来执行和复制日志以及心跳消息请求相关的响应回调的，这个逻辑可以从复制器对象中查看，就从复制器对象的sendEmptyEntries、sendEntries方法中
        //但很遗憾，第一版本还没有sendEntries方法。。所以等到后面的版本大家进一步学习这里的知识吧
        final Executor currExecutor = rpcExecutor != null ? rpcExecutor : this.rpcExecutor;
        try {//判断客户端是否为空，为空则意味着客户端还没初始化好
            if (rc == null) {
                future.failure(new IllegalStateException("Client service is uninitialized."));
                RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                        "Client service is uninitialized."));
                return future;
            }
            //开始异步发送请求了，再往下调用，逻辑就会陷入bolt框架中了，这部分的逻辑讲解，我录制到视频中了
            //这里的异步指的是发送了请求不必同步等待响应，当响应到达时，直接执行回调方法即可
            //回调方法就是该方法中的第三个参数，创建了一个InvokeCallback对象，这个对象内部持有者上面得到的
            //currExecutor，所以，这个currExecutor就是用来执行回调方法的
            //InvokeCallback对象中要被回调的就是complete方法，具体回调的时机和逻辑都在视频中讲解
            //大家把第一版本代码知识掌握清楚了，就可以去视频中专门看看RPC的流程了
            rc.invokeAsync(endpoint, request, ctx, new InvokeCallback() {

                @SuppressWarnings({ "unchecked", "ConstantConditions" })
                @Override
                public void complete(final Object result, final Throwable err) {
                    if (future.isCancelled()) {
                        onCanceled(request, done);
                        return;
                    }
                    if (err == null) {
                        Status status = Status.OK();
                        Message msg;
                        if (result instanceof RpcRequests.ErrorResponse) {
                            status = handleErrorResponse((RpcRequests.ErrorResponse) result);
                            msg = (Message) result;
                        } else if (result instanceof Message) {
                            final Descriptors.FieldDescriptor fd = ((Message) result).getDescriptorForType()
                                    .findFieldByNumber(RpcResponseFactory.ERROR_RESPONSE_NUM);
                            if (fd != null && ((Message) result).hasField(fd)) {
                                final RpcRequests.ErrorResponse eResp = (RpcRequests.ErrorResponse) ((Message) result).getField(fd);
                                status = handleErrorResponse(eResp);
                                msg = eResp;
                            } else {
                                msg = (T) result;
                            }
                        } else {
                            msg = (T) result;
                        }
                        if (done != null) {
                            try {
                                if (status.isOk()) {
                                    done.setResponse((T) msg);
                                }
                                done.run(status);
                            } catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                            }
                        }
                        if (!future.isDone()) {
                            future.setResult(msg);
                        }
                    } else {
                        if (done != null) {
                            try {
                                done.run(new Status(err instanceof InvokeTimeoutException ? RaftError.ETIMEDOUT
                                        : RaftError.EINTERNAL, "RPC exception:" + err.getMessage()));
                            } catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                            }
                        }
                        if (!future.isDone()) {
                            future.failure(err);
                        }
                    }
                }
                //InvokeCallback对象内部持有着执行器
                @Override
                public Executor executor() {
                    return currExecutor;
                }
            }, timeoutMs <= 0 ? this.rpcOptions.getRpcDefaultTimeout() : timeoutMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            future.failure(e);
            // should be in another thread to avoid dead locking.
            RpcUtils.runClosureInExecutor(currExecutor, done,
                    new Status(RaftError.EINTR, "Sending rpc was interrupted"));
        } catch (final RemotingException e) {
            future.failure(e);
            // should be in another thread to avoid dead locking.
            RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                    "Fail to send a RPC request:" + e.getMessage()));

        }

        return future;
    }

    private static Status handleErrorResponse(final RpcRequests.ErrorResponse eResp) {
        final Status status = new Status();
        status.setCode(eResp.getErrorCode());
        if (eResp.hasErrorMsg()) {
            status.setErrorMsg(eResp.getErrorMsg());
        }
        return status;
    }

    private <T extends Message> void onCanceled(final Message request, final RpcResponseClosure<T> done) {
        if (done != null) {
            try {
                done.run(new Status(RaftError.ECANCELED, "RPC request was canceled by future."));
            } catch (final Throwable t) {
                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
            }
        }
    }

}
