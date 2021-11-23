package com.epam.deltix.qsrv.hf.tickdb.web.handler;

import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObject;
import io.jooby.Context;
import io.jooby.Route.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class GetCursorIdsHandler implements Handler {
    @NotNull
    @Override
    public Object apply(@NotNull Context ctx) throws Exception {
        TBCursor[] cursors = ((TBMonitor) AbstractHandler.TDB).getOpenCursors();
        return Arrays.stream(cursors).map(TBObject::getId);
    }
}
