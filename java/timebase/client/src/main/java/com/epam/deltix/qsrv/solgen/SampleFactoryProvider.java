package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.solgen.base.SampleFactory;
import com.epam.deltix.qsrv.solgen.base.SampleFactoryProviderBase;
import com.epam.deltix.qsrv.solgen.cpp.CppSampleFactory;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;
import com.epam.deltix.qsrv.solgen.net.NetSampleFactory;
import com.epam.deltix.qsrv.solgen.python.PythonSampleFactory;

public class SampleFactoryProvider implements SampleFactoryProviderBase {

    private static final SampleFactoryProvider INSTANCE = new SampleFactoryProvider();

    public static SampleFactoryProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public SampleFactory createForJava() {
        return new JavaSampleFactory();
    }

    @Override
    public SampleFactory createForNET() {
        return new NetSampleFactory();
    }

    @Override
    public SampleFactory createForPython() {
        return new PythonSampleFactory();
    }

    @Override
    public SampleFactory createForCpp() {
        return new CppSampleFactory();
    }

    @Override
    public SampleFactory createForGo() {
        return null;
    }
}
