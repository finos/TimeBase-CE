package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;

public class QQLState {

    @XmlElement()
    public ArrayList<Token> tokens;

    @XmlElement()
    public long             errorLocation = -1; // -1 if qql is valid
}