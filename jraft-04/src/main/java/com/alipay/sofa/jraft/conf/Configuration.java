package com.alipay.sofa.jraft.conf;

import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.util.Copiable;
import com.alipay.sofa.jraft.util.Requires;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:集群配置类，该配置类中封装着集群中所有节点的信息
 */
public class Configuration implements Iterable<PeerId>, Copiable<Configuration> {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    //该成员变量在该类的parse方法中被用到了，用来判断新添加进来的节点是否为一个学习者
    //学习者是raft协议中的一个角色，新添加到集群中的节点只需要从领导者那里同步数据即可，投票和决策都不参与
    private static final String LEARNER_POSTFIX = "/learner";
    //存放PeerId的集合
    private List<PeerId> peers = new ArrayList<>();

    //存放学习者的集合，这时候大家应该能意识到，实际上所有的节点信息，不管是正常的节点还是新添加到集群的学习者
    //这些节点的信息都是用PeerID对象封装的，只不过用不同的集合把它们的身份区分开了
    private LinkedHashSet<PeerId> learners = new LinkedHashSet<>();

    //下面都是几个简单的构造方法
    public Configuration() {
        super();
    }


    public Configuration(final Iterable<PeerId> conf) {
        this(conf, null);
    }


    public Configuration(final Configuration conf) {
        this(conf.getPeers(), conf.getLearners());
    }


    public Configuration(final Iterable<PeerId> conf, final Iterable<PeerId> learners) {
        Requires.requireNonNull(conf, "conf");
        for (final PeerId peer : conf) {
            this.peers.add(peer.copy());
        }
        addLearners(learners);
    }

    //下面这些方法从名字上就知道是做什么的，逻辑也很简单，就不添加注释了
    public void setLearners(final LinkedHashSet<PeerId> learners) {
        this.learners = learners;
    }


    public boolean addLearner(final PeerId learner) {
        return this.learners.add(learner);
    }


    public int addLearners(final Iterable<PeerId> learners) {
        int ret = 0;
        if (learners != null) {
            for (final PeerId peer : learners) {
                if (this.learners.add(peer.copy())) {
                    ret++;
                }
            }
        }
        return ret;
    }


    public boolean removeLearner(final PeerId learner) {
        return this.learners.remove(learner);
    }


    public LinkedHashSet<PeerId> getLearners() {
        return this.learners;
    }


    public List<PeerId> listLearners() {
        return new ArrayList<>(this.learners);
    }

    @Override
    public Configuration copy() {
        return new Configuration(this.peers, this.learners);
    }

    //该方法用来校验当前陪配置的有效性
    public boolean isValid() {
        //获得配置中所有的peerId，这里获得的是非学习者节点
        final Set<PeerId> intersection = new HashSet<>(this.peers);
        //求出学习者集合和非学习者集合的交集
        intersection.retainAll(this.learners);
        //如果有交集则配置无效，因为一个节点的身份不可能是学习者，还同时可以参与投票和日志决策
        return !this.peers.isEmpty() && intersection.isEmpty();
    }

    //重制集合的方法
    public void reset() {
        this.peers.clear();
        this.learners.clear();
    }

    public boolean isEmpty() {
        return this.peers.isEmpty();
    }

    public int size() {
        return this.peers.size();
    }

    @Override
    public Iterator<PeerId> iterator() {
        return this.peers.iterator();
    }

    public Set<PeerId> getPeerSet() {
        return new HashSet<>(this.peers);
    }

    public List<PeerId> listPeers() {
        return new ArrayList<>(this.peers);
    }

    public List<PeerId> getPeers() {
        return this.peers;
    }

    public void setPeers(final List<PeerId> peers) {
        this.peers.clear();
        for (final PeerId peer : peers) {
            this.peers.add(peer.copy());
        }
    }

    public void appendPeers(final Collection<PeerId> set) {
        this.peers.addAll(set);
    }

    public boolean addPeer(final PeerId peer) {
        return this.peers.add(peer);
    }

    public boolean removePeer(final PeerId peer) {
        return this.peers.remove(peer);
    }

    public boolean contains(final PeerId peer) {
        return this.peers.contains(peer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.learners == null) ? 0 : this.learners.hashCode());
        result = prime * result + ((this.peers == null) ? 0 : this.peers.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Configuration other = (Configuration) obj;
        if (this.learners == null) {
            if (other.learners != null) {
                return false;
            }
        } else if (!this.learners.equals(other.learners)) {
            return false;
        }
        if (this.peers == null) {
            return other.peers == null;
        } else {
            return this.peers.equals(other.peers);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final List<PeerId> peers = listPeers();
        int i = 0;
        int size = peers.size();
        for (final PeerId peer : peers) {
            sb.append(peer);
            if (i < size - 1 || !this.learners.isEmpty()) {
                sb.append(",");
            }
            i++;
        }
        size = this.learners.size();
        i = 0;
        for (final PeerId peer : this.learners) {
            sb.append(peer).append(LEARNER_POSTFIX);
            if (i < size - 1) {
                sb.append(",");
            }
            i++;
        }

        return sb.toString();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:该方法是本类中最重要的方法，就是在该方法中，会解析用户传递到main方法中的参数，也就是那一堆字符串
     * 就是这些数据：127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083
     */
    public boolean parse(final String conf) {
        //如果传入的字符串为空，直接返回false
        if (StringUtils.isBlank(conf)) {
            return false;
        }
        //重置两个集合的数据
        reset();
        //将长字符串解析为字符串数组
        final String[] peerStrs = StringUtils.split(conf, ',');
        //遍历数组，解析出每一个PeerId对象
        for (String peerStr : peerStrs) {
            //创建一个PeerId对象用来封装每一个节点的信息
            final PeerId peer = new PeerId();
            int index;
            //判断正在解析的节点是否为一个学习者，默认为false
            boolean isLearner = false;
            //如果当前解析的字符串中包含"/learner"，说明当前正在解析的这个节点信息来自于一个学习者
            if ((index = peerStr.indexOf(LEARNER_POSTFIX)) > 0) {
                //去掉"/learner"字符串，只保留ip端口号
                peerStr = peerStr.substring(0, index);
                //设置为true
                isLearner = true;
            }
            //走到这里，不管当前解析出来的节点信息是不是学习者，都会把节点信息封装到刚才创建出来的
            //PeerId对象中
            if (peer.parse(peerStr)) {
                //如果是学习者，就把这个PeerId对象添加到学习者集合中
                if (isLearner) {
                    addLearner(peer);
                } else {//否则添加到非学习者集合中
                    addPeer(peer);
                }
            } else {
                //解析失败记录日志
                LOG.error("Fail to parse peer {} in {}, ignore it.", peerStr, conf);
            }
        }
        return true;
    }


    //该方法用来计算当前配置类对象和另一个配置类对象的差集
    public void diff(final Configuration rhs, final Configuration included, final Configuration excluded) {
        included.peers = new ArrayList<>(this.peers);
        included.peers.removeAll(rhs.peers);
        excluded.peers = new ArrayList<>(rhs.peers);
        excluded.peers.removeAll(this.peers);
    }
}
