package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.util.Endpoint;
import com.google.protobuf.Message;

import java.util.concurrent.Future;


//客户端服务接口，该接口中定义的是业务方法
public interface RaftClientService extends ClientService {


    Future<Message> preVote(final Endpoint endpoint, final RpcRequests.RequestVoteRequest request,
                            final RpcResponseClosure<RpcRequests.RequestVoteResponse> done);


    Future<Message> requestVote(final Endpoint endpoint, final RpcRequests.RequestVoteRequest request,
                                final RpcResponseClosure<RpcRequests.RequestVoteResponse> done);

    Future<Message> appendEntries(final Endpoint endpoint, final RpcRequests.AppendEntriesRequest request,
                                  final int timeoutMs, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> done);

    Future<Message> installSnapshot(final Endpoint endpoint, final RpcRequests.InstallSnapshotRequest request,
                                    final RpcResponseClosure<RpcRequests.InstallSnapshotResponse> done);

    Future<Message> getFile(final Endpoint endpoint, final RpcRequests.GetFileRequest request, final int timeoutMs,
                            final RpcResponseClosure<RpcRequests.GetFileResponse> done);


    Future<Message> timeoutNow(final Endpoint endpoint, final RpcRequests.TimeoutNowRequest request,
                               final int timeoutMs, final RpcResponseClosure<RpcRequests.TimeoutNowResponse> done);

    Future<Message> readIndex(final Endpoint endpoint, final RpcRequests.ReadIndexRequest request, final int timeoutMs,
                              final RpcResponseClosure<RpcRequests.ReadIndexResponse> done);
}
