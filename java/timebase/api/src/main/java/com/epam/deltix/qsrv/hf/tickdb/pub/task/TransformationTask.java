package com.epam.deltix.qsrv.hf.tickdb.pub.task;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.DataOutputStream;

public interface TransformationTask {

    boolean isBackground();
}
