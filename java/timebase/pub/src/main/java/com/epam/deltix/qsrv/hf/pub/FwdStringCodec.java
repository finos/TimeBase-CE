package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Created by IntelliJ IDEA.
 * User: BazylevD
 * Date: Apr 14, 2009
 * Time: 4:01:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class FwdStringCodec {
    private static final Log LOG = LogFactory.getLog(FwdStringCodec.class);

    public static final int NULL = -1;

    public static CharSequence read(MemoryDataInput in) {
        final int               offset = in.readInt ();

        if (offset == NULL)
            return (null);

        final int               savedPosition = in.getPosition ();

        in.seek (offset + savedPosition - 4);

        final CharSequence      result;
        try {
            result = in.readCharSequence();
        } catch (RuntimeException e) {
            LOG.error("Error reading (offset=%s savedPosition=%s): %s").with(offset).with(savedPosition).with(e);
            throw e;
        }

        in.seek (savedPosition);

        return (result);
    }

    public static String readString(MemoryDataInput in) {
        final CharSequence value = read(in);
        return value != null ? value.toString() : null;
    }

    public static void write(CharSequence value, MemoryDataOutput out) {
        if (value == null)
            out.writeInt (NULL);
        else {
            final int               endPos = out.getSize ();

            assert endPos - out.getPosition() >= 4 : "endPos=" + endPos + " pos=" + out.getPosition();
            out.writeInt (endPos - out.getPosition ());

            final int               savedPosition = out.getPosition ();

            out.seek (endPos);
            out.writeString (value);
            out.seek (savedPosition);
        }
    }
}
