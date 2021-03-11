package com.epam.deltix.test.qsrv.hf.tickdb.select;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.StreamMessageSource;
import com.epam.deltix.qsrv.testsetup.*;
import static org.junit.Assert.*;
import static com.epam.deltix.qsrv.testsetup.TickDBCreator.*;

/**
 *
 */
public class TestDBUtil {
    public static String            getLocationString (
        String                          typeName,
        int                             streamIdx,
        int                             entityIdx,
        int                             sequenceIdx
    )
    {
        return (
            "{ Seq: " + sequenceIdx + "; Stream: " + streamIdx +
            "; Entity: " + entityIdx + "; Type: " + typeName + " }: "
        );
    }

    public static void              checkNextTestMessage (
        InstrumentMessageSource         cursor,
        String                          typeName,
        int                             streamIdx,
        int                             entityIdx,
        int                             sequenceIdx
    )
    {
        String      location = getLocationString (typeName, streamIdx, entityIdx, sequenceIdx);
        assertTrue (location + "next ()", cursor.next ());
        checkTestMessage (true, location, cursor, typeName, streamIdx, entityIdx, sequenceIdx);
    }

    public static void              checkTestMessage (
        StreamMessageSource minfo,
        String                          typeName,
        int                             streamIdx,
        int                             entityIdx,
        int                             sequenceIdx
    )
    {
        String      location = getLocationString (typeName, streamIdx, entityIdx, sequenceIdx);
            
        checkTestMessage (true, location, minfo, typeName, streamIdx, entityIdx, sequenceIdx);
    }

    public static void              checkTestMessage (
        boolean                         checkType,
        String                          location,
        StreamMessageSource<InstrumentMessage>             minfo,
        String                          typeName,
        int                             streamIdx,
        int                             entityIdx,
        int                             sequenceIdx
    )
    {
        InstrumentMessage               msg = minfo.getMessage ();
       
        assertNotNull (location + " msg", msg);

        location = location + " [actual msg: " + msg + " @" + msg.getTimeStampMs() + "]";
        
        assertEquals (location + "currentStreamKey", TEST_STREAM_KEYS [streamIdx], minfo.getCurrentStreamKey ());
        assertEquals (location + "currentStream.key", TEST_STREAM_KEYS [streamIdx], minfo.getCurrentStream ().getKey ());
        assertEquals (location + "entity", TEST_IDS [entityIdx], msg);

        if (checkType) {
            assertEquals (location + "currentType.name", typeName, minfo.getCurrentType ().getName ());

            if (msg instanceof RawMessage)
                assertEquals (location + "msg.type.name", typeName, ((RawMessage) msg).type.getName ());
            else
                assertEquals (location + "msg.class.name", typeName, msg.getClass ().getName ());
        }

        long                            t = msg.getTimeStampMs() - TEST_BASE_TIMESTAMP;

        assertEquals (location + "timestamp[entityIdx]", entityIdx, t % 100);
        assertEquals (location + "timestamp[streamIdx]", streamIdx, (t / 100) % 10);
        assertEquals (location + "timestamp[sequenceIdx]", sequenceIdx, t / 1000);

        if (msg instanceof IntMessage) 
            assertEquals (location + ".data", sequenceIdx, ((IntMessage) msg).data);
        else if (msg instanceof FloatMessage)
            assertEquals (location + ".data", sequenceIdx, ((FloatMessage) msg).data, 0.1);
        else if (msg instanceof StringMessage)
            assertEquals (location + ".data", "Seq #" + sequenceIdx, ((StringMessage) msg).data);
    }
}
