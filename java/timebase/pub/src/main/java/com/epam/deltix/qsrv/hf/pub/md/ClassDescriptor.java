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

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.Visitor;
import com.epam.deltix.util.io.CachedLocalIP;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.lang.Util;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.readNullableString;
import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeNullableString;

/**
 *
 */
@XmlType (name = "class")
public abstract class ClassDescriptor 
    extends NamedDescriptor
    implements Serializable, Comparable <ClassDescriptor>
{
    public static final int  T_RECORD =              1;
    public static final int  T_ENUM =                2;

    public interface TypeResolver {
        ClassDescriptor     forGuid (String guid);
    }

    public static class GuidNotFoundException extends RuntimeException {
        public GuidNotFoundException (String guid) {
            super (guid);
        }
    }

	private static final long serialVersionUID = 1L;
	
    public static final char                ASSEMBLY_SEPARATOR = ':';

    public static final Comparator <ClassDescriptor>    ASCENDING_COMPARATOR =
        new Comparator <ClassDescriptor> () {
            public int compare (ClassDescriptor o1, ClassDescriptor o2) {
                return (o1.getName ().compareTo (o2.getName ()));
            }
        };

    /**
     *  Sorts the supplied array of class descriptors so that all
     *  dependent classes precede the classes that depend on them. An attempt
     *  is made to keep the classes sorted in alphabetical order, as long as it 
     *  does not contradict to quasi-ordering by dependency.
     *  This algorithm is deterministic. It will add missing dependencies to the
     *  output list.
     * 
     *  @param cds  An array of classes to sort/augment.
     *  @return     A sorted/augmented list.
     * 
     */
    public static List <ClassDescriptor>  depSort (ClassDescriptor [] cds) {
        cds = cds.clone ();
        
        Arrays.sort (cds, ClassDescriptor.ASCENDING_COMPARATOR);
        
        final ArrayList <ClassDescriptor>   ordered = 
            new ArrayList <ClassDescriptor> ();
        
        Visitor <ClassDescriptor>           v = 
            new Visitor<ClassDescriptor> () {
                public boolean visit (ClassDescriptor cd) {
                    if (!ordered.contains (cd)) {                        
                        cd.visitDependencies (this);
                        ordered.add (cd);
                    }
                    
                    return (true);
                }
            };
        
        for (ClassDescriptor cd : cds) 
            v.visit (cd);
        
        return (ordered);
    }
    
    /**
     *  Unique identifier of this class.
     */
    @XmlID
    @XmlElement (name = "guid")
    @XmlJavaTypeAdapter(StringInternAdapter.class)
    protected String                        guid;

    protected ClassDescriptor (String name, String title) {
        super (name, title);
        this.guid = createGuid().intern();
    }
    
    protected ClassDescriptor () {
    }

    protected ClassDescriptor (Class <?> cls) {
        this (cls, ClassAnnotator.DEFAULT);
    }

    public static String createGuid () {
        return new GUID().toStringWithPrefix(CachedLocalIP.getIP());
    }

    @Override
    public int compareTo (ClassDescriptor o) {
        return (guid.compareTo (o.guid));
    }

    public static String        getClassNameWithAssembly (Class <?> cls) {
        SchemaElement se = cls.getAnnotation (SchemaElement.class);

        if (se != null && !se.name().equals(""))
            return (se.name ());

        String      name = cls.getName ();

        return (name);
    }

    protected ClassDescriptor (ClassDescriptor from) {
        super (from);
        guid = from.guid;
    }

    protected ClassDescriptor (Class <?> cls, ClassAnnotator annotator) {
        super (annotator.getName (cls), annotator.getTitle (cls));
        setDescription (annotator.getDescription (cls));

        guid = annotator.getGuid(cls);
        if (guid == null || guid.trim().length() == 0)
            guid = createGuid ().intern();
    }

    public String               getGuid () {
           return guid;
    }    

    public static boolean       isDotNet (String className) {
        return className.indexOf (ASSEMBLY_SEPARATOR) > 0;
    }

    public Object               newInstanceNoX (TypeLoader loader) {
        try {
            return Util.newInstanceNoX (loader.load(this));
        } catch (ClassNotFoundException x) {
            throw new RuntimeException (x);
        }
    }

    @Override
    public boolean              equals (Object obj) {
        return (
            this == obj ||
            guid != null &&
                (obj instanceof ClassDescriptor) &&
                 Util.xequals(guid, ((ClassDescriptor) obj).guid)
        );
    }

    public boolean               isEquals(ClassDescriptor target) {
        return target != null && Util.xequals(guid, target.guid);
    }

    @Override
    public int                  hashCode () {
        return (guid == null ? 0 : guid.hashCode ());
    }

    public boolean              dependsOn (final ClassDescriptor cd) {
        return (
            !visitDependencies (
                new Visitor <ClassDescriptor> () {
                    public boolean  visit (ClassDescriptor dep) {
                        if (dep == cd)
                            return (false);
                        
                        if (dep.dependsOn (cd))
                            return (false);
                        
                        return (true);
                    }            
                }
            )
        );
    }
    
    public boolean              visitDependencies (Visitor <ClassDescriptor> out) {
        return (true);
    }

    public final ClassDescriptor [] getDependencies () {
        final Set <ClassDescriptor>         cset = new HashSet <ClassDescriptor> ();

        visitDependencies (
            new Visitor <ClassDescriptor> () {
                public boolean      visit (ClassDescriptor cd) {
                    cset.add (cd);
                    return (true);
                }
            }
        );

        return (cset.toArray (new ClassDescriptor [cset.size ()]));
    }

    public static String []      extractGuids (ClassDescriptor [] cds) {
        if (cds == null)
            return (null);

        int         n = cds.length;
        String []   ret = new String [n];

        for (int ii = 0; ii < n; ii++)
            ret [ii] = cds [ii].getGuid ();
        
        return (ret);
    }

    public void                     writeReference (DataOutputStream out)
        throws IOException
    {
        out.writeUTF (guid);
    }

    public static ClassDescriptor   readReference (
        DataInputStream                 in,
        TypeResolver                    resolver
    )
        throws IOException
    {
        String          guid = in.readUTF ();
        ClassDescriptor cd = resolver.forGuid (guid);

        if (cd == null)
            throw new GuidNotFoundException (guid);

        return (cd);
    }

    @Override
    public void                     writeTo (DataOutputStream out, int serial)
        throws IOException
    {
        writeReference (out);
        super.writeTo (out, serial);
        writeNullableString (null, out); // javaClassName
    }

    @Override
    protected final void            readFields (DataInputStream in, int serial) throws IOException {
        throw new UnsupportedOperationException ("call the version with with TypeResolver");
    }
    
    protected void                  readFields (
        DataInputStream                 in,
        TypeResolver                    resolver,
        int                             serial

    )
        throws IOException
    {
        guid = in.readUTF ();
        super.readFields (in, serial);
        readNullableString (in);
    }

    public static ClassDescriptor   readFrom (DataInputStream in, TypeResolver resolver, int serial)
        throws IOException
    {
        int                 tag = in.readUnsignedByte ();
        ClassDescriptor     cd;
        
        switch (tag) {
            case T_RECORD:  cd = new RecordClassDescriptor (); break;
            case T_ENUM:    cd = new EnumClassDescriptor (); break;
            default:        throw new IOException ("Illegal tag: " + tag);
        }

        cd.readFields (in, resolver, serial);
        return (cd);
    }
}
