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
package com.epam.deltix.qsrv.dtb.fs.pub;

import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.util.collections.Visitor;
import com.epam.deltix.util.lang.Wrapper;

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

//    public static FileSystem            getFileSystem(AbstractPath path) {
//        if (path.getFileSystem() instanceof DistributedFS)
//            return ((DistributedFS)path.getFileSystem()).delegate;
//
//        return new LocalFileSystem();
//    }

    @SuppressWarnings("unchecked")
    public static boolean               isDistributedFS(final AbstractFileSystem fs) {
        AbstractFileSystem unwrappedFS = fs;
        if (fs instanceof Wrapper)
            unwrappedFS = ((Wrapper<AbstractFileSystem>) fs).getNestedInstance();

        return !(unwrappedFS instanceof LocalFS);
    }

}