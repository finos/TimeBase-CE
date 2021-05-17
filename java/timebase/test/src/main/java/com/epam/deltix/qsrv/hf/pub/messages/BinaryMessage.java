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
package com.epam.deltix.qsrv.hf.pub.messages;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.SchemaType;
import com.epam.deltix.util.collections.generated.ByteArrayList;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/12/2019
 */
public class BinaryMessage extends InstrumentMessage {

    private static final Introspector ix =
            Introspector.createEmptyMessageIntrospector();
    private static RecordClassDescriptor myDescriptor = null;

    @SchemaElement(
            title = "Nullable BINARY"
    )
    public ByteArrayList binary_n;

    @SchemaElement(
            title = "Non-nullable CHAR"
    )
    @SchemaType(
            isNullable = false
    )
    public char char_c;

    @SchemaElement(
            title = "Nullable CHAR"
    )
    public char char_n;

    public static synchronized RecordClassDescriptor getClassDescriptor() {
        if (myDescriptor == null) {
            try {
                myDescriptor = ix.introspectRecordClass(BinaryMessage.class);
            } catch (Introspector.IntrospectionException x) {
                throw new RuntimeException(x);   // Unexpected, this should be reliable.
            }
        }

        return (myDescriptor);
    }

}
