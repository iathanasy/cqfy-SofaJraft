package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.closure.CatchUpClosure;
import com.alipay.sofa.jraft.core.ReplicatorType;
import com.alipay.sofa.jraft.entity.NodeId;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.ReplicatorGroupOptions;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.rpc.RpcResponseClosure;
import com.alipay.sofa.jraft.util.Describer;
import com.alipay.sofa.jraft.util.ThreadId;

/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/22
 * @Description:复制器组的接口
 */
public interface ReplicatorGroup extends Describer {

    boolean init(final NodeId nodeId, final ReplicatorGroupOptions opts);

    default boolean addReplicator(final PeerId peer) {
        return addReplicator(peer, ReplicatorType.Follower);
    }


    default boolean addReplicator(final PeerId peer, ReplicatorType replicatorType) {
        return addReplicator(peer, replicatorType, true);
    }

    boolean addReplicator(final PeerId peer, ReplicatorType replicatorType, boolean sync);


    ThreadId getReplicator(final PeerId peer);


    void checkReplicator(final PeerId peer, final boolean lockNode);


    boolean resetTerm(final long newTerm);


    boolean contains(final PeerId peer);

    void sendHeartbeat(final PeerId peer, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> closure);

    boolean waitCaughtUp(final String groupId, final PeerId peer, final long maxMargin, final long dueTime, final CatchUpClosure done);

    boolean stopReplicator(final PeerId peer);

    long getLastRpcSendTimestamp(final PeerId peer);


}
