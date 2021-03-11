package com.epam.deltix.qsrv.hf.tickdb.repair.test;

import java.util.Arrays;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.store.impl.PDSFactory;
import com.epam.deltix.qsrv.dtb.store.pub.DataWriter;
import com.epam.deltix.qsrv.dtb.store.pub.PersistentDataStore;
import com.epam.deltix.qsrv.dtb.store.pub.TSMessageProducer;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class Sick5DBCreator {

    public static final long BASE_TIME = 1356998400000000000L;


    private TSRoot root;
    private final PersistentDataStore cache = PDSFactory.create();

    public void prepare(String outPath, AbstractFileSystem fs, int maxFolderSize, int maxFileSize ) {
        cache.start();
        root = cache.createRoot(null, fs, outPath);
        root.setMaxFolderSize(maxFolderSize);
        root.setMaxFileSize(maxFileSize);
    }

    public void generateMessages(int numEntities,
                                 int msgSize,
                                 long numMsgs,
                                 long timestampBatchSize,
                                 String compression) {

        root.setCompression(compression);
        root.format();

        String[] symbols = new String[numEntities];
        int[] ids = new int[numEntities];

        for (int ii = 0; ii < numEntities; ii++) {
            symbols[ii] = "DLTX_" + ii;
            ids[ii] =
                    root.getSymbolRegistry().registerSymbol(
                            symbols[ii],
                            "data for " + symbols[ii]
                    );
        }
        //PDSFactory.allocate(200000);
        final byte[] body = new byte[msgSize];

        Arrays.fill(body, (byte) 0xFF);

        try (DataWriter writer = cache.createWriter()) {
            writer.associate(root);

            TSMessageProducer producer =
                    new TSMessageProducer() {
                        @Override
                        public void writeBody(MemoryDataOutput out) {
                            out.write(body);
                        }
                    };


            long t = BASE_TIME;

            writer.open(t, null);

            for (long count = 0; count < numMsgs; count++) {
                int entity = (int) (count % numEntities);
                long timestamp =
                        t + timestampBatchSize * (count / timestampBatchSize);

                writer.insertMessage(
                        ids[entity],
                        timestamp,
                        0,
                        producer
                );
            }
        }

        cache.waitUntilDataStored(0);
        root.close();

    }


}
