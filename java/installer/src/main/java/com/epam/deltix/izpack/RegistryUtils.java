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
package com.epam.deltix.izpack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.prefs.Preferences;

public class RegistryUtils {

    private static final int REG_SUCCESS = 0;
    private static final int REG_NOTFOUND = 2;
    private static final int KEY_READ = 0x20019;
    private static final int REG_ACCESSDENIED = 5;
    private static final int KEY_ALL_ACCESS = 0xf003f;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;
    private static final String LOCAL_MACHINE = "HKEY_LOCAL_MACHINE";
    private static Preferences userRoot = Preferences.userRoot();
    private static Preferences systemRoot = Preferences.systemRoot();
    private static Class<? extends Preferences> userClass = userRoot.getClass();
    private static Method regOpenKey = null;
    private static Method regCloseKey = null;
    private static Method regQueryValueEx = null;

    static {
        try {
            regOpenKey = findClass("WindowsRegOpenKey",
                new Class[] {int.class, byte[].class, int.class},
                new Class[] {long.class, byte[].class, int.class}
            );
            regOpenKey.setAccessible(true);

            regCloseKey = findClass("WindowsRegCloseKey", new Class[] {int.class}, new Class[] {long.class});
            regCloseKey.setAccessible(true);

            regQueryValueEx = findClass("WindowsRegQueryValueEx",
                new Class[] {int.class, byte[].class},
                new Class[] {long.class, byte[].class}
            );
            regQueryValueEx.setAccessible(true);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Method findClass(String name, Class[]... classes) throws Throwable {
        Throwable lastError = null;
        for (int i = 0 ; i < classes.length; ++i) {
            try {
                return userClass.getDeclaredMethod(name, classes[i]);
            } catch (NoSuchMethodException e) {
                lastError = e;
            }
        }

        if (lastError != null) {
            throw lastError;
        }

        throw new Throwable("Method not found: " + name);
    }

    public static String valueForKey(int hkey, String path, String key)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException
    {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return valueForKey(systemRoot, hkey, path, key);
        }

        return null;
    }

    // =====================

    private static String valueForKey(Preferences root, int hkey, String path, String key)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException
    {
        try {
            return valueForKeyInt(root, hkey, path, key);
        } catch (Throwable t) {
            return valueForKeyLong(root, hkey, path, key);
        }
    }

    private static String valueForKeyInt(Preferences root, int hkey, String path, String key)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException
    {
        int[] handles = (int[]) regOpenKey.invoke(root, new Object[] {new Integer(hkey), toCstr(path), new Integer(KEY_READ)});
        if (handles[1] != REG_SUCCESS)
            throw new IllegalArgumentException("The system can not find the specified path: '"+getParentKey(hkey)+"\\"+path+"'");
        byte[] valb = (byte[]) regQueryValueEx.invoke(root, new Object[] {new Integer(handles[0]), toCstr(key)});
        regCloseKey.invoke(root, new Object[] {new Integer(handles[0])});
        return (valb != null ? parseValue(valb) : queryValueForKey(hkey, path, key));
    }

    private static String valueForKeyLong(Preferences root, int hkey, String path, String key)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException
    {
        long[] handles = (long[]) regOpenKey.invoke(root, new Object[] {new Long(hkey), toCstr(path), new Integer(KEY_READ)});
        if (handles[1] != REG_SUCCESS)
            throw new IllegalArgumentException("The system can not find the specified path: '"+getParentKey(hkey)+"\\"+path+"'");
        byte[] valb = (byte[]) regQueryValueEx.invoke(root, new Object[] {new Long(handles[0]), toCstr(key)});
        regCloseKey.invoke(root, new Object[] {new Long(handles[0])});
        return (valb != null ? parseValue(valb) : queryValueForKey(hkey, path, key));
    }

    private static String queryValueForKey(int hkey, String path, String key) throws IOException {
        return queryValuesForPath(hkey, path).get(key);
    }

    /**
     * Makes cmd query for the given hkey and path then executes the query
     * @param hkey
     * @param path
     * @return the map containing all results in form of key(s) and value(s) obtained by executing query
     * @throws IOException
     */
    private static Map<String, String> queryValuesForPath(int hkey, String path) throws IOException {
        String line;
        StringBuilder builder = new StringBuilder();
        Map<String, String> map = new HashMap<String, String>();
        Process process = Runtime.getRuntime().exec("reg query \""+getParentKey(hkey)+"\\" + path + "\"");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while((line = reader.readLine()) != null) {
            if(!line.contains("REG_"))
                continue;
            StringTokenizer tokenizer = new StringTokenizer(line, " \t");
            while(tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if(token.startsWith("REG_"))
                    builder.append("\t ");
                else
                    builder.append(token).append(" ");
            }
            String[] arr = builder.toString().split("\t");
            map.put(arr[0].trim(), arr[1].trim());
            builder.setLength(0);
        }
        return map;
    }

    /**
     * Determines the string equivalent of hkey
     * @param hkey
     * @return string equivalent of hkey
     */
    private static String getParentKey(int hkey) {
        if(hkey == HKEY_LOCAL_MACHINE)
            return LOCAL_MACHINE;
        return null;
    }

    /**
     *Intern method which adds the trailing \0 for the handle with java.dll
     * @param str String
     * @return byte[]
     */
    private static byte[] toCstr(String str) {
        if(str == null)
            str = "";
        return (str += "\0").getBytes();
    }

    /**
     * Method removes the trailing \0 which is returned from the java.dll (just if the last sign is a \0)
     * @param buf the byte[] buffer which every read method returns
     * @return String a parsed string without the trailing \0
     */
    private static String parseValue(byte buf[]) {
        if(buf == null)
            return null;
        String ret = new String(buf);
        if(ret.charAt(ret.length()-1) == '\0')
            return ret.substring(0, ret.length()-1);
        return ret;
    }
}