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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor.TypeResolver;
import com.epam.deltix.util.codec.HexBinCharEncoder;
import com.epam.deltix.util.codec.HexCharBinDecoder;
import com.epam.deltix.util.collections.generated.ByteArrayList;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;

/**
 * User: BazylevD
 * Date: Dec 2, 2009
 * Time: 7:48:33 PM
 */
@XmlType(name = "BINARY")
public final class BinaryDataType extends DataType {
	private static final long serialVersionUID = 1L;

    public static int UNLIMITED_SIZE = Integer.MIN_VALUE;
    public static int MIN_COMPRESSION = 0;
    public static int MAX_COMPRESSION = 9;

    @XmlElement(name = "maxSize")
    private int                   maxSize;

    @XmlElement(name = "compression")
    private int                   compressionLevel;

    BinaryDataType() { // For JAXB
        super();
        maxSize = -1;
        compressionLevel = MIN_COMPRESSION;
    }

    public static BinaryDataType getDefaultInstance () {
        return new BinaryDataType (true,
                                   UNLIMITED_SIZE,
                                   MIN_COMPRESSION);
    }
    
    public BinaryDataType(boolean nullable, int compressionLevel) {
        this(nullable, UNLIMITED_SIZE, compressionLevel);
    }

    public BinaryDataType(boolean nullable, int maxSize, int compressionLevel) {
        super(null, nullable);
        if (maxSize <= 0 && maxSize != UNLIMITED_SIZE)
            throw new IllegalArgumentException("invalid maxSize: " + maxSize);
        else
            this.maxSize = maxSize;
        if (compressionLevel < MIN_COMPRESSION || compressionLevel > MAX_COMPRESSION)
            throw new IllegalArgumentException("invalid compression level: " + compressionLevel);
        else
            this.compressionLevel = compressionLevel;
    }

    public String           getBaseName () {
        return ("BINARY");
    }

    @Override
    public int              getCode() {
        return T_BINARY_TYPE;
    }

    public int              getMaxSize() {
        return maxSize;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    @Override
    public ConversionType isConvertible(DataType to) {
        if (to instanceof BinaryDataType)
            return ConversionType.Lossless;

        return ConversionType.NotConvertible;
    }
    
    public static String    staticFormat (byte [] value) {
        return (HexBinCharEncoder.encode (value, false, true, 64));
    }
    
    public static byte []   staticParse (CharSequence text) {
        return (HexCharBinDecoder.decode (text));
    }
    
    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof byte []))
            throw unsupportedType (obj);
    }

    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text));
    }

    @Override
    protected String        toStringImpl (Object obj) {
        return (staticFormat ((byte []) obj));
    }
    
    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_BINARY_TYPE);

        super.writeTo (out);

        out.writeInt (maxSize);
        out.writeByte (compressionLevel);
    }

    @Override
    protected void          readFields (
        DataInputStream         in,
        TypeResolver            resolver
    )
        throws IOException
    {
        super.readFields (in, resolver);

        maxSize = in.readInt ();
        compressionLevel = in.readByte ();
    }
}
