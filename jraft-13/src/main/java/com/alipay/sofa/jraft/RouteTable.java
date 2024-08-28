package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.CliRequests;
import com.alipay.sofa.jraft.rpc.RpcRequests;
import com.alipay.sofa.jraft.util.Describer;
import com.alipay.sofa.jraft.util.Requires;
import com.google.protobuf.Message;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;



/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2024/7/7
 * @方法描述：大家可以把这个类的对象当成一个路由表对象，该对象内部存放了集群Id和对应的集群内部节点信息的数据，通过这个路由表
 * 程序可以很快定义集群的领导者，然后集群客户端就可以和领导者通信，这个路由表对象在nacos服务端发挥作用了，到时候大家就知道了
 */
public class RouteTable implements Describer {


    private static final Logger LOG = LoggerFactory.getLogger(RouteTable.class);

    //单例模式，把路由表对象暴露出去
    private static final RouteTable INSTANCE = new RouteTable();

    //集群信息映射表，key为groupId，value就是对应的集群内部的配置信息，也就是所有节点信息
    private final ConcurrentMap<String, GroupConf> groupConfTable = new ConcurrentHashMap<>();

    //得到路由表对象的方法
    public static RouteTable getInstance() {
        return INSTANCE;
    }




   /**
    * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
    * @author：陈清风扬，个人微信号：chenqingfengyangjj。
    * @date:2024/7/7
    * @方法描述：跟新路由表中指定集群的配置信息的方法
    */
    public boolean updateConfiguration(final String groupId, final Configuration conf) {

        //简单的判空操作
        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireNonNull(conf, "Null configuration");

        //根据集群Id创建对应的GroupConf对象，也就是value，这个GroupConf对象中会封装集群内的配置信息
        //除了所有节点的信息，还会单独记录集群领导者节点的信息
        final GroupConf gc = getOrCreateGroupConf(groupId);

        //上锁
        final StampedLock stampedLock = gc.stampedLock;
        final long stamp = stampedLock.writeLock();

        try {
            //更新配置信息
            gc.conf = conf;
            //如果集群领导者并不在最新的配置信息中，那就把GroupConf对象内部的领导者信息置为null
            if (gc.leader != null && !gc.conf.contains(gc.leader)) {
                gc.leader = null;
            }
        } finally {
            //释放锁
            stampedLock.unlockWrite(stamp);
        }
        //返回true，表示操作成功
        return true;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：伪集群Id创建对应的GroupConf对象的方法
     */
    private GroupConf getOrCreateGroupConf(final String groupId) {

        GroupConf gc = this.groupConfTable.get(groupId);
        if (gc == null) {
            gc = new GroupConf();
            final GroupConf old = this.groupConfTable.putIfAbsent(groupId, gc);
            if (old != null) {
                gc = old;
            }
        }
        return gc;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：根据集群ID更新集群对应的配置信息的方法，只不过这个方法使用的是配置字符串更新集群配置信息的
     */
    public boolean updateConfiguration(final String groupId, final String confStr) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireTrue(!StringUtils.isBlank(confStr), "Blank configuration");

        //创建一个Configuration对象
        final Configuration conf = new Configuration();
        //解析字符串，把新的配置信息封装到Configuration对象中
        if (conf.parse(confStr)) {
            //调用updateConfiguration方法真正更新集群配置信息
            return updateConfiguration(groupId, conf);
        } else {
            LOG.error("Fail to parse confStr: {}", confStr);
            return false;
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：获取对应集群领导者的方法
     */
    public PeerId selectLeader(final String groupId) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");

        //从集群对应的GroupConf对象中获得领导者
        final GroupConf gc = this.groupConfTable.get(groupId);

        if (gc == null) {
            return null;
        }
        final StampedLock stampedLock = gc.stampedLock;
        //尝试获得一个乐观锁，乐观锁的知识大家都了解吧？
        long stamp = stampedLock.tryOptimisticRead();
        //从GroupConf对象中获得记录的领导者
        PeerId leader = gc.leader;
        //这里还要对乐观锁校验一下，如果校验没通过，说明领导者节点发生变了，所以要重新获得领导者信息
        if (!stampedLock.validate(stamp)) {
            stamp = stampedLock.readLock();
            try {//在这里获得领导者最新信息
                leader = gc.leader;
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }
        return leader;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：更新集群领导者信息，这个方法逻辑很简单那，我就不重复注释了
     */
    public boolean updateLeader(final String groupId, final PeerId leader) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");

        if (leader != null) {
            Requires.requireTrue(!leader.isEmpty(), "Empty leader");
        }

        final GroupConf gc = getOrCreateGroupConf(groupId);
        final StampedLock stampedLock = gc.stampedLock;
        final long stamp = stampedLock.writeLock();
        try {
            gc.leader = leader;
        } finally {
            stampedLock.unlockWrite(stamp);
        }
        return true;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：更新集群领导者信息，这个方法和上一个方法的不同之处就在于，当前方法使用新的领导者字符串来更新集群领导者信息
     */
    public boolean updateLeader(final String groupId, final String leaderStr) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireTrue(!StringUtils.isBlank(leaderStr), "Blank leader");


        final PeerId leader = new PeerId();
        //解析字符串信息，把领导者信息封装在节点对象中
        if (leader.parse(leaderStr)) {
            //真正更新领导者信息
            return updateLeader(groupId, leader);
        } else {
            LOG.error("Fail to parse leaderStr: {}", leaderStr);
            return false;
        }
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：获取对应集群的配置信息的方法，因为GroupConf对象中也封装了对应集群的配置信息，该方法逻辑和selectLeader方法类似，就不重复添加注释了
     */
    public Configuration getConfiguration(final String groupId) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");


        final GroupConf gc = this.groupConfTable.get(groupId);
        if (gc == null) {
            return null;
        }

        final StampedLock stampedLock = gc.stampedLock;
        long stamp = stampedLock.tryOptimisticRead();
        Configuration conf = gc.conf;
        if (!stampedLock.validate(stamp)) {
            stamp = stampedLock.readLock();
            try {
                conf = gc.conf;
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }
        return conf;
    }


    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：这个方法是本类中非常重要的一个方法，该方法会重新获得指定集群的领导者节点的信息，然后把信息更新到路由表中，该方法的第一个方法参数就是集群客户端对象
     */
    public Status refreshLeader(final CliClientService cliClientService, final String groupId, final int timeoutMs)
            throws InterruptedException, TimeoutException {
        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireTrue(timeoutMs > 0, "Invalid timeout: " + timeoutMs);

        //先获得指定集群的配置信息
        final Configuration conf = getConfiguration(groupId);
        //判断配置信息是否存在
        if (conf == null) {
            return new Status(RaftError.ENOENT,
                    "Group %s is not registered in RouteTable, forgot to call updateConfiguration?", groupId);
        }
        final Status st = Status.OK();
        //构建GetLeaderRequest请求，这个请求会被集群节点的GetLeaderRequestProcessor处理器处理，其实就是返回节点记录的领导者信息
        final CliRequests.GetLeaderRequest.Builder rb = CliRequests.GetLeaderRequest.newBuilder();
        //设置集群Id
        rb.setGroupId(groupId);
        //构建请求
        final CliRequests.GetLeaderRequest request = rb.build();
        TimeoutException timeoutException = null;
        //遍历集群中的每一个节点
        for (final PeerId peer : conf) {
            //用集群客户端和遍历到的集群节点建立连接，并且根据连接建立结果设置操作状态结果
            if (!cliClientService.connect(peer.getEndpoint())) {
                if (st.isOk()) {
                    st.setError(-1, "Fail to init channel to %s", peer);
                } else {
                    final String savedMsg = st.getErrorMsg();
                    st.setError(-1, "%s, Fail to init channel to %s", savedMsg, peer);
                }
                continue;
            }
            //这里就是把刚才构建得得到领导者节点的请求发送给遍历到的节点
            final Future<Message> result = cliClientService.getLeader(peer.getEndpoint(), request, null);
            try {
                //限时等待结果
                final Message msg = result.get(timeoutMs, TimeUnit.MILLISECONDS);
                //判断接收到的是否为错误响应，并根据响应结果设置操作状态结果
                if (msg instanceof RpcRequests.ErrorResponse) {
                    if (st.isOk()) {
                        st.setError(-1, ((RpcRequests.ErrorResponse) msg).getErrorMsg());
                    } else {
                        final String savedMsg = st.getErrorMsg();
                        st.setError(-1, "%s, %s", savedMsg, ((RpcRequests.ErrorResponse) msg).getErrorMsg());
                    }
                } else {
                    //走到这里意味着成功获得了响应，响应中封装着集群领导者的信息
                    final CliRequests.GetLeaderResponse response = (CliRequests.GetLeaderResponse) msg;
                    //从响应中得到集群领导者的信息，然后更新路由表中具体的领导者信息
                    updateLeader(groupId, response.getLeaderId());
                    return Status.OK();
                }
            } catch (final TimeoutException e) {
                //走到这里就意味着出现异常了，接收超时异常结果
                timeoutException = e;
            } catch (final ExecutionException e) {
                if (st.isOk()) {
                    st.setError(-1, e.getMessage());
                } else {
                    final String savedMsg = st.getErrorMsg();
                    st.setError(-1, "%s, %s", savedMsg, e.getMessage());
                }
            }
        }
        if (timeoutException != null) {
            throw timeoutException;
        }
        return st;
    }




    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：刷新路由表中指定集群的配置信息的方法，该方法会从集群领导者获得最新的集群配置信息，然后把信息更新到路由表的map中
     */
    public Status refreshConfiguration(final CliClientService cliClientService, final String groupId, final int timeoutMs)
            throws InterruptedException, TimeoutException {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        Requires.requireTrue(timeoutMs > 0, "Invalid timeout: " + timeoutMs);

        //先从路由表中获得指定集群的配置信息
        final Configuration conf = getConfiguration(groupId);
        //对配置信息进行判空操作
        if (conf == null) {
            return new Status(RaftError.ENOENT,
                    "Group %s is not registered in RouteTable, forgot to call updateConfiguration?", groupId);
        }

        final Status st = Status.OK();
        //得到集群中领导者节点
        PeerId leaderId = selectLeader(groupId);
        //如果目前路由表中并没有集群领导者的信息
        if (leaderId == null) {
            //就在这里刷新路由表的领导者的信息，其实就是使用集群客户端访问集群领导者
            //获取领导者的最新信息，然后更新到路由表中
            refreshLeader(cliClientService, groupId, timeoutMs);
            //再次从更新过的路由表中得到集群领导者信息
            leaderId = selectLeader(groupId);
        }
        //如果还是得不到领导者信息，就返回错误执行状态
        if (leaderId == null) {
            st.setError(-1, "Fail to get leader of group %s", groupId);
            return st;
        }
        //使用客户端和领导者建立连接
        if (!cliClientService.connect(leaderId.getEndpoint())) {
            st.setError(-1, "Fail to init channel to %s", leaderId);
            return st;
        }
        //构建GetPeersRequest请求，这个请求会被领导者节点的GetPeersRequestProcessor处理器处理
        final CliRequests.GetPeersRequest.Builder rb = CliRequests.GetPeersRequest.newBuilder();
        rb.setGroupId(groupId);
        rb.setLeaderId(leaderId.toString());
        try {
            //发送请求并且同步等待结果
            final Message result = cliClientService.getPeers(leaderId.getEndpoint(), rb.build(), null).get(timeoutMs, TimeUnit.MILLISECONDS);
            //下面就是处理相应的具体操作了
            if (result instanceof CliRequests.GetPeersResponse) {
                final CliRequests.GetPeersResponse resp = (CliRequests.GetPeersResponse) result;
                //走到这里意味着响应没有问题，是成功响应，就从响应中获得领导者返回的集群最新配置
                final Configuration newConf = new Configuration();
                //把最新配置信息都封装到newConf对象中
                for (final String peerIdStr : resp.getPeersList()) {
                    final PeerId newPeer = new PeerId();
                    newPeer.parse(peerIdStr);
                    newConf.addPeer(newPeer);
                }
                //这里添加的是学习者信息，上面添加的是跟随者信息
                for (final String learnerIdStr : resp.getLearnersList()) {
                    final PeerId newLearner = new PeerId();
                    newLearner.parse(learnerIdStr);
                    newConf.addLearner(newLearner);
                }
                //判断新旧配置是否不同，如果不同意味着集群配置发生了变化，记录日志信息
                if (!conf.equals(newConf)) {
                    LOG.info("Configuration of replication group {} changed from {} to {}", groupId, conf, newConf);
                }
                //在这里把最新配置信息更新到路由表对象中了
                updateConfiguration(groupId, newConf);
            } else {
                //这里就是得到了错误相应
                final RpcRequests.ErrorResponse resp = (RpcRequests.ErrorResponse) result;
                st.setError(resp.getErrorCode(), resp.getErrorMsg());
            }
        } catch (final Exception e) {
            st.setError(-1, e.getMessage());
        }
        return st;
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：重置路由表的方法，其实就是清空路由表中的信息
     */
    public void reset() {
        this.groupConfTable.clear();
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：从路由表中移除指定的集群信息
     */
    public boolean removeGroup(final String groupId) {

        Requires.requireTrue(!StringUtils.isBlank(groupId), "Blank group id");
        return this.groupConfTable.remove(groupId) != null;
    }

    @Override
    public String toString() {
        return "RouteTable{" + "groupConfTable=" + groupConfTable + '}';
    }


    //路由表的构造方法
    private RouteTable() {
    }

    @Override
    public void describe(final Printer out) {
        out.println("RouteTable:")
                .print("  ")
                .println(toString());
    }



    /**
     * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
     * @author：陈清风扬，个人微信号：chenqingfengyangjj。
     * @date:2024/7/7
     * @方法描述：GroupConf内部类对象中封装了对应集群的配置信息，以及集群领导者信息
     */
    private static class GroupConf {

        //使用该对象可以获得乐观读锁、读锁、以及写锁
        private final StampedLock stampedLock = new StampedLock();


        private Configuration conf;

        private PeerId leader;

        @Override
        public String toString() {
            return "GroupConf{" + "conf=" + conf + ", leader=" + leader + '}';
        }
    }
}
