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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import java.io.*;
import java.util.Properties;

/**
 *
 */
public class FileUtils {
    static int      readUByte (InputStream is) throws IOException {
        return (readByte (is) & 0xFF);
    }
    
    static int      readByte (InputStream is) throws IOException {
        int     b = is.read ();
        
        if (b < 0)
            throw new EOFException ();
        
        return (b);
    }

    public static Properties readProperties(AbstractPath path) throws IOException {
        Properties props = new Properties();
        try (InputStream is = BufferedStreamUtil.wrapWithBuffered(path.openInput(0))) {
            props.load(is);
        } catch (FileNotFoundException x) {
            // ignore
        }

        return props;
    }

    
}