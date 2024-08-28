package com.alipay.sofa.jraft.storage.io;

import com.alipay.sofa.jraft.error.RetryAgainException;
import com.alipay.sofa.jraft.util.ByteBufferCollector;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class LocalDirReader implements FileReader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDirReader.class);

    private final String        path;

    public LocalDirReader(String path) {
        super();
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int readFile(final ByteBufferCollector buf, final String fileName, final long offset, final long maxCount)
            throws IOException,
            RetryAgainException {
        return readFileWithMeta(buf, fileName, null, offset, maxCount);
    }

    @SuppressWarnings("unused")
    protected int readFileWithMeta(final ByteBufferCollector buf, final String fileName, final Message fileMeta,
                                   long offset, final long maxCount) throws IOException, RetryAgainException {
        buf.expandIfNecessary();
        final String filePath = this.path + File.separator + fileName;
        final File file = new File(filePath);
        try (final FileInputStream input = new FileInputStream(file); final FileChannel fc = input.getChannel()) {
            int totalRead = 0;
            while (true) {
                final int nread = fc.read(buf.getBuffer(), offset);
                if (nread <= 0) {
                    return EOF;
                }
                totalRead += nread;
                if (totalRead < maxCount) {
                    if (buf.hasRemaining()) {
                        return EOF;
                    } else {
                        buf.expandAtMost((int) (maxCount - totalRead));
                        offset += nread;
                    }
                } else {
                    final long fsize = file.length();
                    if (fsize < 0) {
                        LOG.warn("Invalid file length {}", filePath);
                        return EOF;
                    }
                    if (fsize == offset + nread) {
                        return EOF;
                    } else {
                        return totalRead;
                    }
                }
            }
        }
    }
}
