package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class WrongArgTypesException extends CompilationException {
    private static String    diag (String name, DataType [] types) {
        StringBuilder   sb = new StringBuilder ();

        sb.append ("Function ");
        sb.append (name);
        sb.append (" () may not be applied to (");

        int             n = types.length;

        for (int ii = 0; ii < n; ii++) {
            if (ii > 0)
                sb.append (", ");

            sb.append (types [ii].getBaseName ());
        }

        sb.append (")");

        return (sb.toString ());
    }

    public WrongArgTypesException (CallExpression e, DataType [] types) {
        super (diag (e.name, types), e);
    }
}
