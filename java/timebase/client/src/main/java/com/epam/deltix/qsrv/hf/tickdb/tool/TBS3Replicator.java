package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.s3.S3DataStore;
import com.epam.deltix.util.s3.S3StorageOptions;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TBS3Replicator extends DefaultApplication {
    private static final Log LOG = LogFactory.getLog(TBS3Replicator.class);

    public TBS3Replicator(String[] args) {
        super(args);
    }

    @Override
    public void run() throws IOException {
        String[] streamKeys = getMandatoryArgValue("-streams").split(",");
        if (streamKeys.length == 0)
            throw new IllegalArgumentException("-streams argument must specify a comma separated list of one or more stream keys");

        String timebaseUrl = getArgValue("-timebase", "dxtick://localhost:8011");
        boolean live = isArgSpecified("-live");
        int retentionPeriod = getIntArgValue("-retain", -1);
        String bucket = getMandatoryArgValue("-bucket"); //ember-s3-test
        String region = getMandatoryArgValue("-region"); //us-east-2
        String accessKeyId = getArgValue("-accessKeyId", System.getenv().get("AWS_ACCESS_KEY_ID"));
        if (accessKeyId == null)
            throw new IllegalArgumentException("Access key ID must be specified in environment variable AWS_ACCESS_KEY_ID or with '-accessKeyId' argument.");
        String accessKey = getArgValue("-accessKey", System.getenv().get("AWS_SECRET_ACCESS_KEY"));
        if (accessKey == null)
            throw new IllegalArgumentException("Access key must be specified in environment variable AWS_SECRET_ACCESS_KEY or with '-accessKey' argument.");
        int maxBatchSize = getIntArgValue("-maxBatchSize", 100000);
        long maxBatchTime = getLongArgValue("-maxBatchTime", TimeUnit.MINUTES.toMillis(15));

        S3StorageOptions options = new S3StorageOptions(bucket, region, accessKeyId, accessKey);
        options.setMaxBatchSize(maxBatchSize);
        options.setMaxBatchTime(maxBatchTime);
        S3DataStore dataStore = new S3DataStore(options);

        DXTickStream[] msgStreams = new DXTickStream[streamKeys.length];

        try (DXTickDB timebase = TickDBFactory.openFromUrl(timebaseUrl, false)) {
            for (int i = 0; i < streamKeys.length; i++) {
                msgStreams[i] = timebase.getStream(streamKeys[i].trim());
                if (msgStreams[i] == null)
                    throw new IllegalArgumentException("Stream " + streamKeys[i] + " does not exist");
            }

            long monitorInterval = TimeUnit.MINUTES.toMillis(5);
            if (maxBatchTime > 0) {
                // keep it between 1 sec and 5 min
                monitorInterval = Math.min(Math.max(maxBatchTime/2, 1000), monitorInterval);
            }

            if (msgStreams.length > 1) {
                ExecutorService executor = Executors.newFixedThreadPool(msgStreams.length);
                for (int i = 0; i < msgStreams.length; i++) {
                    executor.execute(new S3StreamReplicator(msgStreams[i], dataStore, live, retentionPeriod, i, monitorInterval));
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                } catch (InterruptedException ex) {
                    LOG.info("Replication was interrupted");
                }
            } else {
                new S3StreamReplicator(msgStreams[0], dataStore, live, retentionPeriod, 0, monitorInterval).run();
            }
        }
    }

    public static void main(String[] args) {
        new TBS3Replicator(args).start();
        System.exit(0);
    }
}
