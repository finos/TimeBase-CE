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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CursorCommandProcessor {
    private static final boolean DEBUG_COMM = DownloadHandler.DEBUG_COMM;

    private final InstrumentMessageSource cursor;
    private final TickCursor tcursor;

    private final Principal user;
    private final String remoteAddress;
    private final String remoteApplication;

    private final StringBuilder sb = new StringBuilder();

    public CursorCommandProcessor(InstrumentMessageSource cursor, @Nullable TickCursor tcursor, Principal user, String remoteAddress, String remoteApplication) {
        this.cursor = cursor;
        this.tcursor = tcursor;
        this.user = user;
        this.remoteAddress = remoteAddress;
        this.remoteApplication = remoteApplication;
    }

    private String getRemoteAddress() {
        return remoteAddress;
    }
    private String getRemoteApplication() {
        return remoteApplication;
    }

    public void processAddStreams(long serial, long time, DXTickStream[] streams) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " addStreams #" + serial + " at " + time);
        }

        if (UserLogger.canTrace(user)) {
            sb.setLength(0);
            DownloadHandler.toString(sb, streams);
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, sb.toString());
        }

        cursor.setTimeForNewSubscriptions (time);
        cursor.addStream (streams);
    }


    public void processRemoveStreams(long serial, DXTickStream[] streams) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " removeStreams #" + serial);
        }

        if (UserLogger.canTrace(user)) {
            sb.setLength(0);
            DownloadHandler.toString(sb, streams);
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.UNSUBSCRIBE_PATTERN, cursor, sb.toString());
        }

        cursor.removeStream (streams);
    }

    public void processRemoveAllStreams(long serial) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info("SERVER: " + cursor + " removeAllStreams #" + serial);
        }

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.UNSUBSCRIBE_PATTERN, cursor, "STREAMS {<ALL>}");

        cursor.removeAllStreams();
    }

    public void processAllEntities(long serial, long time) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info("SERVER: " + cursor + " allEntities #" + serial + " at " + time);
        }

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, "INSTRUMENTS {<ALL>}");

        cursor.setTimeForNewSubscriptions(time);
        cursor.subscribeToAllEntities();
    }

    public void processAddEntities(long serial, long time, IdentityKey[] ids) throws IOException {
        if (DEBUG_COMM) {
            StringBuilder sb = new StringBuilder();
            sb.append("SERVER: ").append(cursor).append(" addEntities #").append(serial).append(" at ").append(time).append(" (");
            for (IdentityKey id : ids) sb.append(" ").append(id);
            sb.append(")\n");
            TickDBServer.LOGGER.info(sb.toString());
        }

        if (UserLogger.canTrace(user)) {
            sb.setLength(0);
            DownloadHandler.toString(sb, time, ids);
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, sb.toString());
        }

        cursor.setTimeForNewSubscriptions (time);
        cursor.addEntities (ids, 0, ids.length);
    }

    public void processAddEntitiesTypes(
            long serial,
            long time,
            IdentityKey[] ids,
            String[] types
    ) throws IOException
    {
        if (DEBUG_COMM) {
            StringBuilder sb = new StringBuilder();
            sb.append("SERVER: ").append(cursor).append(" addEntities&Types #").append(serial).append(" at ").append(time).append(" (");
            for (IdentityKey id : ids) sb.append(" ").append(id);
            sb.append("; ");
            for (String name : types) sb.append(" ").append(name);
            sb.append(")\n");
            TickDBServer.LOGGER.info(sb.toString());
        }

        if (UserLogger.canTrace(user)) {
            sb.setLength(0);
            DownloadHandler.toString(sb, time, ids);
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, sb.toString());
        }

        cursor.setTimeForNewSubscriptions (time);
        cursor.add(ids, types);
        checkCursorState();
    }

    public void processRemoveEntitiesTypes(long serial, IdentityKey[] ids, String[] types) throws IOException {
        if (DEBUG_COMM) {
            StringBuilder sb = new StringBuilder();
            sb.append("SERVER: ").append(cursor).append(" removeEntities&Types #").append(serial).append(" (");
            for (IdentityKey id : ids) sb.append(" ").append(id);
            sb.append("; ");
            for (String name : types) sb.append(" ").append(name);
            sb.append(")\n");
            TickDBServer.LOGGER.info(sb.toString());
        }

        if (UserLogger.canTrace(user)) {
            sb.setLength(0);
            DownloadHandler.toString(sb, ids);
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.UNSUBSCRIBE_PATTERN, cursor, sb.toString());
        }

        cursor.remove(ids, types);
        checkCursorState();
    }

    public void processRemoveEntities(long serial, IdentityKey[] ids) throws IOException {
        if (DEBUG_COMM) {
            StringBuilder sb = new StringBuilder();
            sb.append("SERVER: ").append(cursor).append(" removeEntities #").append(serial).append(" (");
            for (IdentityKey id : ids) sb.append(" ").append(id);
            sb.append(")\n");
            TickDBServer.LOGGER.info(sb.toString());
        }

        if (UserLogger.canTrace(user)) {
            sb.setLength(0);
            DownloadHandler.toString(sb, ids);
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.UNSUBSCRIBE_PATTERN, cursor, sb.toString());
        }

        cursor.removeEntities (ids, 0, ids.length);
    }

    public void processClearEntities(long serial) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " clearEntities #" + serial);
        }

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.UNSUBSCRIBE_PATTERN, cursor, "INSTRUMENTS {<ALL>}");
        }

        cursor.clearAllEntities ();
    }

    public void processAllTypes(long serial) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " allTypes #" + serial);
        }

        cursor.subscribeToAllTypes ();
    }

    public void processAddTypes(long serial, String[] names) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " addTypes #" + serial);
        }

        cursor.addTypes (names);
        checkCursorState();
    }

    public void processSetTypes(long serial, String[] names) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " setTypes #" + serial);
        }

        cursor.setTypes(names);
        checkCursorState();
    }

    public void processRemoveTypes(long serial, String[] names) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " removeTypes #" + serial);
        }

        cursor.removeTypes (names);
        checkCursorState();
    }


    public void processResetTime(long serial, long time) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " resetTime #" + serial + " t=" + time);
        }
        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, getRemoteAddress(), getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, "RESET {" + time + "}");
        }
        cursor.reset (time);
    }

    private void checkCursorState() {
        checkCursorState(cursor, tcursor);
    }

    private static void checkCursorState(InstrumentMessageSource cursor, @Nullable TickCursor tcursor) {
        if (cursor instanceof SubscriptionManager) {
            SubscriptionManager manager = (SubscriptionManager) cursor;

            // have subscribed entities, but do not have subscribed types
            boolean restricted = !manager.hasSubscribedTypes() && (manager.getSubscribedEntities().length > 0 || manager.isAllEntitiesSubscribed());

            if (restricted) {
                if (tcursor == null || (tcursor instanceof TBCursor && ((TBCursor) tcursor).getSourceStreamKeys().length > 0)) {
                    TickDBServer.LOGGER.log(Level.WARNING, cursor + ": FAST_FORWARD state.");
                }
            }
        }
    }
}