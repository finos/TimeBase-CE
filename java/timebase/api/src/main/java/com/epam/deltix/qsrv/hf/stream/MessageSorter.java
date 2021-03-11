package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class MessageSorter implements Disposable {
    private static final boolean            DEBUG_VERBOSE = false;
   
    private long                            numMsgs = 0;
    private long                            maxMemSize;
    private LongArrayList                   timestamps = 
        new LongArrayList (1 << 13);
    private LongArrayList                   starts = 
        new LongArrayList (1 << 13);
    private IntegerArrayList                lengths =
        new IntegerArrayList(1 << 13);
    private TeraByteOutputStream            buffer = 
        new TeraByteOutputStream ();
    private final Writer                    bufWriter;
    private File                            tmpDir;
    private File                            tmpFile = null;
    private RandomAccessFile                raf = null;
    private RandomAccessFileToOutputStreamAdapterMT rafos = null;
    private LongArrayList                   chunks = new LongArrayList ();
    private long []                         arrTimestamps;
    private long []                         arrStarts;
    private int []                          arrLengths;
    
    public static long          halfAvailableMemory () {
        return (Util.fractionOfAvailableMemory (0.5));
    }
    
    public MessageSorter (File tmpDir, TypeLoader typeLoader, RecordClassDescriptor... descriptors) {
        this (halfAvailableMemory (), typeLoader, descriptors);
    }
    
    public MessageSorter (long memSize, TypeLoader typeLoader, RecordClassDescriptor... descriptors) {
        this (memSize, null, typeLoader, descriptors);
    }

    public MessageSorter (long memSize, File tmpDir, TypeLoader typeLoader, RecordClassDescriptor... descriptors) {
        if (memSize < 4096)
            throw new IllegalArgumentException ("memSize: " + memSize);

        try {
            this.bufWriter = new Writer (buffer, typeLoader, descriptors);
        } catch (Exception e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
        maxMemSize = memSize;
        this.tmpDir = tmpDir;
    }
    
    private void         swap (int i, int j) {
        long        tmp = arrTimestamps [i];
        arrTimestamps [i] = arrTimestamps [j];
        arrTimestamps [j] = tmp;
        
        tmp = arrStarts [i];
        arrStarts [i] = arrStarts [j];
        arrStarts [j] = tmp;
        
        int       stmp = arrLengths [i];
        arrLengths [i] = arrLengths [j];
        arrLengths [j] = stmp;
    }
    
    private long         t (int idx) {
        return (arrTimestamps [idx]);
    }
    
    private int          med3 (int a, int b, int c) {
        long        xa = t (a);
        long        xb = t (b);
        long        xc = t (c);
        
        return (
            xa < xb ?
                (xb < xc ? b : xa < xc ? c : a) :
                (xb > xc ? b : xa > xc ? c : a)
        );
    }
    
    private int          findPivotIdx (int startIncl, int endExcl) {
        int                 len = endExcl - startIncl;
        
        int                 m1 = startIncl + len / 2;
        
        if (len > 7) {
            int m2 = startIncl;
            int m3 = endExcl - 1;
            
            if (len > 40) {             // Big arrays, pseudomedian of 9
                int         s = len / 8;
                int         s2 = s + s;
                
                m2 = med3 (m2, m2 + s, m2 + s2);
                m1 = med3 (m1 - s, m1, m1 + s);
                m3 = med3 (m3 - s2, m3 - s, m3);
            }
            
            m1 = med3 (m1, m2, m3); // Mid-size, med of 3
        }
        
        return (m1);
    }
    
    private void         sort (int startIncl, int endExcl) {
        for (;;) {
            int                 length = endExcl - startIncl;

            if (length < 2) 
                return;

            if (length < 7) {
                for (int i = startIncl + 1; i < endExcl; i++)
                    for (
                        int j = i;
                        j > startIncl && t (j - 1) > t (j); 
                        j--
                    )
                        swap (j, j - 1);

                return;
            }

            int                 lo = startIncl;
            int                 hi = endExcl;   
            int                 midIdx = findPivotIdx (startIncl, endExcl);

            //  Move pivot out of the way
            swap (midIdx, startIncl);

            long                pivotTS = t (startIncl);

            /*  Maintain: 
             *      lo < hi &&
             *      a [1 .. lo].timestamp <= pivotTS &&
             *      a [hi .. endExcl).timestamp >= pivotTS
             * 
             *  Move lo and hi to meet
             */
            partition: for (;;) {
                int             nextLo;

                for (;;) {
                    nextLo = lo + 1;

                    if (nextLo == hi)
                        break partition;

                    if (t (nextLo) > pivotTS)
                        break;

                    lo = nextLo;
                }

                int             nextHi;

                for (;;) {
                    nextHi = hi - 1;

                    if (nextHi == lo)
                        break partition;

                    if (t (nextHi) < pivotTS)
                        break;

                    hi = nextHi;
                }

                assert t (nextLo) > pivotTS;
                assert t (nextHi) < pivotTS;

                swap (nextLo, nextHi);
                
                lo = nextLo;
                hi = nextHi;
            }

            assert hi == lo + 1;

            //  Swap pivot back, say to the [lo] position.
            if (lo != startIncl)
                swap (startIncl, lo);

            /*  
             *  Sort to both sides of pivot.
             *  The following logic is equivalent to the full recursion:
             *      sort (a, startIncl, lo);
             *      sort (a, hi, endExcl);       
             *  However, we make an effort to eliminate tail recursion.
             */            
            if (lo - startIncl > endExcl - hi) {
                sort (hi, endExcl);

                endExcl = lo;
            }
            else {
                sort (startIncl, lo);

                startIncl = hi;
            }            
        }
    }

    private void                flushBuffer () throws IOException {
        int             num = timestamps.size ();
        
        if (DEBUG_VERBOSE)
            System.out.printf ("MessageSorter: Sorting %,d messages ...\n", num);
        
        arrTimestamps = timestamps.getInternalBuffer ();
        arrStarts = starts.getInternalBuffer ();
        arrLengths = lengths.getInternalBuffer ();
        
        sort (0, num);
        
        if (DEBUG_VERBOSE)
            System.out.printf ("MessageSorter: Sorting finished. (%d)\n", num);
        
        if (raf == null) {            
            tmpFile = File.createTempFile ("qsmsg.sort.", ".tmp", tmpDir);
            tmpFile.deleteOnExit ();            
            
            if (DEBUG_VERBOSE)
                System.out.printf (
                    "MessageSorter: Creating tmp file: %s.\n",
                    tmpFile.getPath ()
                );
            
            raf = new RandomAccessFile (tmpFile, "rw");            
            rafos = new RandomAccessFileToOutputStreamAdapterMT (raf);
        }
        
        if (DEBUG_VERBOSE)
            System.out.printf (
                "MessageSorter: Deflating %,d byte chunk (%,d messages) to %s.\n",
                buffer.size (),
                num,
                tmpFile.getPath ()
            );
        
        BufferedOutputStream    os =
            new BufferedOutputStream (new GZIPOutputStream (rafos));
                          
        for (int ii = 0; ii < num; ii++) {
            if (ii > 0 && arrTimestamps [ii] < arrTimestamps[ii-1])
                throw new RuntimeException ("timestamps out of order");
            
            long                offset = arrStarts [ii];
            int                 length = arrLengths [ii];
                        
            buffer.writeTo (os, offset, length);
        }

        os.close ();

        long        endOffset = rafos.getOffset ();
        
        if (DEBUG_VERBOSE)
            System.out.printf (
                "MessageSorter: Done; chunk #%d ends at %,d\n",
                chunks.size (),
                endOffset
            );
        
        chunks.add (endOffset);        
        
        buffer.reset ();
        timestamps.clear ();
        lengths.clear ();
        starts.clear ();
        
        //  Fairly important to release the following:
        arrTimestamps = null;   
        arrStarts = null;   
        arrLengths = null;
    }
    
    public void                 add (InstrumentMessage msg) throws IOException {
        long         start = buffer.size ();
        
        if (start + 10 * timestamps.size () >= maxMemSize) { 
            flushBuffer ();
            start = 0;
        }
        
        timestamps.add (msg.getTimeStampMs());
        starts.add (start);        
        bufWriter.send (msg);
        
        final long  length = buffer.size () - start;        
        
        if (length < 0 || length > Integer.MAX_VALUE)
            throw new RuntimeException ("length: " + length);
        
        lengths.add ((int) length);

        numMsgs++;
    }
    
    public long                 getTotalNumMessages () {
        return (numMsgs);
    }
    
    public void                 close () {
        buffer = null;
        timestamps = null;
        starts = null;
        lengths = null;
        
        if (raf != null) {
            Util.close (raf);
            raf = null;            
        }
                
        if (tmpFile != null) {
            if (DEBUG_VERBOSE)
                System.out.printf (
                    "MessageSorter: Deleting tmp file: %s.\n",
                    tmpFile.getPath ()
                );

            tmpFile.delete ();
        }
    }

    private class Writer extends MessageWriter2
    {
        private Writer(OutputStream out, TypeLoader loader, RecordClassDescriptor... descriptors) throws Exception {
            super(out, null, loader, CodecFactory.INTERPRETED, descriptors);
        }

        @Override
        protected int getTypeIndex(RecordClassDescriptor type) {
            int index = super.getTypeIndex(type); 
            if (index == -1)
                return (addNew (type, null, null));

            return index;
        }
    }

    private class Reader extends MessageReader2 {

        private Reader(InputStream in,
                       long inLength,
                       boolean unzip,
                       int bufferSize,
                       TypeLoader bindLoader,
                       RecordClassDescriptor[] types) throws IOException
        {
            super(in, inLength, unzip, bufferSize, bindLoader, types);
        }

        @Override
        protected RecordClassDescriptor[] readHeader() throws IOException {
            return new RecordClassDescriptor[0];
        }
    }
    
    private class BufferReader implements MessageSource <InstrumentMessage> {
        private int                     idx = -1;
        private int                     num;
        private ByteArrayInputStreamEx  is = new ByteArrayInputStreamEx ();
        private MessageReader2          rd;
        
        BufferReader (TypeLoader bindLoader, RecordClassDescriptor[] types) {
            try {
                rd = new Reader (is, buffer.size (), false, 0, bindLoader, types);
            } catch (Exception ex) {
                throw new RuntimeException (ex);
            }
            
            num = starts.size ();
            
            arrStarts = starts.getInternalBuffer ();
            arrLengths = lengths.getInternalBuffer ();
            arrTimestamps = timestamps.getInternalBuffer ();
            
            sort (0, num);
            
            //  Timestamps are no longer needed.
            arrTimestamps = null;
            timestamps = null;
        }
        
        public InstrumentMessage getMessage () {
            return (rd.getMessage ());
        }

        public boolean          isAtEnd () {
            return (idx >= num);
        }

        public boolean          next () {
            if (isAtEnd ())
                throw new IllegalStateException ("at end");
            
            idx++;
            
            if (idx >= num)
                return (false);
            
            buffer.setUpForReading (is, arrStarts [idx], arrLengths [idx]);
            
            if (!rd.next ())
                throw new RuntimeException ("MessageReader failed");
            
            return (true);
        }

        public void             close () {
            arrStarts = null;
            arrLengths = null;
            rd = null;
            is = null;
        }   
    }

    public MessageSource <InstrumentMessage> finish ()
        throws IOException
    {
        return (finish (TypeLoaderImpl.DEFAULT_INSTANCE));
    }

    public MessageSource<InstrumentMessage> finish (TypeLoader bindLoader)
        throws IOException
    {
        if (raf == null) 
            return (new BufferReader (bindLoader, bufWriter.getTypes()));
        else {
            flushBuffer ();
            
            buffer = null;
            timestamps = null;
            starts = null;
            lengths = null;
            
            rafos.close (); // does nothing
            
            MessageSourceMultiplexer <InstrumentMessage>    mux =
                new MessageSourceMultiplexer <InstrumentMessage> ();
        
            int         numChunks = chunks.size ();
            
            if (numChunks == 0)
                return (null);
            
            long        start = 0;
            
            for (int ii = 0; ii < numChunks; ii++) {
                final long          end = chunks.getLongNoRangeCheck (ii);
                long                length = end - start;
                
                InputStream         chunkInputStream =
                    new RandomAccessFileToInputStreamAdapterMT (raf, start) {
                        @Override
                        protected long additionalLimit () {
                            return end;
                        }                            
                    };
                MessageReader2       mr =
                        new Reader (chunkInputStream, length, true, 8192, bindLoader, bufWriter.getTypes());

//                MessageReader2       mr =
//                    new MessageReader2 (chunkInputStream, length, true, 8192, bindLoader);
                            
                if (numChunks == 1)
                    return (mr);
                
                if (mux == null)
                    mux = new MessageSourceMultiplexer <InstrumentMessage> ();
                
                mux.add (mr);
                start = end;
            }
            
            return (mux);
        }
    }

    // methods for loader.jsp page
    public TeraByteOutputStream getBuffer() {
        return buffer;
    }

    public long getBufferSize() {
        return maxMemSize;
    }

    public File getTmpFile() {
        return tmpFile;
    }
}
