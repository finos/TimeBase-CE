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
package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.servlet.http.HttpServletResponse;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.GrammarUtil;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.LittleEndianDataOutputStream;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class DownloadStructHandler extends AbstractHandler implements Runnable {
    private final DXTickDB db;

    private final SelectAsStructRequest request;
    private final HttpServletResponse response;
    private final boolean isBigEndian;
    private final int bufferSize;

    private final MemoryDataOutput mdo;
    private DataOutputStream dos;
    private GZIPOutputStream gzip_os;

    public DownloadStructHandler(DXTickDB db, SelectAsStructRequest request, HttpServletResponse response) {
        this.db = db;
        this.request = request;
        this.isBigEndian = request.isBigEndian;
        this.response = response;
        this.bufferSize = request.bufferSize;
        // validate buffer size [1Kb..100Mb]
        if (bufferSize < 1024 || bufferSize > 100 * SelectAsStructRequest.SIZE_1MB)
            throw new ValidationException(String.format("Buffer size is out of range [1Kb..100Mb] %d", request.bufferSize));

        mdo = new MemoryDataOutput(bufferSize);
    }

    @Override
    public void run() {
        boolean wasIOException = false;
        try {
            process();
        } catch (Throwable t) {
            // have to send error_block
            if (response.isCommitted()) {
                try {
                    HTTPProtocol.LOGGER.log(Level.SEVERE, "selectAsStruct failed", t);
                    sendErrorBlock(t);
                } catch (IOException e) {
                    wasIOException = true;
                    HTTPProtocol.LOGGER.log(Level.SEVERE, "failed to send error block", e);
                }
            } else {
                // delegate error reporting to the servlet
                if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                else
                    throw new UncheckedException(t);
            }
        } finally {
            try {
                if (dos != null && !wasIOException)
                    dos.flush();
            } catch (IOException e) {
                HTTPProtocol.LOGGER.log(Level.SEVERE, "failed to flush output stream", e);
            }
        }
    }


    private void process() throws IOException {
        final DXTickStream stream = db.getStream(request.stream);
        if (stream == null)
            throw new UnknownStreamException(String.format("Stream \"%s\" does't exist", request.stream));

        final ArrayList<ColumnStruct> columns;
        final ArrayList<ColumnStruct> columnsInRequestOrder;
        final ObjectArrayList<String> concreteTypes;
        final int numRows2Fit;
        // put element type/size to a handy matrix
        final ColumnStruct[][] columnMarix;
        {
            final RecordClassSet rcs = stream.getStreamOptions().getMetaData();
            final RequestPreprocessor rp = new RequestPreprocessor(request.concreteTypes, request.types, request.symbolLength, rcs);
            columnMarix = rp.columnMarix;
            columns = rp.columns;
            columnsInRequestOrder = rp.columnsInRequestOrder;
            concreteTypes = rp.concreteTypes;
            numRows2Fit = rp.numRows2Fit;
        }
        if (columns.isEmpty())
            throw new ValidationException("asStruct query has no defined columns");

        final int num_of_columns = columns.size();

        try (final InstrumentMessageSource cursor = openCursor()) {

            if (request.instruments == null || request.instruments.length == 0)
                cursor.subscribeToAllEntities();
            else
                cursor.addEntities(request.instruments, 0, request.instruments.length);

            if (request.concreteTypes == null || request.concreteTypes.length == 0)
                cursor.subscribeToAllTypes();
            else
                cursor.addTypes(concreteTypes.toArray(new String[concreteTypes.size()]));

            cursor.reset(request.from);

            final UnboundDecoder[] decoders = new UnboundDecoder[concreteTypes.size()];
            // typeIndex (aka ordering of content types in a cursor is unpredictable, that is why I need it
            final ColumnStruct[][] adjustedColumnMarix = new ColumnStruct[concreteTypes.size()][];
            final MemoryDataInput mdi = new MemoryDataInput();
            // I need it to track columns, which values where set and which weren't
            final boolean[] setColumns = new boolean[num_of_columns];

            final ColumnStruct csSymbol = columns.get(0);
            final ColumnStruct csTimestamp = columns.get(1);
            final ColumnStruct csInstrumentType = columns.get(2);

            int rowNum = 0;
            while (cursor.next()) {
                final RawMessage raw = (RawMessage) cursor.getMessage();
                if (raw.getTimeStampMs() > request.to || raw.getTimeStampMs() < request.from)
                    break;

                final int typeIndex = cursor.getCurrentTypeIndex();
                UnboundDecoder currentDecoder = decoders[typeIndex];
                if (currentDecoder == null) {
                    currentDecoder = CodecFactory.COMPILED.createFixedUnboundDecoder(raw.type);
                    decoders[typeIndex] = currentDecoder;

                    adjustedColumnMarix[typeIndex] = columnMarix[concreteTypes.indexOf(cursor.getCurrentType().getName())];
                }
                final ColumnStruct[] columnArray = adjustedColumnMarix[typeIndex];

                // "system" columns always go first in that order
                // put symbol
                final int offsetSymbol = csSymbol.offset + csSymbol.size * rowNum;
                storeANSI(raw.getSymbol(), csSymbol.size, offsetSymbol);
                // put timestamp
                final int offsetTs = csTimestamp.offset + csTimestamp.size * rowNum;
                storeInt64(offsetTs, raw.getNanoTime());

                Arrays.fill(setColumns, false);
//                // put InstrumentType
//                final int offsetIT = csInstrumentType.offset + csInstrumentType.size * rowNum;
//                storeInt8(offsetIT, raw.getInstrumentType().ordinal());
//                setColumns[csInstrumentType.idx] = true;

                int colNum = 0;
                mdi.setBytes(raw.data, raw.offset, raw.length);
                currentDecoder.beginRead(mdi);
                while (currentDecoder.nextField()) {

                    final ColumnStruct columnStruct = columnArray[colNum];
                    if (columnStruct != null) {
                        final int offset = columnStruct.offset + rowNum * columnStruct.size;
                        // store value to the buffer
                        if (columnStruct.type == NativeTypes.STRING)
                            // TODO: Q1
                            storeANSI(currentDecoder.isNull() ? "" : currentDecoder.getString(), columnStruct.size, offset);
                        else
                            store(currentDecoder, columnStruct.type, offset);

                        setColumns[columnStruct.idx] = true;
                    }

                    colNum++;
                }

                // skip two first "system" columns
                for (int currIdx = 2; currIdx < num_of_columns; currIdx++) {
                    if (!setColumns[currIdx])
                        storeNull(columns.get(currIdx), rowNum);
                }

                if (++rowNum >= numRows2Fit) {
                    storeHeader(columnsInRequestOrder, (rowNum - 1));
                    flushBuffer();
                    rowNum = 0;
                }
            }
            if (rowNum > 0) {
                storeHeader(columnsInRequestOrder, (rowNum - 1));
                flushBuffer();
            }

            storeTerminatorBlock();

            if (gzip_os != null)
                gzip_os.finish();
        }
    }

    private InstrumentMessageSource openCursor() {
        final TickStream stream = db.getStream(request.stream);
        if (stream == null)
            throw new UnknownStreamException(String.format("stream \"%s\" does't exist", request.stream));

        final SelectionOptions so = new SelectionOptions(true, false);
        return stream.createCursor(so);
    }

    private void flushBuffer() throws IOException {
        createOrCheckOutputStream();

        dos.write(mdo.getBuffer(), 0, mdo.getSize());
        dos.flush();
        mdo.reset();
    }

    private void storeHeader(ArrayList<ColumnStruct> columns, int rowNum) {
        // message_block_size
        int offset = 0;
        storeInt32(offset, mdo.getSize());
        offset += 4;

        // num_of_columns
        storeInt8(offset, columns.size());
        offset++;

        final int header_size = columns.get(0).offset;
        for (ColumnStruct columnStruct : columns) {
            // column_offset
            storeInt32(offset, columnStruct.offset - header_size);
            offset += 4;
            // column_block_size
            storeInt32(offset, (rowNum + 1) * columnStruct.size);
            offset += 4;
        }

        // store number of rows
        storeInt32(offset, rowNum + 1);
    }

    private void storeTerminatorBlock() throws IOException {
        createOrCheckOutputStream();
        dos.writeInt(HTTPProtocol.TERMINATOR_RECORD);
    }

    private void createOrCheckOutputStream() throws IOException {
        // get the stream here to keep HttpServletResponse.sendError enabled as late as possible
        if (dos == null) {
            final OutputStream os;
            if (request.useCompression) {
                response.setHeader(HTTPProtocol.CONTENT_ENCODING, HTTPProtocol.GZIP);
                gzip_os = new GZIPOutputStream(response.getOutputStream(), 0x1000, true);
                os = gzip_os;
            } else {
                os = response.getOutputStream();
                gzip_os = null;
            }
            dos = new DataOutputStream(os);
        }
    }

    private void store(UnboundDecoder decoder, NativeTypes type, int offset) {
        mdo.seek(offset);
        switch (type) {
            case INT64:
                storeInt64(decoder.isNull() ? IntegerDataType.INT64_NULL : decoder.getLong());
                break;
            case INT32:
                storeInt32(decoder.isNull() ? IntegerDataType.INT32_NULL : decoder.getInt());
                break;
            case INT16:
                storeInt16(decoder.isNull() ? IntegerDataType.INT32_NULL : decoder.getInt());
                break;
            case INT8:
                storeInt8(decoder.isNull() ? IntegerDataType.INT32_NULL : decoder.getInt());
                break;
            case IEEE64:
                storeDouble(decoder.isNull() ? FloatDataType.IEEE64_NULL : decoder.getDouble());
                break;
            case IEEE32:
                storeFloat(decoder.isNull() ? FloatDataType.IEEE32_NULL : decoder.getFloat());
                break;
            case BOOL:
                mdo.writeByte(decoder.isNull() ? BooleanDataType.NULL : decoder.getBoolean() ? BooleanDataType.TRUE : BooleanDataType.FALSE);
                break;
            case CHAR:
                storeChar(decoder.isNull() ? CharDataType.NULL : decoder.getChar());
                break;
            case ENUM8:
                mdo.writeByte(decoder.isNull() ? EnumDataType.NULL : decoder.getLong());
                break;
            case ENUM16:
                storeInt16(decoder.isNull() ? EnumDataType.NULL : (short) decoder.getLong());
                break;
            case ENUM32:
                storeInt32(decoder.isNull() ? EnumDataType.NULL : (int) decoder.getLong());
                break;
            case ENUM64:
                storeInt64(decoder.isNull() ? EnumDataType.NULL : decoder.getLong());
                break;
            default:
                throw new IllegalStateException("unexpected native type " + type);
        }
    }

    private void storeChar(char value) {
        if (isBigEndian)
            mdo.writeChar(value);
        else
            mdo.writeCharInverted(value);
    }

    private void storeNull(ColumnStruct columnStruct, int rowNum) {
        final int offset = columnStruct.offset + rowNum * columnStruct.size;
        mdo.seek(offset);
        switch (columnStruct.type) {
            case INT64:
                storeInt64(IntegerDataType.INT64_NULL);
                break;
            case INT32:
                storeInt32(IntegerDataType.INT32_NULL);
                break;
            case INT16:
                storeInt16(IntegerDataType.INT32_NULL);
                break;
            case INT8:
                storeInt8(IntegerDataType.INT32_NULL);
                break;
            case IEEE64:
                storeDouble(FloatDataType.IEEE64_NULL);
                break;
            case IEEE32:
                storeFloat(FloatDataType.IEEE32_NULL);
                break;
            case BOOL:
                mdo.writeByte(BooleanDataType.NULL);
                break;
            case CHAR:
                mdo.writeChar(CharDataType.NULL);
                break;
            case ENUM8:
                mdo.writeByte(EnumDataType.NULL);
                break;
            case ENUM16:
                storeInt16(EnumDataType.NULL);
                break;
            case ENUM32:
                storeInt32(EnumDataType.NULL);
                break;
            case ENUM64:
                storeInt64(EnumDataType.NULL);
                break;
            case STRING:
                storeANSI(null, columnStruct.size);
                break;
            default:
                throw new IllegalStateException("unexpected native type " + columnStruct.type);
        }
    }

    private void storeInt64(int offset, long value) {
        mdo.seek(offset);
        storeInt64(value);
    }

    private void storeInt64(long value) {
        if (isBigEndian)
            mdo.writeLong(value);
        else
            mdo.writeLongInverted(value);
    }

    private void storeInt32(int offset, int value) {
        mdo.seek(offset);
        storeInt32(value);
    }

    private void storeInt32(int value) {
        if (isBigEndian)
            mdo.writeInt(value);
        else
            mdo.writeIntInverted(value);
    }

    private void storeInt16(int value) {
        if (isBigEndian)
            mdo.writeShort(value);
        else
            mdo.writeShortInverted((short) value);
    }

    private void storeInt8(int value) {
        mdo.writeByte(value);
    }


    private void storeInt8(int offset, int value) {
        mdo.seek(offset);
        mdo.writeByte(value);
    }

    private void storeDouble(double value) {
        if (isBigEndian)
            mdo.writeDouble(value);
        else
            mdo.writeDoubleInverted(value);
    }

    private void storeFloat(float value) {
        if (isBigEndian)
            mdo.writeFloat(value);
        else
            mdo.writeFloatInverted(value);
    }

    private void storeANSI(CharSequence value, int len, int offset) {
        mdo.seek(offset);
        storeANSI(value, len);
    }

    private void storeANSI(CharSequence value, int len) {
        // zero terminator byte is not required
        final int str_len = value == null ? 0 : value.length() < len ? value.length() : len;

        int i;
        for (i = 0; i < str_len; i++) {
            mdo.writeByte(value.charAt(i));
        }

        // fill the rest with zeros
        for (; i < len; i++) {
            mdo.writeByte(0);
        }
    }

    private void sendErrorBlock(Throwable t) throws IOException {
        final DataOutput dout = isBigEndian ? dos : new LittleEndianDataOutputStream(dos);
        dout.writeInt(HTTPProtocol.ERROR_BLOCK_ID_WIDE);
        dout.writeUTF(t.toString());
        if (gzip_os != null)
            gzip_os.finish();
    }

    private static class ColumnStruct {
        int offset;
        final NativeTypes type;
        final int size;
        int idx;

        private ColumnStruct(NativeTypes type) {
            this(type, 0);
        }

        private ColumnStruct(NativeTypes type, int size) {
            this.type = type;
            this.size = (size > 0) ? size : type.getSize();
        }
    }

    // creates array and matrix of ColumnStruct-s and concreteTypes
    private class RequestPreprocessor {
        private HashMap<RecordType, HashMap<String, ColumnStruct>> lookupMap = new HashMap<>();

        // output variables
        final ArrayList<ColumnStruct> columns = new ArrayList<>();
        final ArrayList<ColumnStruct> columnsInRequestOrder = new ArrayList<>();
        final ObjectArrayList<String> concreteTypes = new ObjectArrayList<>();
        final ColumnStruct[][] columnMarix;
        int numRows2Fit;

        private RequestPreprocessor(String[] concreteTypes, RecordType[] types, int symbolLength, RecordClassSet rcs) {
            columnMarix = createColumnStructs(concreteTypes, types, symbolLength, rcs);
            postProcess(types);
            // help GC
            lookupMap.clear();
        }

        // populate array and matrix of ColumnStruct-s and concreteTypes
        private ColumnStruct[][] createColumnStructs(String[] concreteTypes, RecordType[] types, int symbolLength, RecordClassSet rcs) {
            if (types == null || types.length == 0)
                throw new ValidationException("asStruct query has no specified types");

            if (concreteTypes == null || concreteTypes.length == 0)
                for (RecordClassDescriptor rcd : rcs.getContentClasses()) {
                    this.concreteTypes.add(rcd.getName());
                }
            else
                for (String concreteType : concreteTypes) {
                    this.concreteTypes.add(concreteType);
                }

            // put "system" columns first: symbol + timestamp  + instrumentType
            columns.add(new ColumnStruct(NativeTypes.STRING, symbolLength));
            columns.add(new ColumnStruct(NativeTypes.INT64));
            columns.add(new ColumnStruct(NativeTypes.ENUM8));

            // 1st pass: collect requested columns
            for (int i = 0; i < types.length; i++) {
                final RecordType type = types[i];
                final HashMap<String, ColumnStruct> map = new HashMap<>();
                lookupMap.put(type, map);

                final RecordClassDescriptor rcd = (RecordClassDescriptor) rcs.getClassDescriptor(type.name);
                if (rcd == null)
                    throw new ValidationException(String.format("asStruct query: \"%s\" type is not found in stream's schema", type.name));

                // if type has no specified columns, just ignore it
                if (type.columns != null) {
                    for (Column column : type.columns) {
                        // TODO: validate that requested column exists !!!
                        columns.add(createOrFindColumnStruct(type, rcd, column.name));
                    }
                }
            }

            // 2nd pass: create ColumnStruct[] for each selected concrete type
            final ArrayList<ColumnStruct[]> r = new ArrayList<>();
            for (int i = 0; i < this.concreteTypes.size(); i++) {
                final String concreteType = this.concreteTypes.get(i);
                final RecordClassDescriptor rcd = (RecordClassDescriptor) rcs.getClassDescriptor(concreteType);
                if (rcd == null)
                    throw new ValidationException(String.format("asStruct query: \"%s\" concreteType is not found in stream's schema", concreteType));

                // look up
                RecordType type = findRecordType(concreteType, types);
                final ArrayList<ColumnStruct> l = new ArrayList<>();
                RecordClassDescriptor currentRCD = rcd;
                do {
                    // lookup for the parent type
                    if (type == null || !currentRCD.getName().equals(type.name)) {
                        type = null;
                        for (RecordType t : types) {
                            if (currentRCD.getName().equals(t.name)) {
                                type = t;
                                break;
                            }
                        }
                    }

                    final DataField[] fields = currentRCD.getFields();
                    int pos = 0;
                    for (DataField dataField : fields) {
                        if (dataField instanceof NonStaticDataField) {
                            if (type == null)
                                l.add(pos++, null);
                            else {
                                final ColumnStruct cs = createOrFindColumnStruct(type, currentRCD, dataField.getName());
                                l.add(pos++, cs);
                            }
                        }
                    }
                }
                while ((currentRCD = currentRCD.getParent()) != null);

                r.add(l.toArray(new ColumnStruct[l.size()]));
            }

            return r.toArray(new ColumnStruct[r.size()][]);
        }

        private ColumnStruct createOrFindColumnStruct(RecordType type, RecordClassDescriptor rcd, String fieldName) {
            if (type.columns == null)
                return null;

            Column column = null;
            for (Column c : type.columns) {
                if (c.name.equals(fieldName)) {
                    column = c;
                    break;
                }
            }
            if (column == null)
                return null;

            final DataField df = rcd.getField(column.name);
            if (df == null)
                throw new ValidationException(String.format("asStruct query: column \"[%s]%s\" is not found in stream's schema", type.name, column.name));

            // native type ?
            NativeTypes nativeType = getNativeType(df.getType(), type, column);
            if (nativeType == null)
                throw new ValidationException(
                        column.size != 0 ?
                                String.format("asStruct query: Column of size %d is not supported for field \"[%s]%s\" type \"%s\"", column.size, type.name, column.name, GrammarUtil.describe(df.getType(), false)) :
                                (df.getType() instanceof VarcharDataType) ?
                                        String.format("asStruct query: column \"[%s]%s\" type \"%s\" missed obligatory size parameter", type.name, column.name, GrammarUtil.describe(df.getType(), false)) :
                                        String.format("asStruct query: column \"[%s]%s\" type \"%s\" is not supported yet", type.name, column.name, GrammarUtil.describe(df.getType(), false))
                );
            else {
                // lookup first
                HashMap<String, ColumnStruct> map = lookupMap.get(type);
                if (map != null) {
                    final ColumnStruct cs = map.get(fieldName);
                    if (cs != null)
                        return cs;
                } else {
                    map = new HashMap<>();
                    lookupMap.put(type, map);
                }

                // create if not found
                final ColumnStruct cs = new ColumnStruct(nativeType, column.size);
                map.put(fieldName, cs);
                return cs;
            }
        }

        private RecordType findRecordType(String typeName, RecordType[] types) {
            for (RecordType t : types) {
                if (t.name.equals(typeName))
                    return t;
            }

            return null;
        }

        private NativeTypes getNativeType(DataType dataType, RecordType type, Column column) {
            int size = column.size;

            if (dataType instanceof FloatDataType) {
                final int tbSize = ((FloatDataType) dataType).isFloat() ? 4 : 8;
                // validate size
                if (size != 0) {
                    if (size != 4 && size != 8)
                        throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                    if (size < tbSize)
                        throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": column size is lesser than in Timebase: %d<%d", type.name, column.name, GrammarUtil.describe(dataType, false), size, tbSize));
                }

                return ((size > 0) ? size : tbSize) == 4 ?
                        NativeTypes.IEEE32 : NativeTypes.IEEE64;
            } else if (dataType instanceof IntegerDataType) {
                final int tbSize = ((IntegerDataType) dataType).getNativeTypeSize();
                // validate size
                if (size != 0) {
                    if (size != 1 && size != 2 && size != 4 && size != 8)
                        throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                    if (size < tbSize)
                        throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": column size is lesser than in Timebase: %d<%d", type.name, column.name, GrammarUtil.describe(dataType, false), size, tbSize));
                }

                if (size == 0)
                    size = tbSize;

                switch (size) {
                    case 8:
                    case 6: // INT48
                        return NativeTypes.INT64;
                    case 4:
                        return NativeTypes.INT32;
                    case 2:
                        return NativeTypes.INT16;
                    case 1:
                        return NativeTypes.INT8;
                    default:
                        return null;
                }
            } else if (dataType instanceof BooleanDataType) {
                // validate size
                if (size != 0 && size != 1)
                    throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                else
                    return NativeTypes.BOOL;
            } else if (dataType instanceof CharDataType) {
                // validate size
                if (size != 0 && size != 2)
                    throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                else
                    return NativeTypes.CHAR;
            } else if (dataType instanceof DateTimeDataType) {
                // validate size
                if (size != 0 && size != 8)
                    throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                else
                    return NativeTypes.INT64;
            } else if (dataType instanceof EnumDataType) {
                final int tbSize = ((EnumDataType) dataType).descriptor.computeStorageSize();
                // validate size
                if (size != 0) {
                    if (size != 1 && size != 2 && size != 4 && size != 8)
                        throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                    if (size < tbSize)
                        throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": column size is lesser than in Timebase: %d<%d", type.name, column.name, GrammarUtil.describe(dataType, false), size, tbSize));
                }

                if (size == 0)
                    size = tbSize;

                switch (size) {
                    case 1:
                        return NativeTypes.ENUM8;
                    case 2:
                        return NativeTypes.ENUM16;
                    case 4:
                        return NativeTypes.ENUM32;
                    case 8:
                        return NativeTypes.ENUM64;
                    default:
                        return null;
                }
            } else if (dataType instanceof TimeOfDayDataType)
                // validate size
                if (size != 0 && size != 4 && size != 8)
                    throw new ValidationException(String.format("asStruct query: \"[%s]%s\" type \"%s\": Invalid size of the column %d", type.name, column.name, GrammarUtil.describe(dataType, false), size));
                else
                    return size == 8 ? NativeTypes.INT64 : NativeTypes.INT32;
            else if (dataType instanceof VarcharDataType)
                if (size > 0)
                    return NativeTypes.STRING;
                else if (((VarcharDataType) dataType).getEncodingType() == VarcharDataType.ALPHANUMERIC) {
                    // in case of ALPHANUMERIC(n) use n-value as default
                    column.size = ((VarcharDataType) dataType).getLength();
                    return NativeTypes.STRING;
                } else
                    return null;
            else
                return null;
        }

        private void postProcess(RecordType[] types) {
            // cumulative size of a single row
            int rowSize = 0;
            for (ColumnStruct column : columns)
                rowSize += column.size;

            final int num_of_columns = columns.size();
            // message_block_size num_of_columns (column_offset column_block_size)+ num_of_rows
            final int header_size = 4 + 1 + num_of_columns * (4 + 4) + 4;
            // number of rows fit in buffer
            numRows2Fit = (bufferSize - header_size) / rowSize;
            if (numRows2Fit == 0)
                throw new ValidationException(String.format("The specified buffer size too small %d", bufferSize));

            // set TB-indexes
            // calculate column block offsets
            int column_offset = header_size;
            for (int i = 0; i < num_of_columns; i++) {
                final ColumnStruct cs = columns.get(i);
                cs.idx = i;
                cs.offset = column_offset;
                column_offset += cs.size * numRows2Fit;
            }

            // request order
            // three system columns
            columnsInRequestOrder.add(columns.get(0));
            columnsInRequestOrder.add(columns.get(1));
            columnsInRequestOrder.add(columns.get(2));
            for (RecordType type : types) {
                if (type.columns != null) {
                    final HashMap<String, ColumnStruct> map = lookupMap.get(type);
                    if (map == null)
                        throw new IllegalArgumentException(String.format("incorrect type definition \"%s\"", type.name));

                    for (Column column : type.columns) {
                        columnsInRequestOrder.add(map.get(column.name));
                    }
                }
            }
        }
    }
}

// TODO:
// +1. Check sequential data chunks
// +1.1 investigate first-row issue
// +1.2 fix empty response case
// +2. Make buffer size configurable
// +3. Support polymorphic stream
// +3.1 Fix fields in parent type
// +3.2 Check null values in absent trailing fields
// +4. validate request without types
// +5. Add error block in asStruct response
// +Impl. version check
// +listStreams, listEntities, getTimeRange

// +6. Support all data types (unsupported data types: ArrayDataType, BinaryDataType, ClassDataType, QueryDataType)
// +7. Take into account column's size to derive NativeType
// +7.1 outright error on lesser/invalid size
// +7.2 support bigger size conversion
// +7.2.1 check enum with 2,4,8 native size
// +7.2.1.1 check enum64 fix and report csv-import issue
// +8. GZIP compression
// +9. UAC support (TB security reused)

// +10. Impl. periodical flush for Live cursor, check timeouts
// +11. QQL query
// +check multiple-stream select
// +stress test: >500 parallel requests
// +L1-Security bug:  ClassCastException: enum to RCD
// +Docs: xml-schema, BNF, summary/overview
// Revise flashing thread termination
// Memory profiling ?

// Q1: difference between empty and NULL string ?
