package com.alipay.sofa.jraft.rpc;

import com.google.protobuf.Message;

//该接口定义的是处理RPC请求的方法，接口的实现类就是NodeImpl
public interface RaftServerService {

   //处理预投票请求的方法
    Message handlePreVoteRequest(RpcRequests.RequestVoteRequest request);

   //处理正式投票请求的方法
    Message handleRequestVoteRequest(RpcRequests.RequestVoteRequest request);

   //处理日志和心跳的方法
    Message handleAppendEntriesRequest(RpcRequests.AppendEntriesRequest request, RpcRequestClosure done);

    //处理安装快照请求的处理器
    Message handleInstallSnapshot(RpcRequests.InstallSnapshotRequest request, RpcRequestClosure done);

    //处理读请求的方法
    void handleReadIndexRequest(RpcRequests.ReadIndexRequest request, RpcResponseClosure<RpcRequests.ReadIndexResponse> done);


}
