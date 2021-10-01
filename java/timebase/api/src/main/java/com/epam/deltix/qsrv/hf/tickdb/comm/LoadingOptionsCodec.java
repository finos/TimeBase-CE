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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.qsrv.hf.pub.ChannelCompression;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.util.SerializationUtils;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;

import static com.epam.deltix.qsrv.hf.tickdb.comm.SelectionOptionsCodec.VERSION_WITH_FIXED_STREAM_TYPE_SUPPORT;

/**
  *
 */
public abstract class LoadingOptionsCodec {

    public static void write(DataOutputStream out, LoadingOptions options, int serverVersion) throws IOException {
        // do not forget to change protocol version
        
        out.writeBoolean(options.globalSorting);
        if (serverVersion <= VERSION_WITH_FIXED_STREAM_TYPE_SUPPORT) {
            out.writeUTF (options.channelPerformance.toString());
            out.writeUTF (options.compression.toString());
            out.writeUTF (options.writeMode.toString());
        } else {
            out.writeInt(options.channelPerformance.ordinal());
            out.writeInt(options.compression.ordinal());
            out.writeInt (options.writeMode.ordinal());
        }

        Class[] classes = options.getMappedClasses();
        out.writeInt(classes.length);
        for (Class clazz : classes) {
            out.writeUTF(clazz.getName());            
            out.writeUTF(options.getErrorAction(clazz).toString());
        }

        SerializationUtils.writeNullableString(options.space, out);
        SerializationUtils.writeNullableString(options.filterExpression, out);
    }

    public static void read(DataInputStream in, LoadingOptions options, int clientVersion) throws IOException {
        // do not forget to change protocol version

        options.globalSorting = in.readBoolean();

        if (clientVersion <= VERSION_WITH_FIXED_STREAM_TYPE_SUPPORT) {
            options.channelPerformance = Enum.valueOf(ChannelPerformance.class, in.readUTF());
            options.compression = Enum.valueOf(ChannelCompression.class, in.readUTF());
            options.writeMode = Enum.valueOf(LoadingOptions.WriteMode.class, in.readUTF());
        } else {
            options.channelPerformance = ChannelPerformance.values()[in.readInt()];
            options.compression = ChannelCompression.values()[in.readInt()];
            options.writeMode = LoadingOptions.WriteMode.values()[in.readInt()];
        }

        int size = in.readInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {                
                try {
                    Class<?> clazz = Class.forName(in.readUTF());
                    if (LoadingError.class.isAssignableFrom(clazz)) {
                        LoadingOptions.ErrorAction action = LoadingOptions.ErrorAction.valueOf(in.readUTF());
                        options.addErrorAction(clazz.asSubclass(LoadingError.class), action);
                    }
                } catch (ClassNotFoundException e) {
                    
                }

            }
        }

        if (clientVersion > VERSION_WITH_FIXED_STREAM_TYPE_SUPPORT) {
            options.space = SerializationUtils.readNullableString(in);
            options.filterExpression = SerializationUtils.readNullableString(in);
        }
    }
}