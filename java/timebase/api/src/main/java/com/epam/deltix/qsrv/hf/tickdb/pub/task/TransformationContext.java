package com.epam.deltix.qsrv.hf.tickdb.pub.task;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.xml.JAXBContextFactory;
import com.epam.deltix.util.xml.JAXBStackTraceSuppressor;
import com.epam.deltix.util.xml.JAXBUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.HashMap;
import java.util.Map;

@Depends ("../jaxb.index")
public class TransformationContext {
    private static JAXBContext CONTEXT;

    private static JAXBContext createContext (String ... path)
    	throws JAXBException
    {
        Map<String, Object> jaxbConfig = new HashMap<String, Object>();
        jaxbConfig.put(JAXBUtil.ANNOTATION_READER_PROPERTY, new JAXBStackTraceSuppressor());

        return JAXBContextFactory.newInstance(StringUtils.join(":", path), jaxbConfig);
    }

    public static JAXBContext getContext() throws JAXBException {
        synchronized (SchemaChangeTask.class) {
            if (CONTEXT == null)
                CONTEXT = createContext(
                        SchemaChangeTask.class.getPackage().getName(),
                        RecordClassDescriptor.class.getPackage().getName(),
                        MetaDataChange.class.getPackage().getName() );
        }

        return CONTEXT;
    }

    static JAXBContext getContext(TransformationTask task) throws JAXBException {
        return getContext();
    }

    public static Marshaller createMarshaller(TransformationTask task) throws JAXBException {
        return JAXBContextFactory.createStdMarshaller(getContext(task)); 
    }

    public static Unmarshaller createUnmarshaller(TransformationTask task) throws JAXBException {
        return JAXBContextFactory.createStdUnmarshaller(getContext(task));
    }
}
