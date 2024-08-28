package com.alipay.sofa.jraft.entity;

import com.alipay.sofa.jraft.conf.Configuration;

import java.util.ArrayList;
import java.util.List;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/21
 * @Description:计算投票结果的类，整个类的原理实际上是做减法，就是得到集群中所有参与选举的节点
 * 然后计算出最少需要的票数，每得到一票就从需要得到的票数中减一，直到减为0，就说明获得足够的票数了
 * 比如说一个集群中有5个节点，那么当前节点至少需要3票就可以成功当选领导者了，所以没得到1票就让3减1，直到结果为0，就说明可以成功当选了
 */
public class Ballot {

    //该内部类对象是用来表示peerId在集合中未知的，方便快速查找
    public static final class PosHint {
        //这个成员变量表示节点在peers集合中的位置
        //初始化为-1
        int pos0 = -1;
        //如果一个节点是旧配置中的，那么就用下面这个成员变量代表
        //其在oldPeers中的位置
        int pos1 = -1;
    }

    //这个内部类的对象会封装一个个节点，并且给这个节点添加一个未找到的标记
    //然后把这个节点存放到集合中，如果节点是属于当前配置中的就存放奥peers集合中
    //否则存放到oldPeers集合中
    //等当前节点收到集合中存放的节点的投票时，会去集合中查找，看看有没有对应的节点
    //如果有就把该节点标记为已找到，并且把投票数计入结果，说明成功收到投票了
    public static class UnfoundPeerId {
        //节点信息
        PeerId peerId;
        //节点是否被找到，UnfoundPeerId对象刚创建的时候
        //默认这个成员变量为false，代表着该节点对象还没被找到
        boolean found;
        //节点索引
        int index;

        //构造方法
        public UnfoundPeerId(PeerId peerId, int index, boolean found) {
            super();
            this.peerId = peerId;
            this.index = index;
            this.found = found;
        }
    }

    //存放当前配置中的节点信息
    private final List<UnfoundPeerId> peers = new ArrayList<>();
    //当前配置中需要收到的最小票数，才能成功当选领导者或者进入正式投票节点
    private int quorum;
    //存放旧配置中的节点信息
    private final List<UnfoundPeerId> oldPeers = new ArrayList<>();
    //旧配置中需要收到的最小票数，才能成功当选领导者或者进入正式投票节点
    private int oldQuorum;


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/24
     * @Description:该类最核心的方法，初始化该类的对象时，会把集群中的节点信息封装到该类中，并且根据集群中
     * 节点数量计算出当前节点至少需要收到多少票数，才能成功当选领导者或者进入正式投票阶段
     */
    public boolean init(final Configuration conf, final Configuration oldConf) {
        //先清空两个集合
        this.peers.clear();
        this.oldPeers.clear();
        //需要的最小票数也清零
        this.quorum = this.oldQuorum = 0;
        //定义节点索引，该索引会在创建UnfoundPeerId对象时一直递增，当作每一个节点的索引
        int index = 0;
        //将当前配置中的节点信息存放到集合中
        if (conf != null) {
            for (final PeerId peer : conf) {
                this.peers.add(new UnfoundPeerId(peer, index++, false));
            }
        }
        //计算出最少需要的票数才能成功当选或者进入正式投票阶段
        //这里的成员变量是针对当前配置的
        this.quorum = this.peers.size() / 2 + 1;
        //下面判断旧配置是否为空
        if (oldConf == null) {
            return true;
        }
        index = 0;
        //如果旧配置不为空，则执行和上面相同的逻辑
        for (final PeerId peer : oldConf) {
            this.oldPeers.add(new UnfoundPeerId(peer, index++, false));
        }
        this.oldQuorum = this.oldPeers.size() / 2 + 1;
        return true;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/24
     * @Description:根据给订的PeerId，判断集合中是否存在对应节点
     */
    private UnfoundPeerId findPeer(final PeerId peerId, final List<UnfoundPeerId> peers, final int posHint) {
        //这里先判断了一下代表节点在集合中的位置是否是正确的，当一个节点被包装成UnfoundPeerId对象的时候，它在集合中的位置是-1
        //显然是不合理的，这种情况下应该遍历集合，根据对应的peerId找出对应节点，然后根据节点的index来更新自己在集合中的位置
        //也就是更新PosHint对象中的索引
        if (posHint < 0 || posHint >= peers.size() || !peers.get(posHint).peerId.equals(peerId)) {
            for (final UnfoundPeerId ufp : peers) {
                if (ufp.peerId.equals(peerId)) {
                    return ufp;
                }
            }
            return null;
        }
        //如果走到这里说明节点在集合中的位置已经更新了，直接根据位置获得节点即可
        return peers.get(posHint);
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/24
     * @Description:给当前节点投票的方法
     */
    public PosHint grant(final PeerId peerId, final PosHint hint) {
        //在peers集合中查找对应的节点
        UnfoundPeerId peer = findPeer(peerId, this.peers, hint.pos0);
        if (peer != null) {
            //找到的话意味着方法参数中的那个peerId节点的投票是有效的
            //这个peerId在集群中
            if (!peer.found) {
                //将这个节点标记为已找到，或者说是被发现
                peer.found = true;
                //需要的票数减1，减到0则意味着票数够了
                this.quorum--;
            }
            //更新节点在集合中的位置
            hint.pos0 = peer.index;
        } else {
            //如果没找到，就把位置设置为-1
            hint.pos0 = -1;
        }
        //判断一下旧配置的节点集合是否为空
        if (this.oldPeers.isEmpty()) {
            hint.pos1 = -1;
            return hint;
        }
        //如果不为空，就执行和上面相同的逻辑，这里的当前配置和旧配置先不必关心
        //后面将配置变更的时候，会在文章里详细讲解
        peer = findPeer(peerId, this.oldPeers, hint.pos1);
        if (peer != null) {
            if (!peer.found) {
                peer.found = true;
                this.oldQuorum--;
            }
            hint.pos1 = peer.index;
        } else {
            hint.pos1 = -1;
        }
        return hint;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/24
     * @Description:给当前节点投票的方法，方法中传进来的这个peerId，就是给当前节点投票的节点
     */
    public void grant(final PeerId peerId) {
        //在下面这个方法中，真正执行投票的逻辑
        //注意，这里创建了一个PosHint对象，这个对象的作用
        //是用来表示peerId在集合中未知的，方便快速查找
        grant(peerId, new PosHint());
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/24
     * @Description:该方法就是用来判断是否收到足够的票数了，因为没收到一票quorum就会减1，直到为0则意味着收到足够票数了
     */
    public boolean isGranted() {
        return this.quorum <= 0 && this.oldQuorum <= 0;
    }
}