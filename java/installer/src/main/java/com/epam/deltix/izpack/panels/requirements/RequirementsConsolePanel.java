package com.epam.deltix.izpack.panels.requirements;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.panels.htmlhello.HTMLHelloConsolePanel;
import com.izforge.izpack.util.Console;
import com.epam.deltix.izpack.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 */
public class RequirementsConsolePanel extends HTMLHelloConsolePanel {

    public final static Logger LOGGER = Logger.getLogger(RequirementsConsolePanel.class.getName());

    public RequirementsConsolePanel(Resources resources, PanelView<ConsolePanel> panel) {
        super(resources, panel);
    }

    @Override
    public boolean run(InstallData installData, Properties properties) {
        return true;
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        List<String> moduleNames = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        new RequirementsHelper(installData).checkRequirements(moduleNames, errors);

        if (errors.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < moduleNames.size(); ++i)
                sb.append(moduleNames.get(i)).append(": ").append(errors.get(i)).append("\n");

            console.println(installData.getMessages().get(Utils.ERROR_TEXT_1_STR) + "\n" + sb.toString());
        }

        return true;
    }

}
