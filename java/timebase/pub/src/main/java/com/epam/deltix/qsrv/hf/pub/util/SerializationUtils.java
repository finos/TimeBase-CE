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
package com.epam.deltix.qsrv.hf.pub.util;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.io.IOUtil;
import java.io.*;
import java.util.Collection;

/**
 *
 */
public class SerializationUtils {
    public static void                  writeNullableString (
        String                              s,
        DataOutputStream                    out
    )
        throws IOException
    {
        boolean     notNull = s != null;

        out.writeBoolean (notNull);

        if (notNull)
            out.writeUTF (s);
    }

    public static String                readNullableString (
        DataInputStream                     in
    )
        throws IOException
    {
        boolean     notNull = in.readBoolean ();

        if (notNull)
            return (in.readUTF ());
        else
            return (null);
    }

    public static void                  writeNullableInt (
            Integer                             value,
            DataOutputStream                    out
    )
            throws IOException
    {
        boolean     notNull = value != null;
        out.writeBoolean (notNull);
        if (notNull)
            out.writeInt (value);
    }

    public static Integer                readNullableInt (
            DataInputStream                     in
    )
            throws IOException
    {
        boolean     notNull = in.readBoolean ();

        if (notNull)
            return (in.readInt ());
        else
            return (null);
    }

    public static void                  writeNullableBoolean (
            Boolean                             value,
            DataOutputStream                    out
    )
            throws IOException
    {
        boolean     notNull = value != null;
        out.writeBoolean (notNull);
        if (notNull)
            out.writeBoolean(value);
    }

    public static Boolean                readNullableBoolean (
            DataInputStream                     in
    )
            throws IOException
    {
        boolean     notNull = in.readBoolean ();

        if (notNull)
            return (in.readBoolean ());
        else
            return (null);
    }

    public static void                  writeHugeString (
        DataOutputStream                    out,
        CharSequence                        s
    )
        throws IOException
    {
        int             length = s.length ();

        out.writeInt (length);

        for (int ii = 0; ii < length; ii++) {
            char        c = s.charAt (ii);

            if (c < 255)
                out.writeByte (c);
            else {
                out.writeByte (-1);
                out.writeChar (c);
            }
        }
    }

    public static void                  readHugeString (
        DataInputStream                     in,
        StringBuilder                       out
    )
        throws IOException
    {
        int             length = in.readInt ();

        out.setLength (length);

        for (int ii = 0; ii < length; ii++) {
            int         b = in.readByte ();

            out.setCharAt (ii, b == -1 ? in.readChar () : (char) b);
        }
    }

//    public static void                  writeInstrumentType (
//        InstrumentType type,
//        DataOutput out
//    )
//        throws IOException
//    {
//        out.writeByte (type.ordinal ());
//    }
//
//    public static InstrumentType        readInstrumentType (
//        DataInput                     in
//    )
//        throws IOException
//    {
//        return (InstrumentType.values () [in.readByte ()]);
//    }

//    public static void                  writeInstrumentTypes (
//        InstrumentType []                   types,
//        DataOutputStream                    out
//    )
//        throws IOException
//    {
//        if (types == null)
//            out.writeInt (-1);
//        else {
//            out.writeInt (types.length);
//
//            for (InstrumentType type : types)
//                writeInstrumentType (type, out);
//        }
//    }
//
//    public static InstrumentType []     readInstrumentTypes (
//        DataInputStream                     in
//    )
//        throws IOException
//    {
//        int                                 num = in.readInt ();
//
//        if (num < 0)
//            return (null);
//        else {
//            InstrumentType []               types = new InstrumentType [num];
//
//            for (int ii = 0; ii < num; ii++)
//                types [ii] = readInstrumentType (in);
//
//            return (types);
//        }
//    }

    public static void                  writeIdentityKey (
        IdentityKey id,
        DataOutput out
    )
        throws IOException
    {
        IOUtil.writeUTF (id.getSymbol (), out);
    }

    public static ConstantIdentityKey readIdentityKey (
        DataInput                     in
    )
        throws IOException
    {
        String          symbol = in.readUTF ();

        return (new ConstantIdentityKey (symbol));
    }

    public static String readSymbol (
            DataInput                     in
    )
            throws IOException
    {
        return in.readUTF ();
    }

    public static void                  writeInstrumentIdentities (
        IdentityKey []               ids,
        DataOutputStream                    out
    )
        throws IOException
    {
        writeInstrumentIdentities (ids, 0, ids == null ? -1 : ids.length, out);
    }

    public static void                  writeInstrumentIdentities (
        IdentityKey []               ids,
        int                                 offset,
        int                                 length,
        DataOutputStream                    out
    )
        throws IOException
    {
        if (ids == null)
            out.writeInt (-1);
        else {
            out.writeInt (length);

            for (int ii = 0; ii < length; ii++)
                writeIdentityKey (ids [offset + ii], out);
        }
    }

    public static void                  writeIdentities (
            Collection<IdentityKey>             ids,
            DataOutputStream                    out
    )
            throws IOException
    {
        if (ids == null)
            out.writeInt (-1);
        else {
            int length = ids.size();
            out.writeInt (length);

            for (IdentityKey id : ids)
                writeIdentityKey (id, out);

        }
    }

    public static IdentityKey[]  writeSymbols (
            CharSequence []                     symbols,
            DataOutputStream                    out
    )
            throws IOException
    {
        return writeSymbols (symbols, 0, symbols == null ? -1 : symbols.length, out);
    }

    public static IdentityKey[]  writeSymbols (
            CharSequence []                     symbols,
            int                                 offset,
            int                                 length,
            DataOutputStream                    out
    )
            throws IOException
    {
        if (symbols == null) {
            out.writeInt(-1);
            return null;
        } else {
            out.writeInt (length);
            IdentityKey[] ids = new IdentityKey[length];

            for (int i = offset; i < length; i++) {
                ids[i] = new ConstantIdentityKey(symbols[i]);
                writeIdentityKey (ids[i], out);
            }

            return ids;
        }
    }

    public static IdentityKey[]  writeSymbols (
            Collection<CharSequence>            symbols,
            DataOutputStream                    out
    )
            throws IOException
    {
        if (symbols == null) {
            out.writeInt(-1);
            return null;
        } else {
            int length = symbols.size();
            out.writeInt (length);

            IdentityKey[] ids = new IdentityKey[length];
            int index = 0;

            for (CharSequence symbol : symbols) {
                IdentityKey key = ids[index++] = new ConstantIdentityKey(symbol);
                writeIdentityKey (key, out);
            }
            return ids;
        }
    }

    public static IdentityKey [] readInstrumentIdentities (
        DataInputStream                     in
    )
        throws IOException
    {
        int                                 num = in.readInt ();

        if (num < 0)
            return (null);
        else {
            IdentityKey []           ids = new IdentityKey [num];

            for (int ii = 0; ii < num; ii++)
                ids [ii] = readIdentityKey (in);

            return (ids);
        }
    }

    public static CharSequence[]    readSymbols (
            DataInputStream                     in
    )
            throws IOException
    {
        int                                 num = in.readInt ();

        if (num < 0)
            return (null);
        else {
            CharSequence []           ids = new CharSequence [num];

            for (int ii = 0; ii < num; ii++)
                ids [ii] = readSymbol (in);

            return (ids);
        }
    }


}
