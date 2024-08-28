package com.test;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.storage.impl.RocksDBLogStorage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import static com.alipay.sofa.jraft.core.State.STATE_FOLLOWER;
import static com.alipay.sofa.jraft.core.State.STATE_LEADER;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:测试类
 */
public class ServerTest {

    private RaftGroupService raftGroupService;

    public ServerTest(final String dataPath, final String groupId, final PeerId serverId,
                      final NodeOptions nodeOptions) throws IOException {

        //根据用户配置的本地路径创建文件夹
        FileUtils.forceMkdir(new File(dataPath));
        //创建RPC服务器
        final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        //设置日志存储文件的本地路径
        nodeOptions.setLogUri(dataPath + File.separator + "log");
        //设置元数据存储文件的本地路径
        nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
        //创建集群服务对象并赋值
        this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);

    }


    public static void main(String[] args) throws InterruptedException, IOException {

        if (args.length != 4) {
            //如果用户没有传入正确的参数，或者传入的参数不足，就在控制台输出需要的参数形式
            System.out.println("Usage : java com.alipay.sofa.jraft.example.counter.CounterServer {dataPath} {groupId} {serverId} {initConf}");
            System.out.println("Example: java com.alipay.sofa.jraft.example.counter.CounterServer /tmp/server1 counter 127.0.0.1:8081 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083");
            //然后退出程序
            System.exit(1);
        }
        //解析命令行中传进来的字符串
        //解析出文件存放的本地路径
        final String dataPath = args[0];
        //要启动的集群服务的名字
        final String groupId = args[1];
        //当前节点的IP地址和端口号
        final String serverIdStr = args[2];
        //集群中所有节点的IP地址和端口号
        final String initConfStr = args[3];

        //创建的这个对象封装着当前节点需要的配置参数
        final NodeOptions nodeOptions = new NodeOptions();
        //设置超时选举时间，超过这个时间没有接收到领导者发送的信息，就会触发新一轮选举
        nodeOptions.setElectionTimeoutMs(1000);
        //不禁用集群客户端
        nodeOptions.setDisableCli(false);
        //创建的PeerId对象是用来封装代表当前服务器节点的信息的
        final PeerId serverId = new PeerId();
        //把当前节点的信息，比如IP地址，端口号等等都解析道PeerId对象中
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("Fail to parse serverId:" + serverIdStr);
        }
        //创建集群配置类对象，这个对象中封装着整个集群中的节点信息
        final Configuration initConf = new Configuration();
        //把上面得到的字符串信息解析到该对象中，这时候配置类对象就拥有了集群中所有节点的信息
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("Fail to parse initConf:" + initConfStr);
        }
        nodeOptions.setInitialConf(initConf);
        //启动当前节点以及集群服务
        final ServerTest counterServer = new ServerTest(dataPath, groupId, serverId, nodeOptions);
        Node node = counterServer.raftGroupService.start();


        Thread.sleep(4000);

        //产生一条日志，测试落盘效果,这是集群中第一条日志，所以索引为1
        if (STATE_LEADER == node.getNodeState()){
            //日志内容为hello
            for (int i = 0; i <10 ; i++) {
                final ByteBuffer data = ByteBuffer.wrap(("hello").getBytes());
                CountDownLatch latch = new CountDownLatch(10);
                final Task task = new Task(data, new ExpectClosure(latch));
                node.apply(task);
            }
            System.out.println("开始向硬盘存储日志！！！！！");
        }

        Thread.sleep(3000);

        while (true) {
            if (STATE_FOLLOWER == node.getNodeState()){
                try {
                    RocksDBLogStorage storage = (RocksDBLogStorage)node.getLogStorage();
                    //根据日志索引从数据库把日志取出来
                    LogEntry logEntry = storage.getEntry(1);
                    System.out.println("跟随者节点中数据库中索引为1的日志的内容为"+Charset.defaultCharset().decode(logEntry.getData()).toString());
                } catch (Exception e) {

                }
            }
        }


        //Thread.sleep(600000);

    }
}
