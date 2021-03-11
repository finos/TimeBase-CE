package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

/**
 * Date: Sep 3, 2010
 * @author BazylevD
 */
public class SymbolStat {

    public static void main(String[] args) {

        if (args.length != 2)
            System.out.println("Usage:\n" +
                "runjava deltix.qsrv.hf.tickdb.tool.SymbolStat <url> <stream>\n" +
                "where url is Timebase url or local folder\n" +
                "stream - key of stream to process");

        final String url = args[0];
        final String key = args[1];

        TickDB tickDb = TickDBFactory.createFromUrl(url);
        try {
            tickDb.open(true);
            TickStream stream = tickDb.getStream(key);
            if (stream != null)
                new SymbolStat().collectStatistic(stream);
            else
                System.out.println("Stream not found: " + key);
        } finally {
            tickDb.close();
        }
    }

    private void collectStatistic(TickStream stream) {
        final ClassDescriptor[] cds = stream.isFixedType() ?
            new ClassDescriptor[]{stream.getFixedType()} :
            stream.getPolymorphicDescriptors();

        final IdentityKey[] ids = stream.listEntities();
        final long[][] stat = new long[ids.length][cds.length];
        int cnt = 0;
        final TickCursor cur = stream.createCursor(new SelectionOptions(true, false));

        try {
            for (int i = 0; i < ids.length; i++) {
                IdentityKey id = ids[i];
                long[] statOne = stat[i];
                cur.reset(Long.MIN_VALUE);
                cur.addEntity(id);

                while (cur.next()) {
                    statOne[cur.getCurrentTypeIndex()]++;
                    if (cnt++ % 100000 == 0)
                        System.out.print('.');
                }

                cur.removeEntity(id);
            }
        } finally {
            cur.close();
        }

        
        // print statistic
        long grandTotal = 0;
        for (int i = 0; i < ids.length; i++) {
            System.out.println(ids[i]);

            long[] statOne = stat[i];
            long oneTotal = 0;
            for (int j = 0; j < statOne.length; j++) {
                final long l = statOne[j];
                oneTotal += l;
                System.out.println(cds[j].getName() + ": " + l);
            }
            System.out.println("Total: " + oneTotal);
            grandTotal += oneTotal;
        }
        System.out.println("== Grand total: " + grandTotal);
    }
}
