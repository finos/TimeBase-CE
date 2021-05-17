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
package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *  Unchecked exception thrown by a cursor when the current message
 *  has been deleted due to stream truncation.
 */
public class StreamTruncatedException extends CursorException {
    public final String             streamKey;
    public final String             fileId;
    public final long               offset;
    public final long               nanoTime;

    public StreamTruncatedException (String streamKey, String fileId, long offset, long nanoTime) {
        super ("File " + fileId + " in stream " + streamKey + " has been truncated.");
        this.streamKey = streamKey;
        this.fileId = fileId;
        this.offset = offset;
        this.nanoTime = nanoTime;
    }
}