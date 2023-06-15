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

import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.v2.model.annotation.AnnotationReader;
import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.util.SerializationUtils;

import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.xml.JAXBContextFactory;
import com.epam.deltix.util.xml.JAXBStackTraceSuppressor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Protocol {
    //static final Logger LOGGER = Logger.getLogger ("deltix.tickdb");

    public static final String     OLD_NS = "http://xml.deltixlab.com/internal/quantserver/2.0";
    public static final String     NEW_NS = "http://xml.deltixlab.com/internal/quantserver/3.0";

    //  V2.0 constants
    public static final byte [] MAGIC = { 23, 44, -101, 7 };
    public static final byte    VERSION = 16;

    //  V 1.2 constants
    public static final int     SECOND_BAR_CODE = 1;
    public static final int     MINUTE_BAR_CODE = 2;
    public static final int     HOUR_BAR_CODE = 3;
    public static final int     DAY_BAR_CODE = 4;
    public static final int     CUSTOM_BAR_CODE = 255;
    public static final int     CUSTOM_BAR_CODE2 = 254;

    public static final String        FILE_EXTENSION = ".qsmsg.gz";

    public static long          MAX_TIME =
            GMT.getCalendarInstance(2099, 11, 31, 0, 0, 0, 0).getTimeInMillis();

    
    public static boolean                   isDeflatedMessageStream (File f) {
        return (f.getName ().toLowerCase ().endsWith (".gz"));
    }

    public static byte                             getVersion(File f) throws IOException {
        return MessageReader2.readVersion(f);
    }

    public static void                      writeTypes (
        DataOutputStream                    out,
        final RecordClassDescriptor[]       rcd
    )
        throws IOException
    {
        try {
            Marshaller m = JAXBContextFactory.createStdMarshaller(createContext());

            StringWriter s = new StringWriter ();
            synchronized (rcd) {
                m.marshal (new ClassSet(rcd), s);
            }

            String xml = s.toString();
            if (xml.length() < 65535) {
                out.writeUTF(xml);
            } else {
                out.writeUTF("");
                SerializationUtils.writeHugeString(out, xml);
            }

        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

    public static TypeLoader getDefaultTypeLoader() {
        return TypeLoaderImpl.DEFAULT_INSTANCE;
    }

    public static ConsumableMessageSource<InstrumentMessage> openRawReader(File f)
        throws IOException
    {
        byte version = MessageReader2.readVersion(f);

        if (version == 0)
            throw new UnsupportedOperationException("Old file format = " + version + " is not supported.");
            //return new MessageReader(f, true);
        else
            return MessageReader2.createRaw(f);
    }

    public static ConsumableMessageSource<InstrumentMessage> openReader(File f, TypeLoader loader)
        throws IOException
    {
        byte version = MessageReader2.readVersion(f);
        if (version == 0)
            throw new UnsupportedOperationException("Old file format = " + version + " is not supported.");
            //return new MessageReader(f);
        else
            return MessageReader2.create(f, loader);
    }

    public static ConsumableMessageSource<InstrumentMessage> openReader(File f, int bufferSize, TypeLoader loader)
        throws IOException
    {
        byte version = MessageReader2.readVersion(f);
        if (version == 0)
            throw new UnsupportedOperationException("Old file format = " + version + " is not supported.");
            //return new MessageReader(f, bufferSize, false);
        else
            return MessageReader2.create(f, bufferSize, loader);
    }

    public static ConsumableMessageSource<InstrumentMessage> createReader(
              File f,
              RecordClassDescriptor[] types) throws IOException
    {
        byte version = MessageReader2.readVersion(f);
        if (version == 0)
            throw new UnsupportedOperationException("Old file format = " + version + " is not supported.");
            //return new MessageReader(f);
        else
            return MessageReader2.create(f, types);
    }

    private static JAXBContext      createContext ()
    	throws JAXBException
    {
        Map<String, Object> jaxbConfig = new HashMap<String, Object>();
        AnnotationReader reader = new JAXBStackTraceSuppressor();
        jaxbConfig.put(JAXBRIContext.ANNOTATION_READER, reader);

        String path = RecordClassDescriptor.class.getPackage().getName() + ":" +
                        ClassSet.class.getPackage().getName();
        return JAXBContextFactory.newInstance (path, jaxbConfig);
    }

    private static String upgradeMetaData(String xml)
            throws IOException
    {
        if (xml.contains(OLD_NS))
            throw new IllegalArgumentException(OLD_NS + " IS NOT Supported");

        return xml;
    }

    public static MessageFileHeader     readHeader (File file) throws IOException {
        byte v = MessageReader2.readVersion(file);
        if (v != 0)
            return MessageReader2.readHeader(file);

        throw new UnsupportedOperationException("Old file format = " + v + " is not supported.");
    }

    /** @param fileName file name
     *  @return timestamp of the first message stored in the file with given fullFileName. Used by QuantOfficeBlotter
     *  @throws java.io.IOException if the any IO error occurs */

    public static long                  getStartTime (String fileName)
        throws IOException
    {
        ConsumableMessageSource<InstrumentMessage> rd = null;
        try {
            rd = openRawReader(new File(fileName));
            if (rd.next())
                return rd.getMessage().getTimeStampMs();
            return 0;
        } finally {
            Util.close(rd);           
        }
    }

    static ClassSet                  readTypes (DataInputStream in)
            throws IOException
    {
        try {
            String xml = in.readUTF ();
            if (xml.length() == 0) { // read huge string
                StringBuilder sb = new StringBuilder();
                SerializationUtils.readHugeString(in, sb);
                xml = sb.toString();
            }

//            String updXml = TDBUpgrade3.removeJavaClassName(xml);
//            if (updXml != null)
//                xml = updXml;

            //xml = upgradeMetaData(xml);
            Unmarshaller u = JAXBContextFactory.createStdUnmarshaller(createContext());

            ClassSet classSet = (ClassSet) u.unmarshal(new StringReader(xml));
//            try {
//                classSet = new SchemaUpdater(new ClassMappings()).update(classSet);
//            } catch (ClassNotFoundException | Introspector.IntrospectionException e) {
//                LOGGER.log(Level.WARNING, "Failed to update ClassSet " + classSet.toString() + ". ", e);
//            }
            return classSet;
        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

    static MessageFileHeader                  readTypes (DataInputStream in, byte version)
        throws IOException
    {
        try {
            String xml = in.readUTF ();
            if (xml.length() == 0) { // read huge string
                StringBuilder sb = new StringBuilder();
                SerializationUtils.readHugeString(in, sb);
                xml = sb.toString();
            }

//            String updXml = TDBUpgrade3.removeJavaClassName(xml);
//            if (updXml != null)
//                xml = updXml;

            xml = upgradeMetaData(xml);
            Unmarshaller u = JAXBContextFactory.createStdUnmarshaller(createContext());

            ClassSet classSet = (ClassSet) u.unmarshal(new StringReader(xml));
//            try {
//                classSet = new SchemaUpdater(new ClassMappings()).update(classSet);
//            } catch (ClassNotFoundException | Introspector.IntrospectionException e) {
//                LOGGER.log(Level.WARNING, "Failed to update ClassSet " + classSet.toString() + ". ", e);
//            }
            //Interval periodicity = classSet.upgrade();

            return new MessageFileHeader(version, classSet, null);

        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

//    private static ClassSet convert(ClassSet set, MessageFileHeader header) {
//        ClassDescriptor[] all = set.getClasses();
//
//        for (int i = 0; i < all.length; i++) {
//            ClassDescriptor cd = all[i];
//
//            if (cd instanceof RecordClassDescriptor) {
//                RecordClassDescriptor rcd = TDBUpgrade23.removeBarSize((RecordClassDescriptor)cd);
//
//                if (rcd != null) {
//                    all[i] = rcd;
//                    header.periodicity = TDBUpgrade23.getPeriodicity(((RecordClassDescriptor) cd).getField("barSize"));
//                }
//            }
//        }
//
//        ClassSet result = new ClassSet();
//
//    }
//
//    static void readEnumTypes (ArrayList<ClassDescriptor> classes, ClassSet inputClassSet){
//            for (ClassDescriptor cd: inputClassSet.getClasses()){
//                if (cd instanceof EnumClassDescriptor){
//                    classes.add(cd);
//                }
//            }
//    }
//
//    static void readRCDescriptors(ArrayList<ClassDescriptor> classes, ClassSet classSet, MessageFileHeader header){
//        classes.addAll(Arrays.asList(classSet.getContentClasses()));
//        for (int i = 0; i < classes.size(); i++) {
//            RecordClassDescriptor type = (RecordClassDescriptor) classes.get(i);
//            RecordClassDescriptor cd = TDBUpgrade23.removeBarSize(type);
//            if (cd != null) {
//                classes.set(i, cd);
//                if (classes.size() == 1) // we have only bar messages
//                    header.periodicity = TDBUpgrade23.getPeriodicity(type.getField("barSize"));
//            }
//        }
//    }
}