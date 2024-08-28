package com.alipay.sofa.jraft.option;

import com.alipay.sofa.jraft.entity.EnumOutter;


//处理只读请求的枚举类
public enum ReadOnlyOption {


    //这个枚举对象对应的就是按照只读请求策略处理，也就是文章分析的线性一致读流程
    //该处理策略是sofajraft框架中默认的
    ReadOnlySafe,
    //使用租约模式来处理读请求
    ReadOnlyLeaseBased;


    //将枚举对象转换为protocol buffer通信类型的对象
    public static EnumOutter.ReadOnlyType convertMsgType(ReadOnlyOption option) {
        return ReadOnlyOption.ReadOnlyLeaseBased.equals(option) ? EnumOutter.ReadOnlyType.READ_ONLY_LEASE_BASED
                : EnumOutter.ReadOnlyType.READ_ONLY_SAFE;
    }



    //将protocol buffer通信类型的对象转换为枚举对象
    public static ReadOnlyOption valueOfWithDefault(EnumOutter.ReadOnlyType readOnlyType, ReadOnlyOption defaultOption) {
        if (readOnlyType == null) {
            return defaultOption;
        }
        return EnumOutter.ReadOnlyType.READ_ONLY_SAFE.equals(readOnlyType) ? ReadOnlyOption.ReadOnlySafe
                : ReadOnlyOption.ReadOnlyLeaseBased;
    }
}