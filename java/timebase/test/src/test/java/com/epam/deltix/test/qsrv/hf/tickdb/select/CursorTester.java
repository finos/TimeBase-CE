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
package com.epam.deltix.test.qsrv.hf.tickdb.select;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import java.util.ArrayList;
import static com.epam.deltix.qsrv.testsetup.TickDBCreator.*;
import static com.epam.deltix.test.qsrv.hf.tickdb.select.TestDBUtil.*;

/**
 *
 */
public class CursorTester {
    public static boolean               DEBUG = false;
    public static final int             ALL = -1;

    private DXTickStream []             streams;
    private int                         streamMask = 0;
    private int                         entityMask = 0;
    private int                         typeMask = ALL;
    private InstrumentMessageSource     cursor;
    private int                         curStreamIdx;
    private int                         curEntityIdx;
    private int                         curTypeIdx;
    private int                         curSeqIdx;
    private boolean                     curPositionConsumed = false;
    private long                        debugCounter = 0;
    
    public CursorTester (DXTickDB db, SelectionOptions options) {
        streams = new DXTickStream [NUM_TEST_STREAMS];

        for (int ii = 0; ii < NUM_TEST_STREAMS; ii++)
            streams [ii] = db.getStream (TEST_STREAM_KEYS [ii]);

        cursor = db.createCursor (options);
    }

    public InstrumentMessageSource      getCursor () {
        return (cursor);
    }
    
    public int                          getEntityMask () {
        return entityMask;
    }

    public int                          getStreamMask () {
        return streamMask;
    }

    public int                          getTypeMask () {
        return typeMask;
    }

    private static String                      mstr (int mask, int limit) {
        StringBuilder   sb = new StringBuilder ("[");

        for (int n = 0; n < limit; n++) {
            if (mask == ALL)
                sb.append ('*');
            else if (((1 << n) & mask) != 0)
                sb.append (n);
            else
                sb.append (' ');
        }

        sb.append ("]");
        return (sb.toString ());
    }

    private static String                      emstr (int mask) {
        return (mstr (mask, NUM_SYMBOLS));
    }

    private static String                      tmstr (int mask) {
        return (mstr (mask, NUM_TYPES));
    }

    private static String                      smstr (int mask) {
        return (mstr (mask, NUM_TEST_STREAMS));
    }

    public void                         setAllEntities () {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": setAllEntities ()");

