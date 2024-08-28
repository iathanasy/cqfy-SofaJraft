package com.alipay.sofa.jraft.rpc;

import com.google.protobuf.Message;


//接收到响应时要被回调的方法，这个类的对象其实是接收到响应的回调方法的适配器对象
//主要作用就是封装了接收到的响应，既然是响应后回调，肯定是要对响应做处理，肯定要得到这个响应对象才行
//就是从这个适配器对象中得到
public abstract class RpcResponseClosureAdapter<T extends Message> implements RpcResponseClosure<T> {

    private T resp;

    public T getResponse() {
        return this.resp;
    }

    @Override
    public void setResponse(T resp) {
        this.resp = resp;
    }
}
