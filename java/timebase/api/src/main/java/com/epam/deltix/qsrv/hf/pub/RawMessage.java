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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.*;

/**
 *  Special message class used by Tick DB server. Contains undecoded
 *  message bytes. 
 */
public final class RawMessage extends InstrumentMessage {
    public RecordClassDescriptor    type;
    public byte []                  data;
    public int                      offset;
    public int                      length;
    
    public RawMessage () {
    }
        
    public RawMessage (RecordClassDescriptor type) {
        this.type = type;
    }
        
    public final void                 setBytes (MemoryDataOutput out) {
        setBytes (out, 0);
    }

    public final void                 setBytes (MemoryDataOutput out, int offset) {
        data = out.getBuffer ();
        this.offset = offset;
        length = out.getSize () - offset;
    }

    public final void                 setBytes (byte [] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    /**
     *  Copy bytes from the specified MemoryDataOutput into this message, 
     *  reusing its bytes field, unless it is null or too small.          
     */
    public final void                 copyBytes (MemoryDataOutput out, int offset) {
        this.length = out.getSize () - offset;
        this.offset = 0;
        if (this.data == null || this.data.length < length)
            this.data = new byte[length];

        System.arraycopy (out.getBuffer (), offset, this.data, 0, length);
    }

    public final void                 setUpMemoryDataInput (MemoryDataInput mdi) {
        mdi.setBytes (data, offset, length);
    }

    public final void                 writeTo (MemoryDataOutput out) {
        out.write (data, offset, length);
    }
    
    /**
     *  This method is not very efficient, but will
     *  work for console debug output, etc.
     */
    @Override
    public String               toString () {
        if (type == null)
            return (super.toString ());
        else {
            
            StringBuilder   sb = new StringBuilder ();
        
            sb.append (type.getName ());
            sb.append (",");
            sb.append (getSymbol());
            sb.append (",");
            if (getTimeStampMs() == DateTimeDataType.NULL)
                sb.append ("<null>");
            else
                sb.append (getTimeString());
            
            MemoryDataInput         in = new MemoryDataInput (data, offset, length);
            UnboundDecoder          decoder =
                InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory (type).create ();
            
            decoder.beginRead (in);
            
            while (decoder.nextField ()) {
                NonStaticFieldInfo  df = decoder.getField ();
                sb.append (",");
                sb.append (df.getName ());
                sb.append (":");
                try {
                    sb.append (decoder.getString ());
                } catch (NullValueException e) {
                    sb.append ("<null>");
                }
            }
            
            return (sb.toString ());
        }
    }

    @Override
    public RawMessage copyFrom(RecordInfo template) {
        super.copyFrom(template);

        if (template instanceof RawMessage) {
            RawMessage      t = (RawMessage) template;

            this.length = t.length;
            this.type = t.type;

            this.offset = 0;
            if (this.data == null || this.data.length < t.length)
                this.data = new byte[t.length];

            if (t.data != null)
                System.arraycopy (t.data, t.offset, this.data, 0, t.length);
        }

        return this;
    }


    @Override
    protected RawMessage createInstance() {
        return new RawMessage();
    }

    @Override
    public boolean equals (Object obj) {
        if (this == obj)
            return (true);

        if (!(obj instanceof RawMessage))
            return (false);

        final RawMessage    other = (RawMessage) obj;

        return
            type.equals (other.type) &&
            getTimeStampMs() == other.getTimeStampMs() &&
            getNanoTime() == other.getNanoTime() &&
            Util.equals (getSymbol(), other.getSymbol()) &&
            Util.arrayequals (data, offset, length, other.data, other.offset, other.length);
    }

    @Override
    public int hashCode () {
        //  Skip instrumentType - it is rarely a deciding difference
        int             hash =
            Util.xhashCode (type) +
            Util.hashCode (getSymbol()) +
            Util.hashCode (getTimeStampMs());

        for (int ii = 0; ii < length; ii++)
            hash = hash * 31 + data [ii];

        return hash;
    }
    
    public byte [] getData () {
        return (data);
    }
}