package com.alipay.sofa.jraft.storage.io;

import com.alipay.sofa.jraft.error.RetryAgainException;
import com.alipay.sofa.jraft.util.ByteBufferCollector;

import java.io.IOException;




public interface FileReader {

    int EOF = -1;
    String getPath();
    int readFile(final ByteBufferCollector buf, final String fileName, final long offset, final long maxCount)
            throws IOException,
            RetryAgainException;
}
