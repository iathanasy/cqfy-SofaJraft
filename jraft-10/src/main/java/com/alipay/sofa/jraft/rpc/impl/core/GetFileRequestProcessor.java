package com.alipay.sofa.jraft.rpc.impl.core;

import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcRequestProcessor;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.storage.FileService;
import com.google.protobuf.Message;

import java.util.concurrent.Executor;

public class GetFileRequestProcessor extends RpcRequestProcessor<RpcRequests.GetFileRequest> {

    public GetFileRequestProcessor(Executor executor) {
        super(executor, RpcRequests.GetFileResponse.getDefaultInstance());
    }

    @Override
    public Message processRequest(final RpcRequests.GetFileRequest request, final RpcRequestClosure done) {
        return FileService.getInstance().handleGetFile(request, done);
    }

    @Override
    public String interest() {
        return RpcRequests.GetFileRequest.class.getName();
    }
}
