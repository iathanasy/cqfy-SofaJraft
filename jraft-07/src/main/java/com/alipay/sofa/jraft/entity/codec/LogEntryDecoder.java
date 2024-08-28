package com.alipay.sofa.jraft.entity.codec;

import com.alipay.sofa.jraft.entity.LogEntry;

//解码器接口
public interface LogEntryDecoder {

    LogEntry decode(byte[] bs);
}
