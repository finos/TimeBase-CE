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

