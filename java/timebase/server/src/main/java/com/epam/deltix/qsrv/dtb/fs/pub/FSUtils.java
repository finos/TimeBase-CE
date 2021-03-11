package com.epam.deltix.qsrv.dtb.fs.pub;

import com.epam.deltix.qsrv.dtb.fs.hdfs.DistributedFS;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.util.collections.Visitor;
import com.epam.deltix.util.lang.Wrapper;
import org.apache.hadoop.fs.*;

import java.io.*;

/**
 *
 */
public class FSUtils {
    public static boolean           removeRecursive (
        AbstractPath                    path,
        boolean                         inclusive,
        Visitor <? super AbstractPath>  preDelete
    ) 
        throws IOException
    {        
        if (path.isFolder ()) {
            for (String name : path.listFolder ())
                if (!removeRecursive (path.append (name), true, preDelete))
                    return (false);
        }
        
        if (inclusive) {
            if (preDelete != null)
                if (!preDelete.visit (path))
                    return (false);
            
            path.deleteIfExists ();        
        }
        
        return (true);
    }

    public static FileSystem            getFileSystem(AbstractPath path) {
        if (path.getFileSystem() instanceof DistributedFS)
            return ((DistributedFS)path.getFileSystem()).delegate;

        return new LocalFileSystem();
    }

    @SuppressWarnings("unchecked")
    public static boolean               isDistributedFS(final AbstractFileSystem fs) {
        AbstractFileSystem unwrappedFS = fs;
        if (fs instanceof Wrapper)
            unwrappedFS = ((Wrapper<AbstractFileSystem>) fs).getNestedInstance();

        return !(unwrappedFS instanceof LocalFS);
    }

}
