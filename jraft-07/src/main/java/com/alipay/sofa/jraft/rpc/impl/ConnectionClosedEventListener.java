package com.alipay.sofa.jraft.rpc.impl;

import com.alipay.sofa.jraft.rpc.Connection;


public interface ConnectionClosedEventListener {

    void onClosed(final String remoteAddress, final Connection conn);
}
