package com.alipay.sofa.jraft.rpc.impl.cli;

import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.rpc.CliRequests;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;
import com.google.protobuf.Message;

import java.util.List;
import java.util.concurrent.Executor;


//这是一个集群客户端处理器，专门处理向集群中添加新节点的请求的
public class AddPeerRequestProcessor extends BaseCliRequestProcessor<CliRequests.AddPeerRequest> {


    public AddPeerRequestProcessor(Executor executor) {
        super(executor, CliRequests.AddPeerResponse.getDefaultInstance());
    }


    @Override
    protected String getPeerId(final CliRequests.AddPeerRequest request) {
        return request.getLeaderId();
    }


    @Override
    protected String getGroupId(final CliRequests.AddPeerRequest request) {
        return request.getGroupId();
    }

    //下面就是处理向集群中添加节点的AddPeerRequest请求的核心方法
    @Override
    protected Message processRequest0(final CliRequestContext ctx, final CliRequests.AddPeerRequest request, final RpcRequestClosure done) {
        //获取当前集群中的配置，也就是目前集群中所有节点的信息
        final List<PeerId> oldPeers = ctx.node.listPeers();
        //从请求中得到要向集群中添加的节点信息字符串
        final String addingPeerIdStr = request.getPeerId();
        //创建一个peerid对象
        final PeerId addingPeer = new PeerId();
        //解析请求中的节点信息，就是把字符串解析成一个peerid对象
        if (addingPeer.parse(addingPeerIdStr)) {
            //走到这里意味着要添加到集群中的节点信息解析成功了
            LOG.info("Receive AddPeerRequest to {} from {}, adding {}", ctx.node.getNodeId(), done.getRpcCtx()
                    .getRemoteAddress(), addingPeerIdStr);
            //在这里把节点添加到集群中，并且创建了一个回调对象，当
            //节点成功添加到集群中，这个回调对象中的方法就会被回调
            ctx.node.addPeer(addingPeer, status -> {
                //下面就是回调方法的逻辑了
                //现根据执行结果的状态判断操作是否成功
                if (!status.isOk()) {
                    //不成功则直接回复一个错误响应给客户端
                    done.run(status);
                } else {
                    //走到这里就意味着新节点添加集群成功，这时候就要构建一个成功响应回复给客户端
                    final CliRequests.AddPeerResponse.Builder rb = CliRequests.AddPeerResponse.newBuilder();
                    //下面就是把集群当前最新的配置信息，也就是节点封装到响应中，回复给客户端的操作
                    //大家简单看看就行
                    boolean alreadyExists = false;
                    for (final PeerId oldPeer : oldPeers) {
                        rb.addOldPeers(oldPeer.toString());
                        rb.addNewPeers(oldPeer.toString());
                        if (oldPeer.equals(addingPeer)) {
                            alreadyExists = true;
                        }
                    }
                    if (!alreadyExists) {
                        rb.addNewPeers(addingPeerIdStr);
                    }
                    //回复响应
                    done.sendResponse(rb.build());
                }
            });
        } else {
            //走到这里意味着解析节点失败，直接返回错误响应
            return RpcFactoryHelper //
                    .responseFactory() //
                    .newResponse(defaultResp(), RaftError.EINVAL, "Fail to parse peer id %s", addingPeerIdStr);
        }
        return null;
    }

    //这里会和bolt框架扯上关系，就是在寻找处理器的时候，会调用这个方法，返回一个处理请求的名称
    //表明当前处理器对处理AddPeerRequest请求感兴趣
    @Override
    public String interest() {
        return CliRequests.AddPeerRequest.class.getName();
    }

}
