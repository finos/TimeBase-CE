package com.epam.deltix.qsrv.solgen.base;

import java.util.*;

public interface SampleFactory {

    List<String> listSampleTypes();

    List<Property> getCommonProps();

    List<Property> getSampleProps(String sampleType);

    Sample create(String sampleType, Properties properties);

    Sample create(List<String> sampleTypes, Properties properties);

    default List<Property> getSampleProps(List<String> sampleTypes) {
        Set<Property> result = new HashSet<>();
        for (String sampleType : sampleTypes) {
            result.addAll(getSampleProps(sampleType));
        }
        return new ArrayList<>(result);
    }
}
