package com.test;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpectClosure implements Closure {
    private final int expectedErrCode;
    private final String expectErrMsg;
    private final CountDownLatch latch;
    private AtomicInteger successCount;

    public ExpectClosure(final CountDownLatch latch) {
        this(RaftError.SUCCESS, latch);
    }

    public ExpectClosure(final RaftError expectedErrCode, final CountDownLatch latch) {
        this(expectedErrCode, null, latch);

    }

    public ExpectClosure(final RaftError expectedErrCode, final String expectErrMsg, final CountDownLatch latch) {
        super();
        this.expectedErrCode = expectedErrCode.getNumber();
        this.expectErrMsg = expectErrMsg;
        this.latch = latch;
    }

    public ExpectClosure(final int code, final String expectErrMsg, final CountDownLatch latch) {
        this(code, expectErrMsg, latch, null);
    }

    public ExpectClosure(final int code, final String expectErrMsg, final CountDownLatch latch,
                         final AtomicInteger successCount) {
        super();
        this.expectedErrCode = code;
        this.expectErrMsg = expectErrMsg;
        this.latch = latch;
        this.successCount = successCount;
    }

    @Override
    public void run(final Status status) {
        if (this.expectedErrCode >= 0) {
            System.out.println(status.getCode());
        }
        if (this.expectErrMsg != null) {
            System.out.println(status.getErrorMsg());
        }
        if (status.isOk() && this.successCount != null) {
            this.successCount.incrementAndGet();
        }
        this.latch.countDown();
    }

}
