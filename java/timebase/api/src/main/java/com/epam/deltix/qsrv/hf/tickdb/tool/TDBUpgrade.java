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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import java.io.IOException;
import java.io.File;

/**
 * User: karpovicha
 * Date: Oct 26, 2009
 * Time: 6:16:09 PM
 */
public abstract class TDBUpgrade {

    public static interface EventListener {
        public void             folderNotFound (File catalog, File folder);

        public void             upToDate (File f, String streamKey);

        public void             beginUpgrading (File f, String streamKey);

        public void             doneUpgrading (File f, String streamKey);
    }
    
    static String   setProperty (String text, String name, String value)
            throws IOException
    {
        String  open = "<" + name + ">";
        int     a = text.indexOf (open);
        String  close = "</" + name + ">";
        int     b = text.indexOf (close);

        if (b < 0)
            throw new IOException ("Failed to find '" + close + "'");

        return text.substring(0, a) +
                open + value + text.substring(b, text.length());
    }
    
    static String   getElement(String text, String name)
        throws IOException
    {
        String  open = "<" + name;
        int     a = text.indexOf (open);

        if (a < 0)
            return null;

        String  close = "</" + name + ">";
        int     b = text.indexOf (close);

        if (b < 0)
            throw new IOException ("Failed to find '" + close + "'");

        return (text.substring (a, b + close.length()));
    }

     static String   setElement(String text, String name, String value)
        throws IOException
    {
        String  open = "<" + name;
        int     a = text.indexOf (open);

        if (a < 0)
            return text;

        a = text.substring(a).indexOf(">");

        String  close = "</" + name + ">";
        int     b = text.indexOf (close);

        if (b < 0)
            throw new IOException ("Failed to find '" + close + "'");

        return text.substring(0, a) +
                value + text.substring(b, text.length());
    }

    static String   getProperty (String text, String name, String def)
        throws IOException
    {
        String  open = "<" + name + ">";
        int     a = text.indexOf (open);

        if (a < 0)
            return (def);

        String  close = "</" + name + ">";
        int     b = text.indexOf (close);

        if (b < 0)
            throw new IOException ("Failed to find '" + close + "'");

        return (text.substring (a + open.length (), b));
    }

    static int      getIntProperty (String codecText, String name, String def)
        throws NumberFormatException, IOException
    {
        return Integer.parseInt (getProperty (codecText, name, def));
    }
    
}