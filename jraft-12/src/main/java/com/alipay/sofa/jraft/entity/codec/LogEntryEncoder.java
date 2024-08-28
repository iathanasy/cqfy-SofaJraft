package com.alipay.sofa.jraft.entity.codec;


import com.alipay.sofa.jraft.entity.LogEntry;

//编码器接口
public interface LogEntryEncoder {


    byte[] encode(LogEntry log);
}