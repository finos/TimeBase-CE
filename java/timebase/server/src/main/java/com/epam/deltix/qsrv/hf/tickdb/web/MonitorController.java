package com.epam.deltix.qsrv.hf.tickdb.web;

import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionInfo;
import com.epam.deltix.qsrv.hf.tickdb.web.model.monitor.*;
import com.epam.deltix.util.Version;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSDispatcher;
import com.epam.deltix.util.vsocket.VSServerFramework;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("/monitor")
public class MonitorController {

    @GET(path = "/version")
    public String version() {
        return Version.VERSION_STRING;
    }

    @GET(path = "/track")
    public TrackDto track() {
        return new TrackDto(getMonitor().getTrackMessages());
    }

    @POST(path = "/track")
    public void track(TrackDto track) {
        getMonitor().setTrackMessages(track.isOn());
    }

    @GET(path = "/connections")
    public List<ConnectionDto> connections() {
        return Arrays.stream(getVsFramework().getDispatchers())
            .filter(Objects::nonNull)
            .map(this::createConnection)
            .collect(Collectors.toList());
    }

    @GET(path = "/connections/{id}")
    public ConnectionDto connection(@PathParam String id) {
        return createConnection(getVsFramework().getDispatcher(id));
    }

    @GET(path = "/connections/{id}/channels")
    public List<VirtualChannelDto> channels(@PathParam String id) {
        return Arrays.stream(getVsFramework().getDispatcher(id).getVirtualChannels())
            .filter(Objects::nonNull)
            .map(this::createVirtualChannel)
            .collect(Collectors.toList());
    }

    @GET(path = "/cursors")
    public List<CursorDto> cursors() {
        return Arrays.stream(getMonitor().getOpenCursors())
            .filter(Objects::nonNull)
            .map(this::createCursor)
            .collect(Collectors.toList());
    }

    @GET(path = "/cursors/{id}")
    public CursorDto cursor(@PathParam long id) {
        TBObject object = getMonitor().getObjectById(id);
        if (object instanceof TBCursor) {
            return createCursor((TBCursor) object);
        }

        throw new IllegalArgumentException("Invalid object id: " + id);
    }

    @GET(path = "/cursors/{id}/instrumentsStats")
    public List<InstrumentStatsDto> cursorInstrumentsStats(@PathParam long id) {
        TBObject object = getMonitor().getObjectById(id);
        if (object instanceof TBCursor) {
            return Arrays.stream(((TBCursor) object).getInstrumentStats())
                .filter(Objects::nonNull)
                .map(this::createInstrumentStats)
                .collect(Collectors.toList());
        }

        throw new IllegalArgumentException("Invalid object id: " + id);
    }

    @GET(path = "/loaders")
    public List<LoaderDto> loaders() {
        return Arrays.stream(getMonitor().getOpenLoaders())
            .filter(Objects::nonNull)
            .map(this::createLoader)
            .collect(Collectors.toList());
    }

    @GET(path = "/loaders/{id}")
    public LoaderDto loader(@PathParam long id) {
        TBObject object = getMonitor().getObjectById(id);
        if (object instanceof TBLoader) {
            return createLoader((TBLoader) object);
        }

        throw new IllegalArgumentException("Invalid object id: " + id);
    }

    @GET(path = "/loaders/{id}/instrumentsStats")
    public List<InstrumentStatsDto> loaderInstrumentsStats(@PathParam long id) {
        TBObject object = getMonitor().getObjectById(id);
        if (object instanceof TBLoader) {
            return Arrays.stream(((TBLoader) object).getInstrumentStats())
                .filter(Objects::nonNull)
                .map(this::createInstrumentStats)
                .collect(Collectors.toList());
        }

        throw new IllegalArgumentException("Invalid object id: " + id);
    }

    @GET(path = "/locks")
    public List<LockDto> locks() {
        return Arrays.stream(getMonitor().getLocks())
            .filter(Objects::nonNull)
            .map(this::createLock)
            .collect(Collectors.toList());
    }

    private VSServerFramework getVsFramework() {
        VSServerFramework vsFramework = VSServerFramework.INSTANCE;
        if (vsFramework == null) {
            throw new RuntimeException("VSServerFramework is not initialized");
        }

        return vsFramework;
    }

    public TBMonitor getMonitor() {
        TBMonitor monitor = (TBMonitor) com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler.TDB;
        if (monitor == null) {
            throw new RuntimeException("TBMonitor is not initialized");
        }

        return monitor;
    }

    private ConnectionDto createConnection(VSDispatcher dispatcher) {
        if (dispatcher == null) {
            return null;
        }

        ConnectionDto connection = new ConnectionDto();
        connection.setApplicationId(dispatcher.getApplicationID());
        connection.setClientId(dispatcher.getClientId());
        connection.setCreationDate(dispatcher.getCreationDate().getTime());
        connection.setNumTransportChannels(dispatcher.getNumTransportChannels());
        connection.setThroughput(dispatcher.getThroughput());
        connection.setAverageThroughput(dispatcher.getAverageThroughput());
        connection.setRemoteAddress(dispatcher.getRemoteAddress());
        return connection;
    }

