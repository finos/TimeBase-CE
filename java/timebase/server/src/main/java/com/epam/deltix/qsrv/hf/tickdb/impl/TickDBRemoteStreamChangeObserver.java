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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.hf.tickdb.impl.remotestreams.StreamChangeObserver;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.text.SimpleStringCodec;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Receives changes from remove stream change log and applies them to local {@link TickDBImpl}.
 *
 * @author Alexei Osipov
 */
class TickDBRemoteStreamChangeObserver implements StreamChangeObserver {
    private static final Log SYNC_LOGGER = LogFactory.getLog(TickDBRemoteStreamChangeObserver.class);

    private final TickDBImpl tickDB;
    private final Map<String, ServerStreamImpl> streams;
    private final ReadWriteLock streamsLock;
    private final AbstractPath remoteStreamRootPath;

    TickDBRemoteStreamChangeObserver(TickDBImpl tickDB, Map<String, ServerStreamImpl> streams, ReadWriteLock streamsLock, AbstractPath remoteStreamRootPath) {
        this.tickDB = tickDB;
        this.streams = streams;
        this.streamsLock = streamsLock;
        this.remoteStreamRootPath = remoteStreamRootPath;
    }

    @Override
    public void streamAdded(String streamKey) {
        AbstractPath streamMetadataPath = getStreamMetadataPath(streamKey);

        if (!streamMetadataPath.exists()) {
            SYNC_LOGGER.warn("Failed to sync created stream %s because metadata file does not exist").with(streamKey);
            return;
        }

        DXTickStream loadedStream = TickStreamImpl.read(streamMetadataPath);
        if (!(loadedStream instanceof PDStream)) {
            SYNC_LOGGER.warn("Failed to sync created stream %s because metadata defines unexpected stream type: %s")
                    .with(streamKey)
                    .with(loadedStream.getClass());
            return;
        }
        PDStream pdStream = (PDStream) loadedStream;


        FileLocation sf = new FileLocation(streamMetadataPath);
        streamsLock.writeLock().lock();
        try {
            if (streams.containsKey(streamKey)) {
                SYNC_LOGGER.error("Failed to sync created stream %s because stream with same key already exists")
                        .with(streamKey);
                return;
            }

            String streamName = pdStream.getName();
            if (streamName != null) {
                for (DXTickStream tickStream : streams.values()) {
                    if (StringUtils.equals(tickStream.getName(), streamName)) {
                        SYNC_LOGGER.error("Failed to sync created stream %s because stream with same name (\"%s\") already exist")
                                .with(streamKey).with(streamName);
                    }
                }
            }

            pdStream.init(tickDB, sf);

            streams.put(streamKey, pdStream);
            pdStream.addStateListener(tickDB);

            pdStream.open(false); // This have to be done in the sync block. Otherwise client might see incomplete stream.
        } finally {
            streamsLock.writeLock().unlock();
        }

        tickDB.fireCreated(pdStream);

        SYNC_LOGGER.info("Successfully synchronized remote stream \"%s\"").with(streamKey);
    }

    @Override
    public void streamRemoved(String streamKey) {
        ServerStreamImpl stream = (ServerStreamImpl) tickDB.getStream(streamKey);
        if (stream == null) {
            SYNC_LOGGER.warn("Failed to sync deleted stream %s because stream does not exist").with(streamKey);
            return;
        }

        // We have to acquire locks in that order (stream itself first and then TickDBImpl.stream) to avoid deadlocks.
        synchronized (stream) {
            streamsLock.writeLock().lock();
            try {
                // We have to re-checks stream presence
                if (!streams.containsKey(streamKey)) {
                    SYNC_LOGGER.warn("Failed to sync deleted stream %s because stream does not exist").with(streamKey);
                    return;
                }

                if (!(stream instanceof PDStream)) {
                    SYNC_LOGGER.warn("Failed to sync deleted stream %s because existing stream does match type expectations").with(streamKey);
                    return;
                }

                PDStream pdStream = (PDStream) stream;
                if (!pdStream.isRemoteMetadata()) {
                    SYNC_LOGGER.warn("Failed to sync deleted stream %s because existing stream is not remote").with(streamKey);
                    return;
                }

                try {
                    pdStream.lock(LockType.WRITE);
                } catch (StreamLockedException e) {
                    SYNC_LOGGER.warn("Deleting already locked stream %s").with(streamKey);
                }

                pdStream.deleteLocalStream();
                tickDB.streamDeleted(streamKey);
            } finally {
                streamsLock.writeLock().unlock();
            }
        }

        SYNC_LOGGER.info("Successfully deleted remote stream \"%s\"").with(streamKey);
    }

