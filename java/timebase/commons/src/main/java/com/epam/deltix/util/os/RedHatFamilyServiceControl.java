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
package com.epam.deltix.util.os;

import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

/**
 * For Red Hat, Cent OS, Fedora, etc.
 */
public class RedHatFamilyServiceControl extends LinuxServiceControl {

    public static final RedHatFamilyServiceControl INSTANCE = new RedHatFamilyServiceControl();
    
    private RedHatFamilyServiceControl() {
    }        

    @Override
    public String getServiceLauncherTemplatePath(boolean mono) {
        return "com/epam/deltix/util/os/redhat-linux-service-launcher";
    }  
        
    @Override
    public void create(String id, String description, String binPath, CreationParameters params) throws IOException, InterruptedException {
        super.create(id, description, binPath, params);
        
        LinuxOS.commandNoError("chkconfig", "--add", id);
        
        if (params.startMode == StartMode.auto) {
            LinuxOS.commandNoError("chkconfig", id, "on");
        } else {
            LinuxOS.commandNoError("chkconfig", id, "off"); // sometimes chkconfig may ignore script header
        }
    }

    @Override
    public void delete(String id) throws IOException, InterruptedException {                                
        super.delete(id);
        
        LinuxOS.commandNoError("chkconfig", id, "off");
        LinuxOS.commandNoError("chkconfig", "--del", id);        
    }  

    @Override
    public boolean exists(String id) throws IOException, InterruptedException {

        final StringBuilder out = LinuxOS.command(new StringBuilder(), "chkconfig", "--list");
        final Scanner s = new Scanner(new StringReader(out.toString()));
        
        while (s.hasNextLine()) {
            final String line = s.nextLine();
            if (line.startsWith(id)) {                
                return true;
            }
        }        
        
        return false;
    }    
    
}
