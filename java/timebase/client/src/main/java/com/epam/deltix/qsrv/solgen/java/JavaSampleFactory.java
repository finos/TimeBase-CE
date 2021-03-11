package com.epam.deltix.qsrv.solgen.java;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.base.PropertyFactory;
import com.epam.deltix.qsrv.solgen.base.Sample;
import com.epam.deltix.qsrv.solgen.base.SampleFactory;
import com.epam.deltix.qsrv.solgen.java.samples.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class JavaSampleFactory implements SampleFactory {

    public static final Property TB_URL = PropertyFactory.create(
            "timebase.url",
            "The URL of TimeBase location, in the form of dxtick://<host>:<port>",
            false,
            SolgenUtils::isValidUrl,
            "dxtick://localhost:8011"
    );
    private static final List<Property> COMMON_PROPS = Collections.unmodifiableList(Collections.singletonList(TB_URL));

    public static final String LIST_STREAMS = "ListStreams";
    public static final String READ_STREAM = "ReadStream";
    public static final String WRITE_STREAM = "WriteStream";
    public static final String SPEED_TEST = "SpeedTest";

    private static final List<String> SAMPLE_TYPES = Collections.unmodifiableList(Arrays.asList(
            LIST_STREAMS, READ_STREAM, WRITE_STREAM, SPEED_TEST
    ));

    private final BeansGenerator beansGenerator = new BeansGenerator();

    @Override
    public List<String> listSampleTypes() {
        return SAMPLE_TYPES;
    }

    @Override
    public List<Property> getCommonProps() {
        return COMMON_PROPS;
    }

    @Override
    public List<Property> getSampleProps(String sampleType) {
        switch (sampleType) {
            case LIST_STREAMS:
                return ListStreamsSample.PROPERTIES;
            case READ_STREAM:
                return ReadStreamSample.PROPERTIES;
            case WRITE_STREAM:
                return WriteStreamSample.PROPERTIES;
            case SPEED_TEST:
                return SpeedTestSample.PROPERTIES;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public JavaSample create(String sampleType, Properties properties) {
        switch (sampleType) {
            case LIST_STREAMS:
                return new ListStreamsSample(properties);
            case READ_STREAM:
                return new ReadStreamSample(properties);
            case WRITE_STREAM:
                return new WriteStreamSample(properties);
            case SPEED_TEST:
                return new SpeedTestSample(properties);
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public Sample create(List<String> sampleTypes, Properties properties) {
        List<JavaSample> samples = sampleTypes.stream()
                .map(type -> create(type, properties))
                .collect(Collectors.toList());
        String tbUrl = properties.getProperty(TB_URL.getName());
        return project -> {
            samples.forEach(sample -> sample.addToProject(project));
            beansGenerator.generateBeans(project, tbUrl, samples.stream()
                    .filter(JavaSample::generateBeans)
                    .map(JavaSample::key)
                    .distinct()
                    .toArray(String[]::new));
        };
    }
}
