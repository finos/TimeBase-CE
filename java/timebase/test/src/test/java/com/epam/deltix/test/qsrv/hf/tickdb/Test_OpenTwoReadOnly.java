package com.epam.deltix.test.qsrv.hf.tickdb;


import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.lang.Util;
import java.io.File;
import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Test for bug 3542 - ability to simultaneously
 *  open two TickDB instances (in RO mode, of course).
 */
@Category(TickDBFast.class)
public class Test_OpenTwoReadOnly {
    private static final String     STREAM_KEY = "Test Stream";
    private static final String     STREAM_NAME = "Test Name";
    
    private File                    dbFile = new File (TDBRunner.getTemporaryLocation());
    private TradeMessage trade = new TradeMessage();
    private static final double     EPSILON = 0.00001;

    public void     createDB () {
        DXTickDB          db = TickDBFactory.create (dbFile);
        
        db.format ();
        
        DXTickStream      stream = 
            db.createStream (
                STREAM_KEY, 
                STREAM_NAME, 
                "Test Description",
                0
            );
        
        StreamConfigurationHelper.setTradeNoExchNoCur (stream);
        
        TickLoader      loader = stream.createLoader ();

        trade.setSymbol("DLTX");
        trade.setSize(10000);
        trade.setPrice(488.43);
        trade.setTimeStampMs(1220458596972L);
        
        loader.send (trade);
        loader.close ();
        
        db.close ();
    }
            
    public void     removeDB () {
        TickDB          db = TickDBFactory.create (dbFile);
        
        db.delete ();        
    }

    @Test
    public void     openWriteTwo () throws InterruptedException {
        DXTickDB          db1 = TickDBFactory.create (dbFile);
        DXTickDB          db2 = TickDBFactory.create (dbFile);

        db1.open (false);
        
        try {
            db2.open (true);
            assert false;
        } catch (IllegalStateException e) {
            // valid case
        } finally {
            db1.close();
            db2.close();
        }
    }

    @Ignore
    public void     testOpen () throws InterruptedException {
        DXTickDB          db1 = TickDBFactory.create (dbFile.getParentFile());

        try {
            db1.open (true);
            assert false;
        } catch (IllegalStateException e) {
            // valid case
        } finally {
            Util.close(db1);
        }

        DXTickDB          db = TickDBFactory.create (dbFile);
        db.open(false);
        db.close();
    }
    
    @Test
    public void     openTwo () throws InterruptedException {
        createDB();

        DXTickDB          db1 = TickDBFactory.create (dbFile);
        DXTickDB          db2 = TickDBFactory.create (dbFile);
        
        db1.open (true);
        db2.open (true);
        
        WritableTickStream      s1 = db1.getStream (STREAM_KEY);
        WritableTickStream      s2 = db2.getStream (STREAM_KEY);
        
        assertEquals (STREAM_NAME, s1.getName ());
        assertEquals (STREAM_NAME, s2.getName ());
        
        TickCursor      c1 = s1.select (trade.getTimeStampMs(), null);
        TickCursor      c2 = s2.select (trade.getTimeStampMs(), null);
        
        assertTrue (c1.next ());
        assertTrue (c2.next ());
        
        TradeMessage msg1 = (TradeMessage) c1.getMessage ();
        TradeMessage msg2 = (TradeMessage) c2.getMessage ();

        assertTrue (Util.equals (trade.getSymbol(), msg1.getSymbol()));
        assertEquals (trade.getSize(), msg1.getSize(), EPSILON);
        assertEquals (trade.getPrice(), msg1.getPrice(), EPSILON);
        assertEquals (trade.getTimeStampMs(), msg1.getTimeStampMs());

        assertTrue (Util.equals (trade.getSymbol(), msg2.getSymbol()));
        assertEquals (trade.getSize(), msg2.getSize(), EPSILON);
        assertEquals (trade.getPrice(), msg2.getPrice(), EPSILON);
        assertEquals (trade.getTimeStampMs(), msg2.getTimeStampMs());
        
        c1.close ();
        c2.close ();
        
        db1.close ();
        db2.close ();

        removeDB();
    }
}