    private VirtualChannelDto createVirtualChannel(VSChannel channel) {
        if (channel == null) {
            return null;
        }

        VirtualChannelDto virtualChannel = new VirtualChannelDto();
        virtualChannel.setLocalId(channel.getLocalId());
        virtualChannel.setRemoteId(channel.getRemoteId());
        virtualChannel.setState(channel.getState());
        virtualChannel.setAutoFlush(channel.isAutoflush());

        return virtualChannel;
    }

    private CursorDto createCursor(TBCursor cursor) {
        if (cursor == null) {
            return null;
        }

        CursorDto cursorDto = new CursorDto();
        cursorDto.setId(cursor.getId());
        cursorDto.setApplication(cursor.getApplication());
        cursorDto.setUser(cursor.getUser());
        cursorDto.setStreams(cursor.getSourceStreamKeys());
        cursorDto.setLastResetTimestamp(cursor.getLastResetTime());
        cursorDto.setOptions(createCursorOptions(cursor.getOptions()));
        if (cursor instanceof SubscriptionInfo) {
            cursorDto.setSubscription(createCursorSubscription((SubscriptionInfo) cursor));
        }
        cursorDto.setStats(createStats(cursor));
        return cursorDto;
    }

    private LoaderDto createLoader(TBLoader loader) {
        if (loader == null) {
            return null;
        }

        LoaderDto loaderDto = new LoaderDto();
        loaderDto.setId(loader.getId());
        loaderDto.setApplication(loader.getApplication());
        loaderDto.setUser(loader.getUser());
        loaderDto.setStream(loader.getTargetStreamKey());
        loaderDto.setOptions(createLoaderOptions(loader.getOptions()));
        loaderDto.setStats(createStats(loader));
        return loaderDto;
    }

    private CursorOptionsDto createCursorOptions(SelectionOptions options) {
        if (options == null) {
            return null;
        }

        CursorOptionsDto optionsDto = new CursorOptionsDto();
        optionsDto.setRaw(options.isRaw());
        optionsDto.setLive(options.isLive());
        optionsDto.setReversed(options.isReversed());
        optionsDto.setUnordered(options.isAllowLateOutOfOrder());
        optionsDto.setRealTime(options.isRealTimeNotification());
        optionsDto.setChannelPerformance(options.getChannelPerformance());
        return optionsDto;
    }

    private LoaderOptionsDto createLoaderOptions(LoadingOptions options) {
        if (options == null) {
            return null;
        }

        LoaderOptionsDto optionsDto = new LoaderOptionsDto();
        optionsDto.setRaw(options.isRaw());
        optionsDto.setSorted(options.isGlobalSorting());
        optionsDto.setChannelPerformance(options.getChannelPerformance());
        return optionsDto;
    }

    private CursorSubscriptionDto createCursorSubscription(SubscriptionInfo cursor) {
        if (cursor == null) {
            return null;
        }

        CursorSubscriptionDto subscriptionDto = new CursorSubscriptionDto();
        subscriptionDto.setAllEntities(cursor.isAllEntitiesSubscribed());
        if (!cursor.isAllEntitiesSubscribed()) {
            subscriptionDto.setSubscribedEntities(cursor.getSubscribedSymbols());
        }
        subscriptionDto.setAllTypes(cursor.isAllTypesSubscribed());
        if (!cursor.isAllTypesSubscribed()) {
            subscriptionDto.setSubscribedTypes(cursor.getSubscribedTypes());
        }

        return subscriptionDto;
    }

    private ChannelStatsDto createStats(ChannelStats stats) {
        if (stats == null) {
            return null;
        }

        ChannelStatsDto statsDto = new ChannelStatsDto();
        statsDto.setTotalNumMessages(stats.getTotalNumMessages());
        statsDto.setLastMessageTimestamp(stats.getLastMessageTimestamp());
        statsDto.setLastMessageSysTimestamp(stats.getLastMessageSysTime());
        return statsDto;
    }

    private InstrumentStatsDto createInstrumentStats(InstrumentChannelStats stats) {
        if (stats == null) {
            return null;
        }

        InstrumentStatsDto statsDto = new InstrumentStatsDto();
        statsDto.setSymbol(stats.getSymbol().toString());
        statsDto.setTotalNumMessages(stats.getTotalNumMessages());
        statsDto.setLastMessageTimestamp(stats.getLastMessageTimestamp());
        statsDto.setLastMessageSysTimestamp(stats.getLastMessageSysTime());
        return statsDto;
    }

    private LockDto createLock(TBLock lock) {
        if (lock == null) {
            return null;
        }

        LockDto lockDto = new LockDto();
        lockDto.setId(lock.getId());
        lockDto.setGuid(lock.getGuid());
        lockDto.setType(lock.getType());
        lockDto.setStream(lock.getStreamKey());
        lockDto.setApplication(lock.getApplication());
        lockDto.setUser(lock.getUser());
        lockDto.setHost(lock.getHost());
        lockDto.setClientId(lock.getClientId());
        return lockDto;
    }
}
