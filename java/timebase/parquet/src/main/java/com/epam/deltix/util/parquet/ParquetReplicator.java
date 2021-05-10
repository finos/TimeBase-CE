package com.epam.deltix.util.parquet;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.stream.MessageFileHeader;
import com.epam.deltix.qsrv.hf.stream.MessageReader2;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.progress.ConsoleProgressIndicator;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.PropertyConfigurator;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;

import java.io.File;
import java.io.IOException;

import static com.epam.deltix.util.parquet.RawMessageWriteSupport.*;

public class ParquetReplicator extends DefaultApplication {

    private static final Log LOGGER = LogFactory.getLog(ParquetReplicator.class);
    private static final String STREAM = "-stream";
    private static final String SRC_FILE = "-srcfile";
    private static final String DB = "-db";
    private static final String DESTINATION = "-dest";

    protected ParquetReplicator(String[] args) {
        super(args);
    }

    @Override
    protected void run() throws IOException {
        checkArgs();
        if (isArgSpecified(STREAM)) {
            runStream(getArgValue(DB, "dxtick://localhost:8011"), getArgValue(STREAM), getArgValue(DESTINATION));
        } else {
            runDataFile(getFileArg(SRC_FILE), getArgValue(DESTINATION));
        }
    }

    private static ConsoleProgressIndicator createIndicator() {
        ConsoleProgressIndicator indicator = new ConsoleProgressIndicator();
        indicator.setShowPercentage(true);
        indicator.setTotalWork(1);
        return indicator;
    }

    private static double getProgress(long[] timeRange, long current) {
        return (current - timeRange[0]) * 1. / (timeRange[1] - timeRange[0]);
    }

    private void runStream(String timebase, String key, String dest) throws IOException {
        try (DXTickDB db = TickDBFactory.createFromUrl(timebase)) {
            db.open(true);
            DXTickStream stream = db.getStream(key);
            if (stream == null) {
                LOGGER.error("There's no stream %s in TimeBase on url %s.")
                        .with(key)
                        .with(timebase);
                System.exit(1);
            }
            LOGGER.info("Writing stream %s content to parquet file %s")
                    .with(key)
                    .with(dest);
            RawMessageWriteSupport writeSupport = new RawMessageWriteSupport(key, stream.getTypes());
            RawMessageWriteSupport.Builder builder = new RawMessageWriteSupport.Builder(new Path(dest), writeSupport)
                    .withCompressionCodec(COMPRESSION)
                    .withRowGroupSize(ROW_GROUP_SIZE)
                    .withPageSize(PAGE_SIZE)
                    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE);
            long[] timeRange = stream.getTimeRange();
            try (TickCursor cursor = db.select(Long.MIN_VALUE, new SelectionOptions(true, false), stream);
                ParquetWriter<RawMessage> writer = builder.build()) {
                ConsoleProgressIndicator progressIndicator = createIndicator();
                while (cursor.next()) {
                    writer.write((RawMessage) cursor.getMessage());
                    progressIndicator.setWorkDone(getProgress(timeRange, cursor.getMessage().getTimeStampMs()));
                }
                System.out.println();
                LOGGER.info("Computing stats, creating footer. It may take some time.");
            }
            LOGGER.info("Finished successfully.");
        }
    }

    private void runDataFile(File file, String dest) throws IOException {
        LOGGER.info("Writing qsmessage file %s content to parquet file %s")
                .with(file.getAbsolutePath())
                .with(dest);
        MessageFileHeader header = Protocol.readHeader(file);
        RawMessageWriteSupport writeSupport = new RawMessageWriteSupport(file.getName(), header.getTypes());
        RawMessageWriteSupport.Builder builder = new RawMessageWriteSupport.Builder(new Path(dest), writeSupport)
                .withCompressionCodec(COMPRESSION)
                .withRowGroupSize(ROW_GROUP_SIZE)
                .withPageSize(PAGE_SIZE)
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE);
        try (ConsumableMessageSource<InstrumentMessage> reader = MessageReader2.createRaw(file);
             ParquetWriter<RawMessage> writer = builder.build()) {
            ConsoleProgressIndicator progressIndicator = createIndicator();
            while (reader.next()) {
                writer.write((RawMessage) reader.getMessage());
                progressIndicator.setWorkDone(reader.getProgress());
            }
            System.out.println();
            LOGGER.info("Computing stats, creating footer. It may take some time.");
        }
        LOGGER.info("Finished successfully.");
    }

    private void checkArgs() {
        if ((!isArgSpecified(STREAM) && !isArgSpecified(SRC_FILE)) || !isArgSpecified(DESTINATION)) {
            printUsageAndExit();
        }
    }

    public static void main(String[] args) {
        PropertyConfigurator.configure(ParquetReplicator.class.getClassLoader().getResource("deltix/util/parquet/log4j.properties"));
        new ParquetReplicator(args).start();
    }
}
