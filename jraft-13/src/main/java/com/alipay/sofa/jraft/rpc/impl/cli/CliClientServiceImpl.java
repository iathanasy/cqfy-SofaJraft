package com.alipay.sofa.jraft.rpc.impl.cli;

import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.CliRequests;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.rpc.RpcResponseClosure;
import com.alipay.sofa.jraft.rpc.impl.AbstractClientService;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.protobuf.Message;

import java.util.concurrent.Future;


/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2024/7/7
 * @方法描述：这个就是集群客户端的真正实现类，因为这个类继承了AbstractClientService类，所以这个类的对象可以使用AbstractClientService类中的方法
 * 这样一来，CliClientServiceImpl类的对象就可以和其他节点通信了，注意，既然是集群客户端，那么这个客户端发送的请求其实都是发送给集群领导者，领导者接收到请求后再处理请求
 * 然后把集群变化通知给其他节点，比如想在集群中添加一个新节点，这个操作肯定是要领导者发起，发起之后配置发生变更，领导者再把配置变更日志传输给其他跟随者节点
 */
public class CliClientServiceImpl extends AbstractClientService implements CliClientService {

    //客户端配置对象，该对象封装了客户端发送请求的最大超时时间以及重试次数
    private CliOptions cliOptions;


    //初始化方法
    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        //首先还是对父类AbstractClientService进行初始化，初始化的过程会创建真正的通信客户端，也就是RpcClient对象
        boolean ret = super.init(rpcOptions);
        //父类初始化成功了，才会给当前类的客户端配置对象赋值
        if (ret) {
            this.cliOptions = (CliOptions) this.rpcOptions;
        }
        return ret;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：向集群中添加新的节点的方法，方法参数endpoint封装了集群领导者的节点信息，request中封装了要添加的新节点的信息，以下方法基本上都是这样，所以我就不再重复解释了
     * done是回调方法，具体逻辑大家可以从CliServiceImpl类中查看，CliServiceImpl类其实就是集群客户端最外层的类，和集群交互的方法都是由这个CliServiceImpl类的对象发起的
     */
    @Override
    public Future<Message> addPeer(final Endpoint endpoint, final CliRequests.AddPeerRequest request,
                                   final RpcResponseClosure<CliRequests.AddPeerResponse> done) {
        //调用invokeWithDone方法，这里就会调用AbstractClientService类中的方法，把请求发送出去了
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：移除集群中一个节点的方法
     */
    @Override
    public Future<Message> removePeer(final Endpoint endpoint, final CliRequests.RemovePeerRequest request,
                                      final RpcResponseClosure<CliRequests.RemovePeerResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：重置集群节点的方法
     */
    @Override
    public Future<Message> resetPeer(final Endpoint endpoint, final CliRequests.ResetPeerRequest request,
                                     final RpcResponseClosure<RpcRequests.ErrorResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }

    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：从集群某个节点得到快照的方法
     */
    @Override
    public Future<Message> snapshot(final Endpoint endpoint, final CliRequests.SnapshotRequest request,
                                    final RpcResponseClosure<RpcRequests.ErrorResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：改变集群节点的方法
     */
    @Override
    public Future<Message> changePeers(final Endpoint endpoint, final CliRequests.ChangePeersRequest request,
                                       final RpcResponseClosure<CliRequests.ChangePeersResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：向集群中添加学习者的方法
     */
    @Override
    public Future<Message> addLearners(final Endpoint endpoint, final CliRequests.AddLearnersRequest request,
                                       final RpcResponseClosure<CliRequests.LearnersOpResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：移除集群中学习者的方法
     */
    @Override
    public Future<Message> removeLearners(final Endpoint endpoint, final CliRequests.RemoveLearnersRequest request,
                                          final RpcResponseClosure<CliRequests.LearnersOpResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：重置集群学习者的方法
     */
    @Override
    public Future<Message> resetLearners(final Endpoint endpoint, final CliRequests.ResetLearnersRequest request,
                                         final RpcResponseClosure<CliRequests.LearnersOpResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群领导者的方法
     */
    @Override
    public Future<Message> getLeader(final Endpoint endpoint, final CliRequests.GetLeaderRequest request,
                                     final RpcResponseClosure<CliRequests.GetLeaderResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：转移集群领导者给其他节点的方法
     */
    @Override
    public Future<Message> transferLeader(final Endpoint endpoint, final CliRequests.TransferLeaderRequest request,
                                          final RpcResponseClosure<RpcRequests.ErrorResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群配置信息的方法
     */
    @Override
    public Future<Message> getPeers(final Endpoint endpoint, final CliRequests.GetPeersRequest request,
                                    final RpcResponseClosure<CliRequests.GetPeersResponse> done) {
        return invokeWithDone(endpoint, request, done, this.cliOptions.getTimeoutMs());
    }
}
