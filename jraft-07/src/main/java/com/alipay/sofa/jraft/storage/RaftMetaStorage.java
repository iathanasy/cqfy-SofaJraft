package com.alipay.sofa.jraft.storage;

import com.alipay.sofa.jraft.Lifecycle;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.RaftMetaStorageOptions;


//元数据存储器接口
public interface RaftMetaStorage extends Lifecycle<RaftMetaStorageOptions>, Storage {

    boolean setTerm(final long term);


    long getTerm();


    boolean setVotedFor(final PeerId peerId);


    PeerId getVotedFor();


    boolean setTermAndVotedFor(final long term, final PeerId peerId);
}
