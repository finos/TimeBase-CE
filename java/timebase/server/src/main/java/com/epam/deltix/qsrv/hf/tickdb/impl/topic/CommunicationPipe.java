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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Provides an {@link OutputStream} connected to {@link InputStream} with intermediate buffer.
 * Mainly used for communication between threads.
 *
 * @author Alexei Osipov
 */
public class CommunicationPipe {
    private final PipedOutputStream outputStream;
    private final PipedInputStream inputStream;

    public CommunicationPipe() {
        this(8 * 1024);
    }

    public CommunicationPipe(int bufferSize) {
        this.outputStream = new PipedOutputStream();
        try {
            this.inputStream = new PipedInputStream(this.outputStream, bufferSize);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}