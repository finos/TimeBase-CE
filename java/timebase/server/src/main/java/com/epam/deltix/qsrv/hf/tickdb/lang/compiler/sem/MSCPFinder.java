package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 *  Finds the most specific common parent of a number of classes.
 */
public class MSCPFinder {
    private RecordClassDescriptor   mscp = null;

    /**
     *  Create an uninitialized instance. Caller must still call
     *  {@link MSCPFinder#reset(RecordClassDescriptor)}.
     */
    public MSCPFinder () {
    }

    /**
     *  Create an instance and start with the specified class.
     */
    public MSCPFinder (RecordClassDescriptor rcd) {
        reset (rcd);
    }

    /**
     *  Create an instance and find the MSCP of the specified classes.
     */
    public MSCPFinder (RecordClassDescriptor ... rcds) {
        int         n = rcds.length;

        if (n == 0)
            return;

        reset (rcds [0]);

        for (int ii = 1; ii < n; ii++)
            restrict (rcds [ii]);
    }

    public static RecordClassDescriptor findMSCP (RecordClassDescriptor ... rcds) {
        return (new MSCPFinder (rcds).getMSCP ());
    }

    public void                     reset (RecordClassDescriptor rcd) {
        mscp = rcd;
    }

    public void                     restrict (RecordClassDescriptor rcd) {
        while (mscp != null && !mscp.isAssignableFrom (rcd))
            mscp = mscp.getParent ();
    }

    public RecordClassDescriptor    getMSCP () {
        return (mscp);
    }
}
