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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinterFactory;

import java.io.IOException;
import java.io.Writer;

public class JsonIMSPrinter implements IMSPrinter {
    public static final char CR = '\n';

    private InstrumentMessageSource ims;
    private boolean closeWhenDone;
    private int count;
    private int maxCount = Integer.MAX_VALUE;
    private Writer out;
    private boolean newLine = true;
    private final StringBuilder sb = new StringBuilder();

    private final JSONRawMessagePrinter printer = JSONRawMessagePrinterFactory.createForTickDBShell();

    public JsonIMSPrinter(Writer out) {
        this.out = out;
    }

    @Override
    public void setOut(Writer out) {
        this.out = out;
    }

    @Override
    public void setIMS(
            InstrumentMessageSource ims,
            boolean closeWhenDone
    ) {
        this.ims = ims;
        this.closeWhenDone = closeWhenDone;
        this.count = 0;
    }

    @Override
    public InstrumentMessageSource getIMS() {
        return (ims);
    }

    @Override
    public int getMaxCount() {
        return maxCount;
    }

    @Override
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    @Override
    public void printAll() throws IOException {
        try {
            while (count < maxCount && ims.next()) {
                printMessage(ims);
                count++;
            }
        } finally {
            if (closeWhenDone)
                ims.close();
        }
    }

    private void println() throws IOException {
        out.write(CR);
        out.flush();
        newLine = true;
    }

    public void printMessage(InstrumentMessageSource msginfo)
            throws IOException {

        RawMessage rmsg = (RawMessage) msginfo.getMessage();
        sb.setLength(0);
        sb.append(count).append(": ");
        printer.append(rmsg, sb);
        out.write(sb.toString());
        println();
    }
}