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
package com.epam.deltix.qsrv.hf.stream;

import java.util.*;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.*;

/**
 * Helper utility class for comparing RecordClassDescriptors.
 */
public class MessageProcessor {

    public MessageProcessor () {
    }

    public static boolean isCompatible (final RecordClassDescriptor[] in,
                                        final RecordClassDescriptor[] out) {
        if (in.length > out.length)
            return false;

        final ArrayList<RecordClassDescriptor> matched = new ArrayList<RecordClassDescriptor> ();

        for (int i = 0; i < Math.min (in.length, out.length); i++) {
            final RecordClassDescriptor outd = out[i];
            final RecordClassDescriptor match = findMatch (outd, in);
            if (match != null)
                matched.add (match);
        }

        return in.length == matched.size ();
    }

    /*
        Sorts output class descriptors according to order of input descriptors
        Returns null, if sets is not compatible
     */

    public static RecordClassDescriptor[] sort (final RecordClassDescriptor[] in,
                                        final RecordClassDescriptor[] out) {

        if (in.length > out.length)
            return null;

        RecordClassDescriptor[] sorted = new RecordClassDescriptor[out.length];

        for (int i = 0; i < Math.min (in.length, out.length); i++) {
            int index = findIndex(out[i], in);
            if (index != -1)
                sorted[index] = out[i];
            else
                return null;
        }

        return sorted;
    }

    private static int findIndex (final RecordClassDescriptor rd,
                                  final RecordClassDescriptor[] set) {
        for (int i = 0, setLength = set.length; i < setLength; i++) {
            RecordClassDescriptor descriptor = set[i];
            if (rd.getName().equals(descriptor.getName()) &&
                    binaryCompare(descriptor, rd) == CompareResult.equals)
                return i;
        }

        for (int i = 0, setLength = set.length; i < setLength; i++) {
            RecordClassDescriptor descriptor = set[i];
            if (binaryCompare(descriptor, rd) == CompareResult.equals)
                return i;
        }

        return -1;
    }


    // if matches, returns array contains input descriptors matched according to the output descriptors
    public static RecordClassDescriptor[] findMatches (final RecordClassDescriptor[] in,
                                                       final RecordClassDescriptor[] out) {

        final ArrayList<RecordClassDescriptor> matched = new ArrayList<RecordClassDescriptor> ();

        for (int i = 0; i < Math.min (in.length, out.length); i++) {
            final RecordClassDescriptor match = findMatch (out[i], in);
            if (match != null)
                matched.add (match);
        }

        return matched.toArray (new RecordClassDescriptor[matched.size ()]);
    }

    public static String toDetailedString (final RecordClassDescriptor[] dsc) {
        final StringBuffer sw = new StringBuffer ();
        for (final RecordClassDescriptor cd : dsc) {
            sw.append (sw.length () > 0 ? "," : "").append ("[").append (cd.getName ()).append (": ");
            final int length = sw.length ();
            final NonStaticFieldLayout[] nonStaticFields = new RecordLayout (cd).getNonStaticFields ();
            if (nonStaticFields != null) {
                for (final NonStaticFieldLayout dataField : nonStaticFields) {
                    sw.append (sw.length () > length ? "," : "").append (dataField.getName ()).
                            append ("(").append (toString (dataField.getType ())).append (")");
                }
            }
            sw.append ("]");
        }

        return sw.toString ();
    }

    private static String toString (final DataType type) {
        if (type instanceof VarcharDataType)
            return "string";
        else if (type instanceof CharDataType)
            return "char";
        else if (type instanceof BooleanDataType)
            return "bool";
        else if (type instanceof EnumDataType)
            return "enum";

        return type.getEncoding ();
    }

    public static boolean isBinaryCompatible (final RecordClassDescriptor[] in,
                                              final RecordClassDescriptor[] out) {

        for (int i = 0, length = Math.min (in.length, out.length); i < length; i++) {
            if (binaryCompare(in[i], out[i]) != CompareResult.equals)
                return false;
        }

        return in.length <= out.length;
    }

    public static boolean isEquals (RecordClassDescriptor[] in, RecordClassDescriptor[] out) {

        for (int i = 0, length = Math.min (in.length, out.length); i < length; i++) {
            if (!in[i].equals(out[i]))
                return false;
        }

        return in.length == out.length;
    }

    public static RecordClassDescriptor findMatch (final RecordClassDescriptor rd,
                                                   final RecordClassDescriptor[] set) {
        for (final RecordClassDescriptor descriptor : set) {
            if (rd.getName().equals(descriptor.getName()) &&
                    binaryCompare(descriptor, rd) == CompareResult.equals)
                return descriptor;
        }

        for (final RecordClassDescriptor descriptor : set) {
            if (binaryCompare(descriptor, rd) == CompareResult.equals)
                return descriptor;
        }

        return null;
    }

    static CompareResult binaryCompare (final RecordClassDescriptor in,
                                                     final RecordClassDescriptor out) {

        final NonStaticFieldLayout[] inFields = new RecordLayout (in).getNonStaticFields ();
        final NonStaticFieldLayout[] outFields = new RecordLayout (out).getNonStaticFields ();
        
        if (inFields == null && outFields == null)
            return CompareResult.equals;

        if (inFields == null || outFields == null)
            return CompareResult.none;

        for (int i = 0; i < Math.min (inFields.length,
                                      outFields.length); i++) {
            final NonStaticDataField inField = inFields[i].getField ();
            final NonStaticDataField outField = outFields[i].getField ();
            if (outField.getType ().getClass () != inField.getType ().getClass ())
                return CompareResult.none;
            if (!Util.xequals (outField.getType ().getEncoding (),
                               inField.getType ().getEncoding ()))
                return CompareResult.none;
        }

        if (inFields.length == outFields.length)
            return CompareResult.equals;
        else if (inFields.length > outFields.length)
            return CompareResult.left;
        else {
            for (int i = inFields.length - 1; i < outFields.length; i++) {
                if (!outFields[i].getField ().getType().isNullable())
                    return CompareResult.right;
            }

            return CompareResult.equals;
        }
    }

    public static boolean  isBinaryCompatible(RecordClassDescriptor in, RecordClassDescriptor out) {
         return binaryCompare(in, out) == CompareResult.equals;
    }

    enum CompareResult {
        none,
        equals,
        left,
        right
    }
}