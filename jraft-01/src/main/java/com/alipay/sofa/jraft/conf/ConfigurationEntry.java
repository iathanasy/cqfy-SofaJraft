package com.alipay.sofa.jraft.conf;

import com.alipay.sofa.jraft.entity.LogId;
import com.alipay.sofa.jraft.entity.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/20
 * @Description:该类的对象封装的是当前集群的配置信息，这个配置分为新配置和就配置，因为集群的配置也是有可能变更的
 * 这个类在第一版本其实没有发挥什么作用，因为我们也没有对集群进行配置变更，这是后面版本代码要展示的内容
 * 所以，在第一个版本，大家就简单看看这个类即可
 */
public class ConfigurationEntry {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationEntry.class);

    //当前类对象的ID，其实就是日志id，因为集群变更也要作为日志复制给各个跟随者节点
    private LogId id = new LogId(0, 0);
    //当前生效的配置
    private Configuration conf = new Configuration();
    //旧的配置
    private Configuration oldConf = new Configuration();


    public LogId getId() {
        return this.id;
    }


    public void setId(final LogId id) {
        this.id = id;
    }


    public Configuration getConf() {
        return this.conf;
    }


    public void setConf(final Configuration conf) {
        this.conf = conf;
    }


    public Configuration getOldConf() {
        return this.oldConf;
    }


    public void setOldConf(final Configuration oldConf) {
        this.oldConf = oldConf;
    }


    public ConfigurationEntry() {
        super();
    }


    public ConfigurationEntry(final LogId id, final Configuration conf, final Configuration oldConf) {
        super();
        this.id = id;
        this.conf = conf;
        this.oldConf = oldConf;
    }

    //判断旧的配置是否为空，如果旧配置为空，说明当前集群的配置没有进行过变更，也就代表当前集群是很稳定的
    public boolean isStable() {
        return this.oldConf.isEmpty();
    }

    //判断当前配置是否为空
    public boolean isEmpty() {
        return this.conf.isEmpty();
    }

    //把当前配置和就配置中的所有PeerId以集合的形式返回
    public Set<PeerId> listPeers() {
        final Set<PeerId> ret = new HashSet<>(this.conf.listPeers());
        ret.addAll(this.oldConf.listPeers());
        return ret;
    }


    //判断当前的配置是否有效，在这个方法中引入了一个raft协议中的新角色，那就是Learner
    //在sofajraft中，当集群配置有变更时，比如添加新的节点时，新的节点身份默认就是learner
    //这些节点并不参加选举，也不进行决策投票，会先从领导者同步数据
    //如果发现PeerId集合中的数据和Learner集合中的数据产生交集了，那么配置显然就是无效的
    //PeerId是行为正常的节点，可以参与投票和决策
    public boolean isValid() {
        if (!this.conf.isValid()) {
            return false;
        }

        final Set<PeerId> intersection = listPeers();
        intersection.retainAll(listLearners());
        if (intersection.isEmpty()) {
            return true;
        }
        LOG.error("Invalid conf entry {}, peers and learners have intersection: {}.", this, intersection);
        return false;
    }

    //把当前配置和就配置中的所有Learner以集合的形式返回
    public Set<PeerId> listLearners() {
        final Set<PeerId> ret = new HashSet<>(this.conf.getLearners());
        ret.addAll(this.oldConf.getLearners());
        return ret;
    }

    //判断某个learner是否存在于当前配置或旧配置中
    public boolean containsLearner(final PeerId learner) {
        return this.conf.getLearners().contains(learner) || this.oldConf.getLearners().contains(learner);
    }

    //判断某个PeerId是否存在于当前配置或旧配置中
    public boolean contains(final PeerId peer) {
        return this.conf.contains(peer) || this.oldConf.contains(peer);
    }


    @Override
    public String toString() {
        return "ConfigurationEntry [id=" + this.id + ", conf=" + this.conf + ", oldConf=" + this.oldConf + "]";
    }
}
