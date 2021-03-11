package com.epam.deltix.qsrv.util.json;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/6/2019
 */
public class JSONRawMessagePrinterFactory {

    public static JSONRawMessagePrinter createForTimebaseWebGateway() {
        return new JSONRawMessagePrinter(false, true, DataEncoding.STANDARD, true, false, PrintType.FULL);
    }

    public static JSONRawMessagePrinter create(String typeField) {
        return new JSONRawMessagePrinter(false, true, DataEncoding.STANDARD, true,
                false, PrintType.FULL, typeField);
    }
}