        entityMask = ALL;
        cursor.subscribeToAllEntities ();
    }

    public String                       stateToString () {
        return (
            String.format (
                "#%,05d Mask: S%s E%s T%s; (%s) N.S.E.T: %04d.%d.%d.%d; XTS: %d",
                debugCounter,
                smstr (streamMask),
                emstr (entityMask),
                tmstr (typeMask),
                curPositionConsumed ? "X" : "_",
                curSeqIdx, curStreamIdx, curEntityIdx, curTypeIdx,
                getTestTimestamp (curStreamIdx, curEntityIdx, curSeqIdx)
            )
        );
    }

    public void                         addEntities (int mask) {
        debugCounter++;
        
        if (DEBUG)
            System.out.println (stateToString () + ": addEntities (" + emstr (mask) + ")");

        if (entityMask == ALL)
            entityMask = 0;

        IdentityKey []   ids = new IdentityKey [NUM_SYMBOLS];
        int                     n = 0;

        for (int ii = 0; ii < NUM_SYMBOLS; ii++) {
            int             bit = 1 << ii;

            if ((mask & bit) != 0 && (entityMask & bit) == 0) {
                ids [n++] = TEST_IDS [ii];
                entityMask |= bit;
            }
        }

        if (DEBUG)
            System.out.println (stateToString () + ": adding Entities (" + n + ")");

        cursor.addEntities (ids, 0, n);
    }

    public void                         removeEntities (int mask) {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": removeEntities (" + emstr (mask) + ")");

        if (entityMask == ALL)
            entityMask = 0;

        IdentityKey []   ids = new IdentityKey [NUM_SYMBOLS];
        int                     n = 0;

        for (int ii = 0; ii < NUM_SYMBOLS; ii++) {
            int             bit = 1 << ii;

            if ((mask & bit) != 0 && (entityMask & bit) != 0) {
                ids [n++] = TEST_IDS [ii];
                entityMask &= ~bit;
            }
        }

        if (((1 << curEntityIdx) & entityMask) == 0) {
            curTypeIdx = 0;
            curPositionConsumed = false;
        }

        if (DEBUG)
            System.out.println (stateToString () + ": removing entities (" + n + ")");

        if (n == 0)
            cursor.clearAllEntities();
        else
            cursor.removeEntities (ids, 0, n);
    }

    public void                         addStreams (int mask) {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": addStreams (" + smstr (mask) + ")");

        for (int ii = 0; ii < NUM_TEST_STREAMS; ii++) {
            int             bit = 1 << ii;

            if ((mask & bit) != 0 && (streamMask & bit) == 0) {
                cursor.addStream (streams [ii]);
                streamMask |= bit;
            }
        }
    }
 
    public void                         removeStreams (int mask) {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": removeStreams (" + smstr (mask) + ")");

        for (int ii = 0; ii < NUM_TEST_STREAMS; ii++) {
            int             bit = 1 << ii;

            if ((mask & bit) != 0 && (streamMask & bit) != 0) {
                cursor.removeStream (streams [ii]);
                streamMask &= ~bit;
            }
        }

        if (((1 << curStreamIdx) & streamMask) == 0) {
            curTypeIdx = 0;
            curPositionConsumed = false;
        }
    }

    public void                         setAllTypes () {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": setAllTypes ()");

        typeMask = ALL;
        cursor.subscribeToAllTypes ();
    }

    public void                         addTypes (int mask) {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": addTypes (" + tmstr (mask) + ")");

        if (typeMask == ALL)
            typeMask = 0;

        ArrayList <String>      typeNames = new ArrayList <String> ();

        for (int ii = 0; ii < NUM_TYPES; ii++) {
            int             bit = 1 << ii;

            if ((mask & bit) != 0 && (typeMask & bit) == 0) {
                typeNames.add (TYPE_NAMES [ii]);
                typeMask |= bit;
            }
        }

        cursor.addTypes (typeNames.toArray (new String [typeNames.size ()]));
    }

    public void                         removeTypes (int mask) {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": removeTypes (" + tmstr (mask) + ")");

        if (typeMask == ALL)
            typeMask = 0;

        ArrayList <String>      typeNames = new ArrayList <String> ();

        for (int ii = 0; ii < NUM_TYPES; ii++) {
            int             bit = 1 << ii;

            if ((mask & bit) != 0 && (typeMask & bit) != 0) {
                typeNames.add (TYPE_NAMES [ii]);
                typeMask &= ~bit;
            }
        }

        cursor.removeTypes (typeNames.toArray (new String [typeNames.size ()]));
    }

    private boolean                      advance () {
        debugCounter++;

        if (DEBUG)
            System.out.println (stateToString () + ": advance ()");

        if (streamMask == 0 || entityMask == 0)
            return (false);

        for (;;) {
            if (curPositionConsumed)
                curPositionConsumed = false;
            else {
                if (((1 << curStreamIdx) & streamMask) != 0 &&
                    ((1 << curEntityIdx) & entityMask) != 0 &&
                    ((1 << curTypeIdx) & typeMask) != 0)
                    break;
            }

            curTypeIdx++;

            if (curTypeIdx >= NUM_TYPES) {
                curTypeIdx = 0;
                curEntityIdx++;

                if (curEntityIdx >= NUM_SYMBOLS) {
                    curEntityIdx = 0;
                    curStreamIdx++;

                    if (curStreamIdx >= NUM_TEST_STREAMS) {
                        curStreamIdx = 0;
                        curSeqIdx++;

                        if (curSeqIdx >= NUM_MESSAGES)
                            break;
                    }
                }
            }
        }

        if (curSeqIdx < NUM_MESSAGES) {
            curPositionConsumed = true;
            return (true);
        }
        else
            return (false);
    }

    public void                         reset (
        int                                 sequenceIdx,
        int                                 streamIdx,
        int                                 entityIdx
    )
    {
        debugCounter++;

        if (DEBUG)
            System.out.println (
                stateToString () + ": reset (" + sequenceIdx +
                ", " + streamIdx +
                ", " + entityIdx +
                ")"
            );

        curStreamIdx = streamIdx;
        curSeqIdx = sequenceIdx;
        curTypeIdx = 0;
        curEntityIdx = entityIdx;
        curPositionConsumed = false;

        long time = TEST_BASE_TIMESTAMP + 1000 * curSeqIdx + 100 * curStreamIdx + curEntityIdx;
        cursor.reset (time);
        //cursor.setTimeForNewSubscriptions(time);
    }

    public void                         close () {
        cursor.close ();
    }

    public boolean                      checkOne (boolean checkType) {
        return (checkOne (checkType, null));
    }

    public boolean                      checkOne (boolean checkType, Object lock) {
        boolean     shouldHaveNext = advance ();
        String      location = stateToString ();
        boolean     hasNext;

        if (lock == null)
            hasNext = cursor.next ();
        else {
            synchronized (lock) {
                for (;;) {
                    try {
                        hasNext = cursor.next ();
                        break;
                    } catch (UnavailableResourceException x) {
                        try {
                            lock.wait ();
                        } catch (InterruptedException xx) {
                            throw new UncheckedInterruptedException (xx);
                        }
                    }
                }
            }
        }

        if (!shouldHaveNext && hasNext)
            throw new AssertionError (
                location + ": Did not expect a message, but cursor returned: " +
                cursor.getMessage ()
            );

        if (shouldHaveNext && !hasNext)
            throw new AssertionError (
                location + ": Expected a message, but next () returned false"
            );

        if (hasNext) {
            checkTestMessage (
                checkType,
                location + ".",
                cursor,
                TYPE_NAMES [curTypeIdx], curStreamIdx, curEntityIdx, curSeqIdx
            );
        }

        if (DEBUG) {
            System.out.print (location + ": OK: ");
            
            if (hasNext)
                System.out.println (cursor.getMessage ());
            else
                System.out.println ("No message (as expected)");
        }

        return (shouldHaveNext);
    }
}
