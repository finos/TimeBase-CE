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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptorArray;
import com.epam.deltix.qsrv.hf.pub.md.UHFJAXBContext;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.logging.Logger;

/**
 *
 */
public abstract class HTTPProtocol {

    public static final Logger LOGGER = Logger.getLogger(HTTPProtocol.class.getPackage().getName());

    public static final short VERSION = 32;
    public static final short MIN_CLIENT_VERSION = 31;
    public static final short CLIENT_ENTITYID32_SUPPORT_VERSION = 31;

    public static final byte PROTOCOL_INIT          = 0x18;

    public static final byte REQ_UPLOAD_DATA        = 0x01;
    public static final byte REQ_CREATE_CURSOR      = 0x02;
    public static final byte REQ_CREATE_SESSION     = 0x03;

    public static final int RESP_ERROR              = -1;
    public static final int RESP_OK                 = 0;

    public static final byte TYPE_BLOCK_ID          = 1;
    public static final byte INSTRUMENT_BLOCK_ID    = 2;
    public static final byte MESSAGE_BLOCK_ID       = 3;
    public static final byte ERROR_BLOCK_ID         = 4;
    public static final byte TERMINATOR_BLOCK_ID    = 5;
    public static final byte PING_BLOCK_ID          = 6;
    public static final byte CURSOR_BLOCK_ID        = 7;
    public static final byte COMMAND_BLOCK_ID       = 8;
    public static final byte STREAM_BLOCK_ID        = 9;
    public static final byte KEEP_ALIVE_ID          = 10;
    public static final byte RESPONSE_BLOCK_ID      = 11;

    public static final int CURSOR_MESSAGE_HEADER_SIZE = 8 + 2 + 1 + 1; // timestamp + instrument_index + type_index + stream_index
    public static final int LOADER_MESSAGE_HEADER_SIZE = 8 + 2 + 1; // timestamp + instrument_index + type_index

    public static final int TERMINATOR_RECORD = 0xFFFFFFFF;
    public static final int ERROR_BLOCK_ID_WIDE = 0xFFFFFFFE;
    public static final int MAX_MESSAGE_SIZE = 0x400000;

    public static final byte ERR_INVALID_ARGUMENTS = 1;
    public static final byte ERR_PROCESSING = 2;

    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String GZIP = "gzip";

    public static void validateVersion(short clientVersion) {
        if (clientVersion < MIN_CLIENT_VERSION)
            throw new IllegalArgumentException(
                String.format("Incompatible HTTP-TB protocol version %d. Minimal expected version is %d.", clientVersion, MIN_CLIENT_VERSION));
    }

    public static void marshall(Object o, OutputStream os) {
        try {
            final Marshaller m = TBJAXBContext.createMarshaller();
            m.marshal(o, os);
        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }

    public static String marshallUHF(ClassDescriptorArray rcd) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        marshallUHF(rcd, os);
        return os.toString();
    }

    public static Object unmarshallUHF(StringReader reader) {
        try {
            Unmarshaller um = UHFJAXBContext.createUnmarshaller();
            return um.unmarshal(reader);

        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }

    public static void marshallUHF(Object o, OutputStream os) {
        try {
            final Marshaller m = UHFJAXBContext.createMarshaller();
            m.marshal(o, os);
        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }

    public static void marshallUHF(Object o, Writer writer) {
        try {
            final Marshaller m = UHFJAXBContext.createMarshaller();
            m.marshal(o, writer);
        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }
}