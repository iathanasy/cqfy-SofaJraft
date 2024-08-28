package com.alipay.sofa.jraft.rpc.impl.core;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventProcessor;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.sofa.jraft.ReplicatorGroup;
import com.alipay.sofa.jraft.entity.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/24
 * @Description:整个集群的客户端
 */
public class ClientServiceConnectionEventProcessor implements ConnectionEventProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ClientServiceConnectionEventProcessor.class);

    private final ReplicatorGroup rgGroup;

    public ClientServiceConnectionEventProcessor(ReplicatorGroup rgGroup) {
        super();
        this.rgGroup = rgGroup;
    }

    @Override
    public void onEvent(final String remoteAddr, final Connection conn) {
        final PeerId peer = new PeerId();
        if (peer.parse(remoteAddr)) {
            LOG.info("Peer {} is connected", peer);
            this.rgGroup.checkReplicator(peer, true);
        } else {
            LOG.error("Fail to parse peer: {}", remoteAddr);
        }
    }
}
