package com.alipay.sofa.jraft.storage;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.StateMachine;
import com.alipay.sofa.jraft.core.NodeImpl;
import com.alipay.sofa.jraft.option.SnapshotExecutorOptions;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.util.Describer;


public interface SnapshotExecutor extends Lifecycle<SnapshotExecutorOptions>, Describer {

    NodeImpl getNode();

    //异步生成快照
    void doSnapshot(final Closure done);

    //同步生成快照
    void doSnapshotSync(final Closure done);

   //安装快照
    void installSnapshot(final RpcRequests.InstallSnapshotRequest request, final RpcRequests.InstallSnapshotResponse.Builder response,
                         final RpcRequestClosure done);

    //中断下载快照的操作
    void interruptDownloadingSnapshots(final long newTerm);

    //是否正在安装快照
    boolean isInstallingSnapshot();

    //得到快照存储器
    SnapshotStorage getSnapshotStorage();

    //阻塞当前线程
    void join() throws InterruptedException;
}
