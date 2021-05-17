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

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.time.GMT;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

public abstract class LinuxServiceControl extends ServiceControl {
        
    protected static final int              DEFAULT_SS = 80;
    protected static final int              DEFAULT_KK = 20;
            
    public abstract String getServiceLauncherTemplatePath(boolean mono);

    @Override
    public void addDependency(String id, String dependId) throws IOException, InterruptedException {
        // Nothing to do, since linux uses 
        // DCTomcatCMD which resolves dependencied when start
    }
           
    @Override
    public String getExecutablePath(String id) throws InvocationTargetException, IllegalAccessException {
        final Path service = Paths.get("/etc/init.d/" + id);
        
        if (!Files.exists(service)) {
            return null;
        }
        
        if (!Files.isSymbolicLink(service)) {
            return service.toString();
        }            

        try {                            
            return service.toRealPath().toString();
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }
    }
    
    @Override
    public void create(String id, String description, String binPath, CreationParameters params) throws IOException, InterruptedException {
       
        final Path launcher = Paths.get(binPath);
        final Path config = launcher.getParent().resolve("config.txt");
        
        final StringBuilder launcherScript = new StringBuilder();
        
        try (final Reader cfgReader = new FileReader(config.toFile())) {

            String deps = (params.dependencies != null ? StringUtils.join(" ", params.dependencies) : "");
            if (deps.isEmpty()) {
                deps = "$remote_fs $syslog";
            }
            
            boolean dependent = params.dependencies != null && params.dependencies.length > 0;

            int ss = dependent ? DEFAULT_SS + 1 : DEFAULT_SS;

            int kk = dependent ? DEFAULT_KK - 1 : DEFAULT_KK;
            
            final Properties props = new Properties();
            props.put("process.name", id);
            props.put("process.description", description);
            props.put("process.description.long", description);
            props.put("required.start", deps);
            props.put("required.stop", deps);

            props.put("SS", Integer.toString(ss));
            props.put("KK", Integer.toString(kk));

            if (params.startMode == StartMode.auto) {
                props.put("level.start", "2 3 4 5");
                props.put("level.stop", "0 1 6");
                props.put("chkconfig.level.start", "2345");
            } else {
                props.put("level.start", " ");
                props.put("level.stop", " ");
                props.put("chkconfig.level.start", "-");                
            }

            IOUtil.replaceProperties(cfgReader, launcherScript, props);
        }

        IOUtil.writeTextFile(launcher.toFile(), launcherScript.toString());
        
        LinuxOS.commandNoError("chmod", "a+rwx", launcher.toString());

        final Path launcherScriptLink = Paths.get("/etc/init.d").resolve(launcher.getFileName());
        
        Files.deleteIfExists(launcherScriptLink);
        
        LinuxOS.commandNoError("ln", "-s", launcher.toString(), launcherScriptLink.toString());                        
    }
    
    @Override
    public void delete(String id) throws IOException, InterruptedException { 
        Files.deleteIfExists(Paths.get("/etc/init.d/" + id));
    }

    public void status(Appendable out, String id) throws IOException {
        LinuxOS.command(out, "service", id, "status");
    }
    
    @Override
    public String queryStatusName(String id) throws IOException, InterruptedException {
        final StringBuilder out = new StringBuilder();
        
        try {
            status(out, id);
            
            final Scanner s = new Scanner(new StringReader(out.toString()));

            while (s.hasNextLine()) {
                final String line = s.nextLine();
                if (line.startsWith(STATUS_PREFIX) && line.length() > STATUS_PREFIX.length()) {
                    return line.substring(STATUS_PREFIX.length()).trim();
                }
            }
        } catch (ExecutionException e) {
            LOG.trace(out.toString());
        }
        
        return STATUS_STOPPED;
    }    
    
    @Override
    public void start(String id) throws IOException, InterruptedException {
        LinuxOS.command("service", id, "start");
    }

    @Override
    public void startAndWait(String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException {
        long limit = System.currentTimeMillis() + timeout;

        for (;;) {
            String status =
                    ignoreQueryErrors ? queryStatusNameNoErrors(id) : queryStatusName(id);

            if (status.equals(STATUS_RUNNING)) {
                break;
            }

            if (status.equals(STATUS_STOPPED)) {
                start(id);
            }

            if (System.currentTimeMillis() >= limit) {
                throw new InterruptedException("Service failed to start after period of time (" + GMT.formatTime(limit) + ").");
            }

            Thread.sleep(500);
        }
    }

    @Override
    public void stop(String id) throws IOException, InterruptedException {
        LinuxOS.command("service", id, "stop");
    }

    @Override
    public void stopAndWait(String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException {
        long limit = System.currentTimeMillis() + timeout;

        for (;;) {
            String status =
                    ignoreQueryErrors ? queryStatusNameNoErrors(id) : queryStatusName(id);

            if (status.equals(STATUS_STOPPED)) {
                break;
            }

            if (status.equals(STATUS_RUNNING)) {
                stop(id);
            }

            if (System.currentTimeMillis() >= limit) {
                throw new InterruptedException();
            }

            Thread.sleep(500);
        }
    }  

    public void setFailureAction(String id, FailureAction action, int delay, String command) throws IOException, InterruptedException {
        //Not implemented yet
    }
}
