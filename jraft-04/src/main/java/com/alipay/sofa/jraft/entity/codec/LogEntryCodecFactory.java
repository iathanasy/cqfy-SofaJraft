package com.alipay.sofa.jraft.entity.codec;


//编解码工厂接口
public interface LogEntryCodecFactory {

    LogEntryEncoder encoder();


    LogEntryDecoder decoder();
}