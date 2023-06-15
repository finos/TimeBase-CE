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
package com.epam.deltix.qsrv.hf.tickdb.util;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.collections.CharSequenceToIntegerMap;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectToIntegerHashMap;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 */
public class QueryUtils implements Closeable {

    interface DataFactory<T extends AbstractList> {
        T[] newArray(int count);
        T newValue();
    }

    /**
     * The structure holds dynamic allocated arrays with values and timestamps for separate attribute.
     * For internal use only.
     */
    private static class AttributeDataList<T extends AbstractList> {
        private T[] values;
        private LongArrayList[] timestamps;

        private AttributeDataList(int entitiesCount, int timestampsCount, DataFactory<T> factory) {
            values = factory.newArray(entitiesCount);
            for (int i = 0; i < entitiesCount; ++i)
                values[i] = factory.newValue();

            timestamps = new LongArrayList[timestampsCount];
            for (int i = 0; i < timestampsCount; ++i)
                timestamps[i] = new LongArrayList();
        }
    }

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String DEFAULT_TIME_ZONE = "UTC";

    private TickDB db;
    private SimpleDateFormat dateFormat;

    public QueryUtils(String timebaseUrl) {
        this(timebaseUrl, null, null);
    }

    public QueryUtils(String timebaseUrl, String dateTimeFormat) {
        this(timebaseUrl, null, null, dateTimeFormat);
    }

    public QueryUtils(String timebaseUrl, String user, String pass) {
        this(timebaseUrl, user, pass, DEFAULT_DATE_FORMAT);
    }

    public QueryUtils(String timebaseUrl, String user, String pass, String dateTimeFormat) {
        setDateTimeFormat(dateTimeFormat);

        db = TickDBFactory.createFromUrl(timebaseUrl, user, pass);
        db.open(true);
    }

