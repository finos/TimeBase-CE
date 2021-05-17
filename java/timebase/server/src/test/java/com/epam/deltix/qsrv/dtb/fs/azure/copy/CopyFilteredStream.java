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
package com.epam.deltix.qsrv.dtb.fs.azure.copy;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.os.CommonSysProps;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CopyFilteredStream {
    //private static final String sourceStream = "ticks";
    //private static final String destStream = "ticks100copy01";

    //public static final String HOME = "C:\\dev\\tb\\main";
    //public static final String TIMEBASE_LOCATION = "C:\\dev\\QHomes\\Russell_3000_Master\\timebase";
    public static final String SELECTED_SYMBOLS_PATH = "ticks_selected.txt";

    private static final boolean USE_TEST_SYMBOL = false;
    public static final String TEST_SYMBOL = "SP";

    private static PrintStream fileOut;
    private static final PrintStream systemOut = System.out;

    static {
        try {
            fileOut = new PrintStream(new FileOutputStream("out_log.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public CopyFilteredStream() throws FileNotFoundException {
    }

    private static void println(String x) {
        systemOut.println(x);
        fileOut.println(x);
        fileOut.flush();
    }


        //private static final String sourceStream = "cbt";

    //private static final int PORT = 8077;

    public static void main(String[] argv) throws Exception {
        String home = argv[0];
        String qsHome = argv[1];
        String sourceStreamName = argv[2];
        String destStreamName = argv[3];

        Home.set(home);
        CommonSysProps.mergeProps();

        String timebaseLocation = qsHome + "\\timebase";
        File folder = new File(timebaseLocation);

        QSHome.set(folder.getParent());


        DataCacheOptions cacheOptions = new DataCacheOptions(Integer.MAX_VALUE, DataCacheOptions.DEFAULT_CACHE_SIZE, 0);
        cacheOptions.shutdownTimeout = Long.MAX_VALUE;

        FSOptions options = cacheOptions.fs = new FSOptions();

        options.url = FSFactory.AZURE_PROTOCOL_ID + FSFactory.SCHEME_SEPARATOR;


        final DXTickDB db = new TickDBImpl(cacheOptions, folder);
        db.open(false);

        println("Getting stream...");
        final DXTickStream stream = db.getStream(sourceStreamName);
        println("Got stream");

        SelectionOptions selectionOptions = new SelectionOptions();
        TickCursor cursor = stream.createCursor(selectionOptions);
        println("Created cursor");


        List<String> selectedSymbols = getSelectedSymbols();

        //cursor.subscribeToAllTypes();
        //String symbol = "NKDH16";
        //cursor.subscribeToAllEntities();
        println("==========================");

        println("Selected symbols:");
        if (USE_TEST_SYMBOL) {
            cursor.addEntity(new ConstantIdentityKey(TEST_SYMBOL));
            println(TEST_SYMBOL);
        } else {
            for (String symbol : selectedSymbols) {
                cursor.addEntity(new ConstantIdentityKey(symbol));
                println(symbol);
            }
        }
        println("==========================");

        cursor.reset(0);

        // Create new stream
        DXTickStream oldStream = db.getStream(destStreamName);
        if (oldStream != null) {
            oldStream.delete();
        }
        
        StreamOptions streamOptions = stream.getStreamOptions();
        streamOptions.name = destStreamName;
        streamOptions.description = destStreamName;
        streamOptions.location = "azure:///8886594581/" + destStreamName;
        streamOptions.version = "5.0";
        DXTickStream destStream = db.createStream(destStreamName, streamOptions);
        TickLoader loader = destStream.createLoader();



        long count = 0;
        //Set<String> symbols = new HashSet<>();
        long startTime = System.currentTimeMillis();
        while (cursor.next()) {
            InstrumentMessage message = cursor.getMessage();
            loader.send(message);
            /*if (message.getInstrumentType() == InstrumentType.FUTURE) {
                symbols.add(message.getSymbol().toString());
            }*/
            count ++;
            if (count % 10000 == 0) {
                long currentTime = System.currentTimeMillis();
                println(currentTime + "  Count: " + count);

                println(currentTime +"  Avg speed: " + count * 1000 / (currentTime - startTime) + " msg/s");
            }
/*            if (count >= 10_000) {
                break;
            }*/
        }
        long endTime = System.currentTimeMillis();
        println(endTime + "  Final count: " + count);

        println(endTime + "  Avg speed: " + count * 1000 / (endTime - startTime) + " msg/s");
/*        println("==========================");
        for (String symbol : symbols) {
            println(symbol);
        }
        println("==========================");
        println("==========================");
        for (String symbol : symbols) {
            println("\"" + symbol + "\",");
        }
        println("Final count: " + count);*/

        //System.exit(0);
        loader.close();
        cursor.close();
        db.close();
        QuickExecutor.shutdownGlobalInstance();
        println(System.currentTimeMillis() + "  Done!");
    }

    private static List<String> getSelectedSymbols() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(SELECTED_SYMBOLS_PATH)));
        return Lists.newArrayList(Splitter.on("\n").trimResults().omitEmptyStrings().split(content));
    }
}
