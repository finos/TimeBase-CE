package com.epam.deltix.qsrv.hf.archive;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.StubData;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.timebase.messages.service.ErrorMessage;
import com.epam.deltix.util.io.IOUtil;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.SourceLocation;
import io.aeron.driver.MediaDriver;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class AeronArchiveWriteTest {
    static final long MESSAGE_COUNT_TO_SEND = 100_000_000; // 100M msg ~= 6.4 Gb

    private static final int MAX_TEST_DURATION_MS = 60 * 1000;

    public static final String TEST_DIR = "\\temp\\aeron";
    public static final String DRIVER_DIR = TEST_DIR + "\\driver";
    public static final String ARCHIVE_DIR = TEST_DIR + "\\archive";


    private final Aeron aeron;
    private final AeronArchive aeronArchive;

    static final int ORIGINAL_DATA_STREAM_ID = 7777;
    static final String ORIGINAL_CHANNEL = CommonContext.IPC_CHANNEL;

    public AeronArchiveWriteTest(Aeron aeron, AeronArchive aeronArchive) {
        this.aeron = aeron;
        this.aeronArchive = aeronArchive;
    }

    public static void main(String[] args) throws Exception {
        File archiveDir = new File(ARCHIVE_DIR);
        IOUtil.deleteFileOrDir(archiveDir);

        MediaDriver.Context driverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .spiesSimulateConnection(true);

        Archive.Context archiveCtx = new Archive.Context()
                .archiveDir(archiveDir);

        ArchivingMediaDriver archivingMediaDriver = ArchivingMediaDriver.launch(
                driverCtx,
                archiveCtx
        );
        //Archive archive = archivingMediaDriver.archive();

        Aeron client = Aeron.connect();
        AeronArchive aeronArchive = AeronArchive.connect(new AeronArchive.Context()
                .aeron(client)
                //.controlResponseStreamId(archiveCtx.controlStreamId())
                //.ownsAeronClient(false)
        );
        AeronArchiveWriteTest test = new AeronArchiveWriteTest(client, aeronArchive);
        test.executeTest();
        client.close();
        archivingMediaDriver.close();
    }

    void executeTest() throws Exception {
        String channel = ORIGINAL_CHANNEL;
        int dataStreamId = ORIGINAL_DATA_STREAM_ID;
        int serverMetadataStreamId = dataStreamId + 1;

        // Start recording
        aeronArchive.startRecording(channel, dataStreamId, SourceLocation.LOCAL);

        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);
        byte loaderNumber = 1;

        AtomicLong messagesSentCounter = new AtomicLong(0);
        AtomicBoolean senderStopFlag = new AtomicBoolean(false);

        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        Thread loaderThread = new Thread(() -> {

            MessageChannel<InstrumentMessage> messageChannel = new DirectLoaderFactory().create(aeron, false, channel, channel, dataStreamId, serverMetadataStreamId, types, loaderNumber, new ByteArrayOutputStream(8 * 1024), Collections.emptyList(), null, null);

            ErrorMessage msg = new ErrorMessage();
            msg.setSymbol("ABC");
            msg.setSeqNum(234567890);
            long messageSentCounter = 0;
            System.out.println("Sending messages...");
            long startTime = System.nanoTime();
            while (!senderStopFlag.get() && messageSentCounter < MESSAGE_COUNT_TO_SEND) {
                messageSentCounter ++;
                msg.setTimeStampMs(messageSentCounter); // Se store message number in the timestamp field.
                messageChannel.send(msg);
                messagesSentCounter.set(messageSentCounter);
            }
            long stopTime = System.nanoTime();
            messageChannel.close();
            long timeDeltaNanos = stopTime - startTime;

            System.out.printf("Messages: %d\n", messageSentCounter);
            System.out.printf("Seconds: %.3f\n", ((float) timeDeltaNanos) / 1_000_000_000 );
            System.out.printf("Rate: %.3f k msg/s\n", ((float) messageSentCounter * 1_000_000) / timeDeltaNanos);
        });
        loaderThread.setName("SENDER");
        loaderThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
        loaderThread.start();


        // Let test to work
        loaderThread.join(MAX_TEST_DURATION_MS);

        // Ask sender to stop
        senderStopFlag.set(true);
        loaderThread.join(1000);
        Assert.assertFalse(loaderThread.isAlive());

        // Stop recording
        aeronArchive.stopRecording(channel, dataStreamId);


        Assert.assertTrue("Exception in threads", exceptions.isEmpty());

        Assert.assertTrue(messagesSentCounter.get() > 0);
    }
}
