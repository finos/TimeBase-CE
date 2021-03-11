package com.epam.deltix.qsrv.dtb.test;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.qsrv.dtb.store.impl.PDSFactory;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.dtb.store.raw.*;

import java.io.*;
import java.util.Random;
import static org.junit.Assert.*;

/**
 *  
 */
public class DTBCreator {

    private TestConfig                  config;
    private String []                   symbols;

    public DTBCreator (TestConfig config) {
        this.config = config;
        
        symbols = new String [config.numEntities];
        
        for (int ii = 0; ii < config.numEntities; ii++) 
            symbols [ii] = "E" + ii;                    
    }
        
    public TSRoot    format () throws IOException {
        FSUtils.removeRecursive (config.path, false, null);
        
        PersistentDataStore         store = PDSFactory.create ();
        
        store.start ();
        
        TSRoot      root = store.createRoot (null, config.path);
        
        root.setMaxFileSize (config.maxFileSize);
        root.setMaxFolderSize (config.maxFolderSize);
        root.setCompression (config.compression);
        
        root.format ();
        
        for (int ii = 0; ii < config.numEntities; ii++) {
            int     id = 
                root.getSymbolRegistry ().registerSymbol (
                    symbols [ii], 
                    "D:" + symbols [ii]
                );
            
            assertEquals (ii, id);            
        }
        
        return (root);
    }
    
    public TSRoot    open (boolean readOnly) {
        PersistentDataStore         store = PDSFactory.create ();
        
        store.start ();

        TSRoot      root = store.createRoot (null, config.path);
                
        root.open (readOnly);
        
        assertEquals (config.maxFileSize, root.getMaxFileSize ());
        assertEquals (config.maxFolderSize, root.getMaxFolderSize ());
        assertEquals (config.compression, root.getCompression ());
        
        return (root);
    }
    
    public void      close (TSRoot root) {
        PersistentDataStore     store = root.getStore ();
        
        store.waitUntilDataStored (0);
        
        //System.out.println (root);
        
        root.close ();
        
        store.shutdown ();
        
        store.waitForShutdown (0); 
    }
    
    public void         insertMessages (TSRoot root, int from, int to, int step) {
        MessageGenerator    msgen = new MessageGenerator (config);

        try (DataWriter writer = root.getStore ().createWriter ()) {        
            writer.associate (root);
            
            boolean         firstTime = true;                        

            for (int count = from; count < to; count += step) {
                msgen.setSeqNo (count);
                
                if (firstTime) {
                    firstTime = false;
                    writer.open (msgen.getTimestamp (), null);
                }
                
                msgen.writeTo (writer);                
            }                                
        }        
    }
    
    public void         readMessages (
        TSRoot              root,
        long                openTimestamp,
        EntityFilter        filter,
        boolean             forward
    )
    {
        MessageGenerator    msgen = new MessageGenerator (config);
        
        try (DataReader reader = root.getStore ().createReader (false)) {
            reader.associate (root);

            reader.open (openTimestamp, forward, filter);

            int             expectedSeqNo = msgen.timestampToSeqNo (openTimestamp, filter);
                       
            msgen.setSeqNo (expectedSeqNo);
                        
            while (reader.readNext (msgen)) {
                if (forward)
                    expectedSeqNo = msgen.getNextSeqNo (expectedSeqNo + 1, filter); 
                else
                    expectedSeqNo = msgen.getPrevSeqNo (expectedSeqNo - 1, filter); 
                
                msgen.setSeqNo (expectedSeqNo);
            }            
        } 
    }
    
    public void         testRandomMessageSelection (int numTests) {
        TSRoot              root = open (true); 
        MessageGenerator    msgen = new MessageGenerator (config);
        Random              rnd = new Random (config.numMessages);
        
        try (DataReader reader = root.getStore ().createReader (false)) {
            reader.associate (root);

            int             seqNo = rnd.nextInt (config.numMessages);
            
            testOneRandomMessageSelection (reader, msgen, false, seqNo);
        } 
        
        close (root);
    }
        
    public void         testOneRandomMessageSelection (
        DataReader          reader,
        MessageGenerator    msgen,
        boolean             useFilter,
        int                 seqNo
    )
    {
        msgen.setSeqNo (seqNo);
        
        EntityFilter        filter;
        
        if (useFilter)
            filter = new SingleEntityFilter (msgen.getEntity ());
        else
            filter = null;
        
        reader.open (msgen.getTimestamp (), true, filter);

        boolean             ok = reader.readNext (msgen);
        
        assertTrue ("Failed to read message at ts=" + msgen.getTimestamp (), ok); 
        
        reader.close ();
    }
    
    public void     verifyStructure () throws IOException {
        StringBuilder   err = new StringBuilder ();
        DiagPrinter     dp = new DiagPrinter (err);
        
        dp.setPrintProgress (false);
        
        Verifier        vfr = new Verifier (dp, VerificationMode.COMPLETE);
        
        vfr.verifyFolder (config.path);
        
        if (err.length () > 0)
            throw new AssertionError ("ERRORS FOUND:\n\n" + err);
    }
    
    public void         verifyAllByReading (boolean forward) throws IOException {
        TSRoot      root = open (true);        
        readMessages (
            root,
            forward ? config.baseTime : config.baseTime + config.numMessages, 
            EntityFilter.ALL, 
            forward
        );
        
        close (root);
    }
        
    public void         verifyFullDB () throws IOException {
        verifyStructure ();
        verifyAllByReading (true);
        verifyAllByReading (false);
    }
    
    public static void      main (String [] args) throws Exception {
        TestConfig  CONFIG = new TestConfig ();

        //CONFIG.numMessages = 3;

        DTBCreator  CREATOR = new DTBCreator (CONFIG);

        int         n1 = CONFIG.numMessages / 3;
        int         n2 = n1 * 2;
        
        TSRoot      root = CREATOR.format ();        
        CREATOR.insertMessages (root, 0, n1, 1);        
        CREATOR.close (root);
        
        CREATOR.verifyFullDB ();
        
        root = CREATOR.open (false);
        CREATOR.insertMessages (root, n1, n2, 1);
        CREATOR.close (root);
        
        CREATOR.verifyFullDB ();
        
        root = CREATOR.open (false);
        CREATOR.insertMessages (root, n2, CONFIG.numMessages, 1);
        CREATOR.close (root);
        
        CREATOR.verifyFullDB ();
        
        CREATOR.testRandomMessageSelection (CONFIG.numMessages);
    }
}
