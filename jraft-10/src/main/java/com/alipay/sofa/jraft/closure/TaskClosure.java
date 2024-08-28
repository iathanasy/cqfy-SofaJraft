package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;


//第七版本就要用到了
public interface TaskClosure extends Closure {


    void onCommitted();
}