package com.epam.deltix.util.os;

import com.github.sarxos.winreg.HKey;
import com.github.sarxos.winreg.WindowsRegistry;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 *
 */
public class WindowsServiceControl extends ServiceControl {

    public static final WindowsServiceControl  INSTANCE = new WindowsServiceControl();
    public static final WindowsRegistry        REGISTRY = WindowsRegistry.getInstance();
    
    private static final String OSF                  = "[SC] EnumQueryServicesStatus:OpenService FAILED";        

    private static int          ERROR_CODE           = 1067;        
        
    private WindowsServiceControl() {
    }        

    @Override
    public void                 start (String id)
        throws IOException, InterruptedException
    {
        runSimple ("start", id, "SERVICE_NAME: " + id);
    }

    @Override
    public void                 stop (String id)
        throws IOException, InterruptedException
    {
        runSimple ("stop", id, "SERVICE_NAME: " + id);
    }

    @Override
    public void                 delete (String id)
            throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb ("delete", id);
        String              output = exec (pb, true);

        if (output.indexOf ("[SC] OpenService FAILED 1060") >= 0)
            throw new ServiceNotFoundException (id);

        if (output.indexOf ("[SC] DeleteService SUCCESS") < 0)
            throwException (pb, output);
    }

    @Override
    public void                 addDependency (String id, String dependId)
            throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb ("config", id);
        List <String>               c = pb.command ();
        c.add ("depend=");
        c.add (dependId);

        String              output = exec (pb, true);

        if (output.indexOf ("[SC] OpenService FAILED 1060") >= 0)
            throw new ServiceNotFoundException (id);

        if (output.indexOf ("[SC] ChangeServiceConfig SUCCESS") < 0){
            String executedCommand = "";
            for (String commandPart : pb.command()) {
                executedCommand += commandPart + " ";
            }
            LOG.error("Command '%s' failed\n%s").with(executedCommand).with(output);
        }
    }    
    
    @Override
    public void                 create (
        String                      id,
        String                      description,
        String                      binPath,
        CreationParameters          params      
    )
        throws IOException, InterruptedException
    {
        ProcessBuilder              pb = pb ("create", id);
        List <String>               c = pb.command ();

        c.add ("binPath=");

        // see http://support.microsoft.com/default.aspx?scid=kb;en-us;812486
        if (binPath.contains(" "))
            c.add ("temp");
        else
            c.add (binPath);

        if (params.displayName != null) {
            c.add ("DisplayName=");
            c.add (params.displayName);
        }

        if (params.type != null) {
            c.add ("type=");
            c.add (params.type.name ());
        }

        if (params.startMode != null) {
            c.add ("start=");
            c.add (params.startMode.name ());
        }

        if (params.errorMode != null) {
            c.add ("error=");
            c.add (params.errorMode.name ());
        }

        if (params.group != null) {
            c.add ("group=");
            c.add (params.group);
        }

        if (params.dependencies != null && params.dependencies.length != 0) {
            c.add ("depend=");

            StringBuilder   sb = new StringBuilder ();

            for (String d : params.dependencies) {
                if (sb.length () != 0)
                    sb.append ("/");

                sb.append (d);
            }

            c.add (sb.toString ());
        }

        if (params.obj != null) {
            c.add ("obj=");
            c.add (params.obj);
        }

        if (params.password != null) {
            c.add ("password=");
            c.add (params.password);
        }

        String      output = exec (pb);

        if (!output.contains ("[SC] CreateService SUCCESS"))
            throwException (pb, output);

        // see http://support.microsoft.com/default.aspx?scid=kb;en-us;812486
        try {
            REGISTRY.writeStringValue ( HKey.HKLM,
                                               "SYSTEM\\CurrentControlSet\\Services\\" + id,
                                               "ImagePath",
                                               binPath);
        } catch (Throwable x) {
            throw new RuntimeException ( x );
        }
        
        if (description != null && !description.trim().isEmpty()) {
            setDescription(id, description);
        }
    }
    
    @Override
    public String               queryStatusName (String id)
        throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb ("query", id);
        String              output = exec (pb);

        if (output.indexOf ("SERVICE_NAME: " + id) < 0)
            throwException (pb, output);

        String              status = getField (output, "STATE              : ");

        if (status == null)
            throw new IOException ("STATE field not found in: '" + output + "'");

        String []           s = status.split ("[ ]+");

        return (s [1]);
    }

    @Override
    public String               getExecutablePath (String id)
        throws InvocationTargetException, IllegalAccessException {
        
        return WindowsUtils.regQuery (HKey.HKLM,
                "SYSTEM\\CurrentControlSet\\Services\\" + id, "ImagePath" );
    }   

    @Override
    public boolean             exists (String id )
        throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb ("query", id);
        String              output = exec (pb, true);

        if (output.indexOf (OSF) >= 0)
            return (false);

        if (output.indexOf ("SERVICE_NAME: " + id) >= 0)
            return (true);

        throwException (pb, output);
        /*never*/ return (false);
    }

    @Override
    public void               startAndWait (String id, boolean ignoreQueryErrors, long timeout)
        throws IOException, InterruptedException
    {
        long        limit = System.currentTimeMillis () + timeout;

        for (;;) {
            String  status =
                ignoreQueryErrors ? queryStatusNameNoErrors (id) : queryStatusName (id);

            if (status.equals (STATUS_RUNNING))
                break;

            if (status.equals (STATUS_STOPPED)) {
                // on error we should if service fails with predefined error
                if (StringUtils.equals(String.valueOf(ERROR_CODE), queryExitCode(id)))
                    throw new RuntimeException ("Service failed to start.");
                else
                    start (id);
            }

            if (System.currentTimeMillis () >= limit)
                throw new InterruptedException ("Service failed to start after period of time (" + GMT.formatTime(limit) + ")."
                        + "Exit code = " + queryExitCode(id));

            Thread.sleep (500);
        }
    }

    @Override
    public void               stopAndWait (String id, boolean ignoreQueryErrors, long timeout)
        throws IOException, InterruptedException
    {
        long        limit = System.currentTimeMillis () + timeout;

        for (;;) {
            String  status =
                ignoreQueryErrors ? queryStatusNameNoErrors (id) : queryStatusName (id);

            if (ignoreQueryErrors && status.indexOf (OSF) >= 0)
                break;

            if (status.equals (STATUS_STOPPED))
                break;

            //FIXME: PaharelauK error 1060 - The specified service does not exist as an installed service.
            if (status.indexOf ("Exit code") > 0 && status.indexOf ("1060") > 0)
                break;

            if (status.equals (STATUS_RUNNING))
                stop (id);

            if (System.currentTimeMillis () >= limit)
                throw new InterruptedException ();

            Thread.sleep (500);
        }
    }
    
    private static void               query (String id, Status out)
        throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb ("query", id);
        String              output = exec (pb);

        if (output.indexOf ("SERVICE_NAME: " + id) < 0)
            throwException (pb, output);

        String              status = getField (output, "STATE              : ");
        String []           s = status.split ("[ ]+");

        out.state = Integer.parseInt (s [0]);
        out.stateName = s [1];
    }
    
    private static String               queryExitCode (String id)
        throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb ("query", id);
        String              output = exec (pb);

        if (output.indexOf ("SERVICE_NAME: " + id) < 0)
            throwException (pb, output);

        String              code = getField (output, "WIN32_EXIT_CODE    : ");

        if (code == null)
            throw new IOException ("WIN32_EXIT_CODE field not found in: '" + output + "'");

        String []           s = code.split ("( )+");

        return s[0];
    }    
    
    private static void                 setDescription (
       String                      id,
       String                      description )
    	throws IOException, InterruptedException
    {
    	  ProcessBuilder              pb = pb ("description", id);
          List <String>               c = pb.command ();
          c.add (description);

          String      output = exec (pb);

          if (output.indexOf ("[SC] ChangeServiceConfig2 SUCCESS") < 0)
              throwException (pb, output);
    }
    
    private static void                throwException (ProcessBuilder pb, String output)
        throws IOException
    {
        StringBuilder   sb = new StringBuilder ();

        for (String s : pb.command ()) {
            if (sb.length () != 0)
                sb.append (" ");

            sb.append (s);
        }

        sb.append (": failed:\n");
        sb.append (output);

        throw new IOException (sb.toString ());
    }

    private static void                throwException (ProcessBuilder pb, int code)
        throws IOException
    {
        StringBuilder   sb = new StringBuilder ();

        for (String s : pb.command ()) {
            if (sb.length () != 0)
                sb.append (" ");

            sb.append (s);
        }

        sb.append (": failed:\n");
        sb.append("Exit code = ").append(String.valueOf(code));

        throw new ExecutionException (sb.toString (), code);
    }

    private static String              exec (ProcessBuilder pb)
        throws IOException, InterruptedException
    {
        return exec(pb, false);
    }

    private static String              exec (ProcessBuilder pb,  boolean ignoreExitCode)
        throws IOException, InterruptedException
    {
        final Process               proc = pb.start ();
        String                      output = IOUtil.readFromStream (proc.getInputStream ());
        int                         exitVal = proc.waitFor ();

        if (exitVal != 0 && !ignoreExitCode)
            throwException (pb, exitVal);

        closeProcess(proc);

        return (output);
    }

    // HACK: to destroy process
    private static void             closeProcess(Process process) {

        try {
            Util.close(process.getInputStream());
            Util.close(process.getOutputStream());
            Util.close(process.getErrorStream());

//            Method m = process.getClass().getMethod("finalize");
//            m.setAccessible(true);
//            m.invoke(process);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static ProcessBuilder      pb (String cmd, String id) {
        ProcessBuilder              pb = new ProcessBuilder ();

        pb.redirectErrorStream (true);

        pb.command ().add ("sc.exe");
        pb.command ().add (cmd);
        pb.command ().add (id);

        return (pb);
    }

    private static void         runSimple (String cmd, String id, String expect)
        throws IOException, InterruptedException
    {
        ProcessBuilder      pb = pb (cmd, id);
        String              output = exec (pb);

        if (output.indexOf (expect) < 0)
            throwException (pb, output);
    }


    private static String       getField (String output, String field) {
        int                 a = output.indexOf (field);

        if (a < 0)
            return (null);

        a += field.length ();

        int                 b = output.indexOf ('\n', a);

        return (output.substring (a, b).trim ());
    }

    @Override
    public void                 setFailureAction(String id, FailureAction action, int delay, String command)
        throws IOException, InterruptedException
    {
        if (action != null) {
            ProcessBuilder pb = pb("failure", id);
            List<String> c = pb.command();

            c.add("reset=");
            c.add("INFINITE");

            c.add("command=");
            if (command == null || command.isEmpty())
                c.add("\"\"");
            else
                c.add(command);

            c.add("actions=");
            c.add(action.toString() + "/" + delay);

            String output = exec(pb);

            if (!output.contains("[SC] ChangeServiceConfig2 SUCCESS"))
                throwException(pb, output);
        }
    }
}
