package com.epam.deltix.izpack.panels.license;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.panels.userinput.UserInputPanelAutomationHelper;
import com.epam.deltix.izpack.Utils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CheckLicensePanelAutomation extends UserInputPanelAutomationHelper {
    public CheckLicensePanelAutomation() {
    }

    @Override
    public void runAutomated(InstallData installData, IXMLElement panelRoot) {
        super.runAutomated(installData, panelRoot);

        System.out.println("[ Starting to check license ]");

        CheckLicenseHelper checkLicenseHelper = new CheckLicenseHelper(installData);
        checkLicenseHelper.setProxyProperties();

        List<String> moduleNames = new ArrayList<>();
        List<String> licenseErrors = new ArrayList<>();
        getSelectedPacks(installData, panelRoot).forEach(moduleName -> {
            System.out.println("Checking license for module " + moduleName);
            String licenseError = checkLicenseHelper.checkLicense(moduleName);
            if (licenseError != null && !licenseError.isEmpty()) {
                moduleNames.add(moduleName);
                licenseErrors.add(licenseError);
            }
        });

        if (licenseErrors.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < moduleNames.size(); ++i)
                sb.append(moduleNames.get(i)).append(": ").append(licenseErrors.get(i)).append("\n");

            System.out.println(installData.getMessages().get(Utils.ERROR_TEXT_1_STR) + "\n" + sb.toString());
        }

        System.out.println("[ Check license finished ]");
    }

    // IzPack can't provide selected packs on this stage.
    // We can get it from automated installation xml.
    private List<String> getSelectedPacks(InstallData installData, IXMLElement panelRoot) {
        List<String> selectedPacks = new ArrayList<>();

        try {
            // automated installation root node
            Node parentNode = panelRoot.getElement().getParentNode();
            NodeList nodes = parentNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();
                if (attrs == null) {
                    continue;
                }

                Node idNode = attrs.getNamedItem("id");
                if (idNode != null) {
                    if ("packsSelection".equals(idNode.getNodeValue())) {
                        NodeList packNodes = node.getChildNodes();
                        for (int j = 0; j < packNodes.getLength(); ++j) {
                            attrs = packNodes.item(j).getAttributes();
                            if (attrs == null) {
                                continue;
                            }

                            Node nameNode = attrs.getNamedItem("name");
                            Node selectedNode = attrs.getNamedItem("selected");
                            if (nameNode != null && selectedNode != null) {
                                String name = nameNode.getNodeValue();
                                String selected = selectedNode.getNodeValue();
                                if (Boolean.valueOf(selected)) {
                                    selectedPacks.add(name);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("Error getting list of select packs");
            t.printStackTrace();

            //fallback (check license for all packs)
            selectedPacks.clear();
            installData.getAllPacks().forEach(pack -> {
                selectedPacks.add(pack.getName());
            });
        }

        return selectedPacks;
    }

}