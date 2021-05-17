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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.Visitor;
import com.epam.deltix.util.collections.generated.*;

import java.io.*;
import java.util.*;

/**
 *
 */
public final class TypeSet implements ClassDescriptor.TypeResolver {

    public interface TypeSender {
        public DataOutputStream     begin () throws IOException;

        public void                 end () throws IOException;

        public int                  version();
    }

    private final TypeSender                                typeSender;

    private final ObjectArrayList <RecordClassDescriptor>   concreteTypes =
        new ObjectArrayList <RecordClassDescriptor> ();

    private final Map <String, ClassDescriptor>             guidToType =
        new HashMap <String, ClassDescriptor> ();

    private final Visitor <ClassDescriptor>                 adder =
        new Visitor <ClassDescriptor> () {
            public boolean                      visit (ClassDescriptor cd) {
                try {
                    addWithDependencies (cd, -1);
                } catch (IOException iox) {
                    throw new UncheckedIOException (iox);
                }
                return (true);
            }
        };

    public TypeSet (TypeSender typeSender) {
        this.typeSender = typeSender;
    }

    public RecordClassDescriptor        getConcreteTypeByIndex (int idx) {
        return (concreteTypes.get (idx));
    }
    public boolean                      isIndexPresent (int idx) {
        return concreteTypes.size() > idx;
    }

    public RecordClassDescriptor []     getConcreteClasses () {
        return (concreteTypes.toArray (new RecordClassDescriptor [concreteTypes.size ()]));
    }

    public void                         readTypes (DataInputStream in)
        throws IOException {
        int                 idx = in.readShort ();
        ClassDescriptor     cd = ClassDescriptor.readFrom (in, this, typeSender.version() );
        addType(idx, cd);
    }

    public void                         addType(int idx, ClassDescriptor cd) throws IOException {
        guidToType.put (cd.getGuid (), cd);

        if (idx >= 0) {
            if (idx != concreteTypes.size ())
                throw new IOException (
                    "Out-of-order index: " + idx + "; expected: " +
                    concreteTypes.size ()
                );

            concreteTypes.add ((RecordClassDescriptor) cd);
        }
    }

    private void                        sendType (ClassDescriptor cd, int idx)
        throws IOException
    {
        DataOutputStream    out = typeSender.begin ();

        out.writeShort (idx);
        cd.writeTo (out, typeSender.version());

        typeSender.end ();
    }

    public ClassDescriptor              forGuid (String guid) {
        return (guidToType.get (guid));
    }

    private boolean                     addWithDependencies (
        ClassDescriptor                     cd,
        int                                 idx
    )
        throws IOException
    {        
        ClassDescriptor             check = guidToType.put (cd.getGuid (), cd);

        if (check == null) {
            try {
                cd.visitDependencies(adder);   // send deps first
            } catch (UncheckedIOException uio) {
                throw uio.getCause();
            }
            sendType (cd, idx);
            return true;
        }
        else if (!check.isEquals(cd))
            throw new IllegalArgumentException (
                "Duplicate guid: " + check + " and " + cd
            );

        return false;
    }

    public int                          getIndexOfConcreteType (
        RecordClassDescriptor               type
    )
        throws IOException
    {
        int         idx = concreteTypes.indexOf (type);

        if (idx < 0) {
            idx = concreteTypes.size ();

            if (!addWithDependencies (type, idx))
                sendType(type, idx);

            concreteTypes.add (type);
        }

        return (idx);
    }

    /**
     * Unlike {@link #getIndexOfConcreteType(RecordClassDescriptor)} will not add missing type.
     * @return type index if present or negative value if type was not found
     */
    public int getIndexOfConcreteTypeNoAdd(RecordClassDescriptor type) {
        return indexOf(type);
    }

    public int indexOf(RecordClassDescriptor type) {

        final int length = concreteTypes.size();
        final Object[] elements = concreteTypes.getInternalBuffer();

        switch (length) {
            default:
                for (int code = length - 1; code >= 7; code--)
                    if (type.equals(concreteTypes.get(code)))
                        return (code);

            case 7:     if (type.equals(elements[6])) return (6);  // else fall-through
            case 6:     if (type.equals(elements[5])) return (5);  // else fall-through
            case 5:     if (type.equals(elements[4])) return (4);  // else fall-through
            case 4:     if (type.equals(elements[3])) return (3);  // else fall-through
            case 3:     if (type.equals(elements[2])) return (2);  // else fall-through
            case 2:     if (type.equals(elements[1])) return (1);  // else fall-through
            case 1:     if (type.equals(elements[0])) return (0);  // else fall-through
            case 0:     break;
        }

        return -1;
    }

    public int count() {
        return concreteTypes.size();
    }
}
