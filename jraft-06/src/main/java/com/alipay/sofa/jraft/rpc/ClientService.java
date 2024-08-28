package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.protobuf.Message;

import java.util.concurrent.Future;


//提供客户端服务的接口，该接口中定义的都是连接方法
public interface ClientService extends Lifecycle<RpcOptions> {

    //初始化方法
    boolean init(RpcOptions rpcOptions);

    boolean connect(final Endpoint endpoint);

    boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent);

    boolean disconnect(final Endpoint endpoint);


    boolean isConnected(final Endpoint endpoint);


    <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                       final RpcResponseClosure<T> done, final int timeoutMs);
}
