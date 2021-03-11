package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.codec.HexBinCharEncoder;
import com.epam.deltix.util.io.CSVWriter;
import com.epam.deltix.util.io.MessageDigestOutputStream;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeFormatter;
import java.io.*;
import java.util.*;

/**
 *
 */
public class IMSPrinter {
    public static final String          NULLSTR = "_";
    public static final char            SEP = ',';
    public static final char            CR = '\n';
    public static final char            FSEP = ':';
    public static final String          NEWTYPE = ">";
    public static final String          STATICHDR = ">>";
    
    private InstrumentMessageSource     ims;
    private boolean                     closeWhenDone;
    private int                         count;
    private int                         maxCount = Integer.MAX_VALUE;
    private int                         maxBinary = 8;
    private RecordClassDescriptor       type;
    private Writer                      out;
    private boolean                     newLine = true;
    private CodecFactory                cf = CodecFactory.newInterpretingCachingFactory ();
    private UnboundDecoder              decoder = null;
    private final MemoryDataInput       mdi = new MemoryDataInput ();
    private final Calendar              cal = Calendar.getInstance (GMT.TZ);
    private final StringBuilder         sb = new StringBuilder ();
    private MessageDigestOutputStream   mdos = null;
    private HexBinCharEncoder           xe = null;
    private int                         numTypesSeen = 0;

    // copied from deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;
    public static final String      KEYWORD_ENTITY = "ENTITY";
    public static final String      KEYWORD_TIMESTAMP = "TIMESTAMP";
    public static final String      KEYWORD_SYMBOL = "SYMBOL";
    public static final String      KEYWORD_TYPE = "TYPE";

    public IMSPrinter (Writer out) {
        this.out = out;
    }
    
    public void                     setOut (Writer out) {
        this.out = out;
    }    
    
    public void                     setIMS (
        InstrumentMessageSource         ims,
        boolean                         closeWhenDone
    )
    {
        this.ims = ims;
        this.closeWhenDone = closeWhenDone;
        this.count = 0;
        this.type = null;
    }
    
    public InstrumentMessageSource  getIMS () {
        return (ims);
    }

    public int                      getMaxCount () {
        return maxCount;
    }

    public void                     setMaxCount (int maxCount) {
        this.maxCount = maxCount;
    }

    public void                     printAll () throws IOException {        
        try {            
            while (count < maxCount && ims.next ()) {
                InstrumentMessage msg = ims.getMessage ();

                printMessage (ims);
                count++;
            }            
        } finally {
            if (closeWhenDone)
                ims.close ();
        }        
    }
    
    private void                    print (String s, Object ... args) 
        throws IOException 
    {
        print (String.format (s, args));
    }
    
    private void                    print (CharSequence s) throws IOException {
        if (newLine) 
            newLine = false;        
        else
            out.write (SEP);
        
        CSVWriter.printCell (s == null ? NULLSTR : s, out);           
    }
    
    private void                    println () throws IOException {
        out.write (CR);
        out.flush ();
        newLine = true;
    }
    
    private void                    printTime (
        final long                      nanos,
        boolean                         skipZeroTod
    ) 
        throws IOException 
    {
        if (nanos == TimeConstants.TIMESTAMP_UNKNOWN) {
            print (NULLSTR);
            return;
        }

        final int ns;
        final long timestamp;
        if(skipZeroTod) {
            ns = 0;
            timestamp = nanos;
        }else {
            ns = (int) (nanos % TimeStamp.NANOS_PER_MS);
            timestamp = nanos / TimeStamp.NANOS_PER_MS;
        }

        cal.setTimeInMillis (timestamp);        
        
        sb.setLength (0);
        
        sb.append (
            String.format (
                "%04d-%02d-%02d", 
                cal.get (Calendar.YEAR),
                cal.get (Calendar.MONTH) + 1,
                cal.get (Calendar.DAY_OF_MONTH)
            )
        );
        
        int             s = cal.get (Calendar.SECOND);
        int             m = cal.get (Calendar.MINUTE);
        int             h = cal.get (Calendar.HOUR_OF_DAY);
        int             ms = cal.get (Calendar.MILLISECOND);
        boolean         hasMillis = ms != 0;
        boolean         hasNanos = ns != 0;
        boolean         hasSec = s != 0 || hasMillis || hasNanos || !skipZeroTod;
        boolean         hasHours = m != 0 || h != 0 || hasSec;
        
        if (hasHours) {
            sb.append (String.format (" %02d:%02d", h, m));
            
            if (hasSec)
                sb.append (String.format (":%02d", s));
                                    
            if (hasNanos)
                sb.append (String.format (".%09d", ns + ms * 1000000));            
            else if (hasMillis)
                sb.append (String.format (".%03d", ms));
        }      
        
        print (sb);
    }        
    
