package com.alipay.sofa.jraft.closure;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;

public interface LoadSnapshotClosure extends Closure {


    SnapshotReader start();
}
