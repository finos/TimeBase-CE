package com.epam.deltix.izpack.panels.license;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.util.Console;

import java.util.Properties;

/**
 * Skip this panel for console mode
 */
public class HTMLLicenceConsolePanel extends com.izforge.izpack.panels.htmllicence.HTMLLicenceConsolePanel {
    public HTMLLicenceConsolePanel(PanelView<ConsolePanel> panel, Resources resources) {
        super(panel, resources);
    }

    @Override
    public boolean run(InstallData installData, Properties properties) {
        return true;
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        return true;
    }
}
