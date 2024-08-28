package com.alipay.sofa.jraft.rpc.impl.core;

import com.alipay.sofa.jraft.rpc.RaftServerService;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.google.protobuf.Message;

import java.util.concurrent.Executor;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/24
 * @Description:JRaft集群中每个节点用来处理RequestVoteRequest请求的处理器，也是服务端使用的
 */
public class RequestVoteRequestProcessor extends NodeRequestProcessor<RpcRequests.RequestVoteRequest> {

    public RequestVoteRequestProcessor(Executor executor) {
        super(executor, RpcRequests.RequestVoteResponse.getDefaultInstance());
    }

    @Override
    protected String getPeerId(final RpcRequests.RequestVoteRequest request) {
        return request.getPeerId();
    }

    @Override
    protected String getGroupId(final RpcRequests.RequestVoteRequest request) {
        return request.getGroupId();
    }


    @Override
    public Message processRequest0(final RaftServerService service, final RpcRequests.RequestVoteRequest request,
                                   final RpcRequestClosure done) {
        //如果是预选举，就进入下面的分支
        if (request.getPreVote()) {
            return service.handlePreVoteRequest(request);
        } else {
            //走到这里就意味着是正式选举活动
            return service.handleRequestVoteRequest(request);
        }
    }

    @Override
    public String interest() {
        return RpcRequests.RequestVoteRequest.class.getName();
    }
}
