package com.epam.deltix.test.qsrv.hf.tickdb.qql;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBQQL;

/**
 *
 */
@Category(TickDBQQL.class)
public class Test_Queries {

    public void             test (String path)
        throws IOException, InterruptedException 
    {
        TickDBShell             shell = new TickDBShell ();
        
        try {
            shell.doExec (path);
            assertEquals (0, shell.getErrorCode ());
        } finally {
            Util.close (shell.dbmgr.getDB ());
        }        
    }
    
    @Test()
    public void             testSelect () throws Exception {
        Home.getFile("test/qqltest").mkdirs();

        test ("${home}/java/timebase/client/src/test/java/qql/select/*.q.txt");
    }

    @Test()
    public void             testDDL () throws Exception {
        test ("${home}/java/timebase/client/src/test/java/qql/ddl/*.q.txt");
    }

//    @Test()
//    public void             testSingle () throws Exception {
//        TickDBShell             shell = new TickDBShell ();
//
//        try {
//            shell.doExec ("${home}/java/timebase/client/src/test/java/qql/select/000-prep.q.txt");
//            shell.doExec ("${home}/java/timebase/client/src/test/java/qql/select/102-names.q.txt");
//            assertEquals (0, shell.getErrorCode ());
//        } finally {
//            Util.close (shell.dbmgr.getDB ());
//        }
//    }
}
