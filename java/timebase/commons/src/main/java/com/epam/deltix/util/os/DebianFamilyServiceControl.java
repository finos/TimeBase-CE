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

import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;


public class DebianFamilyServiceControl extends LinuxServiceControl {

    public static final DebianFamilyServiceControl INSTANCE = new DebianFamilyServiceControl();
    
    private DebianFamilyServiceControl() {
    }

    @Override
    public String getServiceLauncherTemplatePath(boolean mono) {
        if (mono)
            return "com/epam/deltix/util/os/debian-mono-service-launcher";
        else
            return "com/epam/deltix/util/os/debian-linux-service-launcher";
    }  
        
    @Override
    public void create(String id, String description, String binPath, CreationParameters params) throws IOException, InterruptedException {
        super.create(id, description, binPath, params);
                
        if (params.startMode == StartMode.auto) {
            boolean dependent = params.dependencies != null && params.dependencies.length > 0;

            int ss = dependent ? DEFAULT_SS + 1 : DEFAULT_SS;

            int kk = dependent ? DEFAULT_KK - 1 : DEFAULT_KK;
                        
            LinuxOS.commandNoError("update-rc.d", "-f", id, "defaults", Integer.toString(ss), Integer.toString(kk));                            
        }

        LinuxOS.commandNoError("systemctl", "daemon-reload");
    }

    @Override
    public void delete(String id) throws IOException, InterruptedException {                                
        super.delete(id);
        
        LinuxOS.commandNoError("update-rc.d", "-f", id, "remove");
    }    
    
    @Override
    public boolean exists(String id) throws IOException, InterruptedException {

        final StringBuilder out = LinuxOS.command(new StringBuilder(), "service", "--status-all");
        final Scanner s = new Scanner(new StringReader(out.toString()));
        
        while (s.hasNextLine()) {
            final String line = s.nextLine();
            if (line.endsWith(id)) {                
                return true;
            }
        }        
        
        return false;
    }

    @Override
    public void status(Appendable out, String id) throws IOException {
        LinuxOS.command(out, "/etc/init.d/" + id, "status");
    }
}