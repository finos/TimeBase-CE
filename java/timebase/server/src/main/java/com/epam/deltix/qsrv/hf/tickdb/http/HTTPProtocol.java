/*
 * Copyright 2024 EPAM Systems, Inc
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.bind.marshaller.CharacterEscapeHandler;

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

    public static final short VERSION = 104;
    public static final short MIN_CLIENT_VERSION = 8;
    public static final short CLIENT_SSL_SUPPORT_VERSION = 9;
    public static final short CLIENT_ENTITYID32_SUPPORT_VERSION = 11;
    public static final short CLIENT_LOCKS_SUPPORT_VERSION = 12;
    public static final short CLIENT_APPLICATION_NAME_SUPPORT_VERSION = 13;
    public static final short CLIENT_RANGED_LOCKS_SUPPORT_VERSION = 14;
    public static final short CLIENT_SEPARATE_WEB_PORT_VERSION = 19;
    public static final short CLIENT_SESSION_HANDLER_SUPPORT = 100;

    public static final byte PROTOCOL_INIT          = 0x18;


    public static final int     REQ_GET_STREAMS =           1;
    public static final int     REQ_GET_STREAM_PROPERTY =   2;
    public static final int     REQ_CLOSE_SESSION =         3;
    public static final int     REQ_GET_STREAMS_CHUNKED =   4;

    public static final int     STREAM_PROPERTY_CHANGED =   11;
    public static final int     STREAM_DELETED =            12;
    public static final int     STREAM_CREATED =            13;
    public static final int     STREAM_RENAMED =            14;
    public static final int     STREAMS_DEFINITION =        15;
    public static final int     STREAM_PROPERTY =           16;
    public static final int     SESSION_CLOSED =            17;
    public static final int     SESSION_STARTED =           18;
    public static final int     STREAMS_CHANGED =           19;
    public static final int     STREAMS_DEFINITION_CHUNKED = 20;
    public static final int     END_STREAMS_DEFINITION_CHUNKED = 21;

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

    public static final int TERMINATOR_RECORD = 0xFFFF_FFFF;
    public static final int ERROR_BLOCK_ID_WIDE = 0xFFFF_FFFE;
    public static final int MAX_MESSAGE_SIZE = 0x400000;

    public static final byte ERR_INVALID_ARGUMENTS = 1;
    public static final byte ERR_PROCESSING = 2;

    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String GZIP = "gzip";

    public static final String UNKNOWN_APPLICATION_NAME = "Unknown REST";

    public static ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static void validateVersion(short clientVersion) {
        if (clientVersion < MIN_CLIENT_VERSION)
            throw new IllegalArgumentException(
                String.format("Incompatible HTTP-TB protocol version %d. Minimal expected version is %d.", clientVersion, MIN_CLIENT_VERSION));
    }

    public static void marshall(Object o, OutputStream os) {
        try {
            tbMarshaller().marshal(o, os);
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
            uhfMarshaller().marshal(o, os);
        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }

    public static void marshallUHF(Object o, Writer writer) {
        try {
            uhfMarshaller().marshal(o, writer);
        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }

    private static Marshaller tbMarshaller() throws JAXBException {
        final Marshaller m = TBJAXBContext.createMarshaller();
        m.setProperty(CharacterEscapeHandler.class.getName(), EscapeSpacesHandler.theInstance);
        return m;
    }

    private static Marshaller uhfMarshaller() throws JAXBException {
        final Marshaller m = UHFJAXBContext.createMarshaller();
        m.setProperty(CharacterEscapeHandler.class.getName(), EscapeSpacesHandler.theInstance);
        return m;
    }
}