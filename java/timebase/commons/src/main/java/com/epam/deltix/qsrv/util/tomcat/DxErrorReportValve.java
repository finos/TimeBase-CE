package com.epam.deltix.qsrv.util.tomcat;

import org.apache.catalina.valves.ErrorReportValve;

public class DxErrorReportValve extends ErrorReportValve {
    public DxErrorReportValve() {
        super();
        setShowServerInfo(false);
    }

}

