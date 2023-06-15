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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.timebase.messages.schema.SchemaChangeMessage;
import com.epam.deltix.util.collections.generated.IntegerHashSet;
import com.epam.deltix.util.time.Periodicity;

import javax.xml.bind.annotation.XmlElement;
import java.util.Arrays;

/**
 * Stream definition attributes.
 */
public class StreamOptions {

    private static RecordClassDescriptor schemaChangeDescriptor;

    /**
     *  "Maximum" distribution factor value.
     *
     *  For example: having DF=1 all data will be stored in a single file.
     */
    public static final int             MAX_DISTRIBUTION = 0;
    
    /**
     *  Optional user-readable name.
     */
    @XmlElement
    public String                       name = null;

    /**
     *  Optional multi-line description.
     */
    @XmlElement
    public String                       description = null;

    /**
     *  Location of the stream (by default null). When defined this attribute provides alternative stream location (rather than default location under QuantServerHome)
     */
    @XmlElement
    public String                       location = null;

    /**
     *
     *  The number of M-files into which to distribute the
     *  data. Supply {@link #MAX_DISTRIBUTION} to keep a separate file
     *  for each instrument (default).
     */
    @XmlElement
    public int                          distributionFactor = MAX_DISTRIBUTION;

    /**
     * Options that control data buffering.
     */
    @XmlElement
    public BufferOptions                bufferOptions;

    /**
     * Unique streams maintain in-memory cache of resent messages.
     * This concept assumes that stream messages will have some field(s) marked as primary key {@link com.epam.deltix.timebase.messages.PrimaryKey}.
     * Primary key may be a simple field (e.g. symbol) or composite (e.g. symbol and portfolio ID).
     * For each key TimeBase runtime maintains a copy of the last message received for this key (cache).
     * Each new consumer will receive a snapshot of current cache at the beginning of live data subscription.
     */
    @XmlElement
    public boolean                      unique;

    /**
     * Indicates that loader will ignore binary similar messages(for 'unique' streams only).
     */
    @XmlElement
    public boolean                      duplicatesAllowed = true;

    /**
     *  Determines persistent properties of a stream.
     */
    @XmlElement
    public StreamScope                  scope = StreamScope.DURABLE;   

    /**
     *  Stream periodicity, if known.
     */
    @XmlElement
    public Periodicity                  periodicity = Periodicity.mkIrregular();

    /**
     *  Class name of the distribution rule
     */
    public String                       distributionRuleName = null;
    
    /**
     *  High availability durable streams are cached on startup.
     */
    public boolean                      highAvailability = false;

    /**
     * Set of additional stream flags (identified by integer key).
     * Use methods {@link #setFlag(int, boolean)} and {@link #isFlagSet} toggle flags.
     *
     * Example: {@link TDBProtocol#AF_STUB_STREAM}
     */
    public IntegerHashSet               additionalFlags = null;

    /**
     *  Optional owner of stream.
     *  During stream creation it will be set 
     *  equals to authenticated user name.
     */
    @XmlElement
    public String                       owner = null;

    public String                       version;    

    @XmlElement
    public boolean                      polymorphic = true;
    
    private RecordClassSet              md = new RecordClassSet ();

    public RecordClassSet               getMetaData () {
        return (md);
    }

    public void                         setMetaData (
        boolean                             polymorphic,
        RecordClassSet                      md
    )
    {
        this.polymorphic = polymorphic;
        this.md = md;
    }

    /**
     *  Marks this stream as polymorphic,
     *  capable of containing messages of several specified types.
     *
     *  @param cds          The descriptors of classes describing messages to be
     *                      contained in the stream.
     */
    public void                         setPolymorphic (
        RecordClassDescriptor ...           cds
    )
    {
        for (RecordClassDescriptor cd : cds)
            if (cd.isAbstract ())
                throw new IllegalArgumentException (
                    "Class " + cd.getName () + " is abstract."
                );

        polymorphic = true;
        md.clear ();
        md.addContentClasses (cds);
    }

    /**
     *  Marks this stream as fixed-type (monomorphic),
     *  capable of containing messages of a single specified type.
     *
     *  @param cd           The descriptor of the class describing messages to be
     *                      contained in the stream.
     */
    public void                         setFixedType (
        RecordClassDescriptor               cd
    )
    {
        if (cd.isAbstract ())
            throw new IllegalArgumentException (
                "Class " + cd.getName () + " is abstract."
            );

        polymorphic = false;

        md.clear ();
        md.addContentClasses (cd);
    }

