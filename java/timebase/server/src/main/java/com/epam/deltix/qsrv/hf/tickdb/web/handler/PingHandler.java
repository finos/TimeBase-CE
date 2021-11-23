package com.epam.deltix.qsrv.hf.tickdb.web.handler;

import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import io.jooby.Context;
import io.jooby.Route.Handler;
import io.jooby.StatusCode;
import org.jetbrains.annotations.NotNull;

public class PingHandler implements Handler {

    @NotNull
    @Override
    public Object apply(@NotNull Context ctx) throws Exception {
        try {
            if (AbstractHandler.TDB.isOpen()) {
                return "TimeBase is ready.";
            }
        } catch (Exception ignored) {
        }
        ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE_CODE);
        return "TimeBase is not ready.";
    }
}
