package com.epam.deltix.qsrv.solgen.java.samples;

import com.epam.deltix.qsrv.solgen.base.Sample;

public interface JavaSample extends Sample {

    boolean generateBeans();

    String key();

    String tbUrl();

}
