package com.epam.deltix.qsrv.hf.tickdb.http.upload;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.readIdentityKey;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.http.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions.WriteMode;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.LittleEndianDataInputStream;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public class UploadHandler extends AbstractHandler implements Runnable, LoadingErrorListener {
    private final DXTickDB db;

    private final DataInput din;
    private final InputStream is;
    private final HttpServletResponse response;
    private final Principal user;

    private volatile boolean cancel = false;
    private short maxAllowedErrors;
    private final ObjectArrayList<LoadingError> errors = new ObjectArrayList<>();

    private TickLoader loader;
    private RecordClassDescriptor[] concreteTypes;
    private final ObjectArrayList<ConstantIdentityKey> entities = new ObjectArrayList<>();
    private byte[] streamMsgBuffer = new byte[256];
    private final RawMessage raw = new RawMessage();

    public UploadHandler(DXTickDB db, boolean useCompression, InputStream plainIs, HttpServletResponse response, Principal user) throws IOException {
        this.db = db;

        is = useCompression ? new GZIPInputStream(plainIs) : plainIs;
        final int endianness = this.is.read();
        switch (endianness) {
            case 0: // Little-endian
                this.din = new LittleEndianDataInputStream(is);
                break;
            case 1: // Big-endian
                this.din = new DataInputStream(is);
                break;
            default:
                throw new ValidationException(String.format("invalid endianness field %d", endianness));
        }

        this.response = response;
        this.user = user;
    }

    private void process() throws IOException, JAXBException {
        // version stream write_mode allowed_errors
        final short version = din.readShort();
        HTTPProtocol.validateVersion(version);
        final String streamKey = din.readUTF();

        final WriteMode writeMode = WriteMode.values()[din.readByte()];
        maxAllowedErrors = din.readShort();

        final DXTickStream stream = db.getStream(streamKey);
        if (stream == null)
            throw new UnknownStreamException(String.format("Stream \"%s\" doesn't exist", streamKey));

        LoadingOptions options = new LoadingOptions(true);
        options.writeMode = writeMode;

        concreteTypes = stream.getStreamOptions().getMetaData().getTopTypes();

        try (final TickLoader loader = stream.createLoader(options)) {
            loader.addEventListener(this);
            this.loader = loader;
            int code;

            while ((code = is.read()) != -1) {

                if (cancel) {
                    completeUpload();
                    break;
                }

                switch (code) {

                    case HTTPProtocol.MESSAGE_BLOCK_ID:
                        while (readMessageRecord()) {
                            if (cancel) {
                                completeUpload();
                                break;
                            }
                        }
                        break;
                    case HTTPProtocol.INSTRUMENT_BLOCK_ID:
                        entities.add(readIdentityKey(din));
                        break;
                    case HTTPProtocol.TERMINATOR_BLOCK_ID:
                        completeUpload();
                        break;
                    default:
                        throw new IllegalStateException("unexpected code=" + code);
                }
            }
        }

        //HTTPProtocol.LOGGER.log(Level.INFO, "Stream loader " + loader.getTargetStream() + ": " + System.currentTimeMillis() + ": upload is done.");
    }

    private boolean readMessageRecord() throws IOException {
        int size = din.readInt();

        if (size == HTTPProtocol.TERMINATOR_RECORD)
            return false;

        size -= HTTPProtocol.LOADER_MESSAGE_HEADER_SIZE; // readLong + readShort + readByte
        if (size < 0)
            throw new IllegalStateException("size=" + size);

        // read: timestamp instrument_index type_index body
        raw.setNanoTime(din.readLong());
        final ConstantIdentityKey id = entities.get(din.readShort());
        raw.setSymbol(id.symbol);
        byte typeIndex = din.readByte();
        raw.type = concreteTypes[typeIndex];

        if (streamMsgBuffer.length < size)
            streamMsgBuffer = new byte[Util.doubleUntilAtLeast(streamMsgBuffer.length, size)];

        din.readFully(streamMsgBuffer, 0, size);
        raw.setBytes(streamMsgBuffer, 0, size);
        loader.send(raw);

        return true;
    }

    @Override
    public void onError(LoadingError e) {
        HTTPProtocol.LOGGER.log(Level.WARNING, "Error while loading message: ", e);

        errors.add(e);
        if (errors.size() > maxAllowedErrors)
            cancel = true;
    }

    private void completeUpload() throws IOException, JAXBException {

        Util.close(loader);

        final StringBuilder sb = new StringBuilder();
        for (LoadingError error : errors) {
            sb.append(error.getMessage()).append(Util.NATIVE_LINE_BREAK);
        }

        final LoadResponse lr = new LoadResponse();
        lr.wasError = !errors.isEmpty();
        lr.responseMessage = cancel ?
                "Number of loading errors exceeded the specified threshold" :
                lr.wasError ? ("Number of loading errors was " + errors.size()) :
                        "Loading was successful";
        lr.details = sb.toString();

        Marshaller m = TBJAXBContext.createMarshaller();
        m.marshal(lr, response.getOutputStream());
    }

    @Override
    public void run() {
        try {
            process();
        } catch (IOException | JAXBException e) {
            try {
                HTTPProtocol.LOGGER.log(Level.WARNING, "Error while loading message: ", e);
                AbstractHandler.sendError(response, e);
            } catch (IOException e1) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            }
        }
    }
}
