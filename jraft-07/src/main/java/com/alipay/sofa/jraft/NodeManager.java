package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.entity.NodeId;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.jraft.util.Utils;
import com.alipay.sofa.jraft.util.concurrent.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/22
 * @Description:节点管理器，这个管理器管理着当前服务器，或者说当前进程内所有的raft节点，这个类的具体功能暂时不必考虑，后面会在文章中为大家举例讲解
 */
public class NodeManager {

    //单例模式，把当前类的对象暴露出去
    private static final NodeManager INSTANCE = new NodeManager();

    //存储节点ID和节点的MaP
    private final ConcurrentMap<NodeId, Node> nodeMap = new ConcurrentHashMap<>();

    //存储集群ID和集群节点的Map
    private final ConcurrentMap<String, List<Node>> groupMap = new ConcurrentHashMap<>();

    //存放程序中所有节点的地址
    private final ConcurrentHashSet<Endpoint> addrSet = new ConcurrentHashSet<>();

    //把当前类对象暴露出去
    public static NodeManager getInstance() {
        return INSTANCE;
    }

    //判断传进来的这个RPC地址是否已经存在了
    public boolean serverExists(final Endpoint addr) {
        if (addr.getIp().equals(Utils.IP_ANY)) {
            return this.addrSet.contains(new Endpoint(Utils.IP_ANY, addr.getPort()));
        }//如果集合中包含这个对象，就说明这个地址添加到集合中了
        return this.addrSet.contains(addr);
    }

    //从addrSet集合移除一个地址
    public boolean removeAddress(final Endpoint addr) {
        return this.addrSet.remove(addr);
    }

    //向addrSet集合中添加节点的地址
    public void addAddress(final Endpoint addr) {
        this.addrSet.add(addr);
    }

    //添加节点的方法
    public boolean add(final Node node) {
        //判断当前节点的IP地址和端口号是否已经添加到集合中
        //没有的话直接返回false
        if (!serverExists(node.getNodeId().getPeerId().getEndpoint())) {
            return false;
        }
        //得到当前节点的ID
        final NodeId nodeId = node.getNodeId();
        //将当前节点添加到nodeMap中，如果nodeId已经存在Map中了，该方法就会返回nodeId，否则返回null
        if (this.nodeMap.putIfAbsent(nodeId, node) == null) {
            //走到这里说明Map中还没有存放当前节点呢，获得当前节点所在的组ID
            //我习惯把这个groupId当作集群ID，因为不管这个JVM进程中启动了多少节点
            //这些raft节点又分为几个不同的组，组和组之间的数据并没有交集呀，一个组就相当于一个小集群
            //所以我就在注释中也就写为集群ID了，大家知道就行
            final String groupId = node.getGroupId();
            //根据groupId获得存放该集群节点的List
            List<Node> nodes = this.groupMap.get(groupId);
            //如果list为null
            if (nodes == null) {
                //就创建一个线程安全的ArrayList来存放集群中的节点
                nodes = Collections.synchronizedList(new ArrayList<>());
                //把集合添加到Map中
                List<Node> existsNode = this.groupMap.putIfAbsent(groupId, nodes);
                //如果返回null，说明groupMap中还没有对应的value，如果不反回null，说明已经有了value
                if (existsNode != null) {
                    //这里直接获得Map中的value即可
                    nodes = existsNode;
                }
            }
            //把当前节点添加到集合中
            nodes.add(node);
            return true;
        }
        //走到这里意味着nodeMap中已经存在对应的节点了，直接返回即可
        return false;
    }


    //下面这些方法都很简单，就不再一一注释了
    public boolean remove(final Node node) {
        if (this.nodeMap.remove(node.getNodeId(), node)) {
            final List<Node> nodes = this.groupMap.get(node.getGroupId());
            if (nodes != null) {
                return nodes.remove(node);
            }
        }
        return false;
    }


    public Node get(final String groupId, final PeerId peerId) {
        return this.nodeMap.get(new NodeId(groupId, peerId));
    }


    public List<Node> getNodesByGroupId(final String groupId) {
        return this.groupMap.get(groupId);
    }


    public List<Node> getAllNodes() {
        return this.groupMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }


    private NodeManager() {
    }
}