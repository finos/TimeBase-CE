package com.epam.deltix.qsrv.dtb;

import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.dtb.test.*;
import java.io.*;
import org.junit.*;

/**
 *
 */
public class Test_DTBasics {
    private TestConfig      CONFIG = new TestConfig ();
    private DTBCreator      CREATOR = new DTBCreator (CONFIG);
    
    @Test
    public void         createAllAtOnce () throws IOException {
        TSRoot      root = CREATOR.format ();        
        CREATOR.insertMessages (root, 0, CONFIG.numMessages, 1);        
        CREATOR.close (root);
    
        CREATOR.verifyFullDB ();        
    }
    
    @Test
    public void         randomSelection () throws IOException {
        CREATOR.testRandomMessageSelection (CONFIG.numMessages);
    }
    
    @Test
    public void         createInAFewIterations () throws IOException {
        int         n1 = CONFIG.numMessages / 3;
        int         n2 = n1 * 2;
        
        TSRoot      root = CREATOR.format ();        
        CREATOR.insertMessages (root, 0, n1, 1);        
        CREATOR.close (root);
        
        root = CREATOR.open (false);
        CREATOR.insertMessages (root, n1, n2, 1);
        CREATOR.close (root);
        
        root = CREATOR.open (false);
        CREATOR.insertMessages (root, n2, CONFIG.numMessages, 1);
        CREATOR.close (root);
        
        CREATOR.verifyFullDB ();       
    }
}
