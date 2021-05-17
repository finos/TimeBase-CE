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
package com.epam.deltix.util.io;

import java.io.File;
import java.nio.file.*;

/**
 * Helper class to obtain Quant Server installation root directory at run time. 
 * It can be defined as environment variable DELTIX_HOME or Java System property "deltix.home".
 */
public abstract class Home {
    private static final boolean  IS_WINDOWS_OS = System.getProperty ("path.separator").equals(";");

    public static final String              DELTIX_HOME_SYS_PROP =
        "deltix.home";
    
    /** Please use Home.get() to access DELTIX_HOME */
    private static final String              DELTIX_HOME_ENV =
        "DELTIX_HOME";

    public static void          set (String home) {
        System.setProperty(DELTIX_HOME_SYS_PROP, home);            
    }
    
    public static boolean       isSet () {
        return (
            System.getProperty (DELTIX_HOME_SYS_PROP) != null ||
            System.getenv (DELTIX_HOME_ENV) != null
        );
    }

    public static String        get () {
        String      home =
            System.getProperty (
                DELTIX_HOME_SYS_PROP, 
                null
            );        

        if (home == null) {
            home = System.getenv (DELTIX_HOME_ENV);
            if (home != null)
                System.setProperty(DELTIX_HOME_SYS_PROP, home);
            else
                throw new RuntimeException (
                    "Neither the " + DELTIX_HOME_ENV +
                        " environment, nor the " +
                        DELTIX_HOME_SYS_PROP + " system property is set"
                );
        }


        return (home);
    }
    
    public static File          getFile () {
        return (new File (get ()));
    }
    
    public static File          getFile (String subPath) {
        return (new File (get (), subPath));
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form
     * a path string, to a {@code Path}. If {@code more} does not specify any
     * elements then the value of the {@code first} parameter is the path string
     * to convert. If {@code more} specifies one or more elements then each
     * non-empty string, including {@code first}, is considered to be a sequence
     * of name elements (see {@link Path}) and is joined to form a path string.
     * The details as to how the Strings are joined is provider specific but
     * typically they will be joined using the {@link FileSystem#getSeparator
     * name-separator} as the separator. For example, if the name separator is
     * "{@code /}" and {@code getPath("/foo","bar","gus")} is invoked, then the
     * path string {@code "/foo/bar/gus"} is converted to a {@code Path}.
     * A {@code Path} representing an empty path is returned if {@code first}
     * is the empty string and {@code more} does not contain any non-empty
     * strings.
     *
     * <p> The {@code Path} is obtained by invoking the {@link FileSystem#getPath
     * getPath} method of the {@link FileSystems#getDefault default} {@link
     * FileSystem}.
     *
     * <p> Note that while this method is very convenient, using it will imply
     * an assumed reference to the default {@code FileSystem} and limit the
     * utility of the calling code. Hence it should not be used in library code
     * intended for flexible reuse. A more flexible alternative is to use an
     * existing {@code Path} instance as an anchor, such as:
     * <pre>
     *     Path dir = ...
     *     Path path = dir.resolve("file");
     * </pre>
     * @param   subPaths
     *          additional strings to be joined to form the path string
     *
     * @return  the resulting {@code File}
     *
     * @throws InvalidPathException
     *          if the path string cannot be converted to a {@code Path}
     *
     * @see FileSystem#getPath
     */
    public static File          getFile (String ... subPaths) {
        return (new File(Paths.get(get(), subPaths).toString()));
    }
    
    public static String        getPath (String subPath) {
        return (getFile (subPath).getPath ());
    }

    public static String        getPath (String  ... subPaths) {
        return Paths.get(get(), subPaths).toString();
    }

    public static Path          getNioPath() {
        String home = get();
        return Paths.get(home);
    }
 
    public static String        getCommandPath (String name) {
        String      cmd = getPath ("bin", name);

        if (IS_WINDOWS_OS)
            cmd += ".cmd";
        
        return (cmd);
    }
}
