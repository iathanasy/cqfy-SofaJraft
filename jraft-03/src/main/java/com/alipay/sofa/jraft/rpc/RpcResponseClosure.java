package com.alipay.sofa.jraft.rpc;

import com.alipay.sofa.jraft.Closure;
import com.google.protobuf.Message;


//接收到响应要回调的方法
public interface RpcResponseClosure<T extends Message> extends Closure {

    //把接收到的响应设置到RpcResponseClosureAdapter适配器对象中
    void setResponse(T resp);
}