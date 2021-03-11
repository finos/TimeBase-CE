package com.epam.deltix.izpack.panels.properties;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Overrides;
import com.izforge.izpack.installer.automation.PanelAutomation;

/**
 *
 */
public class InitPanelAutomation implements PanelAutomation {
    public InitPanelAutomation() {
    }

    @Override
    public void createInstallationRecord(InstallData installData, IXMLElement panelRoot) {
    }

    @Override
    public void runAutomated(InstallData installData, IXMLElement panelRoot) {
        PropertiesHelper propertiesHelper = new PropertiesHelper(installData);
        propertiesHelper.loadProperties();
        String errorMessage = propertiesHelper.checkPlatform();
        if (errorMessage != null)
            throw new RuntimeException(errorMessage);
    }

    @Override
    public void processOptions(InstallData installData, Overrides overrides) {

    }

}
