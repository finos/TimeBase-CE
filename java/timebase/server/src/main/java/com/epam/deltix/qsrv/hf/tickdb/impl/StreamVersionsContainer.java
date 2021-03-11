package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.AbstractMessageWriter;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.ramdisk.FD;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.schema.SchemaChangeMessage;
import com.epam.deltix.timebase.messages.service.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class StreamVersionsContainer extends AbstractMessageWriter implements Closeable{

    private final ArrayList<StreamVersionsReader> readers = new ArrayList<StreamVersionsReader>();
    private long                    version = -1;
    private final DataFile          file;
    final MemoryDataOutput          buffer = new MemoryDataOutput(8192);
    private TickStreamImpl          stream;
    private final RecordClassDescriptor[]   descriptors;

    public static class DataFile extends FD {
        static final int        HEADER_SIZE = 64;
        static final int        LENGTH_OFFSET = 16;
        static final int        VERSION_OFFSET = 24;
        private byte []         header = new byte [HEADER_SIZE];

        long             version = -1;

        public DataFile(TickDBImpl db, File file) {
            super(db.ramdisk, file, false);
        }

        @Override
        public long                 getPrivateHeaderLength() {
            return HEADER_SIZE;
        }

        @Override
        protected void          onOpen () throws IOException {
            directRead(0, header, 0, HEADER_SIZE);
            setLogicalLength (DataExchangeUtils.readLong(header, LENGTH_OFFSET));
            version = DataExchangeUtils.readLong(header, VERSION_OFFSET);
        }

//        @Override
//        protected void          onCommitLength(long length) throws IOException {
//            DataExchangeUtils.writeLong (header, LENGTH_OFFSET, length);
//            directWrite(LENGTH_OFFSET, header, LENGTH_OFFSET, 16);
//        }

        public void            commit() throws IOException {
            directForce();
            onCommitLength();
        }

        @Override
        protected void          onCommitLength() throws IOException {
            DataExchangeUtils.writeLong (header, VERSION_OFFSET, version);
            DataExchangeUtils.writeLong (header, LENGTH_OFFSET, getLogicalLength());
            directWrite(LENGTH_OFFSET, header, LENGTH_OFFSET, 16);
        }

        @Override
        protected void          onFormat () throws IOException {
            header = new byte [HEADER_SIZE];

            DataExchangeUtils.writeLong (header, LENGTH_OFFSET, HEADER_SIZE);
            DataExchangeUtils.writeLong (header, VERSION_OFFSET, version = -1);
            directWrite (0, header, 0, HEADER_SIZE);
            setLogicalLength (HEADER_SIZE);
        }
    }

     public StreamVersionsContainer(TickStreamImpl stream, CodecFactory factory, Class[] classes, RecordClassDescriptor[] descriptors)
            throws IOException {

        // assuming condition is true !isReadOnly || versionsFile.exists()

        this.stream = stream;
        this.descriptors = descriptors;
        File vFile = stream.getVersionsFile();
        file = new DataFile(stream.getDBImpl(), vFile);

        boolean ro = stream.isReadOnly;

        assert !ro || vFile.exists();

        if (!vFile.exists() && !ro)
            file.format();
        else
            file.open(ro);

        version = file.version;

        for (int i = 0; i < classes.length; i++) {
            FixedBoundEncoder encoder = factory.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, descriptors[i]);
            addNew(descriptors[i], classes[i], encoder);
        }

        for (RecordClassDescriptor rcd : descriptors)
            addNew(rcd, null, null);
    }

    public boolean add(InstrumentMessage msg) {
        if (msg instanceof StreamTruncatedMessage)
            return add((StreamTruncatedMessage)msg);

        if (msg instanceof MetaDataChangeMessage)
            return add((MetaDataChangeMessage)msg);

        if (msg instanceof SchemaChangeMessage)
            return add((SchemaChangeMessage) msg);

        throw new UnsupportedOperationException("Message " + msg + " is not supported.");
    }

//    public boolean     add(StreamPurgedMessage msg) {
//        synchronized (file) {
//            file.version = msg.version = ++version;
//            return addMessage(msg);
//        }
//    }

    public boolean     add(MetaDataChangeMessage msg) {
        synchronized (file) {
            msg.setVersion(++version);
            file.version = msg.getVersion();
            return addMessage(msg);
        }
    }

    public boolean     add(StreamTruncatedMessage msg) {
        synchronized (file) {
            msg.setVersion(++version);
            file.version = msg.getVersion();
            return addMessage(msg);
        }
    }

    public boolean     add(SchemaChangeMessage msg) {
        synchronized (file) {
            msg.setVersion(++version);
            file.version = msg.getVersion();
            return  addMessage(msg);
        }
    }
    
    private boolean                 addMessage(SystemMessage msg) {

        try {
            encode(msg, buffer);

            final int   size = buffer.getSize ();

            writeMessage(buffer.getBuffer(), 0, size);

            for (int i = 0; i < readers.size(); i++) {
                StreamVersionsReader reader = readers.get(i);
                if (reader.waiting)
                    reader.submitNotifier();
            }

        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }

        return true;
    }

    private boolean                 addMessage(SchemaChangeMessage msg) {

        try {
            encode(msg, buffer);

            final int   size = buffer.getSize ();

            writeMessage(buffer.getBuffer(), 0, size);

            for (int i = 0; i < readers.size(); i++) {
                StreamVersionsReader reader = readers.get(i);
                if (reader.waiting)
                    reader.submitNotifier();
            }

        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }

        return true;
    }

    private void                    writeMessage (byte[] data, int offset, int length) throws IOException {
        long address = file.getLogicalLength();
        address = LocalMessageSizeCodec.write(address, length, file);
        file.write(address, data, offset, length);
        file.commit();
    }

    StreamVersionsReader            getReader (long time, SelectionOptions options) {
        StreamVersionsReader reader = new StreamVersionsReader(this, 
                new RawReader<DataFile>(getStream(), file, DataFile.HEADER_SIZE, true),
                time, options);

        synchronized (readers) {
            readers.add (reader);
        }

        return (reader);
    }

    public TickStreamImpl           getStream() {
        return stream;
    }

    public synchronized long        getVersion() {
        return version;
    }

    public void                     onClose(StreamVersionsReader reader) {
        synchronized (readers) {
            readers.remove (reader);
        }
    }
    
    public void                     close() {
        Util.close(file);
    }

    public boolean                  delete() {
        close();
        return file.getFile().delete();
    }
}
