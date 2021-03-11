package com.epam.deltix.izpack;

import com.izforge.izpack.panels.userinput.processorclient.ProcessingClient;
import com.izforge.izpack.panels.userinput.validator.Validator;

/**
 *
 */
public class InstallationNameValidator implements Validator {

    private static final String []  invalidDirectoryChars = {"<", ">", ":", "\"", "/", "\\", "|", "?", "*", "'"};

    @Override
    public boolean                  validate(ProcessingClient client) {
        String instName = client.getText();
        if (instName == null)
            return true;

        for (String invalidChar : invalidDirectoryChars) {
            if (instName.contains(invalidChar)) {
                return false;
            }
        }

        return true;
    }
}
