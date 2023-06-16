/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.archive;

import com.epam.deltix.qsrv.hf.BaseTopicReadingTest;
import com.epam.deltix.qsrv.hf.RatePrinter;
import com.epam.deltix.qsrv.hf.StubData;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.Subscription;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import org.agrona.collections.MutableLong;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class AeronArchiveReadTest {
    private static final int TEST_DURATION_MS = 60 * 1000;

    static final int REPLAY_STREAM_ID = 88888;


    private final Aeron aeron;
    private final AeronArchive aeronArchive;

    private AtomicBoolean running;

    public AeronArchiveReadTest(Aeron aeron, AeronArchive aeronArchive) {
        this.aeron = aeron;
        this.aeronArchive = aeronArchive;
    }

    public static void main(String[] args) throws Exception {
        File archiveDir = new File(AeronArchiveWriteTest.ARCHIVE_DIR);

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
        AeronArchiveReadTest test = new AeronArchiveReadTest(client, aeronArchive);
        test.executeTest();
        client.close();
        archivingMediaDriver.close();
    }

    void executeTest() throws Exception {
        String replayChannel = CommonContext.IPC_CHANNEL;
        int replayStreamId = REPLAY_STREAM_ID;
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);

        AtomicLong messagesReceivedCounter = new AtomicLong(0);

        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());


        BaseTopicReadingTest.MessageValidator messageValidator = new BaseTopicReadingTest.MessageValidator();
        Runnable runnable = createReader(messagesReceivedCounter, replayChannel, replayStreamId, types, messageValidator, AeronArchiveWriteTest.MESSAGE_COUNT_TO_SEND);

        Thread readerThread = new Thread(runnable);
        readerThread.setName("READER");
        readerThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
        readerThread.start();

        long recordingId = findRecordingId();
        Subscription replaySub = aeronArchive.replay(recordingId, 0, Long.MAX_VALUE, replayChannel, replayStreamId);
        replaySub.close();
        //aeronArchive.archiveProxy().replay(recordingId, 0, Long.MAX_VALUE, replayChannel, replayStreamId);

        // Let test to work
        //Thread.sleep(TEST_DURATION_MS);
        readerThread.join(TEST_DURATION_MS);

        // Let reader finish off the queue
        //Thread.sleep(1000);

        // Stop reader
        stopReader();
        readerThread.join(1000);
        Assert.assertFalse(readerThread.isAlive());

        Assert.assertTrue("Exception in threads", exceptions.isEmpty());

        Assert.assertTrue(messagesReceivedCounter.get() > 0);

        System.out.println("Message count: " + messagesReceivedCounter);
    }

    private long findRecordingId() {
        final MutableLong foundRecordingId = new MutableLong();

        final int recordingsFound = aeronArchive.listRecordingsForUri(
                0L,
                100,
                AeronArchiveWriteTest.ORIGINAL_CHANNEL,
                AeronArchiveWriteTest.ORIGINAL_DATA_STREAM_ID,
                (
                        controlSessionId,
                        correlationId,
                        recordingId,
                        startTimestamp,
                        stopTimestamp,
                        startPosition,
                        stopPosition,
                        initialTermId,
                        segmentFileLength,
                        termBufferLength,
                        mtuLength,
                        sessionId,
                        streamId,
                        strippedChannel,
                        originalChannel,
                        sourceIdentity
                ) -> foundRecordingId.set(recordingId));

        if (recordingsFound == 0) {
            throw new IllegalStateException(
                    "No recordings found for channel=" + AeronArchiveWriteTest.ORIGINAL_CHANNEL + " streamId=" + AeronArchiveWriteTest.ORIGINAL_DATA_STREAM_ID);
        }

        return foundRecordingId.get();
    }

    protected Runnable createReader(AtomicLong messagesReceivedCounter, String channel, int dataStreamId, List<RecordClassDescriptor> types, BaseTopicReadingTest.MessageValidator messageValidator, long expectedMessageCount) {
        AtomicBoolean runningFlag = new AtomicBoolean(true);
        running = runningFlag;



        return () -> {
            MessagePoller messagePoller = new DirectReaderFactory().createPoller(aeron, false, channel, dataStreamId, types, StubData.getStubMappingProvider());

            RatePrinter ratePrinter = new RatePrinter("Reader");
            YieldingIdleStrategy idleStrategy = new YieldingIdleStrategy();

            MessageProcessor processor = m -> {
                messageValidator.validate(m);
                messagesReceivedCounter.incrementAndGet();
                ratePrinter.inc();
            };

            long startTime = System.nanoTime();
            boolean x = true;
            while (runningFlag.get() && messagesReceivedCounter.get() < expectedMessageCount) {
                idleStrategy.idle(
                        messagePoller.processMessages(100, processor)
                );
            }

            long stopTime = System.nanoTime();

            messagePoller.close();

            long timeDeltaNanos = stopTime - startTime;

            System.out.printf("Messages: %d\n", messagesReceivedCounter.get());
            System.out.printf("Seconds: %.3f\n", ((float) timeDeltaNanos) / 1_000_000_000 );
            System.out.printf("Rate: %.3f k msg/s\n", ((float) messagesReceivedCounter.get() * 1_000_000) / timeDeltaNanos);
        };
    }

    protected void stopReader() {
        running.set(false);
    }
}