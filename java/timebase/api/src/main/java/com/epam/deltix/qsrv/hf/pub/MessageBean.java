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

import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.lang.Util;

import java.util.HashMap;
import java.util.Map;

/**
 *  A representation of arbitrary message that holds a map from field name to
 *  field value.
 */
public class MessageBean extends InstrumentMessage {
    public static final Object      NULL_VALUE = 
        new Object () {
            @Override
            public String       toString () {
                return ("MessageBean.NULL_VALUE");
            }
        };

    public RecordClassDescriptor    type;
    public Map <String, Object>     fields;

    public MessageBean () {
    }

    public MessageBean (RecordClassDescriptor type) {
        this.type = type;
    }

    public final void           initFieldMap () {
        fields = new HashMap <String, Object> ();
    }

    @Override
    public MessageBean copyFrom(RecordInfo template) {
        super.copyFrom(template);

        if (template instanceof MessageBean) {
            MessageBean      t = (MessageBean) template;

            this.type = t.type;

            fields = new HashMap <String, Object> (t.fields);
        }
        return this;
    }

    @Override
    protected MessageBean createInstance() {
        return new MessageBean();
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

            if (fields != null) {
                for (Map.Entry <String, Object> e : fields.entrySet ()) {
                    sb.append (",");
                    sb.append (e.getKey ());
                    sb.append (":");

                    Object  value = e.getValue ();

                    if (value == NULL_VALUE)
                        sb.append ("<null>");
                    else
                        sb.append (value);
                }
            }

            return (sb.toString ());
        }
    }

    @Override
    public boolean equals (Object obj) {
        if (this == obj)
            return (true);

        if (!(obj instanceof MessageBean))
            return (false);

        final MessageBean    other = (MessageBean) obj;

        if (!type.equals (other.type) ||
            getTimeStampMs() != other.getTimeStampMs() ||
            !Util.equals (getSymbol(), other.getSymbol()))
            return (false);

        Map <String, Object>    of = other.fields;

        if (fields == null)
            if (of == null)
                return (true);
            else
                return (false);

        if (of == null)
            return (false);

        int     n = fields.size ();

        if (n != of.size ())
            return (false);

        for (Map.Entry <String, Object> e : fields.entrySet ()) {
            if (!e.getValue ().equals (of.get (e.getKey ())))
                return (false);
        }

        return (true);
    }

    @Override
    public int hashCode () {
        //  Skip instrumentType - it is rarely a deciding difference
        int             hash =
            Util.xhashCode (type) +
            Util.hashCode (getSymbol()) +
            Util.hashCode (getTimeStampMs());

        for (Object v : fields.values ())
            hash = hash * 31 + v.hashCode (); // skip keys

        return hash;
    }
}