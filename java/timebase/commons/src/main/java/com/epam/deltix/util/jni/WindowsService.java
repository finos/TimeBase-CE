package com.epam.deltix.util.jni;

import com.epam.deltix.util.io.Home;

import java.io.*;

/**
 *
 */
public abstract class WindowsService {
    static {
        System.load(Home.getFile("bin").getAbsolutePath() + "/winsvc" + System.getProperty("os.arch") + ".dll");
    }

    public static final int SERVICE_CONTROL_STOP                   = 0x00000001;
    public static final int SERVICE_CONTROL_PAUSE                  = 0x00000002;
    public static final int SERVICE_CONTROL_CONTINUE               = 0x00000003;
    public static final int SERVICE_CONTROL_INTERROGATE            = 0x00000004;
    public static final int SERVICE_CONTROL_SHUTDOWN               = 0x00000005;
    public static final int SERVICE_CONTROL_PARAMCHANGE            = 0x00000006;
    public static final int SERVICE_CONTROL_NETBINDADD             = 0x00000007;
    public static final int SERVICE_CONTROL_NETBINDREMOVE          = 0x00000008;
    public static final int SERVICE_CONTROL_NETBINDENABLE          = 0x00000009;
    public static final int SERVICE_CONTROL_NETBINDDISABLE         = 0x0000000A;
    public static final int SERVICE_CONTROL_DEVICEEVENT            = 0x0000000B;
    public static final int SERVICE_CONTROL_HARDWAREPROFILECHANGE  = 0x0000000C;
    public static final int SERVICE_CONTROL_POWEREVENT             = 0x0000000D;
    public static final int SERVICE_CONTROL_SESSIONCHANGE          = 0x0000000E;
            
    public static final int SERVICE_STOPPED                        = 0x00000001;
    public static final int SERVICE_START_PENDING                  = 0x00000002;
    public static final int SERVICE_STOP_PENDING                   = 0x00000003;
    public static final int SERVICE_RUNNING                        = 0x00000004;
    public static final int SERVICE_CONTINUE_PENDING               = 0x00000005;
    public static final int SERVICE_PAUSE_PENDING                  = 0x00000006;
    public static final int SERVICE_PAUSED                         = 0x00000007;
    
    public static final int SERVICE_ACCEPT_STOP                    = 0x00000001;
    public static final int SERVICE_ACCEPT_PAUSE_CONTINUE          = 0x00000002;
    public static final int SERVICE_ACCEPT_SHUTDOWN                = 0x00000004;
    public static final int SERVICE_ACCEPT_PARAMCHANGE             = 0x00000008;
    public static final int SERVICE_ACCEPT_NETBINDCHANGE           = 0x00000010;
    public static final int SERVICE_ACCEPT_HARDWAREPROFILECHANGE   = 0x00000020;
    public static final int SERVICE_ACCEPT_POWEREVENT              = 0x00000040;
    public static final int SERVICE_ACCEPT_SESSIONCHANGE           = 0x00000080;
    
    public static native String         getExecutablePath ();
    
    public native void                  setWaitHint (int millis);
        
    public native void                  setErrorCode (int error);
        
    public native void                  setAcceptCtrlMask (int mask);
    
    public native void                  setStatus (int status);
    
    public native void                  reportStatus ();
        
    protected abstract void             runService ();
    
    public abstract void                control (int command);
    
    public native void                  run (String svcName);

    public static File                  getExecutableFile () {
        return (new File (getExecutablePath ()));
    }

    public static File                  getExecutableDir () {
        return (getExecutableFile ().getParentFile ());
    }
}
