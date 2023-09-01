/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.client;

/**
 * Timebase Client Version.
 * @author Alex Karpovich on 4/4/2018.
 */
public class Version {

    // valid only when package exists only in single jar
    private static final String version = Version.class.getPackage().getImplementationVersion();

//    static {
//        try {
//            URL url = cl.findResource("META-INF/MANIFEST.MF");
//            Manifest manifest = new Manifest(url.openStream());
//            Attributes mainAttributes = manifest.getMainAttributes();
//            version = mainAttributes.getValue("Implementation-Version");
//        } catch (IOException E) {
//            // handle
//        }
//   }

    public static String    getVersion() {
        return version;
    }
}