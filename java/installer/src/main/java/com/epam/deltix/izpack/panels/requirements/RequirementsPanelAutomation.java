package com.epam.deltix.izpack.panels.requirements;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Overrides;
import com.izforge.izpack.installer.automation.PanelAutomation;
import com.epam.deltix.izpack.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RequirementsPanelAutomation implements PanelAutomation {

    public RequirementsPanelAutomation() {
    }

    @Override
    public void createInstallationRecord(InstallData installData, IXMLElement panelRoot) {
    }

    @Override
    public void runAutomated(InstallData installData, IXMLElement panelRoot) {
        System.out.println("[ Starting to check requirements ]");

        List<String> moduleNames = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        RequirementsHelper requirementsHelper = new RequirementsHelper(installData);
        requirementsHelper.checkRequirements(moduleNames, errors);

        if (errors.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < moduleNames.size(); ++i)
                sb.append(moduleNames.get(i)).append(": ").append(errors.get(i)).append("\n");

            System.out.println(installData.getMessages().get(Utils.ERROR_TEXT_1_STR) + "\n" + sb.toString());
        }

        System.out.println("[ Check requirements finished ]");
    }

    @Override
    public void processOptions(InstallData installData, Overrides overrides) {

    }
}
