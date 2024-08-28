package com.alipay.sofa.jraft.entity;

import com.alipay.sofa.jraft.core.ElectionPriority;
import com.alipay.sofa.jraft.util.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/11/25
 * @Description:这个类就是用来解析当前节点信息的，当前节点的IP地址和端口号，节点参与选举的优先级，这些信息都会封装在该类的对象中
 */
public class PeerId implements Copiable<PeerId>, Serializable, Checksum {

    private static final long serialVersionUID = 8083529734784884641L;

    private static final Logger LOG = LoggerFactory.getLogger(PeerId.class);

    //当前节点的IP地址和端口号就封装在这个成员变量对象中
    private Endpoint endpoint = new Endpoint(Utils.IP_ANY, 0);
    //这个是在同样的IP地址下，用来区分集群中不同节点的
    //比如在一个容器中有三个节点，三个节点使用的IP地址是相同的，这时候就可以通过这个idx来区分这三个节点了
    //比如IP地址和端口号都是localhost:8080
    //idx分别为1，2，3这样就区分开了
    private int idx;
    //该类对象的toString结果的缓存
    private String str;
    //节点初始化的时候，默认选举优先级功能是禁止的
    private int priority = ElectionPriority.Disabled;
    //这个成员变量起到一个标识符的作用，具体使用的时候我会为大家讲解的
    public static final PeerId ANY_PEER = new PeerId();

    //校验两个PeerId对象是否相等的成员变量
    private long checksum;

    public PeerId() {
        super();
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:计算当前PeerId对象的校验和，对比两个PeerId对象的校验和，就可以判断这两个对象是否相等，在LogEntry这个类中，也会有这个方法
     * LogEntry类的对象封装了日志信息，当进行日志复制的时候，会对日志的内容进行判断，这个是后面的知识了，第三版本代码会为大家详细讲解
     */
    @Override
    public long checksum() {
        if (this.checksum == 0) {
            this.checksum = CrcUtil.crc64(AsciiStringUtil.unsafeEncode(toString()));
        }
        return this.checksum;
    }

    //创建一个空节点对象
    public static PeerId emptyPeer() {
        return new PeerId();
    }

    //深拷贝方法
    @Override
    public PeerId copy() {
        return new PeerId(this.endpoint.copy(), this.idx, this.priority);
    }


    //根据给定的字符串解析并创建一个PeerId对象
    public static PeerId parsePeer(final String s) {
        final PeerId peer = new PeerId();
        if (peer.parse(s)) {
            return peer;
        }
        return null;
    }

    public PeerId(final Endpoint endpoint, final int idx) {
        super();
        this.endpoint = endpoint;
        this.idx = idx;
    }

    public PeerId(final String ip, final int port) {
        this(ip, port, 0);
    }

    public PeerId(final String ip, final int port, final int idx) {
        super();
        this.endpoint = new Endpoint(ip, port);
        this.idx = idx;
    }

    public PeerId(final Endpoint endpoint, final int idx, final int priority) {
        super();
        this.endpoint = endpoint;
        this.idx = idx;
        this.priority = priority;
    }

    public PeerId(final String ip, final int port, final int idx, final int priority) {
        super();
        this.endpoint = new Endpoint(ip, port);
        this.idx = idx;
        this.priority = priority;
    }

    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    public String getIp() {
        return this.endpoint.getIp();
    }

    public int getPort() {
        return this.endpoint.getPort();
    }

    public int getIdx() {
        return this.idx;
    }

    public int getPriority() {
        return priority;
    }

    //设置当前节点选举优先级
    public void setPriority(int priority) {
        this.priority = priority;
        this.str = null;
    }

    //当前节点信息对象是否为空
    public boolean isEmpty() {
        return getIp().equals(Utils.IP_ANY) && getPort() == 0 && this.idx == 0;
    }

    @Override
    public String toString() {
        if (this.str == null) {
            final StringBuilder buf = new StringBuilder(this.endpoint.toString());

            if (this.idx != 0) {
                buf.append(':').append(this.idx);
            }

            if (this.priority != ElectionPriority.Disabled) {
                if (this.idx == 0) {
                    buf.append(':');
                }
                buf.append(':').append(this.priority);
            }

            this.str = buf.toString();
        }
        return this.str;
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/11/20
     * @Description:该方法是本类最核心的方法，将传入进来的字符串解析为一个PeerId对象
     */
    public boolean parse(final String s) {
        //输入的字符串不能为空，否则直接返回
        if (StringUtils.isEmpty(s)) {
            return false;
        }
        //注意，这里传进来的字符串的形式是IP地址，端口号，idx，优先级
        //默认是按照这种形式来解析字符串的
        //但是，我在测试类中传进来的都是 127.0.0.1:8081 这样的地址，并没有idx和当前节点的优先级
        //这里大家要弄清楚，idx和优先级都是可以被配置的，只不过我并没有设置而已
        //下面就是把字符串解析成字符串数组了
        final String[] tmps = Utils.parsePeerId(s);
        //如果字符串数组的长度小于2或者大于4则返回false
        //小于二则说明用户配置的节点信息不完整，最少要有IP地址和端口号吧，所以长度最小为2
        //如果idx和优先级也都配置了，那么最大长度就为4，大于4显然也不正确
        if (tmps.length < 2 || tmps.length > 4) {
            return false;
        }
        try {
            //把端口号转化为int
            final int port = Integer.parseInt(tmps[1]);
            //创建Endpoint对象封装IP地址和端口号信息
            this.endpoint = new Endpoint(tmps[0], port);
            //开始判断数组的长度了
            switch (tmps.length) {
                case 3:
                    //如果数组长度为3，说明用户配置了idx，给idx赋值即可
                    this.idx = Integer.parseInt(tmps[2]);
                    break;
                case 4:
                    //为4则说明优先级和idx都被配置了，先判断了一下idx是否为空
                    if (tmps[2].equals("")) {
                        //为空则赋值为0
                        this.idx = 0;
                    } else {
                        //不为空则直接给idx赋值
                        this.idx = Integer.parseInt(tmps[2]);
                    }
                    //给优先级赋值
                    this.priority = Integer.parseInt(tmps[3]);
                    break;
                default:
                    break;
            }
            this.str = null;
            return true;
        } catch (final Exception e) {
            LOG.error("Parse peer from string failed: {}.", s, e);
            return false;
        }
    }

    //判断当前节点是否会不会参加选举
    public boolean isPriorityNotElected() {
        return this.priority == ElectionPriority.NotElected;
    }

    //判断当前节点是否禁用了选举优先级功能
    //返回true表示禁用该功能
    //返回false表示启用了该功能
    public boolean isPriorityDisabled() {
        return this.priority <= ElectionPriority.Disabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.endpoint == null ? 0 : this.endpoint.hashCode());
        result = prime * result + this.idx;
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
        final PeerId other = (PeerId) obj;
        if (this.endpoint == null) {
            if (other.endpoint != null) {
                return false;
            }
        } else if (!this.endpoint.equals(other.endpoint)) {
            return false;
        }
        return this.idx == other.idx;
    }
}