    public synchronized void setDateTimeFormat(String dateTimeFormat) {
        setDateTimeFormat(dateTimeFormat, TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
    }

    public synchronized void setDateTimeFormat(String dateTimeFormat, TimeZone timeZone) {
        this.dateFormat = new SimpleDateFormat(dateTimeFormat);
        this.dateFormat.setTimeZone(timeZone);
    }

    // Query timebase stream data for each attribute
    // 'stream' - string
    // 'symbols' - array of strings
    // 'start' - start time as string
    // 'end' - end time as string
    // 'attributes' - list of fields to get values from
    // 'messageTypes' - list of message types to query
    public AttributeData[] query(Object streamKey, Object[] symbols,
                                 Object start, Object end,
                                 Object[] attributeObjects,
                                 Object[] messageTypes)
    {
        return query(streamKey, symbols, start, end, attributeObjects, messageTypes, true);
    }

    public AttributeData[] query(Object streamKey, Object[] symbols,
                                 Object start, Object end, Object[] attributeObjects, Object[] messageTypes,
                                 boolean skipNulls)
    {
        AttributeDataList[] attributeDataLists = query(streamKey, symbols, start, end, attributeObjects, messageTypes,
            new DataFactory<DoubleArrayList>() {
                @Override
                public DoubleArrayList[] newArray(int count) {
                    return new DoubleArrayList[count];
                }

                @Override
                public DoubleArrayList newValue() {
                    return new DoubleArrayList();
                }
            },
            skipNulls
        );

        return buildAttributeData(attributeDataLists);
    }

    public AttributeDataExt[] queryExt(Object streamKey, Object[] symbols,
                                       Object start, Object end,
                                       Object[] attributeObjects,
                                       Object[] messageTypes)
    {
        return queryExt(streamKey, symbols, start, end, attributeObjects, messageTypes, true);
    }

    public AttributeDataExt[] queryExt(Object streamKey, Object[] symbols,
                                       Object start, Object end, Object[] attributeObjects, Object[] messageTypes,
                                       boolean skipNulls)
    {
        AttributeDataList[] attributeDataLists = query(streamKey, symbols, start, end, attributeObjects, messageTypes,
            new DataFactory<ObjectArrayList>() {
                @Override
                public ObjectArrayList[] newArray(int count) {
                    return new ObjectArrayList[count];
                }

                @Override
                public ObjectArrayList newValue() {
                    return new ObjectArrayList();
                }
            },
            skipNulls
        );

        return buildAttributeDataExt(attributeDataLists);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractList> AttributeDataList[] query(Object streamKey, Object[] symbols,
                                                               Object start, Object end, Object[] attributeObjects, Object[] messageTypes,
                                                               DataFactory<T> dataFactory, boolean skipNulls)
    {
        checkInput(streamKey, symbols, start, end, attributeObjects, messageTypes);

        TickStream stream = db.getStream((String) streamKey);
        if (stream == null)
            throw new IllegalStateException("Stream '" + streamKey + "' not found.");

        String[] attributes = (String[]) attributeObjects;
        IdentityKey[] entities = getEntities(stream, (String[]) symbols);
        CharSequenceToIntegerMap symbolsToIndex = buildEntitiesSearchMap(entities);
        AttributeDataList[] attributeDataList = buildAttributeDataLists(attributes.length, entities.length, dataFactory, skipNulls);
        RawMessageHelper helper = new RawMessageHelper();

        try (Reader reader = createReaderImpl(
                    stream, entities,
                    getTime((String) start, Long.MIN_VALUE), getTime((String) end, Long.MAX_VALUE),
            (String[]) messageTypes))
        {
            while (reader.next()) {
                RawMessage message = (RawMessage) reader.getMessage();

                int index = symbolsToIndex.get(message.getSymbol(), -1);
                if (index < 0)
                    continue;

                for (int attributeNum = 0; attributeNum < attributes.length; ++attributeNum) {
                    String attribute = attributes[attributeNum];
                    if (skipNulls) {
                        attributeDataList[attributeNum].values[index].add(helper.getValue(message, attribute, Double.NaN));
                        attributeDataList[attributeNum].timestamps[index].add(message.getTimeStampMs());
                    } else {
                        attributeDataList[attributeNum].timestamps[0].add(message.getTimeStampMs());
                        for (int entityNum = 0; entityNum < entities.length; ++entityNum) {
                            if (index == entityNum)
                                attributeDataList[attributeNum].values[index].add(helper.getValue(message, attribute, Double.NaN));
                            else
                                //fill with default values
                                attributeDataList[attributeNum].values[entityNum].add(Double.NaN);
                        }
                    }

                }
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return attributeDataList;
    }

    // Creates a stream reader
    public Reader createReader(Object streamKey, Object[] symbols, Object start, Object end, Object[] messageTypes) {
        checkInput(streamKey, symbols, start, end, null, messageTypes);

        TickStream stream = db.getStream((String) streamKey);
        if (stream == null)
            throw new IllegalStateException("Stream '" + streamKey + "' not found.");

        try {
            return createReaderImpl(
                stream,
                getEntities(stream, (String[]) symbols),
                getTime((String) start, Long.MIN_VALUE), getTime((String) end, Long.MAX_VALUE),
                (String[]) messageTypes
            );
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private Reader createReaderImpl(TickStream stream, IdentityKey[] entities, long startTime, long endTime, String[] messageTypes) {
        return new ReaderImpl(
            stream.select(startTime, new SelectionOptions(true, false), messageTypes, entities),
            endTime
        );
    }

    private IdentityKey[] getEntities(final TickStream stream, final String[] symbols) {
        IdentityKey[] allEntities = stream.listEntities();
        ObjectToIntegerHashMap<String> allEntitiesMap = new ObjectToIntegerHashMap<>();
        for (int i = 0; i < allEntities.length; ++i)
            allEntitiesMap.put(allEntities[i].getSymbol().toString(), i);

        IdentityKey[] entities = new IdentityKey[symbols.length];
        for (int i = 0; i < symbols.length; ++i) {
            int index = allEntitiesMap.get(symbols[i], -1);
            if (index < 0)
                throw new IllegalArgumentException("Can't find symbol '" + symbols[i] + "' in stream.");

            entities[i] = allEntities[index];
        }

        return entities;
    }

    private CharSequenceToIntegerMap buildEntitiesSearchMap(final IdentityKey[] entities) {
        CharSequenceToIntegerMap result = new CharSequenceToIntegerMap();
        for (int i = 0; i < entities.length; ++i)
            result.put(entities[i].getSymbol(), i);

        return result;
    }

    private <T extends AbstractList> AttributeDataList[] buildAttributeDataLists(
        int attributesCount, int entitiesCount, DataFactory<T> dataFactory, boolean skipNulls)
    {
        AttributeDataList[] attributeDataList = new AttributeDataList[attributesCount];
        for (int i = 0; i < attributeDataList.length; ++i)
            attributeDataList[i] = new AttributeDataList<>(entitiesCount, skipNulls ? entitiesCount : 1, dataFactory);

        return attributeDataList;
    }

    private AttributeData[] buildAttributeData(final AttributeDataList[] attributeDataLists) {
        AttributeData[] attributeData = new AttributeData[attributeDataLists.length];
        for (int attributeNum = 0; attributeNum < attributeDataLists.length; ++attributeNum) {
            attributeData[attributeNum] = new AttributeData();

            attributeData[attributeNum].values = new double[attributeDataLists[attributeNum].values.length][];
            for (int entityNum = 0; entityNum < attributeDataLists[attributeNum].values.length; ++entityNum) {
                DoubleArrayList list = (DoubleArrayList) attributeDataLists[attributeNum].values[entityNum];
                attributeData[attributeNum].values[entityNum] = list.toDoubleArray();
            }

            attributeData[attributeNum].timestamps = new long[attributeDataLists[attributeNum].timestamps.length][];
            for (int entityNum = 0; entityNum < attributeDataLists[attributeNum].timestamps.length; ++entityNum)
                attributeData[attributeNum].timestamps[entityNum] = attributeDataLists[attributeNum].timestamps[entityNum].toLongArray();
        }

        return attributeData;
    }

    private AttributeDataExt[] buildAttributeDataExt(final AttributeDataList[] attributeDataLists) {
        AttributeDataExt[] attributeData = new AttributeDataExt[attributeDataLists.length];
        for (int attributeNum = 0; attributeNum < attributeDataLists.length; ++attributeNum) {
            attributeData[attributeNum] = new AttributeDataExt();

            attributeData[attributeNum].values = new Object[attributeDataLists[attributeNum].values.length][];
            for (int entityNum = 0; entityNum < attributeDataLists[attributeNum].values.length; ++entityNum) {
                ObjectArrayList list = (ObjectArrayList) attributeDataLists[attributeNum].values[entityNum];
                attributeData[attributeNum].values[entityNum] = list.toObjectArray();
            }

            attributeData[attributeNum].timestamps = new long[attributeDataLists[attributeNum].timestamps.length][];
            for (int entityNum = 0; entityNum < attributeDataLists[attributeNum].timestamps.length; ++entityNum)
                attributeData[attributeNum].timestamps[entityNum] = attributeDataLists[attributeNum].timestamps[entityNum].toLongArray();
        }

        return attributeData;
    }

    private synchronized long getTime(String time, long defaultTime) throws ParseException {
        if (time == null)
            return defaultTime;

        Date date = dateFormat.parse(time);
        return date.getTime();
    }

    private void checkInput(Object streamKey, Object[] symbols, Object start, Object end, Object[] attributes, Object[] messageTypes) {
        if (!(streamKey instanceof String))
            throw new IllegalArgumentException("Stream key must be a string.");

        if (start != null && !(start instanceof String)) // can be null
            throw new IllegalArgumentException("Start value must be a string.");

        if (end != null && !(end instanceof String)) // can be null
            throw new IllegalArgumentException("End value must be an integer.");

        if (!(symbols instanceof String[]))
            throw new IllegalArgumentException("Symbols must be an array of strings.");

        if (!(attributes instanceof String[]))
            throw new IllegalArgumentException("Attributes types must be an array of strings.");

        if (messageTypes != null && !(messageTypes instanceof String[])) // can be null
            throw new IllegalArgumentException("Message types must be an array of strings.");
    }

    @Override
    public void close() throws IOException {
        if (db != null)
            db.close();
    }

}