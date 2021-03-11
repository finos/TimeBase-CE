package com.epam.deltix.izpack;

import com.izforge.izpack.panels.userinput.processorclient.ProcessingClient;
import com.izforge.izpack.panels.userinput.validator.Validator;

/**
 *
 */
public class SerialValidator implements Validator {

    @Override
    public boolean                  validate(ProcessingClient client) {
        String serial = client.getText();
        return serial != null && isValid(serial);
    }

    private static int              shortHash (CharSequence s, int start, int end) {
        int             check = 0;

        for (int ii = start; ii < end; ii++)
            check = check * 149 + s.charAt (ii);

        return (check & 0xFFFF);
    }

    public static boolean           isValid (String serial) {
        int     pos = serial.lastIndexOf ('-');

        if (pos < 1)
            return (false);

        int             checkHash;

        try {
            checkHash = Integer.parseInt (serial.substring (pos + 1), 16);
        } catch (Throwable x) {
            return (false);
        }

        return (checkHash == shortHash (serial, 0, pos));
    }
}
