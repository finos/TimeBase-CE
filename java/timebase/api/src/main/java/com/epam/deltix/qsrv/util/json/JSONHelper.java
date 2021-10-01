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
package com.epam.deltix.qsrv.util.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/6/2019
 */
public class JSONHelper {

    public static void parseAndLoad(JsonArray jsonArray, DXTickStream stream) {
        JSONRawMessageParser parser = new JSONRawMessageParser(stream.getTypes(), "$type");
        try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement msg = jsonArray.get(i);
                try {
                    RawMessage raw = parser.parse((JsonObject) msg);
                    loader.send(raw);
                } catch (Exception e) {
                    throw new LoadingError("Message is invalid:" + msg.toString().replace("\"", "'"),e);
                }
            }
        }
    }

    public static void parseAndLoad(String jsonArray, DXTickStream stream) {
        JsonArray parsed = (JsonArray) new JsonParser().parse(jsonArray);
        parseAndLoad(parsed, stream);
    }

}