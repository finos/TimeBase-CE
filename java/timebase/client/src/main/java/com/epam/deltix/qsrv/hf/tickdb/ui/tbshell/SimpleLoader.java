package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.util.csvx.CSVXReader;
import com.epam.deltix.util.lang.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helper class for loading sample data into TimeBase
 */
public class SimpleLoader {

    //
    //  Minute bar stream
    //
    public static void          loadBars (
            String                      symbol,
            InputStream is,
            TickLoader loader
    )
            throws IOException
    {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        CSVXReader in =
                new CSVXReader (new InputStreamReader(is), ',', true, symbol);

        StringBuilder               sb = new StringBuilder ();
        SimpleBarMessage bar = new SimpleBarMessage();

        bar.setSymbol(symbol);

        int                         row = 0;

        while (in.nextLine ()) {
            sb.setLength (0);
            sb.append (in.getCell (0));
            sb.append (' ');
            sb.append (in.getCell (1));

            try {
                bar.setTimeStampMs(df.parse (sb.toString ()).getTime ());
            } catch (ParseException px) {
                throw new IOException (in.getDiagPrefixWithLineNumber (), px);
            }

            bar.open = (in.getDouble (2));
            bar.high = (in.getDouble (3));
            bar.low = (in.getDouble (4));
            bar.close = (in.getDouble (5));
            bar.volume = (in.getDouble (6));

            loader.send (bar);
            row++;
        }
    }

    public static void          loadBarsFromZip (ZipInputStream zis, TickLoader loader)
            throws IOException
    {
        for (;;) {
            ZipEntry zentry = zis.getNextEntry ();

            if (zentry == null)
                break;

            String          name = zentry.getName ();
            int             dot = name.indexOf ('.');

            if (dot > 0)
                name = name.substring (0, dot);

            if (!Util.QUIET)
                System.out.println ("    " + name + " ...");
            loadBars (name, zis, loader);
        }
    }

    public static void          loadBarsFromZipResource (String path, TickLoader loader)
            throws IOException
    {
        if (!Util.QUIET)
            System.out.println ("Loading " + path + " ...");

        ZipInputStream      zis =
                new ZipInputStream (SimpleLoader.class.getClassLoader().getResourceAsStream (path));

        try {
            loadBarsFromZip (zis, loader);
        } finally {
            Util.close (zis);
        }
    }
}
