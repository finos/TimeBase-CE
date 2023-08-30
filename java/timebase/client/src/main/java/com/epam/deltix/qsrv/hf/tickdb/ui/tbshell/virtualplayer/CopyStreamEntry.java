/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

/**
 * @author Alexei Osipov
 */
final class CopyStreamEntry<T extends TimeStampedMessage> {
    private final MessageSource<T> src;
    private final MessageChannel<T> dst;
    private final SchemaConverter schemaConverter;

    public CopyStreamEntry(MessageSource<T> src, MessageChannel<T> dst, SchemaConverter schemaConverter) {
        this.src = src;
        this.dst = dst;
        this.schemaConverter = schemaConverter;
    }

    public MessageSource<T> getSrc() {
        return src;
    }

    public MessageChannel<T> getDst() {
        return dst;
    }

    public SchemaConverter getConverter() {
        return schemaConverter;
    }
}