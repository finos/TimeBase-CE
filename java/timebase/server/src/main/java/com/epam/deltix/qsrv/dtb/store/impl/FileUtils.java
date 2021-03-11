package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import java.io.*;
import java.util.Properties;

/**
 *
 */
public class FileUtils {
    static int      readUByte (InputStream is) throws IOException {
        return (readByte (is) & 0xFF);
    }
    
    static int      readByte (InputStream is) throws IOException {
        int     b = is.read ();
        
        if (b < 0)
            throw new EOFException ();
        
        return (b);
    }

    public static Properties readProperties(AbstractPath path) throws IOException {
        Properties props = new Properties();
        try (InputStream is = BufferedStreamUtil.wrapWithBuffered(path.openInput(0))) {
            props.load(is);
        } catch (FileNotFoundException x) {
            // ignore
        }

        return props;
    }

    
}
