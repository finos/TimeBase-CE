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
package com.epam.deltix.qsrv.util.json.parser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.util.json.DateFormatter;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.lang.Util;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.Collections;

import static java.lang.String.format;

public class JsonMessageSource implements MessageSource<RawMessage> {

    private final ObjectToObjectHashMap<String, RecordClassDescriptor> descriptors;
    private final RecordClassDescriptor[] rcds;
    private final Reader reader;
    private final RawMessage raw = new RawMessage();
    private final DateFormatter dateFormatter = new DateFormatter();
    private final ObjectToObjectHashMap<String, Object> map = new ObjectToObjectHashMap<>();
    private final JsonParser jsonParser;
    private final JsonPool jsonPool = new JsonPool();
    private final JsonWriter jsonWriter = new JsonWriter(jsonPool);

    private boolean finished = false;

    public JsonMessageSource(final RecordClassDescriptor[] descriptors,
                             final Reader reader) throws IOException {
        this.rcds = descriptors;
        this.descriptors = new ObjectToObjectHashMap<>(descriptors.length);
        for (RecordClassDescriptor rcd : descriptors) {
            this.descriptors.put(rcd.getName(), rcd);
        }
        this.reader = reader;
        final JsonFactory jsonFactory = new JsonFactory();
        this.jsonParser = jsonFactory.createParser(reader);
        this.jsonParser.setFeatureMask(this.jsonParser.getFeatureMask() | JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS.getMask());
    }

    @Override
    public RawMessage getMessage() {
        return raw;
    }

    @Override
    public boolean next() {
        try {
            JsonToken token = jsonParser.nextToken();
            if (token == null) {
                finished = true;
            } else {
                switch (token) {
                    case START_ARRAY: {
                        return next();
                    }
                    case END_ARRAY: {
                        finished = true;
                        break;
                    }
                    case START_OBJECT: {
                        map.clear();
                        raw.setSymbol("");
                        raw.setTimeStampMs(Long.MIN_VALUE);
                        readRootObject();
                        finished = false;
                        break;
                    }
                    default: {
                        finished = true;
                        throw new JsonParseException(jsonParser, format("Unexpected token %s while parsing.", token));
                    }
                }
            }
        } catch (IOException exc) {
            throw new UncheckedIOException(exc);
        } catch (NoSuchDescriptorInSchemaException | TypeNotSetException | ParseException exc) {
            throw new RuntimeException(exc);
        }
        return !finished;
    }

