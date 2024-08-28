package com.alipay.sofa.jraft.core;

import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.JRaftException;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.CliRequests;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.util.Requires;
import com.alipay.sofa.jraft.util.Utils;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2024/7/7
 * @方法描述：集群客户端，这个类的对象就是用来和raft集群打交道的
 */
public class CliServiceImpl implements CliService {


    private static final Logger LOG = LoggerFactory.getLogger(CliServiceImpl.class);

    //客户端配置对象
    private CliOptions cliOptions;

    //集群客户端的真正实现类，这个类的对象可以和集群节点进行网络通信
    private CliClientService cliClientService;

    //初始化客户端的方法
    @Override
    public synchronized boolean init(final CliOptions opts) {
        //对配置对象判空
        Requires.requireNonNull(opts, "Null cli options");

        //判断cliClientService是否已经创建了，也就是说是否初始化过了
        if (this.cliClientService != null) {
            //如果初始化过就返回true
            return true;
        }

        this.cliOptions = opts;
        //创建cliClientService对象
        this.cliClientService = new CliClientServiceImpl();
        //初始化cliClientService对象
        return this.cliClientService.init(this.cliOptions);
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：关闭集群客户端的方法
     */
    @Override
    public synchronized void shutdown() {
        if (this.cliClientService == null) {
            return;
        }
        this.cliClientService.shutdown();
        this.cliClientService = null;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：记录集群配置变更信息的方法
     */
    private void recordConfigurationChange(final String groupId, final List<String> oldPeersList,
                                           final List<String> newPeersList) {
        //创建配置信息对象，这个配置信息对象封装旧的配置信息
        final Configuration oldConf = new Configuration();
        //把旧的配置信息存放到上面创建的oldConf对象中
        for (final String peerIdStr : oldPeersList) {
            final PeerId oldPeer = new PeerId();
            oldPeer.parse(peerIdStr);
            oldConf.addPeer(oldPeer);
        }
        //该配置对象封装的是新的配置信息
        final Configuration newConf = new Configuration();
        //把新的配置信息封装到newConf对象中
        for (final String peerIdStr : newPeersList) {
            final PeerId newPeer = new PeerId();
            newPeer.parse(peerIdStr);
            newConf.addPeer(newPeer);
        }//记录日志
        LOG.info("Configuration of replication group {} changed from {} to {}.", groupId, oldConf, newConf);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：检查领导者并且和集群领导者节点建立连接的方法
     */
    private Status checkLeaderAndConnect(final String groupId, final Configuration conf, final PeerId leaderId) {
        //根据集群Id，或者这个集群中的领导者节点
        final Status st = getLeader(groupId, conf, leaderId);
        //判断操作状态是否成功，如果成功意味着获取到了领导者
        //并且把领导者信息封装到了leaderId对象中
        if (!st.isOk()) {
            //如果操作结果失败，则返回失败结果
            return st;
        }
        //集群客户端与集群领导者建立连接
        if (!this.cliClientService.connect(leaderId.getEndpoint())) {
            return new Status(-1, "Fail to init channel to leader %s", leaderId);
        }
        //返回操作执行的结果状态
        return Status.OK();
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：向集群中添加新的节点
     */
    @Override
    public Status addPeer(final String groupId, final Configuration conf, final PeerId peer) {
        //判断集群Id是否为空
        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        //判断集群配置是否为空，其实就是向conf中添加新的节点
        Requires.requireNonNull(conf, "Null configuration");
        //peer就是要新添加的节点
        Requires.requireNonNull(peer, "Null peer");
        //创建一个节点对象，这个节点封装的就是集群领导者的节点信息
        final PeerId leaderId = new PeerId();
        //这里会通过checkLeaderAndConnect方法得到集群的领导者
        final Status st = checkLeaderAndConnect(groupId, conf, leaderId);
        //判断是否成功获得领导者了
        if (!st.isOk()) {
            return st;
        }
        //下面就是构建AddPeerRequest请求，这个请求会被领导者节点的AddPeerRequestProcessor处理器处理
        //之后就是领导者自己的事了，把新节点添加到集群中，然后把配置变更日志传输给集群中的其他节点
        final CliRequests.AddPeerRequest.Builder rb = CliRequests.AddPeerRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString())
                //把要添加的新节点设置到请求中
                .setPeerId(peer.toString());
        try {
            //使用cliClientService客户端对象把请求发送给集群的领导者
            final Message result = this.cliClientService.addPeer(leaderId.getEndpoint(), rb.build(), null).get();
            //下面就是处理响应的操作
            if (result instanceof CliRequests.AddPeerResponse) {
                final CliRequests.AddPeerResponse resp = (CliRequests.AddPeerResponse) result;
                //记录集群配置变更信息
                recordConfigurationChange(groupId, resp.getOldPeersList(), resp.getNewPeersList());
                //返回操作成功结果
                return Status.OK();
            } else {
                //这里就意味着响应不是AddPeerResponse，那就直接根据响应生成对应的结果状态
                return statusFromResponse(result);
            }

        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：根据响应生成结果状态的方法
     */
    private Status statusFromResponse(final Message result) {
        //把响应转换为错误响应
        final RpcRequests.ErrorResponse resp = (RpcRequests.ErrorResponse) result;
        //返回错误响应状态
        return new Status(resp.getErrorCode(), resp.getErrorMsg());
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：从集群中移除一个节点的方法，这个方法的逻辑和添加节点的逻辑类似，我就不再重复注释了
     */
    @Override
    public Status removePeer(final String groupId, final Configuration conf, final PeerId peer) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(conf, "Null configuration");
        Requires.requireNonNull(peer, "Null peer");
        Requires.requireTrue(!peer.isEmpty(), "Removing peer is blank");


        final PeerId leaderId = new PeerId();
        //得到集群领导者
        final Status st = checkLeaderAndConnect(groupId, conf, leaderId);
        if (!st.isOk()) {
            return st;
        }
        //构建RemovePeerRequest请求，这个请求会被领导者节点的RemovePeerRequestProcessor处理器处理
        final CliRequests.RemovePeerRequest.Builder rb = CliRequests.RemovePeerRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString())
                //设置要被溢出的节点信息
                .setPeerId(peer.toString());

        try {//下面就是客户端发送请求给集群领导者的操作
            final Message result = this.cliClientService.removePeer(leaderId.getEndpoint(), rb.build(), null).get();
            if (result instanceof CliRequests.RemovePeerResponse) {
                final CliRequests.RemovePeerResponse resp = (CliRequests.RemovePeerResponse) result;
                recordConfigurationChange(groupId, resp.getOldPeersList(), resp.getNewPeersList());
                return Status.OK();
            } else {
                return statusFromResponse(result);
            }
        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：更改集群配节点的方法，其实就是更改集群配置信息的方法，conf是旧的配置信息，newPeers是新的节点信息
     */
    @Override
    public Status changePeers(final String groupId, final Configuration conf, final Configuration newPeers) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(conf, "Null configuration");
        Requires.requireNonNull(newPeers, "Null new peers");


        final PeerId leaderId = new PeerId();
        //这里得到了集群中领导者节点
        final Status st = checkLeaderAndConnect(groupId, conf, leaderId);
        if (!st.isOk()) {
            return st;
        }
        //构建ChangePeersRequest请求，这个请求会被领导者节点的ChangePeersRequestProcessor处理器处理
        final CliRequests.ChangePeersRequest.Builder rb = CliRequests.ChangePeersRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString());
        //在这里把要添加到集群中的新节点都设置到请求中
        for (final PeerId peer : newPeers) {
            rb.addNewPeers(peer.toString());
        }
        try {
            //客户端把请求发送给集群领导者
            final Message result = this.cliClientService.changePeers(leaderId.getEndpoint(), rb.build(), null).get();
            if (result instanceof CliRequests.ChangePeersResponse) {
                final CliRequests.ChangePeersResponse resp = (CliRequests.ChangePeersResponse) result;
                recordConfigurationChange(groupId, resp.getOldPeersList(), resp.getNewPeersList());
                return Status.OK();
            } else {
                return statusFromResponse(result);
            }
        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：重置集群节点的方法
     */
    @Override
    public Status resetPeer(final String groupId, final PeerId peerId, final Configuration newPeers) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(peerId, "Null peerId");
        Requires.requireNonNull(newPeers, "Null new peers");

        //在这里和方法参数中的peerId建立连接，这个peerId节点就是领导者节点
        if (!this.cliClientService.connect(peerId.getEndpoint())) {
            return new Status(-1, "Fail to init channel to %s", peerId);
        }

        //构建ResetPeerRequest请求，这个请求会被领导者节点的ResetPeerRequestProcessor处理器处理
        final CliRequests.ResetPeerRequest.Builder rb = CliRequests.ResetPeerRequest.newBuilder()
                .setGroupId(groupId)
                .setPeerId(peerId.toString());
        //把新的节点配置信息设置到请求中
        for (final PeerId peer : newPeers) {
            rb.addNewPeers(peer.toString());
        }

        try {
            //把请求发送给集群领导者
            final Message result = this.cliClientService.resetPeer(peerId.getEndpoint(), rb.build(), null).get();
            return statusFromResponse(result);
        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：检查PeerId集合中的每一个节点是否为空
     */
    private void checkPeers(final Collection<PeerId> peers) {
        for (final PeerId peer : peers) {
            Requires.requireNonNull(peer, "Null peer in collection");
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：添加学习者节点到集群中的方法
     */
    @Override
    public Status addLearners(final String groupId, final Configuration conf, final List<PeerId> learners) {
        //检查要添加的学习者参数是否合规
        checkLearnersOpParams(groupId, conf, learners);
        final PeerId leaderId = new PeerId();
        //得到集群中的领导者
        final Status st = getLeader(groupId, conf, leaderId);
        if (!st.isOk()) {
            return st;
        }

        //客户端和集群领导者建立连接
        if (!this.cliClientService.connect(leaderId.getEndpoint())) {
            return new Status(-1, "Fail to init channel to leader %s", leaderId);
        }
        //构建AddLearnersRequest请求，在集群领导者中，这个请求会被AddLearnersRequestProcessor处理器请求
        final CliRequests.AddLearnersRequest.Builder rb = CliRequests.AddLearnersRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString());
        //把要添加的学习者节点设置到请求中
        for (final PeerId peer : learners) {
            rb.addLearners(peer.toString());
        }
        try {
            //把请求发送给集群领导者
            final Message result = this.cliClientService.addLearners(leaderId.getEndpoint(), rb.build(), null).get();
            //处理返回的结果响应
            return processLearnersOpResponse(groupId, result, "adding learners: %s", learners);

        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：检查要添加到集群中的学习者节点的参数是否合规
     */
    private void checkLearnersOpParams(final String groupId, final Configuration conf, final List<PeerId> learners) {
        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(conf, "Null configuration");
        Requires.requireTrue(learners != null && !learners.isEmpty(), "Empty peers");
        //判断每一个学习者节点是否不为空
        checkPeers(learners);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：处理和学习者有关操作的响应的方法，其实这个方法就是判断响应类型是否正确，如果正确则记录学习者变更操作日志
     */
    private Status processLearnersOpResponse(final String groupId, final Message result, final String fmt,
                                             final Object... formatArgs) {

        if (result instanceof CliRequests.LearnersOpResponse) {
            final CliRequests.LearnersOpResponse resp = (CliRequests.LearnersOpResponse) result;
            final Configuration oldConf = new Configuration();
            //下面就是得到旧的配置信息的操作
            for (final String peerIdStr : resp.getOldLearnersList()) {
                final PeerId oldPeer = new PeerId();
                oldPeer.parse(peerIdStr);
                oldConf.addLearner(oldPeer);
            }
            //下面就是得到新的配置信息的操作
            final Configuration newConf = new Configuration();
            for (final String peerIdStr : resp.getNewLearnersList()) {
                final PeerId newPeer = new PeerId();
                newPeer.parse(peerIdStr);
                newConf.addLearner(newPeer);
            }

            //记录集群中学习者变更的日志
            LOG.info("Learners of replication group {} changed from {} to {} after {}.", groupId, oldConf, newConf, String.format(fmt, formatArgs));
            return Status.OK();
        } else {
            return statusFromResponse(result);
        }
    }



   /**
    * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
    * @author：陈清风扬，个人微信号：chenqingfengyangjj。
    * @date:2024/7/7
    * @方法描述：从集群中移除学习者的方法
    */
    @Override
    public Status removeLearners(final String groupId, final Configuration conf, final List<PeerId> learners) {
        //检查学习者参数是否合法
        checkLearnersOpParams(groupId, conf, learners);

        final PeerId leaderId = new PeerId();
        //得到集群领导者
        final Status st = getLeader(groupId, conf, leaderId);
        if (!st.isOk()) {
            return st;
        }

        //客户端和集群领导者建立连接
        if (!this.cliClientService.connect(leaderId.getEndpoint())) {
            return new Status(-1, "Fail to init channel to leader %s", leaderId);
        }
        //构建RemoveLearnersRequest请求，这个请求会被领导者内部的RemoveLearnersRequestProcessor处理器处理
        final CliRequests.RemoveLearnersRequest.Builder rb = CliRequests.RemoveLearnersRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString());
        //把要移除的学习者添加到请求中
        for (final PeerId peer : learners) {
            rb.addLearners(peer.toString());
        }

        try {
            //发送请求给领导者
            final Message result = this.cliClientService.removeLearners(leaderId.getEndpoint(), rb.build(), null).get();
            //处理响应
            return processLearnersOpResponse(groupId, result, "removing learners: %s", learners);
        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：将学习者节点转换为跟随者节点的方法
     */
    @Override
    public Status learner2Follower(final String groupId, final Configuration conf, final PeerId learner) {
        //首先从集群中把学习者节点移除
        Status status = removeLearners(groupId, conf, Arrays.asList(learner));
        if (status.isOk()) {
            //如果移除操作执行成功，接下来就把这些节点添加到集群中，也就是让这些节点成为跟随者
            status = addPeer(groupId, conf, new PeerId(learner.getIp(), learner.getPort()));
        }
        return status;
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：重置集群中学习者结点的方法，这个方法和重置跟随者节点的逻辑类似，我就不添加详细注释了
     */
    @Override
    public Status resetLearners(final String groupId, final Configuration conf, final List<PeerId> learners) {

        checkLearnersOpParams(groupId, conf, learners);

        final PeerId leaderId = new PeerId();
        //得到集群中的领导者
        final Status st = getLeader(groupId, conf, leaderId);
        if (!st.isOk()) {
            return st;
        }

        //和集群领导者建立连接
        if (!this.cliClientService.connect(leaderId.getEndpoint())) {
            return new Status(-1, "Fail to init channel to leader %s", leaderId);
        }
        //构建ResetLearnersRequest请求，这个请求会被集群领导者的ResetLearnersRequestProcessor处理器处理
        final CliRequests.ResetLearnersRequest.Builder rb = CliRequests.ResetLearnersRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString());
        //把学习者添加到请求中
        for (final PeerId peer : learners) {
            rb.addLearners(peer.toString());
        }

        try {
            //把请求发送给领导者
            final Message result = this.cliClientService.resetLearners(leaderId.getEndpoint(), rb.build(), null).get();
            //处理响应
            return processLearnersOpResponse(groupId, result, "resetting learners: %s", learners);

        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：转移集群领导者的方法，就是把领导者转移到另一个节点，也就是方法参数中的peer节点
     */
    @Override
    public Status transferLeader(final String groupId, final Configuration conf, final PeerId peer) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(conf, "Null configuration");
        Requires.requireNonNull(peer, "Null peer");


        final PeerId leaderId = new PeerId();
        //得到集群领导者
        final Status st = checkLeaderAndConnect(groupId, conf, leaderId);
        if (!st.isOk()) {
            return st;
        }

        //构建TransferLeaderRequest请求，这个请求会被领导者内部的TransferLeaderRequestProcessor处理器处理
        final CliRequests.TransferLeaderRequest.Builder rb = CliRequests.TransferLeaderRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString());
        //把新的领导者节点信息设置到请求中
        if (!peer.isEmpty()) {
            rb.setPeerId(peer.toString());
        }

        try {
            final Message result = this.cliClientService.transferLeader(leaderId.getEndpoint(), rb.build(), null).get();
            return statusFromResponse(result);
        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }




    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：让集群领导者节点生成快照的方法
     */
    @Override
    public Status snapshot(final String groupId, final PeerId peer) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(peer, "Null peer");

        //和领导者建立连接
        if (!this.cliClientService.connect(peer.getEndpoint())) {
            return new Status(-1, "Fail to init channel to %s", peer);
        }

        //构建SnapshotRequest请求，这个请求会被领导者内部的SnapshotRequestProcessor处理器处理
        final CliRequests.SnapshotRequest.Builder rb = CliRequests.SnapshotRequest.newBuilder()
                .setGroupId(groupId)
                .setPeerId(peer.toString());

        try {
            //发送请求给节点
            final Message result = this.cliClientService.snapshot(peer.getEndpoint(), rb.build(), null).get();
            return statusFromResponse(result);
        } catch (final Exception e) {
            return new Status(-1, e.getMessage());
        }
    }



   /**
    * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
    * @author：陈清风扬，个人微信号：chenqingfengyangjj。
    * @date:2024/7/7
    * @方法描述：从集群中得到领导者的方法，这个方法是一个核心方法，因为客户端不管执行什么操作，都要知道集群领导者的信息，才能和领导者建立连接，然后通信
    */
    @Override
    public Status getLeader(final String groupId, final Configuration conf, final PeerId leaderId) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(leaderId, "Null leader id");
        if (conf == null || conf.isEmpty()) {
            return new Status(RaftError.EINVAL, "Empty group configuration");
        }

        //初始化一个操作状态，默认为失败状态
        final Status st = new Status(-1, "Fail to get leader of group %s", groupId);
        //conf封装了集群中所有节点的信息，接下来的操作就是遍历集群中所有的节点
        for (final PeerId peer : conf) {
            //客户端和遍历到的集群节点建立连接，看是否能连接成功，如果连接失败则跳过当前节点
            if (!this.cliClientService.connect(peer.getEndpoint())) {
                LOG.error("Fail to connect peer {} to get leader for group {}.", peer, groupId);
                continue;
            }

            //构建GetLeaderRequest请求，这个请求会被节点内部的GetLeaderRequestProcessor处理器处理，其实就是让接收到请求的节点返回自己记录的领导者节点信息
            final CliRequests.GetLeaderRequest.Builder rb = CliRequests.GetLeaderRequest.newBuilder()
                    .setGroupId(groupId)
                    .setPeerId(peer.toString());

            //发送请求给节点
            final Future<Message> result = this.cliClientService.getLeader(peer.getEndpoint(), rb.build(), null);
            try {
                //同步等待结果，超时时间在这里用上了
                final Message msg = result.get(this.cliOptions.getTimeoutMs() <= 0 ? this.cliOptions.getRpcDefaultTimeout() : this.cliOptions.getTimeoutMs(), TimeUnit.MILLISECONDS);
                //判断返回的响应是否是错误的
                if (msg instanceof RpcRequests.ErrorResponse) {
                    if (st.isOk()) {
                        st.setError(-1, ((RpcRequests.ErrorResponse) msg).getErrorMsg());
                    } else {
                        final String savedMsg = st.getErrorMsg();
                        st.setError(-1, "%s, %s", savedMsg, ((RpcRequests.ErrorResponse) msg).getErrorMsg());
                    }
                } else {
                    //走到这里就意味着相应没有问题，那就直接从响应中得到集群领导者的信息
                    final CliRequests.GetLeaderResponse response = (CliRequests.GetLeaderResponse) msg;
                    if (leaderId.parse(response.getLeaderId())) {
                        //在这里退出循环，不用再遍历下一个节点了
                        break;
                    }
                }
            } catch (final Exception e) {
                if (st.isOk()) {
                    st.setError(-1, e.getMessage());
                } else {
                    final String savedMsg = st.getErrorMsg();
                    st.setError(-1, "%s, %s", savedMsg, e.getMessage());
                }
            }
        }
        if (leaderId.isEmpty()) {
            return st;
        }
        //返回执行成功状态
        return Status.OK();
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群中所有节点信息
     */
    @Override
    public List<PeerId> getPeers(final String groupId, final Configuration conf) {
        return getPeers(groupId, conf, false, false);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群中所有存活的节点
     */
    @Override
    public List<PeerId> getAlivePeers(final String groupId, final Configuration conf) {
        return getPeers(groupId, conf, false, true);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群中的学习者节点
     */
    @Override
    public List<PeerId> getLearners(final String groupId, final Configuration conf) {
        return getPeers(groupId, conf, true, false);
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群中所有存活的学习者节点
     */
    @Override
    public List<PeerId> getAliveLearners(final String groupId, final Configuration conf) {
        return getPeers(groupId, conf, true, true);
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：重新平衡集群资源的方法，简单解释一下就是说假如说目前存在多个集群，这些集群中有的节点是多个集群的领导者，比如说现在有1，2，3，4，5，6个集群，有a，b，c，d四个节点，其中节点a是1，2，3，4这四个集群的领导者
     * 也就是说当前领导者节点负载比较大，这时候，就可以把节点a的领导者身份转移给其他节点一些，以使服务器的资源利用平衡一些。这时候大家可能会想，难道一个node的groupId不是唯一的吗？为什么一个节点会成为多个集群的领导者呢？
     * 这个我要给大家解释一下，现在我希望大家站在服务器的角度上思考问题，一个服务器上可以启动多个节点，这个大家肯定清楚吧，假如一个服务器上启动了3个节点，节点1是集群1的领导者，节点2是集群2的领导者，节点3是集群三的领导者
     * 这样一来，是不是一个服务器上存在三个领导者，并且三个领导者共享网络地址，这不就是一个过载的现象吗？也许我们可以把节点2的领导者转移到其他服务器同集群的节点内，以此来减轻当前服务器的负担
     * 那么就可以通过这个方法把领导者转移到集群中负载较小的节点上，这就是所谓的平衡资源的方法，第一个方法参数balanceGroupIds就是集群Id的集合，要平衡领导者们所在的集群信息就封装在这个balanceGroupIds对象中
     * 第二个参数封装了所有集群的所有节点信息，第三个参数封装的就是平衡之后的集群Id和领导者的映射
     */
    @Override
    public Status rebalance(final Set<String> balanceGroupIds, final Configuration conf,
                            final Map<String, PeerId> rebalancedLeaderIds) {

        //以下都是一些简单的判空操作，就不再重复注释了
        Requires.requireNonNull(balanceGroupIds, "Null balance group ids");
        Requires.requireTrue(!balanceGroupIds.isEmpty(), "Empty balance group ids");
        Requires.requireNonNull(conf, "Null configuration");
        Requires.requireTrue(!conf.isEmpty(), "No peers of configuration");

        //记录操作开始执行的日志信息
        LOG.info("Rebalance start with raft groups={}.", balanceGroupIds);

        //得到操作开始执行的时间
        final long start = Utils.monotonicMs();

        //定义一个局部变量，记录转移领导者的数量
        int transfers = 0;
        //定义一个局部变量，记录操作执行状态
        Status failedStatus = null;

        //把GroupId集合转换为队列
        final Queue<String> groupDeque = new ArrayDeque<>(balanceGroupIds);

        //这里创建了一个LeaderCounter对象，这个对象就是用来辅助计算集群领导者是否负载均衡的
        final LeaderCounter leaderCounter = new LeaderCounter(balanceGroupIds.size(), conf.size());

        //在一个死循环中处理所有集群的领导者，直到集群处理完毕为止
        for (;;) {
            //从队列中取出一个集群Id
            final String groupId = groupDeque.poll();
            //队列没有数据，就意味着处理完毕了，可以退出循环了
            if (groupId == null) {
                break;
            }

            //创建一个PeerId对象，用来封装集群领导者信息
            final PeerId leaderId = new PeerId();
            //根据集群Id得到集群领导者
            final Status leaderStatus = getLeader(groupId, conf, leaderId);
            //判断操作是否成功了
            if (!leaderStatus.isOk()) {
                failedStatus = leaderStatus;
                break;
            }

            //记录集群Id和对应领导者的映射关系，注意，这个时候还没有判断当前集群领导者是否过载了
            if (rebalancedLeaderIds != null) {
                rebalancedLeaderIds.put(groupId, leaderId);
            }

            //这里就是判断当前集群领导者是否过载的操作，这里的incrementAndGet方法的操作原理很简单，因为已经遍历到领导者了
            //所以给这个领导者节点自增一，意味着它是一个集群的领导者了，如果下一次发现它还是另外集群的领导者，就会再自增一
            //当然，自增完毕之后还要判断，这个领导者担任的领导者信息是否超过了预期的平均值，如果超过了就意味着它过载了
            //就要执行领导者转移操作
            if (leaderCounter.incrementAndGet(leaderId) <= leaderCounter.getExpectedAverage()) {
                continue;
            }

            //走到这里就意味着找到的领导者过载了，调用findTargetPeer方法，找到可以转移领导者的目标节点
            final PeerId targetPeer = findTargetPeer(leaderId, groupId, conf, leaderCounter);
            //对目标节点判空
            if (!targetPeer.isEmpty()) {
                //如果目标节点不为空，就直接转移领导者身份给目标节点
                final Status transferStatus = transferLeader(groupId, conf, targetPeer);
                //自增转移次数
                transfers++;
                //转移操作失败则退出循环
                if (!transferStatus.isOk()) {
                    failedStatus = transferStatus;
                    break;
                }

                //记录转移操作成功日志
                LOG.info("Group {} transfer leader to {}.", groupId, targetPeer);
                //更新当前领导者的领导数量
                leaderCounter.decrementAndGet(leaderId);
                //把集群Id添加到队列中，还要进行再次校验，只有校验通过的才可以从队列中移除
                groupDeque.add(groupId);
                //记录资源平衡之后的集群Id和领导者的映射关系
                if (rebalancedLeaderIds != null) {
                    rebalancedLeaderIds.put(groupId, targetPeer);
                }
            }
        }

        final Status status = failedStatus != null ? failedStatus : Status.OK();
        //记录操作日志，记录操作执行耗时
        if (LOG.isInfoEnabled()) {
            LOG.info(
                    "Rebalanced raft groups={}, status={}, number of transfers={}, elapsed time={} ms, rebalanced result={}.",
                    balanceGroupIds, status, transfers, Utils.monotonicMs() - start, rebalancedLeaderIds);
        }
        return status;
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：找到可以转移领导者的目标节点
     */
    private PeerId findTargetPeer(final PeerId self, final String groupId, final Configuration conf,
                                  final LeaderCounter leaderCounter) {
        //得到集群中所有存活的节点
        for (final PeerId peerId : getAlivePeers(groupId, conf)) {
            //如果当前节点是自己，就跳过循环
            if (peerId.equals(self)) {
                continue;
            }
            //看看遍历到的这个节点的领导数量是否超过了预期值，如果超过了就继续遍历下一个节点
            if (leaderCounter.get(peerId) >= leaderCounter.getExpectedAverage()) {
                continue;
            }
            //没超过则意味着找到了目标节点
            return peerId;
        }
        //如果找了一圈没找到就返回一个空节点
        return PeerId.emptyPeer();
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群中所有节点的方法
     */
    private List<PeerId> getPeers(final String groupId, final Configuration conf, final boolean returnLearners, final boolean onlyGetAlive) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(conf, "Null conf");


        final PeerId leaderId = new PeerId();
        //得到集群的领导者节点
        final Status st = getLeader(groupId, conf, leaderId);
        if (!st.isOk()) {
            throw new IllegalStateException(st.getErrorMsg());
        }

        //和领导者节点建立连接
        if (!this.cliClientService.connect(leaderId.getEndpoint())) {
            throw new IllegalStateException("Fail to init channel to leader " + leaderId);
        }

        //构建GetPeersRequest请求，这个请求会被领导者内部的GetPeersRequestProcessor处理器处理
        final CliRequests.GetPeersRequest.Builder rb = CliRequests.GetPeersRequest.newBuilder()
                .setGroupId(groupId)
                .setLeaderId(leaderId.toString())
                //这个参数是用来定义是否只返回存活的节点
                .setOnlyAlive(onlyGetAlive);

        try {
            //发送请求并且同步等待获取到结果
            final Message result = this.cliClientService.getPeers(leaderId.getEndpoint(), rb.build(), null).get(this.cliOptions.getTimeoutMs() <= 0 ? this.cliOptions.getRpcDefaultTimeout()
                            : this.cliOptions.getTimeoutMs(), TimeUnit.MILLISECONDS);

            //下面就是处理相应的操作了
            if (result instanceof CliRequests.GetPeersResponse) {
                final CliRequests.GetPeersResponse resp = (CliRequests.GetPeersResponse) result;
                //从响应中得到节点信息，具体步骤就不详细注释了，非常简单，大家自己看看就成
                final List<PeerId> peerIdList = new ArrayList<>();
                final ProtocolStringList responsePeers = returnLearners ? resp.getLearnersList() : resp.getPeersList();
                for (final String peerIdStr : responsePeers) {
                    final PeerId newPeer = new PeerId();
                    newPeer.parse(peerIdStr);
                    peerIdList.add(newPeer);
                }
                return peerIdList;
            } else {
                final RpcRequests.ErrorResponse resp = (RpcRequests.ErrorResponse) result;
                throw new JRaftException(resp.getErrorMsg());
            }
        } catch (final JRaftException e) {
            throw e;
        } catch (final Exception e) {
            throw new JRaftException(e);
        }
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：得到集群客户端的方法
     */
    public CliClientService getCliClientService() {
        // 返回当前实例的cliClientService对象。
        return this.cliClientService;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：该类的对象就是一个领导数量计数器
     */
    private static class LeaderCounter {

        //节点对应的领导数量
        private final Map<PeerId, Integer> counter = new HashMap<>();

        //期望的平均领导值
        private final int expectedAverage;

        //构造方法，在构造方法中计算了期望的平均领导值
        public LeaderCounter(final int groupCount, final int peerCount) {
            this.expectedAverage = (int) Math.ceil((double) groupCount / peerCount);
        }

        //以下方法都很简单，都是纯粹的计数方法，大家自己看看就成

        public int getExpectedAverage() {

            return this.expectedAverage;
        }


        public int incrementAndGet(final PeerId peerId) {
            return this.counter.compute(peerId, (ignored, num) -> num == null ? 1 : num + 1);
        }


        public int decrementAndGet(final PeerId peerId) {

            return this.counter.compute(peerId, (ignored, num) -> num == null ? 0 : num - 1);
        }


        public int get(final PeerId peerId) {
            return this.counter.getOrDefault(peerId, 0);
        }
    }
}
