package com.epam.deltix.qsrv.comm.cat;

import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.util.concurrent.Signal;
import com.google.gson.Gson;
import io.jooby.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebApp extends Jooby {

    public static final Logger LOGGER = Logger.getLogger (WebApp.class.getName());

    private final Signal stopSignal = new Signal();

    private final Gson gson = new Gson();
    private final  StartConfiguration config;

    public WebApp(StartConfiguration config){
        this.config = config;
        setExecutionMode(ExecutionMode.WORKER);
        setServerOptions(new ServerOptions()
                .setBufferSize(16384)
                .setPort(config.httpPort)
                .setIoThreads(16)
                .setDefaultHeaders(true)
                .setMaxRequestSize(10485760)
        );

        encoder(MediaType.json, (ctx, result) -> {
            String json = gson.toJson(result);
            ctx.setDefaultResponseType(MediaType.json);
            return json.getBytes(StandardCharsets.UTF_8);
        });

        get("/cursor", ctx -> {
            TBCursor [] cursors = ((TBMonitor)AbstractHandler.TDB).getOpenCursors();
            return Arrays.asList(cursors);
        });

        get("/ping", ctx -> {
            try {
                if (AbstractHandler.TDB.isOpen()) {
                    return "TimeBase is ready.";
                }
            } catch (Exception ignored) {
            }
            ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE_CODE);
            return "TimeBase is not ready.";
        });
    }

    @NotNull
    @Override
    public Jooby stop() {
        stopSignal.set();
        return super.stop();
    }

    public void  waitForStop() {
        try {
            stopSignal.await();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Awaiting has been interrupted.", e);
        }
    }

}