    private void readRootObject() throws IOException,
            NoSuchDescriptorInSchemaException,
            TypeNotSetException, ParseException {
        boolean isTypeSet = false;
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
                String fieldName = jsonParser.getText();
                switch (fieldName) {
                    case "type":
                        setType();
                        isTypeSet = true;
                        break;
                    case "symbol":
                        jsonParser.nextToken();
                        raw.setSymbol(jsonParser.getValueAsString());
                        break;
                    case "timestamp":
                        jsonParser.nextToken();
                        raw.setTimeStampMs(dateFormatter.fromDateString(jsonParser.getValueAsString()));
                        break;
                    default:
                        parseField(fieldName, map);
                }
            } else {
                throw new JsonParseException(jsonParser, "Expected FIELD_NAME.");
            }
        }
        if (isTypeSet) {
            jsonWriter.writeValues(raw, map);
        } else {
            throw new TypeNotSetException();
        }
    }

    private void setType() throws IOException, NoSuchDescriptorInSchemaException {
        JsonToken token = jsonParser.nextToken();
        if (token == JsonToken.VALUE_STRING) {
            String type = jsonParser.getValueAsString();
            final RecordClassDescriptor rcd = descriptors.get(type, null);
            if (rcd != null) {
                raw.type = rcd;
            } else {
                throw new NoSuchDescriptorInSchemaException(type, rcds);
            }
        } else {
            throw new JsonParseException(jsonParser, format("Expected string value for field 'type', but got %s.", token));
        }
    }

    private void parseField(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        switch (jsonParser.nextToken()) {
            case VALUE_FALSE:
            case VALUE_NULL:
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
            case VALUE_STRING:
            case VALUE_TRUE:
                parsePrimitive(fieldName, current);
                break;
            case START_ARRAY:
                parseArray(fieldName, current);
                break;
            case START_OBJECT:
                current.put(fieldName, parseObject());
                break;
            default:
                throw new JsonParseException(jsonParser, format("Expected value for field %s", fieldName));
        }
    }

    private void parsePrimitive(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        switch (jsonParser.getCurrentToken()) {
            case VALUE_TRUE:
            case VALUE_FALSE:
                current.put(fieldName, jsonParser.getValueAsBoolean());
                break;
            case VALUE_STRING:
                current.put(fieldName, parseVarchar());
                break;
            case VALUE_NUMBER_FLOAT:
                current.put(fieldName, jsonParser.getDoubleValue());
                break;
            case VALUE_NUMBER_INT:
                current.put(fieldName, jsonParser.getValueAsLong());
                break;
        }
    }

    private StringBuilderWriter parseVarchar() throws IOException {
        StringBuilderWriter writer = jsonPool.getStringBuilderWriter();
        jsonParser.getText(writer);
        return writer;
    }

    private ObjectToObjectHashMap<String, Object> parseObject() throws IOException {
        ObjectToObjectHashMap<String, Object> object = jsonPool.getMap();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
                String name = jsonParser.getText();
                parseField(name, object);
            } else {
                throw new JsonParseException(jsonParser, "Expected FIELD_NAME.");
            }
        }
        return object;
    }

    private void parseArray(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        switch (jsonParser.nextToken()) {
            case VALUE_NUMBER_INT:
                parseIntArray(fieldName, current);
                break;
            case VALUE_TRUE:
            case VALUE_FALSE:
                parseBooleanArray(fieldName, current);
                break;
            case VALUE_NUMBER_FLOAT:
                parseFloatArray(fieldName, current);
                break;
            case VALUE_STRING:
                parseVarcharArray(fieldName, current);
                break;
            case START_OBJECT:
                parseObjectArray(fieldName, current);
                break;
            case END_ARRAY:
                current.put(fieldName, Collections.EMPTY_LIST);
                break;
            default:
                throw new JsonParseException(jsonParser, "Unexpected value in array.");
        }
    }

    private void parseIntArray(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        LongArrayList list = jsonPool.getLongList();
        current.put(fieldName, list);
        list.add(jsonParser.getValueAsLong());
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                list.add(jsonParser.getValueAsLong());
            } else {
                throw new JsonParseException(jsonParser, "Expected int number in array.");
            }
        }
    }

    private void parseFloatArray(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        DoubleArrayList list = jsonPool.getDoubleList();
        current.put(fieldName, list);
        list.add(jsonParser.getValueAsDouble());
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT ||
                    jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                list.add(jsonParser.getValueAsDouble());
            } else {
                throw new JsonParseException(jsonParser, "Expected float number in array.");
            }
        }
    }

    private void parseBooleanArray(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        BooleanArrayList list = jsonPool.getBooleanList();
        current.put(fieldName, list);
        list.add(jsonParser.getValueAsBoolean());
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.getCurrentToken() == JsonToken.VALUE_TRUE ||
                    jsonParser.getCurrentToken() == JsonToken.VALUE_FALSE) {
                list.add(jsonParser.getValueAsBoolean());
            } else {
                throw new JsonParseException(jsonParser, "Expected boolean value in array.");
            }
        }
    }

    private void parseVarcharArray(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        ObjectArrayList<Object> list = jsonPool.getObjectList();
        current.put(fieldName, list);
        list.add(parseVarchar());
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.currentToken() == JsonToken.VALUE_STRING) {
                list.add(parseVarchar());
            } else {
                throw new JsonParseException(jsonParser, "Expected string value in array.");
            }
        }
    }

    private void parseObjectArray(String fieldName, ObjectToObjectHashMap<String, Object> current) throws IOException {
        ObjectArrayList<Object> list = jsonPool.getObjectList();
        current.put(fieldName, list);
        list.add(parseObject());
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.currentToken() == JsonToken.START_OBJECT) {
                list.add(parseObject());
            } else {
                throw new JsonParseException(jsonParser, "Expected object value in array.");
            }
        }
    }

    @Override
    public boolean isAtEnd() {
        return finished;
    }

    @Override
    public void close() {
        jsonPool.clear();
        Util.close(reader);
        Util.close(jsonParser);
    }
}
