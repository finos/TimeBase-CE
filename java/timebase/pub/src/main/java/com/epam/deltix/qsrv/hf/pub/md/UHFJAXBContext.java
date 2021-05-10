package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.xml.JAXBContextFactory;

import java.util.logging.Level;
import javax.xml.bind.*;

import com.epam.deltix.util.xml.JAXBStackTraceSuppressor;

/**
 *
 */
@Depends ("../jaxb.index")
public class UHFJAXBContext {
	public static final String      PACKAGE_PATH =
        "com.epam.deltix.qsrv.hf.pub.md";
    
    public static final JAXBContext       INSTANCE;
    
    static {
        try {
        	INSTANCE = create ();
        } catch (JAXBException x) {
            Util.logException("Failed to initialize JAXB context for UHF communication: %s", x);
            throw new ExceptionInInitializerError (x);
        }
    }    
    
    public static Unmarshaller      createUnmarshaller ()
        throws JAXBException
    {
        return (JAXBContextFactory.createStdUnmarshaller (INSTANCE));
    }

    public static Marshaller        createMarshaller ()
        throws JAXBException
    {

        return (JAXBContextFactory.createStdMarshaller (INSTANCE));
    }
    
    private static JAXBContext      create () 
    	throws JAXBException
    {
        return JAXBStackTraceSuppressor.createContext (PACKAGE_PATH);            
    }
}
