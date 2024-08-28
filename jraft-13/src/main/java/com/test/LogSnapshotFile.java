package com.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class LogSnapshotFile {

    private static final Logger LOG = LoggerFactory.getLogger(LogSnapshotFile.class);

    private String path;

    public LogSnapshotFile(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    /**
     * Save value to snapshot file.
     */
    public boolean save(final String value) {
        try {
            FileUtils.writeStringToFile(new File(path), value);
            return true;
        } catch (IOException e) {
            LOG.error("Fail to save snapshot", e);
            return false;
        }
    }

    public String load() throws IOException {
        return FileUtils.readFileToString(new File(path));
    }
}
