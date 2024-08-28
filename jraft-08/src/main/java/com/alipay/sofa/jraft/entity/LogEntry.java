package com.alipay.sofa.jraft.entity;

import com.alipay.sofa.jraft.util.CrcUtil;

import java.nio.ByteBuffer;
import java.util.List;


/**
 * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
 * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
 * @Date:2023/12/1
 * @Description:该类的对象就是一个日志条目
 */
public class LogEntry implements Checksum {

    public static final ByteBuffer EMPTY_DATA = ByteBuffer.wrap(new byte[0]);

    //日志类型，可能是业务日志，也可能是配置变更日志
    private EnumOutter.EntryType type;
    //日志ID，注意，日志ID中封装着日志的索引和任期
    private LogId id = new LogId(0, 0);
    //当前集群中的节点，这里也许有朋友会感到困惑，日志条目不是只包含日志锁云和任期以及日志内容吗？
    //为什么还要包含集群中的节点信息，这里解释一下，在jraft框架中除了业务日志，还有配置变更日志
    //配置变更日志也是使用当前类的对象来封装的，因此会有以下几个成员变量
    //并且配置日志也是会传输给跟随者的
    private List<PeerId> peers;
    //旧配置中的节点
    private List<PeerId> oldPeers;
    //当前配置中的学习者
    private List<PeerId> learners;
    //旧配置中的学习者
    private List<PeerId> oldLearners;
    //真正的日志信息，初始化时为空
    private ByteBuffer data = EMPTY_DATA;
    //校验和，这个成员变量很重要，在日志传输之后，会在跟随者节点判断校验和是否和之前的相等，以此来判断日志条目是否有损坏
    private long checksum;
    //是否有校验和
    private boolean hasChecksum;

    public List<PeerId> getLearners() {
        return this.learners;
    }

    public void setLearners(final List<PeerId> learners) {
        this.learners = learners;
    }

    public List<PeerId> getOldLearners() {
        return this.oldLearners;
    }

    public void setOldLearners(final List<PeerId> oldLearners) {
        this.oldLearners = oldLearners;
    }

    public LogEntry() {
        super();
    }

    public LogEntry(final EnumOutter.EntryType type) {
        super();
        this.type = type;
    }

    public boolean hasLearners() {
        return (this.learners != null && !this.learners.isEmpty())
                || (this.oldLearners != null && !this.oldLearners.isEmpty());
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/1
     * @Description:计算校验和的方法
     */
    @Override
    public long checksum() {
        long c = checksum(this.type.getNumber(), this.id.checksum());
        c = checksum(this.peers, c);
        c = checksum(this.oldPeers, c);
        c = checksum(this.learners, c);
        c = checksum(this.oldLearners, c);
        if (this.data != null && this.data.hasRemaining()) {
            c = checksum(c, CrcUtil.crc64(this.data));
        }
        return c;
    }



    public boolean hasChecksum() {
        return this.hasChecksum;
    }

    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/12/1
     * @Description:这个方法就是用来计算日志是否损坏，在这个方法中重新计算了一次调用当前方法的日志条目对象的
     * 校验和，然后判断这个数据是否和之前的相等，如果不想等就说明损坏了
     * 注意，这里返回false意味着日志没有损坏
     */
    public boolean isCorrupted() {
        return this.hasChecksum && this.checksum != checksum();
    }


    public long getChecksum() {
        return this.checksum;
    }

    public void setChecksum(final long checksum) {
        this.checksum = checksum;
        this.hasChecksum = true;
    }

    public EnumOutter.EntryType getType() {
        return this.type;
    }

    public void setType(final EnumOutter.EntryType type) {
        this.type = type;
    }

    public LogId getId() {
        return this.id;
    }

    public void setId(final LogId id) {
        this.id = id;
    }

    public List<PeerId> getPeers() {
        return this.peers;
    }

    public void setPeers(final List<PeerId> peers) {
        this.peers = peers;
    }

    public List<PeerId> getOldPeers() {
        return this.oldPeers;
    }

    public void setOldPeers(final List<PeerId> oldPeers) {
        this.oldPeers = oldPeers;
    }

    public ByteBuffer getData() {
        return this.data;
    }


    public ByteBuffer sliceData() {
        return this.data != null ? this.data.slice() : null;
    }


    public ByteBuffer getReadOnlyData() {
        return this.data != null ? this.data.asReadOnlyBuffer() : null;
    }

    public void setData(final ByteBuffer data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "LogEntry [type=" + this.type + ", id=" + this.id + ", peers=" + this.peers + ", oldPeers="
                + this.oldPeers + ", learners=" + this.learners + ", oldLearners=" + this.oldLearners + ", data="
                + (this.data != null ? this.data.remaining() : 0) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.data == null) ? 0 : this.data.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.learners == null) ? 0 : this.learners.hashCode());
        result = prime * result + ((this.oldLearners == null) ? 0 : this.oldLearners.hashCode());
        result = prime * result + ((this.oldPeers == null) ? 0 : this.oldPeers.hashCode());
        result = prime * result + ((this.peers == null) ? 0 : this.peers.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
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
        LogEntry other = (LogEntry) obj;
        if (this.data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!this.data.equals(other.data)) {
            return false;
        }
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        if (this.learners == null) {
            if (other.learners != null) {
                return false;
            }
        } else if (!this.learners.equals(other.learners)) {
            return false;
        }
        if (this.oldLearners == null) {
            if (other.oldLearners != null) {
                return false;
            }
        } else if (!this.oldLearners.equals(other.oldLearners)) {
            return false;
        }
        if (this.oldPeers == null) {
            if (other.oldPeers != null) {
                return false;
            }
        } else if (!this.oldPeers.equals(other.oldPeers)) {
            return false;
        }
        if (this.peers == null) {
            if (other.peers != null) {
                return false;
            }
        } else if (!this.peers.equals(other.peers)) {
            return false;
        }
        return this.type == other.type;
    }

}
