package com.epam.deltix.util.security;

import java.lang.reflect.InvocationTargetException;
import java.security.*;

/**
 *	Usage:
 *<pre>
 *SecurityManager		saveSecMan = System.getSecurityManager ();
 *		
 *System.setSecurityManager (ExitPreventingSecurityManager.INSTANCE);
 *
 *try {
 *    ... call some code that might do a System.exit () ...
 *} catch (ExitPreventingException x) {
 *    ... do something about x.getStatus () ...
 *} finally {
 *    System.setSecurityManager (saveSecMan);
 *}
 *</pre>
 */
public class ExitPreventingSecurityManager extends SecurityManager {
    private ExitPreventingSecurityManager () { }
    
    public static final ExitPreventingSecurityManager   INSTANCE = 
        new ExitPreventingSecurityManager ();
    
    @Override
	public void     checkExit (int status) {
		throw new ExitPreventingException (status);
	}
		
    @Override
	public void     checkPermission (Permission perm) {
	}
					
    @Override
	public void     checkPermission (Permission perm, Object context) {
	}     
    
    /**
     *  Run a main method of some class but prevent it from exiting the JVM.
     *  @param cls      The class to run.
     *  @param args     Command-line arguments.
     *  @return The exit code
     *  @throws java.lang.Exception
     */
    public static int      runMainNoExit (Class <?> cls, String [] args) 
        throws Throwable 
    {
        SecurityManager		saveSecMan = System.getSecurityManager ();
 		
        System.setSecurityManager (INSTANCE);

        try {
            cls.getMethod ("main", String [].class).invoke (null, (Object) args);
            return (0);
        } catch (InvocationTargetException itx) {
            Throwable       t = itx.getCause ();
            
            if (t instanceof ExitPreventingException) {
                ExitPreventingException epx = (ExitPreventingException) t;
                return (epx.getStatus ());
            }
            else 
                throw t;
        } catch (ExitPreventingException x) {
            return (x.getStatus ());
        } finally {
            System.setSecurityManager (saveSecMan);
        }
    }
}

