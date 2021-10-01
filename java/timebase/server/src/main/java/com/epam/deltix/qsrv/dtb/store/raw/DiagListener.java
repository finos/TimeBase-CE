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
package com.epam.deltix.qsrv.dtb.store.raw;

/**
 *
 */
public class DiagListener {
    public void     unknownFormat (
        RawNode         node,
        int             formatVersion
    )
    {        
    }

    public void     illegalVersionInHeader (
        RawNode         node,
        long             version
    )
    {        
    }

    public void     duplicateChildId (
        RawFolder       folder, 
        RawFolderEntry  prev, 
        int             nextIdx, 
        RawFolderEntry  next
    )
    {        
    }

    public void     badEntityOrder (
        RawNode         node, 
        int             prevEntity, 
        int             nextIdx, 
        int             nextEntity
    )
    {        
    }

    public void     badFirstChildIdInIndex (
        RawFolder       folder, 
        int             atIdx, 
        int             firstId
    )
    {        
    }

    public void     badLastChildIdInIndex (
        RawFolder       folder, 
        int             atIdx, 
        int             lastId
    )
    {        
    }

    public void             badFolderIndexRange (RawFolder folder, int atIdx, int fidx, int lidx) {
    }

    public void             fileTooShortForIndex (RawTSF tsf, int blockOffset, long physicalLength) {
    }

    public void             fileTooShortForBlock (RawTSF tsf, RawDataBlock rdb, long physicalLength) {
    }

    public void             badFileIndexRange (RawTSF tsf, RawDataBlock rdb) {
    }

    public void             fileLengthDiscrepancy (RawTSF tsf, int blockOffset, long physicalLength) {
    }

    public void             unindexedEntity (RawFolder folder, RawFolderEntry child, int entity) {
    }

    public void             entityOutOfIndexedRange (RawFolder folder, RawEntityIndexEntry entry, RawFolderEntry child) {
    }

    public void             indexedEntityNotPresent (RawFolder folder, RawEntityIndexEntry actual) {
    }

    public void             entityRangeDiscrepancy (RawFolder folder, RawEntityIndexEntry observed, RawEntityIndexEntry readFromIndex) {
    }

    public void            firstChildHasWrongTimestamp (RawFolder folder, RawFolderEntry rfe, long startTimestamp) {
    }

    public void            badChildTimeOrder (RawFolder folder, RawFolderEntry prev, RawFolderEntry rfe) {
    }

    public void            childTimeBeyondLimit (RawFolder folder, RawFolderEntry rfe, long limitTimestamp) {
    }

    public void            processingFolder (RawFolder folder) {
    }

    public void            processingFile (RawTSF file) {
    }

    public void             blockOutOfTimeRange (RawTSF file, RawDataBlock rdb, long startTimestamp, long limitTimestamp) {
    }

    public void             unreadableTimestamp (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, Throwable x) {
    }

    public void             firstTimestampMismatch (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts) {
    }

    public void             endOfBlockReadingMessageType (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, int position) {
    }

    public void             unreadableBodySize (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, boolean inHead, int sizeOffset, Throwable x) {
    }

    public void             tailSizeMismatch (
        RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, 
        int tailSizeOffset, int tailSize
    )
    {
    }

    public void             endOfBlockReadingBody (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, int bodyOffset, int size) {
    }

    public void             lastTimestampMismatch (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts) {
    }

    public void             messageOutOfOrder (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long prevTimestamp, long ts) {
    }
}