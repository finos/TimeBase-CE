package com.epam.deltix.util.os;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class ServiceControl {
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

    public abstract void        load(String home);
    
    public String               queryStatusNameNoErrors (String id) {
        try {
            return (queryStatusName(id));
        } catch (Throwable x) {
            return ("ERROR: " + x.toString ());
        }
    }


    public abstract void        addDependency (String id, String dependId) throws IOException, InterruptedException;

    public abstract void        create(String id, String description, String binPath, CreationParameters params) throws IOException, InterruptedException;

    public abstract void        delete(String id) throws IOException, InterruptedException;

    public abstract boolean     exists(String id) throws IOException, InterruptedException;

    public abstract String      getExecutablePath(String id) throws InvocationTargetException, IllegalAccessException;

    public abstract String      queryStatusName(String id) throws IOException, InterruptedException;

    public abstract void        start(String id) throws IOException, InterruptedException;

    public abstract void        startAndWait(String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException;

    public abstract void        stop(String id) throws IOException, InterruptedException;

    public abstract void        stopAndWait (String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException;

    public abstract void        setFailureAction(String id, FailureAction action, int delay, String command) throws IOException, InterruptedException;
}
