package com.epam.deltix.qsrv.dtb.fs.hdfs;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.lang.Util;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.net.URI;

public class DistributedFS implements AbstractFileSystem {
    private static final Log LOG = LogFactory.getLog(DistributedFS.class);

    public static final String TIMEBASE_DFS_CONNECTION_TIMEOUT = "Timebase.DFS.timeout";
    public static final String TIMEBASE_DFS_CONNECTION_RETRIES = "Timebase.DFS.retries.on.timeout";

    private static final String CONNECTION_TIMEOUT_DEF = "6000";
    private static final String CONNECTION_RETRIES_DEF = "5";

    public final String         address;
    public final FileSystem     delegate;

    DistributedFS(String address, FileSystem delegate) {
        this.address = address;
        this.delegate = delegate;
    }

    @Override
    public boolean isAbsolutePath(String path) {
        return new Path(normalize(path)).isAbsolute();
    }

    public String  normalize(String path) {
        return path.startsWith(address) ? path.substring(address.length()) : path;
    }

    @Override
    public AbstractPath createPath(String path) {
        return new FilePathImpl(normalize(path), this);
    }

    @Override
    public AbstractPath createPath(AbstractPath parent, String child) {
        FilePathImpl p = (FilePathImpl) Util.unwrap(parent);

        return new FilePathImpl(new Path(p.path, child), this);
    }

    public static DistributedFS createFromUrl(String url) throws IOException {
        FileSystem fs = FileSystem.get(URI.create(url), createConfiguration(url));
        FsStatus status = fs.getStatus();
        LOG.log(LogLevel.INFO)
            .append("Successfully connected to '").append(url).append("'. ")
            .append("FileSystem capacity: ").append(status.getCapacity() >> 30).append("Gb. ")
            .append("Remaining: ").append(status.getRemaining() >> 30).append("Gb.")
            .commit();

        return new DistributedFS(url , fs);
    }

    private static Configuration createConfiguration(String url) {
        Configuration config = new Configuration();
        config.set(CommonConfigurationKeysPublic.IPC_CLIENT_CONNECT_TIMEOUT_KEY,
                   System.getProperty(TIMEBASE_DFS_CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_DEF));
        config.set(CommonConfigurationKeysPublic.IPC_CLIENT_CONNECT_MAX_RETRIES_ON_SOCKET_TIMEOUTS_KEY,
                   System.getProperty(TIMEBASE_DFS_CONNECTION_RETRIES, CONNECTION_RETRIES_DEF));
        FileSystem.setDefaultUri(config, url);

        return config;
    }

    @Override
    public String getSeparator() {
        return Path.SEPARATOR;
    }
}
