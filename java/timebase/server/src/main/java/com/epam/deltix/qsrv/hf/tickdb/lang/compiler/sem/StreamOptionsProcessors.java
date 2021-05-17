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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalFixedTypeStreamException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.GrammarUtil;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.time.Periodicity;

/**
 *
 */
public abstract class StreamOptionsProcessors {
    private static final OptionProcessor <StreamOptions>             PERIODICITY_PROC =
        new OptionProcessor <StreamOptions> ("periodicity", StandardTypes.NULLABLE_VARCHAR) {
            @Override
            public void     process (
                OptionElement       option, 
                CompiledConstant    value,                                 
                StreamOptions       target
            )
            {
                target.periodicity = Periodicity.parse(value == null ? null : value.toString());
            }

            @Override
            protected boolean shouldPrint (StreamOptions source) {
                return (source.periodicity != null);
            }
            
            @Override
            protected void  printValue (StreamOptions source, StringBuilder out) {
                if (source.periodicity == null)
                    out.append ("NULL");
                else
                    GrammarUtil.escapeStringLiteral (source.periodicity.toString (), out);
            }                        
        };
    
    private static final OptionProcessor <StreamOptions>             LOCATION_PROC =
        new OptionProcessor <StreamOptions> ("location", StandardTypes.NULLABLE_VARCHAR) {
            @Override
            public void     process (
                OptionElement       option, 
                CompiledConstant    value,                                 
                StreamOptions       target
            )
            {
                if (value == null || value.isNull ())
                    target.location = null;
                else 
                    target.location = value.getString ();
            }

            @Override
            protected boolean shouldPrint (StreamOptions source) {
                return (source.location != null);
            }
            
            @Override
            protected void  printValue (StreamOptions source, StringBuilder out) {
                if (source.location == null)
                    out.append ("NULL");
                else
                    GrammarUtil.escapeStringLiteral (source.location, out);
            }                        
        };
    
    private static final IntegerOptionProcessor <StreamOptions> DF_PROC =
        new IntegerOptionProcessor <StreamOptions> ("df", 0L, 100000L) {
            @Override
            public void     set (StreamOptions target, long value) {
                target.distributionFactor = (int) value;
            }     
            
            @Override
            protected boolean shouldPrint (StreamOptions source) {
                return false;
                //return (source.distributionFactor != StreamOptions.MAX_DISTRIBUTION);
            }
            
            @Override
            public void     printValue (StreamOptions source, StringBuilder out) {
                out.append (source.distributionFactor);
            }
        };
    
    private static final IntegerOptionProcessor <StreamOptions> IBS_PROC =
        new IntegerOptionProcessor <StreamOptions> ("initSize", 1L << 10, 1L << 30) {
            @Override
            public void     set (StreamOptions target, long value) {
                target.bufferOptions.initialBufferSize = (int) value;
            }  
            
            @Override
            public void     printValue (StreamOptions source, StringBuilder out) {
                out.append (source.bufferOptions.initialBufferSize);
            }
        };
    
    private static final IntegerOptionProcessor <StreamOptions> MAXBS_PROC =
        new IntegerOptionProcessor <StreamOptions> ("maxSize", 1L << 10, 1L << 30) {
            @Override
            public void     set (StreamOptions target, long value) {
                target.bufferOptions.maxBufferSize = (int) value;
            }  
            
            @Override
            public void     printValue (StreamOptions source, StringBuilder out) {
                out.append (source.bufferOptions.maxBufferSize);
            }
        };
    
    private static final IntegerOptionProcessor <StreamOptions> MAXTD_PROC =
        new IntegerOptionProcessor <StreamOptions> ("maxTime", 1L, 86400L) {
            @Override
            public void     set (StreamOptions target, long value) {
                target.bufferOptions.maxBufferTimeDepth = value * 1000;
            }
            
            @Override
            public boolean  shouldPrint (StreamOptions source) {
                return (source.bufferOptions.maxBufferTimeDepth != Long.MAX_VALUE);
            }
            
            @Override
            public void     printValue (StreamOptions source, StringBuilder out) {
                out.append (source.bufferOptions.maxBufferTimeDepth / 1000);
            }
        };
    
    private static final FlagOptionProcessor <StreamOptions>    LOSSLESS_PROC =
        new FlagOptionProcessor <StreamOptions> ("lossless") {
            @Override
            public void     set (OptionElement option, StreamOptions target) {
                target.bufferOptions.lossless = true;
            } 
            
            @Override
            public boolean  shouldPrint (StreamOptions source) {
                return (source.bufferOptions.lossless);
            }
        };
    
    private static final FlagOptionProcessor <StreamOptions>    LOSSY_PROC =
        new FlagOptionProcessor <StreamOptions> ("lossy") {
            @Override
            public void     set (OptionElement option, StreamOptions target) {
                target.bufferOptions.lossless = false;
            }  
            
            @Override
            public boolean  shouldPrint (StreamOptions source) {
                return (!source.bufferOptions.lossless);
            }
        };
    
    private static final FlagOptionProcessor <StreamOptions>    FIXED_TYPE_PROC =
        new FlagOptionProcessor <StreamOptions> ("fixedType") {
            @Override
            public void     set (OptionElement option, StreamOptions target) {
                int             n = 
                    target.getMetaData ().getContentClasses ().length;
                
                if (n != 1)
                    throw new IllegalFixedTypeStreamException (option, n);
                
                target.setPolymorphic (false);
            } 
            
            @Override
            public boolean  shouldPrint (StreamOptions source) {
                return (source.isFixedType ());
            }
        };
    
    private static final FlagOptionProcessor <StreamOptions>    POLYMORPHIC_PROC =
        new FlagOptionProcessor <StreamOptions> ("polymorphic") {
            @Override
            public void     set (OptionElement option, StreamOptions target) {
                target.setPolymorphic (true);
            }  
            
            @Override
            public boolean  shouldPrint (StreamOptions source) {
                return (source.isPolymorphic ());
            }
        };
    
    private static final BooleanOptionProcessor <StreamOptions>    HA_PROC =
        new BooleanOptionProcessor <StreamOptions> ("highAvailability") {
            @Override
            protected boolean   get (StreamOptions source) {
                return (source.highAvailability);
            }

            @Override
            protected void      set (StreamOptions target, boolean value) {
                target.highAvailability = value;
            }            
        };
    
    static final OptionProcessor []     DURABLE_STREAM_OPS = {
        //LOCATION_PROC,
        FIXED_TYPE_PROC,
        POLYMORPHIC_PROC,
        PERIODICITY_PROC,
        DF_PROC,
        HA_PROC
    }; 
    
    static final OptionProcessor []     TRANSIENT_STREAM_OPS = {
        //LOCATION_PROC,
        FIXED_TYPE_PROC,
        POLYMORPHIC_PROC,
        PERIODICITY_PROC,
        LOSSLESS_PROC,
        LOSSY_PROC,
        IBS_PROC,
        MAXBS_PROC,
        MAXTD_PROC
    };
    
    @SuppressWarnings ("unchecked")
    public static OptionProcessor <StreamOptions> []    forStream (DXTickStream s) {
        switch (s.getScope ()) {
            case DURABLE:   return (DURABLE_STREAM_OPS);
            case TRANSIENT: return (TRANSIENT_STREAM_OPS);
            default: return (null);
        }
    }
}
