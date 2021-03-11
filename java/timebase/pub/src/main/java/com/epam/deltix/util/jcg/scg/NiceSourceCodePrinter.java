package com.epam.deltix.util.jcg.scg;

import java.io.*;

/**
 *
 */
public abstract class NiceSourceCodePrinter extends SourceCodePrinter {
    private boolean                     finished = false;
        
    public NiceSourceCodePrinter () {
        super (new StringBuilder ());        
    }

    public abstract ImportTracker   getImportTracker ();
    
    protected abstract void         doPrintHeader (StringBuilder out);
    
    protected void                  doPrintFooter (StringBuilder out) {        
    }
    
    @Override
    public void                     finish () {  
        if (finished)
            return;
        
        finished = true;
        
        try {
            super.finish ();
        } catch (IOException x) {
            throw new RuntimeException (x);
        }
        
        StringBuilder   head = new StringBuilder ();

        doPrintHeader (head);

        ((StringBuilder) out).insert (0, head);     

        doPrintFooter ((StringBuilder) out);        
    }
    
    public String                   getSourceCode () {
        finish ();        
        return (out.toString ());
    }
    
    @Override
    public void                     printRefClassName (String cn) 
        throws IOException 
    {       
        super.print (
            getImportTracker () == null ? 
                cn : 
                getImportTracker ().getPrintClassName (cn)
        );        
    }        
}
