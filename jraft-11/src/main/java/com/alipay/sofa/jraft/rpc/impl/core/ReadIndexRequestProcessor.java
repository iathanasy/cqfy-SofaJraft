package com.alipay.sofa.jraft.rpc.impl.core;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RaftServerService;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.rpc.RpcResponseClosureAdapter;
import com.google.protobuf.Message;

import java.util.concurrent.Executor;

public class ReadIndexRequestProcessor extends NodeRequestProcessor<RpcRequests.ReadIndexRequest> {

    public ReadIndexRequestProcessor(Executor executor) {
        super(executor, RpcRequests.ReadIndexResponse.getDefaultInstance());
    }

    @Override
    protected String getPeerId(final RpcRequests.ReadIndexRequest request) {
        return request.getPeerId();
    }

    @Override
    protected String getGroupId(final RpcRequests.ReadIndexRequest request) {
        return request.getGroupId();
    }

    @Override
    public Message processRequest0(final RaftServerService service, final RpcRequests.ReadIndexRequest request,
                                   final RpcRequestClosure done) {
        service.handleReadIndexRequest(request, new RpcResponseClosureAdapter<RpcRequests.ReadIndexResponse>() {

            @Override
            public void run(final Status status) {
                if (getResponse() != null) {
                    done.sendResponse(getResponse());
                } else {
                    done.run(status);
                }
            }

        });
        return null;
    }

    @Override
    public String interest() {
        return RpcRequests.ReadIndexRequest.class.getName();
    }
}
