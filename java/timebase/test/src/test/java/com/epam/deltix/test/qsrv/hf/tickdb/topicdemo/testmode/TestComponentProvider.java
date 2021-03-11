package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.testmode;

import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.ReadAndReplyBase;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.ReadEchoBase;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.WriteBase;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;

/**
 * @author Alexei Osipov
 */
public interface TestComponentProvider {
    ReadAndReplyBase getReader();
    WriteBase getWriter();
    ReadEchoBase getEchoReader(ExperimentFormat experimentFormat);
}
