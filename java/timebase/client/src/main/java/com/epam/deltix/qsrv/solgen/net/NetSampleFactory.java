package com.epam.deltix.qsrv.solgen.net;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.base.PropertyFactory;
import com.epam.deltix.qsrv.solgen.base.Sample;
import com.epam.deltix.qsrv.solgen.base.SampleFactory;
import com.epam.deltix.qsrv.solgen.net.samples.ProgramSample;
import com.epam.deltix.qsrv.solgen.net.samples.ReadStreamSample;
import com.epam.deltix.qsrv.solgen.net.samples.WriteStreamSample;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class NetSampleFactory implements SampleFactory {

    public static final Property TB_URL = PropertyFactory.create(
            "timebase.url",
            "The URL of TimeBase location, in the form of dxtick://<host>:<port>",
            false,
            SolgenUtils::isValidUrl,
            "dxtick://localhost:8011"
    );
    private static final List<Property> COMMON_PROPS = Collections.unmodifiableList(Collections.singletonList(TB_URL));

    public static final String READ_STREAM = "ReadStream";
    public static final String WRITE_STREAM = "WriteStream";

    @Override
    public List<String> listSampleTypes() {
        return Arrays.asList(READ_STREAM, WRITE_STREAM);
    }

    @Override
    public List<Property> getCommonProps() {
        return COMMON_PROPS;
    }

    @Override
    public List<Property> getSampleProps(String sampleType) {
        switch (sampleType) {
            case READ_STREAM:
                return ReadStreamSample.PROPERTIES;
            case WRITE_STREAM:
                return WriteStreamSample.PROPERTIES;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public Sample create(String sampleType, Properties properties) {
        return new ProgramSample(Collections.singletonList(sampleType), properties);
    }

    @Override
    public Sample create(List<String> sampleTypes, Properties properties) {
        return new ProgramSample(sampleTypes, properties);
    }
}
