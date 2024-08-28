package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.ReplicatorGroup;
import com.alipay.sofa.jraft.entity.NodeId;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.option.ReplicatorGroupOptions;
import com.alipay.sofa.jraft.option.ReplicatorOptions;
import com.alipay.sofa.jraft.rpc.RaftClientService;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.ThreadId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/23
 * @Description:该类的对象就是复制器组，复制器组对象管理着所有的复制器对象，复制器对象是相对于领导者来说的
 * 一个节点一旦成为领导者，那么这个节点就会根据配置类中封装的集群中节点的信息，把其他的跟随者节点包装成一个个复制器对象
 * 日志复制就是通过复制器对象实现的
 */
public class ReplicatorGroupImpl implements ReplicatorGroup {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicatorGroupImpl.class);

    //这个Map中的value是一个ThreadId对象，这个对象并不真的是创建了一个线程，然后给这个线程一个ID
    //实际上，这个ThreadId对象内部就持有者PeerId对应的节点包装成的复制器对象，每一个复制器对象都是独立的个体
    //也不会共用线程处理复制器中的回调方法，因为就将每个复制器对象封装到了一个threadId对象中，代表独立的对象
    private final ConcurrentMap<PeerId, ThreadId> replicatorMap = new ConcurrentHashMap<>();
    //复制器对象需要的配置参数
    private ReplicatorOptions commonOptions;
    //动态超时时间，这个其实就是心跳超时时间
    private int dynamicTimeoutMs = -1;
    //选举超时时间
    private int electionTimeoutMs = -1;
    private RaftOptions raftOptions;
    //表明每一个节点身份的Map，相对于领导者而言，其他的节点可能是跟随者，也可能是一个学习者，这个Map中存放着映射关系
    //这个是节点出现异常的Map映射
    private final Map<PeerId, ReplicatorType> failureReplicators = new ConcurrentHashMap<>();

    //初始化方法
    @Override
    public boolean init(final NodeId nodeId, final ReplicatorGroupOptions opts) {
        this.dynamicTimeoutMs = opts.getHeartbeatTimeoutMs();
        this.electionTimeoutMs = opts.getElectionTimeoutMs();
        this.raftOptions = opts.getRaftOptions();
        this.commonOptions = new ReplicatorOptions();
        this.commonOptions.setDynamicHeartBeatTimeoutMs(this.dynamicTimeoutMs);
        this.commonOptions.setElectionTimeoutMs(this.electionTimeoutMs);
        this.commonOptions.setRaftRpcService(opts.getRaftRpcClientService());
        this.commonOptions.setLogManager(opts.getLogManager());
        this.commonOptions.setBallotBox(opts.getBallotBox());
        this.commonOptions.setNode(opts.getNode());
        this.commonOptions.setTerm(0);
        this.commonOptions.setGroupId(nodeId.getGroupId());
        this.commonOptions.setServerId(nodeId.getPeerId());
        this.commonOptions.setTimerManager(opts.getTimerManager());
        return true;
    }




    @Override
    public ThreadId getReplicator(final PeerId peer) {
        return this.replicatorMap.get(peer);
    }

    @Override
    public void checkReplicator(PeerId peer, boolean lockNode) {
        //暂时不做实现
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:该方法是本类的核心方法，会在NodeImpl类的becomeLeader方法中被调用
     * 当一个节点当选为领导者后，就会把集群中其他节点包装成一个个复制器对象
     */
    @Override
    public boolean addReplicator(final PeerId peer, final ReplicatorType replicatorType, final boolean sync) {
        //判断commonOptions中的term不等于0，ReplicatorGroupImpl类的对象是在NodeImpl类的init方法中创建的
        //那个时候该类对象的commonOptions成员变量中的任期就初始化为0了
        //但是在当前节点当选为领导者后，该类对象的commonOptions成员变量中的任期就被设置为领导者的任期了
        //然后才执行当前的添加复制器对象的方法，因为每一个复制器对象的任期都要和领导者任期一致
        //所以这里要判断commonOptions中的任期不等于0，因为这个commonOptions成员变量中的任期一会就要传递给新创建的每一个复制器对象了
        Requires.requireTrue(this.commonOptions.getTerm() != 0);
        //添加复制器之前，先从节点异常的Map集合中删除一下对应节点，如果有就删除，没有就算了
        this.failureReplicators.remove(peer);
        //判断复制器对象是否已经添加过了
        if (this.replicatorMap.containsKey(peer)) {
            return true;
        }
        //得到复制器对象需要的配置参数
        final ReplicatorOptions opts = this.commonOptions == null ? new ReplicatorOptions() : this.commonOptions.copy();
        //设置复制器类型，可能是跟随者，也可能是学习者
        opts.setReplicatorType(replicatorType);
        opts.setPeerId(peer);
        //如果 sync 为 false，执行以下操作
        //这里是事先检查一下要给发送消息的节点是否能连接成功
        //当前方法中传入的sync参数为true，取反为false，并不会进入下面的分支
        //实际上，这里就是判断一下是否在创建复制器之前检查复制器对应的节点可以连接成功
        if (!sync) {
            //得到RPC客户端
            final RaftClientService client = opts.getRaftRpcService();
            //检查是否能连接成功
            if (client != null && !client.checkConnection(peer.getEndpoint(), true)) {
                LOG.error("Fail to check replicator connection to peer={}, replicatorType={}.", peer, replicatorType);
                //如果无法连接成功，就把当前节点和其身份添加到failureReplicators这个Map中
                this.failureReplicators.put(peer, replicatorType);
                return false;
            }
        }
        //在这里真正创建了一个复制器对象，虽然返回了一个ThreadId对象，但是该对象内部持有者复制器对象
        final ThreadId rid = Replicator.start(opts, this.raftOptions);
        //如果复制器对象创建失败则返回false
        if (rid == null) {
            LOG.error("Fail to start replicator to peer={}, replicatorType={}.", peer, replicatorType);
            //记录失败节点到Map中
            this.failureReplicators.put(peer, replicatorType);
            return false;
        }
        //到这里就一切顺利，把包装着复制器对象的ThreadId对象添加到replicatorMap这个Map中
        return this.replicatorMap.put(peer, rid) == null;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/23
     * @Description:重置commonOptions成员变量中任期的方法，该方法会在NodeImpl类的becomeLeader方法中被调用
     */
    @Override
    public boolean resetTerm(final long newTerm) {
        if (newTerm <= this.commonOptions.getTerm()) {
            return false;
        }
        this.commonOptions.setTerm(newTerm);
        return true;
    }


    @Override
    public boolean contains(final PeerId peer) {
        return this.replicatorMap.containsKey(peer);
    }

    @Override
    public void describe(final Printer out) {
        out.print("  replicators: ") //
                .println(this.replicatorMap.values());
        out.print("  failureReplicators: ") //
                .println(this.failureReplicators);
    }

}
