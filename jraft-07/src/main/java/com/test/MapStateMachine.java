package com.test;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.error.RaftException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;



/**
 * @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。
 * @author：陈清风扬，个人微信号：chenqingfengyangjj。
 * @date:2023/12/18
 * @方法描述：这是第七版本，我为大家提供的一个状态机实现类
 */
public class MapStateMachine  extends StateMachineAdapter{

    private static final Logger LOG = LoggerFactory.getLogger(MapStateMachine.class);


    private HashMap<String,String> map = new HashMap<>();

    @Override
    public void onApply(final Iterator iter) {
        //注意，这里实际上调用的是IteratorWrapper的hasNext方法
        //该方法会判断要处理的日志条目是不是EnumOutter.EntryType.ENTRY_TYPE_DATA类型的，如果不是就不会进入循环
        //由此可见，迭代器在真正的状态机中会把业务日志全部应用到状态机中
        //只有遇到非业务日志了，才会退出该方法，在FSMCallerImpl的doCommitted方法中继续之前的操作
        while (iter.hasNext()) {
            final Closure done = iter.done();
            final ByteBuffer data = iter.getData();
            String string = Charset.defaultCharset().decode(data).toString();
            if (done == null) {
                //走到这里意味着是跟随者将日志应用到状态机上,当然，这里肯定还要解析是get还是put指令，这里我就不搞这么麻烦了，状态机本来就是用户自己定义的
                //因为说到底sofajraft也只是一个raft共识算法库，业务操作属于用户自己定义的框架中的功能，这个是由用户自由发挥的
                map.put("日志",string);
                System.out.println("领导者应用了日志==========================="+map.get("日志"));
            } else {
                //走到这里意味着是领导者将日志应用到状态机上
                map.put("日志",string);
                System.out.println("跟随者应用了日志==========================="+map.get("日志"));
            }//获取下一个日志条目
            iter.next();
        }
    }



    public String get(final String key) throws Exception {
        return map.get(key);
    }


    @Override
    public void onError(final RaftException e) {
        LOG.error("Raft error: {}", e, e);
    }



    @Override
    public void onLeaderStart(final long term) {
        super.onLeaderStart(term);

    }

    @Override
    public void onLeaderStop(final Status status) {
        super.onLeaderStop(status);
    }

}
