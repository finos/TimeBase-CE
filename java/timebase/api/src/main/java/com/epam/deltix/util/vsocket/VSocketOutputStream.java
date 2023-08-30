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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.collections.ByteQueue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

/**
 *
 */
public class VSocketOutputStream extends OutputStream {
    private final String socketIdStr;

    public static int           CAPACITY = 1024 * 512;
    public static int           INCREMENT = CAPACITY / 4;

    private final ByteQueue     buffer;
    private final OutputStream  out;
    long                        confirmed;

    // How many bytes we need to skip from the output buffer to make it in-sync with last "confirmed" position
    private int bufferDebt = 0;

    // Set to true if we got any IOException from output stream
    private volatile boolean broken = false;

    public VSocketOutputStream(OutputStream out, String socketIdStr) {
        this.out = out;
        this.socketIdStr = socketIdStr;
        this.buffer = new ByteQueue(CAPACITY);
    }

    @Override
    public void     write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void     write(int b) {
        try {
            synchronized (out) {
                out.write(b);
            }
        } catch (IOException e) {
            broken = true;
            //throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            dump(b);
        }
    }

    @Override
    public void     write(byte[] b, int off, int len) {
        try {
            synchronized (out) {
                out.write(b, off, len);
            }
        } catch (IOException e) {
            broken = true;
            //throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            dump(b, off, len);
        }
    }

    /**
     * Same as calling {@link #write(byte[], int, int)} two times but a little bit more effective.
     */
    @SuppressWarnings("SameParameterValue")
    public void     writeTwoArrays(byte[] b1, int off1, int len1, byte[] b2, int off2, int len2) {
        try {
            synchronized (out) {
                out.write(b1, off1, len1);
                out.write(b2, off2, len2);
            }
        } catch (IOException e) {
            broken = true;
            //throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            dumpTwoArrays(b1, off1, len1, b2, off2, len2);
        }
    }

    private void    dump(byte[] b, int off, int len) {
        synchronized (buffer) {
            dumpInternal(b, off, len);
        }
    }

    /**
     * Same as calling {@link #dump(byte[], int, int)} two times but a little bit more effective.
     */
    private void    dumpTwoArrays(byte[] b1, int off1, int len1, byte[] b2, int off2, int len2) {
        synchronized (buffer) {
            dumpInternal(b1, off1, len1);
            dumpInternal(b2, off2, len2);
        }
    }

    private void dumpInternal(byte[] b, int off, int len) {
        // assert Thread.holdsLock(buffer);
        int overflow = buffer.size() + len - buffer.capacity();
        if (overflow > 0) {
            int incrementsToAdd = divideRoundUp(overflow, INCREMENT);
            buffer.addCapacity(INCREMENT * incrementsToAdd);
        }

        buffer.offer(b, off, len);
    }

    static int divideRoundUp(int val, int divisor) {
        int result = val / divisor;
        if (val > result * divisor) {
            result += 1;
        }
        return result;
    }

    private void    dump(int b) {
        synchronized (buffer) {
            if (buffer.size() + 1 > buffer.capacity())
                buffer.addCapacity(INCREMENT);

            buffer.offer(b);
        }
    }

    @Override
    public void     flush() throws IOException {
        try {
            synchronized (out) {
                out.flush();
            }
        } catch (IOException e) {
            broken = true;
            throw e;
        }
    }

    @Override
    public void     close() throws IOException {
        synchronized (out) {
            out.close();
        }
    }

    /*
        Returns number of bytes written to stream
     */
    public long             getBytesWritten() {
        return confirmed + buffer.size();
    }

    public void     confirm(long size) {

        synchronized (buffer) {
            int length = (int) (size - confirmed);
            int bufferSize = buffer.size();
            if (length > bufferSize) {
                bufferDebt = length - bufferSize;
                length = bufferSize;
            } else {
                bufferDebt = 0;
            }

            // buffer is not synchronized with out, so
            // confirmed bytes signal maybe processed before dump() occurred
            if (length > 0) {
                buffer.skip(length);
                confirmed += length;
            }
        }
    }

    boolean hasUnconfirmedData() {
        return buffer.size() - bufferDebt > 0;
    }

    public void     writeTo(VSocketOutputStream out) throws IOException {
        synchronized (buffer) {
            if (bufferDebt > buffer.size()) {
                throw new IllegalStateException("We confirmed more than we have in buffer");
            } else {
                buffer.skip(bufferDebt);
            }
            if (VSProtocol.LOGGER.isLoggable(Level.FINE)) {
                VSProtocol.LOGGER.fine("Sending output buffer of " + out.socketIdStr + " with size " + buffer.size());
            }
            buffer.poll(out);
        }
    }

    public boolean isBroken() {
        return broken;
    }

    @Override
    public String toString() {
        return getClass().getName() + socketIdStr;
    }
}