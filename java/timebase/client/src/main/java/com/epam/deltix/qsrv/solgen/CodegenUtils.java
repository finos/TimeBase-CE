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
package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.util.lang.InstanceOfFilter;
import com.epam.deltix.util.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CodegenUtils {

    public static final String INDENT0 = "";
    public static final String INDENT1 = INDENT0 + "    ";
    public static final String INDENT2 = INDENT1 + "    ";
    public static final String INDENT3 = INDENT2 + "    ";
    public static final String INDENT4 = INDENT3 + "    ";
    public static final String INDENT5 = INDENT4 + "    ";
    public static final String NL = "\n";
    public static final String NL2 = NL + "\n";

    public static final String[] INDENT = new String[] {
        INDENT0, INDENT1, INDENT2, INDENT3, INDENT4, INDENT5
    };

    public static StreamMetaData getStreamMetadata(String tbUrl, String streamKey) {
        StreamMetaData md = new StreamMetaData(
            SolgenUtils.getEscapedName(StringUtils.toSafeFileName(streamKey))
        );

        try (DXTickDB db = TickDBFactory.openFromUrl(tbUrl, true)) {
            DXTickStream stream = db.getStream(streamKey);
            if (stream == null) {
                throw new RuntimeException("Stream " + streamKey + " not found");
            }

            RecordClassSet recordClassSet = stream.getStreamOptions().getMetaData();
            md.getConcreteTypes().addAll(
                Arrays.asList(recordClassSet.getContentClasses())
            );
            md.getAllTypes().addAll(
                Arrays.stream(recordClassSet.getClasses())
                    .filter(c -> c instanceof RecordClassDescriptor)
                    .map(c -> (RecordClassDescriptor) c)
                    .collect(Collectors.toList())
            );

//            if (stream.isFixedType()) {
//                addClass(stream.getFixedType(), md);
//            } else {
//                for (RecordClassDescriptor rcd : stream.getPolymorphicDescriptors()) {
//                    addClass(rcd, md);
//                }
//            }
        }

        return md;
    }

//    private static void addClass(RecordClassDescriptor rcd, StreamMetaData md) {
//        md.addConcreteType(rcd);
//        while (md.addType(rcd)) {
//            RecordClassDescriptor parentDescriptor = rcd.getParent();
//            if (parentDescriptor == null)
//                break;
//
//            rcd = parentDescriptor;
//        }
//    }
}
