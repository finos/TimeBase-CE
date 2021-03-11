package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.BasicStreamSelector;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs.SingleMessagePreparedQuery;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.lang.JavaCompilerHelper;

/**
 *
 */
public abstract class QCodeGenerator {
    static void                 move (
        QValue                      from,
        QValue                      to,
        JCompoundStatement          addTo
    )
    {
        to.type.move (from, to, addTo);
    }

    public static PreparedQuery        createQuery (CompiledQuery cq) {
        if (cq instanceof StreamSelector)
            return (createStreamSelector ((StreamSelector) cq));

        if (cq instanceof CompiledFilter) {
            CompiledFilter  cf = (CompiledFilter) cq;
            PreparedQuery   source = createQuery (cf.source);
            JavaCompilerHelper helper = new JavaCompilerHelper (CompiledFilter.class.getClassLoader ());
            return (new FilterGenerator ((CompiledFilter) cq).finish (helper, source));
        }

        if (cq instanceof SingleMessageSource)
            return (new SingleMessagePreparedQuery ());
        
        throw new UnsupportedOperationException (cq.getClass ().getName ());
    }

    private static PreparedQuery       createStreamSelector (StreamSelector ss) {
        return (new BasicStreamSelector (ss.mode, ss.streams));
    }
}
