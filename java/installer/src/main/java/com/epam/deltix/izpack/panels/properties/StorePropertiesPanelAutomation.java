/*
 * Copyright 2023 EPAM Systems, Inc
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