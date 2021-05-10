package com.epam.deltix.qsrv.solgen.net.samples;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.net.NetSampleFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class WriteStreamSample implements Sample {

    public static final Property STREAM_KEY = PropertyFactory.create(
            "timebase.stream",
            "TimeBase stream key.",
            true,
            StringUtils::isNotEmpty
    );
    public static final List<Property> PROPERTIES = Collections.unmodifiableList(Collections.singletonList(STREAM_KEY));

    private static final String WRITE_STREAM_TEMPLATE = "WriteStream.cs-template";

    private final Source writeStreamSource;

    public WriteStreamSample(Properties properties) {
        this(properties.getProperty(NetSampleFactory.TB_URL.getName()),
                properties.getProperty(STREAM_KEY.getName()));
    }

    public WriteStreamSample(String tbUrl, String key) {
        Map<String, String> params = new HashMap<>();
        params.put(NetSampleFactory.TB_URL.getName(), tbUrl);
        params.put(STREAM_KEY.getName(), key);

        writeStreamSource = new StringSource("WriteStream.cs",
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), WRITE_STREAM_TEMPLATE, params));
    }

    @Override
    public void addToProject(Project project) {
        project.addSource(writeStreamSource);
    }
}
