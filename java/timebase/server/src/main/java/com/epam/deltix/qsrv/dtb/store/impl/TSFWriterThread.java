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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.qsrv.dtb.store.codecs.BlockCompressor;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.lang.StringUtils;

/**
 *
 */
class TSFWriterThread extends Thread {
    private static final Log LOGGER = PDSImpl.LOGGER;
    private final PDSImpl       pds;
    private final int           index;

    // compression related
    private BlockCompressor     compressor = null;
    private String              compression   = null;

    private final ByteArrayList buffer = new ByteArrayList();

    TSFWriterThread (PDSImpl pds, int idx) {
        super ("TSF Writer Thread #" + idx);
        this.pds = pds;
        this.index = idx;

        // Should not be a daemon thread because it executes IO operations
    }

    @Override
    public void                 run () {

        try {
            for (;;) {
                boolean logThisOne = LOGGER.isDebugEnabled();

                final TSFile          tsf = pds.getTSFToWrite (index);
                final TSRootFolder    root = tsf.root;

                if (!StringUtils.equals(root.getCompression(), compression)) {
                    compression = root.getCompression();
                    compressor = root.createCompressor(buffer);
                }

                // file destroyed
                if (tsf.getState() == null) {
                    LOGGER.warn().append("File was closed: ").append(tsf).commit();
                    pds.fileHasFailed(tsf, null);
                    continue;
                }

                boolean lockAcquired = false;

                try {
                    // should be under try/catch to not crash WriterThread
                    try {
                        lockAcquired = root.acquireWriteLock();
                    } catch (IllegalStateException ex) {
                        // root is invalid
                        LOGGER.warn().append("Cannot get lock on ").append(root).commit();
                        continue;
                    }

                    // if file was checked out again, it's fine to store it assuming we have locks on method
                    // DataAccessorBase.getBlockLink(int, long) and data won't be corrupted

                    if (tsf.getState() == TSFState.DIRTY_CHECKED_OUT) {
                        LOGGER.info().append("Storing ").append(tsf).append(" with DIRTY_CHECKED_OUT state." ).commit();
                    }

//                    if (tsf.getState() == TSFState.DIRTY_CHECKED_OUT) {
//                        pds.fileWasStored(tsf);
//                        LOGGER.warn().append("Skip storing ").append(tsf).append(" ...").commit();
//                        continue;
//                    } else if (tsf.getState() != TSFState.DIRTY_QUEUED_FOR_WRITE) {
//                        LOGGER.warn().append("Storing ").append(tsf).append(" with wrong state: ").append(tsf.getState()).commit();
//                    }

                    root.storeAdditionalDirtyData ();
                    TreeOps.storeIndexFile(tsf.getParent());

                    if (tsf.isDropped()) {
                        if (logThisOne)
                            LOGGER.debug().append("Dropping ").append(tsf.getPath().getPathString()).append(" ...").commit();

                        // finalize index before deleting file, while folder is still "used"
                        TreeOps.finalizeIndex(tsf.getParent());

                        // delete file, because we can have last usage here
                        TreeOps.tryDrop(tsf);

                        pds.fileWasDropped (tsf);
                    } else {
                        if (logThisOne)
                            LOGGER.debug().append("Storing ").append(tsf).append(" ...").commit();

                        if (tsf.store(compressor)) {
                            if (logThisOne)
                                LOGGER.debug().append(tsf).append(" was stored [").append(tsf.getState()).append("]").commit();
                        } else {
                            if (logThisOne)
                                LOGGER.debug().append(tsf).append(" wasn't stored [").append(tsf.getState()).append("]").commit();
                        }

                        // finalize index in any case
                        TreeOps.finalizeIndex(tsf.getParent());

                        pds.fileWasStored(tsf);
                    }
                } catch (Throwable x) {
                    pds.fileHasFailed (tsf, x);
                } finally {
                    if (lockAcquired)
                        root.releaseWriteLock ();
                }
            }
        } catch (InterruptedException x) {
            LOGGER.debug().append(getName()).append(" is interrupted. Terminating.").commit();
        } catch (Throwable x) {
            LOGGER.error().append(getName()).append(" is crashed.").append(x).commit();
            pds.writerFailed(this);
        }
    }
}