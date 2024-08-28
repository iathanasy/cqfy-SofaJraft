package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.core.NodeImpl;


//元数据存储器需要用到的配置参数封装在这个类中，其实这个类中就一个成员变量
//就是这个NodeImpl对象，但是为了是每一个组件的体系都十分齐全，sofajraft框架
//在框架中设计了非常多的xxxxOptions类，这样从名字上一眼就能看出这个配置类封装的配置参数
//都是给哪个组件使用的
public class RaftMetaStorageOptions {
    private NodeImpl node;

    public NodeImpl getNode() {
        return this.node;
    }

    public void setNode(NodeImpl node) {
        this.node = node;
    }
}