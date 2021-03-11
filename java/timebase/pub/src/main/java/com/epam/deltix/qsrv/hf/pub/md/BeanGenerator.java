package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.util.lang.StringUtils;


public class BeanGenerator {
    public static String    escapeIdentifierForJava (String id) {
        if (StringUtils.isJavaReservedWord (id))
            return (escapeJavaReservedWord (id));

        if (StringUtils.isValidJavaIdOrKeyword (id))
            return (id);

        return (doEscapeIdentifierForJava (id));
    }

    public static String    escapeIdentifierForCS (String id) {
        if (StringUtils.isCSReservedWord (id))
            return ("@" + id);

        if (StringUtils.isValidCSIdOrKeyword (id))
            return (id);

        return (doEscapeIdentifierForCS (id));
    }

    public static String    doEscapeIdentifierForJava (String id) {
        final StringBuilder     sb = new StringBuilder ();
        final int               fnameLength = id.length ();

        for (int ii = 0; ii < fnameLength; ii++) {
            char    c = id.charAt (ii);

            if (c == '$')
                sb.append ("$$");
            else if (ii == 0 ?
                        !Character.isJavaIdentifierStart (c) :
                        !Character.isJavaIdentifierPart (c))
            {
                sb.append ("$");
                sb.append ((int) c);
                sb.append ("$");
            }
            else
                sb.append (c);
        }

        return (sb.toString ());
    }

    public static String    doEscapeIdentifierForCS (String id) {
        final StringBuilder     sb = new StringBuilder ();
        final int               n = id.length ();

        for (int ii = 0; ii < n; ii++) {
            char    c = id.charAt (ii);

            if (c == '_')
                sb.append ("__");
            else if (ii == 0 ?
                        !StringUtils.isCSIdentifierStart (c) :
                        !StringUtils.isCSIdentifierPart (c))
            {
                sb.append ("_");
                sb.append ((int) c);
                sb.append ("_");
            }
            else
                sb.append (c);
        }

        return (sb.toString ());
    }

    public static String escapeJavaReservedWord (String keyword){
        return "$" + keyword;
    }
}
