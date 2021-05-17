/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.izpack.panels.license;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.api.resource.Messages;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.panels.userinput.UserInputConsolePanel;
import com.izforge.izpack.util.Console;
import com.izforge.izpack.util.PlatformModelMatcher;
import com.epam.deltix.izpack.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class CheckLicenseConsolePanel extends UserInputConsolePanel {

    public final static Logger LOGGER = Logger.getLogger(CheckLicenseConsolePanel.class.getName());

    private CheckLicenseHelper checkLicenseHelper;

    public CheckLicenseConsolePanel(Resources resources, ObjectFactory factory,
                                    RulesEngine rules, PlatformModelMatcher matcher, Console console, Prompt prompt,
                                    PanelView<ConsolePanel> panelView, InstallData installData)
    {
        super(resources, factory, rules, matcher, console, prompt, panelView, installData);
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        if (!super.run(installData, console))
            return false;

        try {
            CheckLicenseHelper checkLicenseHelper = getCheckLicenseHelper(installData);

            checkLicenseHelper.setProxyProperties();

            List<String> moduleNames = new ArrayList<>();
            List<String> licenseErrors = new ArrayList<>();
            checkLicenseHelper.checkLicenseForModules(moduleNames, licenseErrors);

            if (licenseErrors.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < moduleNames.size(); ++i)
                    sb.append(moduleNames.get(i)).append(": ").append(licenseErrors.get(i)).append("\n");

                console.println(installData.getMessages().get(Utils.ERROR_TEXT_1_STR) + "\n" + sb.toString());
                return promptProceedPanel(installData, console);
            }
        } catch (Throwable t) {
            console.println("Error checking license: " + t.getMessage());
            LOGGER.log(Level.WARNING, "Error checking license.", t);
        }

        return true;
    }

    private boolean promptProceedPanel(InstallData installData, Console console) {
        boolean result = true;
        final Messages messages = installData.getMessages();
        String prompt = messages.get(Utils.PROCEED_STR);
        console.println(prompt);
        int value = console.prompt("1 - Yes, 2 - No", 1, 2, 2);
        switch (value) {
            case 1:
                break;
            default:
                result = run(installData, console);
                break;
        }
        return result;
    }

    private CheckLicenseHelper getCheckLicenseHelper(final InstallData installData) {
        if (checkLicenseHelper == null)
            checkLicenseHelper = new CheckLicenseHelper(installData);

        return checkLicenseHelper;
    }
}