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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.codec.ClassCodecFactory;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.util.cmdline.DefaultApplication;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: BazylevD
 * Date: Mar 26, 2009
 * Time: 3:25:26 PM
 */
public class DumpCodecs extends DefaultApplication {
    public DumpCodecs(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Throwable {
        new DumpCodecs(args).run();
    }

    protected void run() throws Exception {
        final String streamXml = getArgValue("-xml");
        final String inDirectory = getArgValue("-src");
        final String outDirectory = getMandatoryArgValue("-out");
        if ((streamXml == null && inDirectory == null) ||
                (streamXml != null && inDirectory != null)) {
            System.out.println("Either xml or scr parameter must be specified.");
            return;
        }

        if (inDirectory != null) {
            File dir = new File(inDirectory);
            String[] fileNames = dir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml") && !name.equalsIgnoreCase("md.xml");
                }
            });
            if (fileNames == null || fileNames.length == 0) {
                System.out.println("Files are not found");
                return;
            }
            for (String name : fileNames)
                dumpStream(inDirectory + "\\" + name, outDirectory);
        } else
            dumpStream(streamXml, outDirectory);
    }

    private void dumpStream(String streamXml, String outDirectory) throws Exception {
        final DXTickStream s = TickStreamImpl.read(new File(streamXml));
        final ClassDescriptor[] cds = s.getAllDescriptors();
        for (ClassDescriptor cd : cds) {
            final RecordLayout layout = new RecordLayout((RecordClassDescriptor) cd);
            
            layout.bind(TypeLoaderImpl.DEFAULT_INSTANCE);
            
            final boolean bound = layout.getTargetClass() != InstrumentMessage.class;
            
            for (ClassCodecFactory.Type type : ClassCodecFactory.Type.values()) {
                if (bound || type == ClassCodecFactory.Type.UNBOUND_DECODER || type == ClassCodecFactory.Type.UNBOUND_ENCODER)
                    ClassCodecFactory.dumpCode(layout, type, outDirectory);
            }
        }
    }
}