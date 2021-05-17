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

/**
 *
 */
final class LosslessMessageQueueReader 
    extends MessageQueueReader <LosslessMessageQueue>
{
    private int offset = 0;
    
    LosslessMessageQueueReader (LosslessMessageQueue mq) {
        super (mq);
    }

    @Override
    public void                 close () {
        mfile.rawReaderClosed (this);
    }

    @Override
    public int                  getBufferOffset() {
        return offset;
    }

    @Override
    public int                  getBufferSize() {
        return buffer.length - offset;
    }

    @Override
    protected void              invalidateBuffer() {
        if (available() > 0) {  // buffer contains incomplete message
            offset = buffer.length - bufferPosition;

            // if remaining part > half of buffer - extend buffer
            if (offset > buffer.length / 2) {
                byte[] temp = new byte[buffer.length * 2];
                System.arraycopy(buffer, bufferPosition, temp, 0, offset);
                buffer = temp;
            } else {
                System.arraycopy(buffer, bufferPosition, buffer, 0, offset);
            }

            bufferFileOffset = getCurrentOffset() - offset;
        } else {
            offset = 0;
            bufferFileOffset = getCurrentOffset();
        }

        bufferPosition = 0;
    }
}
