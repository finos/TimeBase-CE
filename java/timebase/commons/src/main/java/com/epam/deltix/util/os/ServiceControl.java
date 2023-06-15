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

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class ServiceControl {
    protected static final Log LOG = LogFactory.getLog(ServiceControl.class);
    public static final String          STATUS_PREFIX = "STATUS:";

    public static final String  STATUS_RUNNING       = "RUNNING";
    public static final String  STATUS_STARTED       = "STARTED";
    public static final String  STATUS_START_PENDING = "START PENDING";
    public static final String  STATUS_STOPPED       = "STOPPED";
    public static final String  STATUS_PAUSED        = "PAUSED";
    
    public enum Type {
        own,
        share,
        interact,
        kernel,
        filesys,
        rec
    };

    public enum StartMode {
        auto,
        demand
    };

    public enum ErrorMode {
        normal,
        severe,
        critical,
        ignore
    };

    public enum FailureAction {
        reboot,
        restart,
        external
    }
    
    public static class CreationParameters {
        public String                   displayName;
        public Type                     type;
        public StartMode                startMode;
        public ErrorMode                errorMode;
        public String                   group;
        public String                   obj;
        public String                   password;
        public String []                dependencies;
        public String                   servicePath;
    };
    
    public static class Status {
        public int                     state;
        public String                  stateName;
    };

    public static class ServiceNotFoundException extends IOException {
        public ServiceNotFoundException (String message) {
            super (message);
        }
    }
    
    public String               queryStatusNameNoErrors (String id) {
        try {
            return (queryStatusName(id));
        } catch (Throwable x) {
            LOG.warn("sc query failed - ignoring: %s").with(x);
            return ("ERROR: " + x.toString ());
        }
    }

    public abstract void        addDependency (String id, String dependId) throws IOException, InterruptedException;
    
    public abstract void        create(String id, String description, String binPath, CreationParameters params) throws IOException, InterruptedException;
        
    public abstract void        delete(String id) throws IOException, InterruptedException;

    public abstract boolean     exists(String id ) throws IOException, InterruptedException;
    
    public abstract String      getExecutablePath(String id) throws InvocationTargetException, IllegalAccessException;
    
    public abstract String      queryStatusName(String id) throws IOException, InterruptedException;
    
    public abstract void        start(String id) throws IOException, InterruptedException;
    
    public abstract void        startAndWait(String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException;
    
    public abstract void        stop(String id) throws IOException, InterruptedException;
    
    public abstract void        stopAndWait (String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException;

    public abstract void        setFailureAction(String id, FailureAction action, int delay, String command) throws IOException, InterruptedException;
}