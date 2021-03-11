package com.epam.deltix.izpack.panels.properties;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.config.Options;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Overrides;
import com.izforge.izpack.installer.automation.PanelAutomation;

/**
 *
 */
public class StorePropertiesPanelAutomation implements PanelAutomation {
    public StorePropertiesPanelAutomation() {
    }

    @Override
    public void createInstallationRecord(InstallData installData, IXMLElement panelRoot) {
    }

    @Override
    public void runAutomated(InstallData installData, IXMLElement panelRoot) {
        System.out.println("[ Starting to store properties ]");
        new PropertiesHelper(installData).storeProperties();
        System.out.println("[ Store properties finished ]");
    }

    @Override
    public void processOptions(InstallData installData, Overrides overrides) {

    }

}