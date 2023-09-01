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
package com.epam.deltix.util.os;

import com.epam.deltix.util.lang.Util;

import java.io.File;

public class ServiceControlFactory {
    private static final ServiceControl INSTANCE;

    static {
        if (Util.IS_WINDOWS_OS) {
            boolean isNative = !Boolean.getBoolean("service.control.native.disable");
            if (isNative)
                INSTANCE = WindowsNativeServiceControl.INSTANCE;
            else
                INSTANCE = WindowsServiceControl.INSTANCE;
        } else {
            if (new File("/usr/sbin/update-rc.d").exists() || new File("/usr/sbin/chkconfig").exists()) {
                INSTANCE = SystemdServiceControl.INSTANCE;
//                INSTANCE = DebianFamilyServiceControl.INSTANCE;
//            } else if (new File("/usr/sbin/chkconfig").exists()) {
//                INSTANCE = RedHatFamilyServiceControl.INSTANCE;
            } else {
                throw new IllegalStateException("Unsupported OS.");
            }
        }
    }

    public static ServiceControl getInstance() {
        return INSTANCE;
    }
}