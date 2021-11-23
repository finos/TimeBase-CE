package com.epam.deltix.qsrv.comm.cat;

import com.epam.deltix.qsrv.hf.tickdb.web.handler.GetCursorIdsHandler;
import com.epam.deltix.qsrv.hf.tickdb.web.handler.PingHandler;
import com.epam.deltix.util.concurrent.Signal;
import com.google.gson.Gson;
import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ServerOptions;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
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

        initRoutingTable();
    }

    private void initRoutingTable() {
        get("/cursor", new GetCursorIdsHandler());
        get("/ping", new PingHandler());
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
