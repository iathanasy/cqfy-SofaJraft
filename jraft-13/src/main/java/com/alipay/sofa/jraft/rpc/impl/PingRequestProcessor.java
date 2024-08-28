package com.alipay.sofa.jraft.rpc.impl;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.util.RpcFactoryHelper;

/**
 * Ping request processor.
 *
 * @author boyan (boyan@alibaba-inc.com)
 * @author jiachun.fjc
 */
public class PingRequestProcessor implements RpcProcessor<RpcRequests.PingRequest> {

    @Override
    public void handleRequest(final RpcContext rpcCtx, final RpcRequests.PingRequest request) {
        rpcCtx.sendResponse( //
                RpcFactoryHelper //
                        .responseFactory() //
                        .newResponse(RpcRequests.ErrorResponse.getDefaultInstance(), 0, "OK"));
    }

    @Override
    public String interest() {
        return RpcRequests.PingRequest.class.getName();
    }
}