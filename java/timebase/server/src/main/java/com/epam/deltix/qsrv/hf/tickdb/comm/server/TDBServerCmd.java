package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.io.Home;

import java.io.File;

/**
 *
 */
public class TDBServerCmd extends DefaultApplication {
    public TDBServerCmd (String [] args) {
        super (args);
    }
    
    @Override
    protected void                  run () throws Throwable {
        int             port = getIntArgValue ("-port", TDBProtocol.DEFAULT_PORT);
        DXTickDB          db = new TickDBImpl(new File(getArgValue ("-db", Home.getPath ("temp/qstest/tickdb"))) );
        
        if (isArgSpecified ("-format")) {
            System.out.println ("Formatting " + db.getId () + " ...");
            db.format ();
        }
        else {
            boolean     ro = isArgSpecified ("-ro");
                
            System.out.println (
                "Opening " + db.getId () + 
                (ro ? " as Read-Only ..." : " as Read-Write ...")               
            );

            db.open (ro);
        }
        
        db.warmUp();
        TickDBServer    server = new TickDBServer (port, db);
        
        server.start ();
        
        System.out.printf ("******************************************\n");
        System.out.printf (
            "* TB Server is Up on port %5d PV# %3d  *\n", 
            server.getPort (), TDBProtocol.VERSION
        );
        System.out.printf ("******************************************\n");
    }

    public static final void        main (String [] args) throws Throwable {
        new TDBServerCmd (args).start ();
    }
}
