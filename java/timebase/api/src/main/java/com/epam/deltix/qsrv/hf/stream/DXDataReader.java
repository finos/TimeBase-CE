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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.util.archive.DXDataEntry;
import com.epam.deltix.qsrv.util.archive.DXDataInputStream;
import com.epam.deltix.qsrv.util.archive.DXHeaderEntry;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DataExchangeUtils;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class DXDataReader implements Disposable{

    private final DXDataInputStream     in;
    private ClassSet                    classes;
    private DXDataEntry                 current;
    
    private final long[]                range = new long[2];

    public DXDataReader(File file) throws IOException {
        this.in = new DXDataInputStream(file);
        DXHeaderEntry header = (DXHeaderEntry) in.getNextEntry();
        
        range[0] = DataExchangeUtils.readLong(header.data, 0);
        range[1] = DataExchangeUtils.readLong(header.data, 8);
    }

    public ConsumableMessageSource<InstrumentMessage> readBlock() throws IOException {
        DXDataEntry entry = (DXDataEntry) in.getNextEntry();
        if (entry != null) {
            if (entry.getName().endsWith("xml"))
                classes = Protocol.readTypes(new DataInputStream(in));

            current = (DXDataEntry) in.getNextEntry();
            GZIPInputStream gzip = new GZIPInputStream(new BufferedInputStream(in, 1 << 14), 1 << 14);
            return new MessageReader2(gzip, current.getSize(), classes.getContentClasses());
        }

        return null;
    }

    public boolean nextBlock() throws IOException {
        DXDataEntry entry = (DXDataEntry) in.getNextEntry();

        if (entry != null) {
            if (entry.getName().endsWith("xml"))
                Protocol.readTypes(new DataInputStream(in));

            current = (DXDataEntry) in.getNextEntry();
        }

        return entry != null;
    }

	public RecordClassDescriptor[] 	   getBlockTypes() {
        return classes.getContentClasses();
    }

	public String 	                    getBlockName() {
        return current.getName();
    }

    public long[] 						getTimeRange() {
        return range;
    }

    @Override
    public void                         close() {
        Util.close(in);
    }
}