    @Override
    public void streamRenamed(String oldStreamKey, String newStreamKey) {
        DXTickStream stream = tickDB.getStream(oldStreamKey);
        if (stream == null) {
            SYNC_LOGGER.warn("Failed to sync renamed stream %s because source stream does not exist").with(oldStreamKey);
            return;
        }

        // We have to acquire locks in that order (stream itself first and then TickDBImpl.stream) to avoid deadlocks.
        synchronized (stream) {
            streamsLock.writeLock().lock();
            try {
                // We have to re-checks stream presence
                if (!streams.containsKey(oldStreamKey)) {
                    SYNC_LOGGER.warn("Failed to sync renamed stream %s because source stream does not exist").with(oldStreamKey);
                    return;
                }

                if (streams.containsKey(newStreamKey)) {
                    SYNC_LOGGER.warn("Failed to sync renamed stream %s because destination %s stream already exists").with(oldStreamKey).with(newStreamKey);
                    return;
                }

                if (!(stream instanceof PDStream)) {
                    SYNC_LOGGER.warn("Failed to sync renamed stream %s because existing stream does match type expectations").with(oldStreamKey);
                    return;
                }

                PDStream pdStream = (PDStream) stream;

                tickDB.streamRenamed(oldStreamKey, newStreamKey);
                pdStream.renameInternal(newStreamKey, oldStreamKey, false, false);
            } finally {
                streamsLock.writeLock().unlock();
            }
        }
        SYNC_LOGGER.info("Successfully renamed remote stream \"%s\" to \"%s\"").with(oldStreamKey).with(newStreamKey);
    }

    @Override
    public void streamUpdated(String streamKey, EnumSet<TickStreamPropertiesEnum> changeSet) {
        DXTickStream stream = tickDB.getStream(streamKey);
        if (stream == null) {
            SYNC_LOGGER.warn("Failed to sync updated stream %s because source stream does not exist").with(streamKey);
            return;
        }

        AbstractPath streamMetadataPath = getStreamMetadataPath(streamKey);

        if (!streamMetadataPath.exists()) {
            SYNC_LOGGER.warn("Failed to sync created stream %s because metadata file does not exist").with(streamKey);
            return;
        }

        DXTickStream loadedStream = TickStreamImpl.read(streamMetadataPath);

        if (!(loadedStream instanceof PDStream)) {
            SYNC_LOGGER.warn("Failed to sync updated stream %s because loaded stream does match type expectations").with(streamKey);
            return;
        }

        // We have to acquire locks in that order (stream itself first and then TickDBImpl.stream) to avoid deadlocks.
        synchronized (stream) {
            streamsLock.writeLock().lock();
            try {
                // We have to re-checks stream presence
                if (!streams.containsKey(streamKey)) {
                    SYNC_LOGGER.warn("Failed to sync updated stream %s because source stream does not exist").with(streamKey);
                    return;
                }

                if (!(stream instanceof PDStream)) {
                    SYNC_LOGGER.warn("Failed to sync updated stream %s because existing stream does match type expectations").with(streamKey);
                    return;
                }

                PDStream pdStream = (PDStream) stream;

                pdStream.updateFromRemote(changeSet, (PDStream) loadedStream);
            } finally {
                streamsLock.writeLock().unlock();
            }
        }
        SYNC_LOGGER.info("Successfully updated remote stream \"%s\"").with(streamKey);
    }

    private AbstractPath getStreamMetadataPath(String streamKey) {
        String encodedKey = SimpleStringCodec.DEFAULT_INSTANCE.encode(streamKey);
        return remoteStreamRootPath.append(encodedKey).append(encodedKey + TickDBImpl.STREAM_EXTENSION);
    }
}