    private void                    printBinary (UnboundDecoder decoder) 
        throws IOException    
    {
        int         len = decoder.getBinaryLength ();
        
        if (xe == null)
            xe = new HexBinCharEncoder (out, false, true, 0);
        
        print ("");
        
        if (len > maxBinary) {
            //  Compute and print the hash
            if (mdos == null)
                mdos = new MessageDigestOutputStream (); //Note: Nov 2019: changed default algo from MD5 to SHA-256
            
            mdos.md.reset ();
            
            decoder.getBinary (0, len, mdos);
            
            byte [] d = mdos.md.digest ();
            
            out.write ('#');
            
            xe.write (d, d.length - 4, 4); // not secure, but good enough.
        }
        else                                    
            decoder.getBinary (0, len, xe);        
    }
        
    private void                    print (UnboundDecoder decoder) 
        throws IOException 
    {
        DataType    dt = decoder.getField ().getType ();
        
        if (dt instanceof DateTimeDataType)
            printTime (decoder.getLong (),  true);
        else if (dt instanceof BinaryDataType)
            printBinary (decoder);
        else if (dt instanceof TimeOfDayDataType)
            print (TimeFormatter.formatTimeofDayMillis (decoder.getInt ()));
        else
            print (decoder.getString ());
    }
    
    public void                     printTypeHeader () 
        throws IOException
    {
        String      typeName = type.getName ();
        
        if (typeName == null)
            typeName = NULLSTR;
        
        print ("%s%s", NEWTYPE, typeName);
    }
    
    public void                     printMessage (StreamMessageSource msginfo)
        throws IOException 
    {
        RawMessage          rmsg = (RawMessage) msginfo.getMessage ();        
        //
        //  Handle type
        //
        boolean             newType = msginfo.getCurrentTypeIndex () == numTypesSeen;
        
        if (newType) 
            numTypesSeen++;
            
        if (rmsg.type != type) {
            type = rmsg.type;
            decoder = cf.createFixedUnboundDecoder (type);// cached

            RecordClassInfo         cinfo = decoder.getClassInfo ();                        
            
            NonStaticFieldInfo []   nsf = cinfo.getNonStaticFields ();

            printTypeHeader ();
            print (KEYWORD_TIMESTAMP);
            print (KEYWORD_SYMBOL);
            print (KEYWORD_TYPE);

            int                     numNsf = 0;
            
            if (nsf != null)
                for (NonStaticFieldInfo fi : nsf) {
                    print (fi.getName ());      
                    numNsf++;
                }

            if (newType) {
                StaticFieldInfo []  sf = cinfo.getStaticFields ();
                
                if (sf != null && sf.length != 0) {
                    for (StaticFieldInfo fi : sf) 
                        print (fi.getName ()); 
                    
                    println ();
                    
                    print (STATICHDR);
                    
                    for (int ii = -3; ii < numNsf; ii++)
                        print ("");
                    
                    for (StaticFieldInfo fi : sf) 
                        print (fi.getString ());
                }
            }
            
            println ();
        }
                
        print ("%,d", count);
        printTime (rmsg.getNanoTime(), false);
        print (rmsg.getSymbol());
                       
        rmsg.setUpMemoryDataInput (mdi);
        decoder.beginRead (mdi);

        while (decoder.nextField ()) {
            if (decoder.isNull ())
                print (NULLSTR);
            else 
                print (decoder);            
        }        
        
        println ();
    }           
}
