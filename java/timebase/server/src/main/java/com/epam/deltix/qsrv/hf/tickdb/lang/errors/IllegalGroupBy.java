package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class IllegalGroupBy extends CompilationException {
    private static String   catIds (FieldIdentifier [] ids) {
        StringBuilder   sb = new StringBuilder ();
        
        ids [0].print (sb);
        
        for (int ii = 1; ii < ids.length; ii++) {
            sb.append (", ");
            ids [ii].print (sb);
        }
        
        return (sb.toString ());
    }
    
    public IllegalGroupBy (FieldIdentifier [] ids) {
        super ("Cannot group by " + catIds (ids), ids);
    }
}
