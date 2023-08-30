/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.xml.JAXBContextFactory;
import com.epam.deltix.util.xml.JAXBUtil;
import com.epam.deltix.util.xml.TransientAnnotationReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Depends ({ "../jaxb.index", "../download/jaxb.index", "../stream/jaxb.index" })
public class TBJAXBContext {
    private static final String PACKAGE_PATH =
            "com.epam.deltix.qsrv.hf.tickdb.http:" +
            "com.epam.deltix.qsrv.hf.tickdb.http.download:" +
            "com.epam.deltix.qsrv.hf.tickdb.http.stream:";

    private static final JAXBContext INSTANCE;

    static {
        try {
            INSTANCE = create();
        } catch (JAXBException x) {
            Util.logException("Failed to initialize JAXB context for Timebase HTTP API: %s", x);
            throw new ExceptionInInitializerError(x);
        }
    }

    public static Unmarshaller createUnmarshaller ()
            throws JAXBException
    {
        return (JAXBContextFactory.createStdUnmarshaller(INSTANCE));
    }

    public static Marshaller createMarshaller ()
            throws JAXBException
    {

        return (JAXBContextFactory.createStdMarshaller (INSTANCE));
    }

    private static JAXBContext create()
            throws JAXBException {
        TransientAnnotationReader reader = new TransientAnnotationReader();
        reader.addTransientClass(PropertyChangeSupport.class);

        Map<String, Object> jaxbConfig = new HashMap<>();
        jaxbConfig.put(JAXBUtil.ANNOTATION_READER_PROPERTY, reader);

        return JAXBContext.newInstance(PACKAGE_PATH, TBJAXBContext.class.getClassLoader(), jaxbConfig);
    }
}