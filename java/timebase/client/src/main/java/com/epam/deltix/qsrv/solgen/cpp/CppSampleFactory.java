/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.base.PropertyFactory;
import com.epam.deltix.qsrv.solgen.base.Sample;
import com.epam.deltix.qsrv.solgen.base.SampleFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class CppSampleFactory implements SampleFactory {

    public static final Property TB_URL = PropertyFactory.create(
            "timebase.url",
            "The URL of TimeBase location, in the form of dxtick://<host>:<port>",
            false,
            SolgenUtils::isValidUrl,
            "dxtick://localhost:8011"
    );
    public static final Property STREAM_KEY = PropertyFactory.create(
        "timebase.stream",
        "TimeBase stream key.",
        true,
        StringUtils::isNotEmpty
    );
    public static final Property NUM_THREADS = PropertyFactory.create(
        "cpp.SpeedTest.numThreads",
        "Num threads (number)",
        true,
        (s) -> {
            try {
                int value = Integer.parseInt(s);
                return value > 0;
            } catch (Throwable t) {
                return false;
            }
        },
        "4"
    );

    private static final List<Property> COMMON_PROPS = Collections.unmodifiableList(Collections.singletonList(TB_URL));

    public static final String LIST_STREAMS = "ListStreams";
    public static final String READ_STREAM = "ReadStream";
    public static final String WRITE_STREAM = "WriteStream";
    public static final String SPEED_TEST = "SpeedTest";
    private static final List<String> SAMPLE_TYPES = Collections.unmodifiableList(Arrays.asList(
        LIST_STREAMS, READ_STREAM, WRITE_STREAM, SPEED_TEST
    ));

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
    public Sample create(String sampleType, Properties properties) {
        return new MainSample(properties, createCppSample(sampleType, properties));
    }

    @Override
    public Sample create(List<String> sampleTypes, Properties properties) {
        return new MainSample(
            properties,
            sampleTypes.stream().map(s -> createCppSample(s, properties)).collect(Collectors.toList())
        );
    }

    private CppSample createCppSample(String sampleType, Properties properties) {
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
}