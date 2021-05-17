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

import com.epam.deltix.util.io.RandomAccessFileStore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LockFile extends RandomAccessFileStore {
    private final Charset charset = StandardCharsets.US_ASCII;
    private final State state;

    public enum State {
        CLOSED,
        TERMINATED
    }

    private static final byte []                MAGIC = { 'R', 'O', 'O', 'T' };
    
    public  LockFile(File f, State state) {
        super(f);

        this.state = state;
    }

    public State getState() {
        return state;
    }

    //    static boolean       isRoot(File lockFile) throws IOException {
//        byte[] bytes = new byte[4];
//
//        try {
//            IOUtil.readBytes(lockFile, bytes, 0, 4);
//            return Util.arraycomp(MAGIC, 0, bytes, 0, MAGIC.length) == 0;
//        } catch (EOFException e) {
//            return false;
//        }
//    }

    void       writeMagic(String magic) throws IOException {
        raf.seek(0);
        raf.write(magic.getBytes(charset));
    }

    String     readMagic() throws IOException {
        raf.seek(0);
        byte[] buffer = new byte[(int) raf.length()];
        raf.readFully(buffer);
        return new String(buffer, charset);
    }
}
