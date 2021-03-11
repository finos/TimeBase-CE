package com.epam.deltix.test.qsrv.hf.tickdb.testframework;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.testframework.TestMessageGenerator;
import com.epam.deltix.util.cmdline.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.*;
import java.io.*;
import java.util.*;
import java.util.Timer;

/**
 *  Starts TB, loads data, then kills TB and checks DB for corruption.
 */
public class DeathTest extends DefaultApplication {

    public enum Shutdown {
        Clean,
        Graceful,
        Abnormal,
        All
    }

    public static  Shutdown fromCode(String code) {
        switch (code) {
            case "c":
                return Shutdown.Clean;
            case "g":
                return Shutdown.Graceful;
            case "a":
                return Shutdown.Abnormal;
        }

        throw new IllegalArgumentException("Cannot parse shutdown code: " + code);
    }

    private static final String PREFIX = "####### >> ";
    
//    class DiagLnrImpl implements DiagListener {
//        private Shutdown shutdown;
//
//        DiagLnrImpl(Shutdown mode) {
//            this.shutdown = mode;
//        }
//
//        @Override
//        public void         onDiag (DiagBase diag) {
//            switch (diag.getLevel ()) {
//                case INFO:
//                    break;
//
//                case MAINTENANCE:
//                    if (shutdown != Shutdown.Abnormal) {
//                        log ("SCAN: Unexpected discrepancy after a graceful shutdown: ", diag);
//                        System.exit (1);
//                    }
//                    break;
//
//                default:
//                    log ("SCAN: Unexpected error: ", diag);
//                    System.exit (1);
//                    break;
//            }
//        }
//    }
    
    private File                workFolder;
    private File                tbFolder;
    private int                 runsWithoutWipeout;
    private int                 totalRuns;
    private long                aggTimeSec;
    private Shutdown            mode = Shutdown.All;
    private String []           command;
    private volatile Process    p;
    private OutputStream        pin;
    private final Timer         killer = new Timer ("TB Killer", true);
    
    private DeathTest (String [] args) throws Exception {
        super (args);
    }
    
    public static void          main (String [] args) throws Exception {
        new DeathTest (args).start ();
    }
    
    private static void         log (Object ... args) {
        synchronized (System.out) {
            System.out.print (PREFIX);
            
            for (Object arg : args)
                System.out.print (arg);
            
            System.out.println ();
        }
    }
    
    @Override
    public void                 run () throws Exception {       
        aggTimeSec = getIntArgValue ("-at", 10);        
        runsWithoutWipeout = getIntArgValue ("-rww", 2);
        totalRuns = getIntArgValue ("-nr", 1000);

        if (isArgSpecified ("-mode"))
            mode = fromCode(getArgValue("-mode"));

        workFolder = new File (getArgValue ("-w", Home.getPath ("temp", "tbdeath")));
        IOUtil.mkDirIfNeeded (workFolder);
               
        Runtime.getRuntime ().addShutdownHook (
            new Thread () {
                @Override
                public void run () {
                    if (p != null)
                        p.destroy ();
                }                
            }
        );
                
        tbFolder = getFileArg ("-db");
        
        if (tbFolder == null)
            tbFolder = new File (workFolder, "db");

        IOUtil.removeRecursive (tbFolder);

        log ("Starting with clean TimeBase in ", tbFolder);
        
       for (int ii = 1; ii <= totalRuns; ii++) {
           //String name = String.valueOf(ii);
           String name = "0";

            IOUtil.mkDirIfNeeded (new File(tbFolder, name));
            command =
                    new String [] {
                            Home.getPath ("jre", "bin", Util.IS_WINDOWS_OS ? "java.exe" : "java"),
                            "-jar", Home.getPath ("bin", "runjava.jar"),
                            TestMessageGenerator.class.getName (),
                            tbFolder.getAbsolutePath () + File.separator + name,
                            "taq"
                    };
            System.out.print ("\n\n");
            
            log ("TEST RUN # ", ii);

            final Shutdown shutdown = mode != Shutdown.All ? mode : getShutdownMode(ii);
            
            startProcess ();
            
            killer.schedule (
                new TimerTask () {
                    @Override
                    public void     run () {
                        stopProcess (shutdown);
                    }
                }, 
                aggTimeSec * 1000
            );            
            
            waitForProcess ();

            checkDB(0);
        }
    }

    private Shutdown getShutdownMode(int runNumber) {
        int index = runNumber / runsWithoutWipeout;

        switch (index % 3) {
            case 0 : return Shutdown.Clean;
            case 1 : return Shutdown.Graceful;
            case 2 : return Shutdown.Abnormal;
            default: throw new RuntimeException("Unexpected change in code");
        }
    }

    private void                startProcess () throws IOException {
        StringBuilder   sb = new StringBuilder ("Starting ");
        
        for (String s : command) {
            sb.append (" ");
            sb.append (s);
        }
        
        sb.append (" ...");
        
        log (sb);
        
        ProcessBuilder      pb = new ProcessBuilder (command);
        
        pb.redirectErrorStream (true);
              
        pb.directory (workFolder);
        
        p = pb.start ();
        
        pin = p.getOutputStream ();
        
        new StreamPump (p.getInputStream (), System.out, true, false).start ();        
    }

    private void                stopProcess (Shutdown mode) {
        if (p == null) {
            log ("TB is already stopped");
            return;
        }
        
        try {
            if (mode == Shutdown.Graceful) {
                log ("Executing 'Graceful' shutdown ...");
                pin.write ('g');
                pin.flush ();
            } else if (mode == Shutdown.Clean) {
                log ("Executing 'Clean' shutdown ...");
                pin.write ('c');
                pin.flush ();
            } else {
                log ("Destroying process ...");            
                p.destroy ();
            }
        } catch (Throwable x) {
            x.printStackTrace ();
            System.exit (1003);
        }
    }
    
    private void                waitForProcess () throws InterruptedException {        
        int         code = p.waitFor ();

        p = null;
        
        log ("Process finished with code " + code);     
        
        if (code > 1000)
            System.exit (code);
    }

    private void                checkDB (int folderNumber) throws InterruptedException, IOException {
        DXTickDB db = TickDBFactory.createFromUrl(tbFolder.getAbsolutePath() + File.separator + folderNumber);
        db.open(false);
        DXTickStream[] streams = db.listStreams();

        for (DXTickStream stream : streams) {
            try (TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false))) {
                long msgCount = 0;
                while (cursor.next()) {
                    ++msgCount;
                }
                System.out.println("Read " + stream.getKey() + " stream: " + msgCount + " messages");
            }
        }

        db.close();

    }

}
