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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.xml.JAXBContextFactory;
import com.epam.deltix.util.xml.SkipValidationEventHandler;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 */
@Depends ({ "../jaxb.index", "../../pub/jaxb.index" })
//@DependsOnClass (UHFJAXBContext.class)
public class TickDBJAXBContext {
    private static final Log LOG = LogFactory.getLog(TickDBJAXBContext.class);
    public static final JAXBContext       INSTANCE;
    
    static {
        try {
        	INSTANCE = create ();
        } catch (JAXBException x) {
            LOG.error("Failed to initialize JAXB context for TimeBase: %s").with(x);
            throw new ExceptionInInitializerError (x);
        }
    }    
    
    public static Unmarshaller createUnmarshaller ()
        throws JAXBException
    {
        Unmarshaller unmarshaller = INSTANCE.createUnmarshaller();
        unmarshaller.setEventHandler(new SkipValidationEventHandler(LOG));
        return unmarshaller;
    }

    public static Marshaller        createMarshaller ()
        throws JAXBException
    {
        return (JAXBContextFactory.createStdMarshaller (INSTANCE));
    }
    
    private static JAXBContext      create () 
    	throws JAXBException
    {
        return JAXBContextFactory.newInstance (
            "com.epam.deltix.qsrv.hf.tickdb.pub:" + "com.epam.deltix.qsrv.hf.tickdb.impl:" + "com.epam.deltix.qsrv.hf.pub.md"
        );            
    }
}
