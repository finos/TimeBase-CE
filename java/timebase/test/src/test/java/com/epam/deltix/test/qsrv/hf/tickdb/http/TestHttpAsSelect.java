package com.epam.deltix.test.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.http.*;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;

/**
 * Check column size validation on server side
 */
public class TestHttpAsSelect {
    private static final int[] IEEE32_VALID_SIZES = {0, 4, 8};
    // IEEE64 DECIMAL DECIMAL(0) DECIMAL(2)
    private static final int[] VALID_SIZES_0_8 = {0, 8};

    private static final int[] INT8_VALID_SIZES = {0, 1, 2, 4, 8};
    private static final int[] INT16_VALID_SIZES = {0, 2, 4, 8};
    private static final int[] INT32_VALID_SIZES = {0, 4, 8};
    private static final int[] BOOL_VALID_SIZES = {0, 1};
    private static final int[] CHAR_VALID_SIZES = {0, 2};

    public static void main(String[] args) throws Exception {
        test("IEEE32", IEEE32_VALID_SIZES);
        test("IEEE64", VALID_SIZES_0_8);
        test("DECIMAL", VALID_SIZES_0_8);
        test("DECIMAL0", VALID_SIZES_0_8);
        test("DECIMAL8", VALID_SIZES_0_8);

        test("INT8", INT8_VALID_SIZES);
        test("INT16", INT16_VALID_SIZES);
        test("INT32", INT32_VALID_SIZES);
        test("INT64", VALID_SIZES_0_8);
        test("INT48", VALID_SIZES_0_8);
        test("PUINT30", INT32_VALID_SIZES);
        test("PUINT61", VALID_SIZES_0_8);
        test("PINTERVAL", INT32_VALID_SIZES);

        test("BOOL", BOOL_VALID_SIZES);
        test("CHAR", CHAR_VALID_SIZES);
        test("TIMESTAMP", VALID_SIZES_0_8);
        test("TIME", INT32_VALID_SIZES);

        test("ENUM", INT8_VALID_SIZES);
        test("ENUM16", INT16_VALID_SIZES);
        test("ENUM32", INT32_VALID_SIZES);
        test("ENUM64", VALID_SIZES_0_8);
    }

    private static void test(String columnName, int[] valid_sizes) throws Exception {
        for (int i = 0; i <= 10; i++) {
            if (Arrays.binarySearch(valid_sizes, 0, valid_sizes.length, i) <= 0) {
                SelectAsStructRequest r = requestCustomAsStruct(columnName, i);
                try {
                    query(r);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private static SelectAsStructRequest requestCustomAsStruct(String columnName, int size) throws ParseException {
        SelectAsStructRequest r = new SelectAsStructRequest();
        RecordType im = new RecordType();
        RecordType rt = new RecordType();
        rt.name = "MyClass";
        rt.columns = new Column[]{
                new Column(columnName, size)
        };

        r.types = new RecordType[]{
                rt
        };
        r.stream = "custom_fields2";
        return r;
    }

    private static void query(XmlRequest request) throws IOException, JAXBException {
        Marshaller m = TBJAXBContext.createMarshaller();

        final URL url = new URL("http://localhost:8011/tb/xml");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        m.marshal(request, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());
        }
    }
}

