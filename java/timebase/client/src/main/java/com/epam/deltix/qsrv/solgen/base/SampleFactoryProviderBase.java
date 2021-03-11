package com.epam.deltix.qsrv.solgen.base;

public interface SampleFactoryProviderBase {

    SampleFactory createForJava();

    SampleFactory createForNET();

    SampleFactory createForPython();

    SampleFactory createForCpp();

    SampleFactory createForGo();

    default SampleFactory create(Language language) {
        switch (language) {
            case JAVA:
                return createForJava();
            case NET:
                return createForNET();
            case PYTHON:
                return createForPython();
            case CPP:
                return createForCpp();
            case GO:
                return createForGo();
            default:
                throw new UnsupportedOperationException();
        }
    }

}
