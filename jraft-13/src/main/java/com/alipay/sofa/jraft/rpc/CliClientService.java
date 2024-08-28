package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.util.Endpoint;
import com.google.protobuf.Message;

import java.util.concurrent.Future;


public interface CliClientService extends ClientService {


    Future<Message> addPeer(Endpoint endpoint, CliRequests.AddPeerRequest request,
                            RpcResponseClosure<CliRequests.AddPeerResponse> done);


    Future<Message> removePeer(Endpoint endpoint, CliRequests.RemovePeerRequest request,
                               RpcResponseClosure<CliRequests.RemovePeerResponse> done);


    Future<Message> resetPeer(Endpoint endpoint, CliRequests.ResetPeerRequest request,
                              RpcResponseClosure<RpcRequests.ErrorResponse> done);


    Future<Message> snapshot(Endpoint endpoint, CliRequests.SnapshotRequest request,
                             RpcResponseClosure<RpcRequests.ErrorResponse> done);


    Future<Message> changePeers(Endpoint endpoint, CliRequests.ChangePeersRequest request,
                                RpcResponseClosure<CliRequests.ChangePeersResponse> done);


    Future<Message> addLearners(Endpoint endpoint, CliRequests.AddLearnersRequest request,
                                RpcResponseClosure<CliRequests.LearnersOpResponse> done);


    Future<Message> removeLearners(Endpoint endpoint, CliRequests.RemoveLearnersRequest request,
                                   RpcResponseClosure<CliRequests.LearnersOpResponse> done);


    Future<Message> resetLearners(Endpoint endpoint, CliRequests.ResetLearnersRequest request,
                                  RpcResponseClosure<CliRequests.LearnersOpResponse> done);


    Future<Message> getLeader(Endpoint endpoint, CliRequests.GetLeaderRequest request,
                              RpcResponseClosure<CliRequests.GetLeaderResponse> done);


    Future<Message> transferLeader(Endpoint endpoint, CliRequests.TransferLeaderRequest request,
                                   RpcResponseClosure<RpcRequests.ErrorResponse> done);


    Future<Message> getPeers(Endpoint endpoint, CliRequests.GetPeersRequest request,
                             RpcResponseClosure<CliRequests.GetPeersResponse> done);
}