    public void                         setPolymorphic (boolean poly) {
        if (!poly) {
            int             ncs = md.getContentClasses ().length;
            
            if (ncs != 1)
                throw new IllegalArgumentException (
                    "Metadata has " + ncs + " content classes, must be 1"
                );                        
        }
        
        polymorphic = poly;
    }
    
    public boolean                      isFixedType () {
        return (!polymorphic);
    }

    public boolean                      isPolymorphic () {
        return (polymorphic);
    }

    /**
     * Creates monomorphic stream options with given attributes
     *
     * @param scope stream durability scope ()
     * @param name descriptive name
     * @param description description
     * @param distributionFactor Distribution Factor
     * @param cd RecordClassDescriptor object contains stream schema definition
     *
     * @return StreamOptions instance
     */

    public static StreamOptions         fixedType (
        StreamScope                         scope,
        String                              name,
        String                              description,
        int                                 distributionFactor,
        RecordClassDescriptor               cd
    )
    {
        StreamOptions       so = new StreamOptions ();

        so.scope = scope;
        so.name = name;
        so.description = description;
        so.distributionFactor = distributionFactor;
        so.setFixedType (cd);

        return (so);
    }

    /**
     * Creates polymorphic stream options with given attributes
     *
     * @param scope stream durability scope ()
     * @param name descriptive name
     * @param description description
     * @param distributionFactor Distribution Factor
     * @param cds RecordClassDescriptor objects contains stream schema definition
     *
     * @return StreamOptions instance
     */

    public static StreamOptions         polymorphic (
        StreamScope                         scope,
        String                              name,
        String                              description,
        int                                 distributionFactor,
        RecordClassDescriptor ...           cds
    )
    {
        StreamOptions       so = new StreamOptions ();

        so.scope = scope;
        so.name = name;
        so.description = description;
        so.distributionFactor = distributionFactor;
        so.setPolymorphic (sort(cds));

        return (so);
    }

    public static RecordClassDescriptor[]  sort(RecordClassDescriptor ... cds) {
        RecordClassDescriptor[]      descriptors = new RecordClassDescriptor[cds.length];

        Arrays.asList(cds).toArray(descriptors);
        Arrays.sort (descriptors, ClassDescriptor.ASCENDING_COMPARATOR);

        return (descriptors);
    }

    public StreamOptions () {        
    }

    public StreamOptions (
        StreamScope                         scope,
        String                              name,
        String                              description,
        int                                 distributionFactor
    )
    {
        this.scope = scope;
        this.name = name;
        this.description = description;
        this.distributionFactor = distributionFactor;
    }

    /**
     * Checks if specified additional flag is set.
     * @param flag flag identifier. Example: {@link TDBProtocol#AF_STUB_STREAM}
     * @return true if flag is set
     */
    public boolean isFlagSet(int flag) {
        return additionalFlags != null && additionalFlags.contains(flag);
    }

    /**
     * Changes status of an additional flag.
     * @param flag flag identifier. Example: {@link TDBProtocol#AF_STUB_STREAM}
     * @param enabled true to set flag, false to clear it
     */
    public void setFlag(int flag, boolean enabled) {
        if (enabled) {
            if (additionalFlags == null) {
                additionalFlags = new IntegerHashSet();
            }
            additionalFlags.add(flag);
        } else {
            if (additionalFlags != null) {
                additionalFlags.remove(flag);
            }
        }
    }

    /**
     * Introspects and returns RecordClassDescriptor of {@link SchemaChangeMessage}.
     * @return {@link RecordClassDescriptor}
     */
    public static RecordClassDescriptor getSchemaChangeMessageDescriptor() {
       if (schemaChangeDescriptor == null) {
           introspectSchemaChangeMessage();
       }
       return schemaChangeDescriptor;
    }

    private static void introspectSchemaChangeMessage() {
        Introspector introspector = Introspector.createEmptyMessageIntrospector();
        try {
            schemaChangeDescriptor = introspector.introspectRecordClass(SchemaChangeMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}