package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

@Category(JUnitCategories.TickDBFast.class)
public class Test_TimeRange extends TDBRunnerBase {


    @SuppressWarnings("unchecked")
    @Test
    public void testTimeRange() {

        DXTickDB clientDb = getTickDb();
        DXTickDB serverDb = getServerDb();

        String streamKey = "test1";
        DXTickStream serverStream = createTestStream(serverDb, streamKey);
        DXTickStream clientStream = clientDb.getStream(streamKey);

        LoadingOptions options = new LoadingOptions(false, LoadingOptions.WriteMode.APPEND);

        int msgCount = 100;

        long[] messageTimes = new long[msgCount];


        String prefix = "S_";
        try (var loader = (TickLoader<InstrumentMessage>) serverStream.createLoader(options)) {
            BarMessage message = new BarMessage();

            GregorianCalendar calendar = new GregorianCalendar(2018, Calendar.JANUARY, 1);

            for (int i = 0; i < msgCount; i++) {
                sendMessages(message, calendar, loader, 1, prefix + i);
                messageTimes[i] = message.getTimeStampMs();
            }
        }

        IdentityKey[] allEntities = serverStream.listEntities();

        TimeInterval[] serverIntervals = serverStream.listTimeRange(allEntities);
        TimeInterval[] clientIntervals = clientStream.listTimeRange(allEntities);

        for (int i = 0; i < allEntities.length; i++) {
            TimeInterval timeIntervalServ = serverIntervals[i];
            TimeInterval timeIntervalClient = clientIntervals[i];

            String symbol = allEntities[i].getSymbol().toString();
            if (symbol.startsWith(prefix)) {
                int symbolIndex = Integer.parseInt(symbol.substring(prefix.length()));
                long expectedStartTime = messageTimes[symbolIndex];
                long expectedEndTime = messageTimes[symbolIndex];

                assertEquals("Unexpected start time for " + symbol + " (server) from listTimeRange()", expectedStartTime, timeIntervalServ.getFromTime());
                assertEquals("Unexpected end time for " + symbol + " (server) from listTimeRange()", expectedEndTime, timeIntervalServ.getToTime());

                assertEquals("Unexpected start time for " + symbol + " (client) from listTimeRange()", expectedStartTime, timeIntervalClient.getFromTime());
                assertEquals("Unexpected end time for " + symbol + " (client) from listTimeRange()", expectedEndTime, timeIntervalClient.getToTime());

                long[] rangeServ = serverStream.getTimeRange(allEntities[i]);
                assertEquals("Unexpected start time for " + symbol + " (server) from getTimeRange()", expectedStartTime, rangeServ[0]);
                assertEquals("Unexpected end time for " + symbol + " (server) from getTimeRange()", expectedEndTime, rangeServ[1]);

                long[] rangeClient = clientStream.getTimeRange(allEntities[i]);
                assertEquals("Unexpected start time for " + symbol + " (client) from getTimeRange()", expectedStartTime, rangeClient[0]);
                assertEquals("Unexpected end time for " + symbol + " (client) from getTimeRange()", expectedEndTime, rangeClient[1]);
            }
        }

        serverStream.getTimeRange();
    }


    private static @NotNull DXTickStream createTestStream(DXTickDB db, String name) {

        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null,
                "DECIMAL(4)",
                "DECIMAL(0)"
        );

        StreamOptions so = StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0, descriptor);
        DXTickStream stream = db.createStream(name, so);
        return stream;
    }

    private static void sendMessages(BarMessage message, GregorianCalendar calendar, TickLoader<InstrumentMessage> loader, int count, CharSequence symbol) {
        for (int i = 0; i < count; i++) {
            message.setSymbol(symbol);
            message.setTimeStampMs(calendar.getTimeInMillis());

            message.setHigh(i * 100.0);
            message.setLow(i * 99.0);
            message.setOpen(i * 98);
            message.setClose(i * 99.5);
            message.setVolume(i);

            loader.send(message);

            calendar.add(Calendar.SECOND, 1);
        }
    }


}
