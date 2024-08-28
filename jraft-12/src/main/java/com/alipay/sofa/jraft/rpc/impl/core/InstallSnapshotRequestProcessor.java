package com.alipay.sofa.jraft.rpc.impl.core;

import com.alipay.sofa.jraft.rpc.RaftServerService;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.google.protobuf.Message;

import java.util.concurrent.Executor;


/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/22
 * @方法描述：安装快照处理器
 */
public class InstallSnapshotRequestProcessor extends NodeRequestProcessor<RpcRequests.InstallSnapshotRequest> {

    public InstallSnapshotRequestProcessor(Executor executor) {
        super(executor, RpcRequests.InstallSnapshotResponse.getDefaultInstance());
    }

    @Override
    protected String getPeerId(final RpcRequests.InstallSnapshotRequest request) {
        return request.getPeerId();
    }

    @Override
    protected String getGroupId(final RpcRequests.InstallSnapshotRequest request) {
        return request.getGroupId();
    }

    @Override
    public Message processRequest0(final RaftServerService service, final RpcRequests.InstallSnapshotRequest request,
                                   final RpcRequestClosure done) {
        return service.handleInstallSnapshot(request, done);
    }

    @Override
    public String interest() {
        return RpcRequests.InstallSnapshotRequest.class.getName();
    }
}
