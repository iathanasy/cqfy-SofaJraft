package com.alipay.sofa.jraft.rpc;

/**
 * @author jiachun.fjc
 */
public interface RpcContext {

    /**
     * Send a response back.
     *
     * @param responseObj the response object
     */
    void sendResponse(final Object responseObj);

    /**
     * Get current connection.
     *
     * @return current connection
     */
    Connection getConnection();

    /**
     * Get the remote address.
     *
     * @return remote address
     */
    String getRemoteAddress();
}
