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
package com.epam.deltix.izpack.panels.timebase;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.ButtonFactory;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.data.UninstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;
import com.epam.deltix.izpack.LinuxOS;
import com.epam.deltix.izpack.Utils;
import com.epam.deltix.izpack.WindowsOS;
import com.epam.deltix.util.os.ServiceControl;
import com.epam.deltix.util.os.SystemdServiceControl;
import com.epam.deltix.util.os.WindowsNativeServiceControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 *
 */
public class TimebasePanel extends IzPanel implements ActionListener {

    public final static Logger LOGGER = Logger.getLogger(TimebasePanel.class.getName());

    private final Log izpackLog;
    private final UninstallData uninstallData;

    private static final String PANEL_NAME = "TimebasePanel";
    private static final String TB_HOME_PROP = "TB_HOME_PROP";
    public static final String TB_PORT_PROP = "TB_PORT_PROP";
    private static final String DEFAULT_TB_HOME = Paths.get(System.getProperty("user.home"), "Deltix", "timebase").toString();

    private static final String UNINSTALL_DATA_SERVICES = "services";
    private static final String UNINSTALL_DATA_PLATFORM = "platform";

    private static final String UNINSTALL_DATA_DCS_86_RESOURCE = "resources/windscx86.dll";
    private static final String UNINSTALL_DATA_DCS_64_RESOURCE = "resources/windscamd64.dll";
    private static final String SAMPLE_DATA_RESOURCE = "/resources/samples.zip";

    private TBHomeSelectionPanel homeSelectionPanel;
    private JTextField portField;
    private JTextField userField;
    private JRadioButton serviceRadio;
    private JRadioButton cmdRadio;
    private JButton launchButton;
    private JButton uninstallButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final boolean isWindows;
    private final boolean isSuperUser;
    private final ServiceControl serviceControl;
    private final Set<String> services = new HashSet<>();

    private volatile boolean panelActive = true;
    private boolean layoutInitialized;

    public TimebasePanel(Panel panel, InstallerFrame parent, GUIInstallData installData, UninstallData uninstallData, Resources resources, Log log) {
        super(panel, parent, installData, new GridBagLayout(), resources);

        this.izpackLog = log;
        this.uninstallData = uninstallData;

        installData.setVariable(TB_HOME_PROP, DEFAULT_TB_HOME);
        String platform = installData.getVariable(Utils.PLATFORM_VAR);
        isWindows = Utils.WINDOWS_PLATFORM.equalsIgnoreCase(platform);
        if (isWindows) {
            serviceControl = WindowsNativeServiceControl.INSTANCE;
            isSuperUser = true;
            uninstallData.getNativeLibraries().add(UNINSTALL_DATA_DCS_86_RESOURCE);
            uninstallData.getNativeLibraries().add(UNINSTALL_DATA_DCS_64_RESOURCE);
        } else {
            serviceControl = SystemdServiceControl.INSTANCE;
            isSuperUser = LinuxOS.isSuperUser();
        }

        uninstallData.addAdditionalData(UNINSTALL_DATA_PLATFORM, platform);
    }

    @Override
    public void panelActivate() {
        panelActive = true;

        this.parent.lockPrevButton();
        this.parent.getNavigator().setPreviousVisible(false);
        this.parent.setQuitButtonText(this.getI18nStringForClass("done"));
        this.parent.setQuitButtonIcon("done");
        if (!Utils.isPackSelected(installData, Utils.WEB_GATEWAY) || !installData.isInstallSuccess()) {
            this.parent.lockNextButton();
            this.parent.getNavigator().setNextVisible(false);
        }

        if (isSuperUser) {
            startUpdateServiceStatusThread();
        }

        if (layoutInitialized) {
            return;
        }

        try {
            serviceControl.load(installData.getVariable(Utils.INSTALL_PATH_VAR) + "/bin");
        } catch (Throwable t) {
            emitError("Warning", "Can't load native library. Service control will not work.");
        }

        buildLayout();
    }

    @Override
    public void panelDeactivate() {
        super.panelDeactivate();

        panelActive = false;
        installData.setVariable(TB_PORT_PROP, portField.getText());
    }

    private void buildLayout() {
        Insets inset = new Insets(10, 20, 2, 2);
        GridBagConstraints constraints = new GridBagConstraints(
            0, 0,
            2, 1,
            1, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            inset,
            0, 0
        );
        try {
            if (installData.isInstallSuccess()) {
                createUi(constraints);
            } else {
                this.add(LabelFactory.create(this.getI18nStringForClass("fail"), (Icon) this.parent.getIcons().get("stop"), 10), constraints);
            }
        } finally {
            this.getLayoutHelper().completeLayout();
            layoutInitialized = true;
        }
    }

    private void createUi(GridBagConstraints constraints) {
        // select folder
        add(createLabel("selectHome", PANEL_NAME, "open", LEFT, true), constraints);
        constraints.gridy++;
        homeSelectionPanel = new TBHomeSelectionPanel(this, installData, PANEL_NAME, TB_HOME_PROP, izpackLog);
        add(homeSelectionPanel, constraints);

        // select tb port
        FlowLayout portLayout = new FlowLayout();
        Container portContainer = new Container();
        portContainer.setLayout(portLayout);
        portContainer.add(
            createLabel("port", PANEL_NAME, "open", LEFT, false)
        );
        portField = new JTextField("8011", 0);
        Dimension d = portField.getPreferredSize();
        d.width = 80;
        portField.setPreferredSize(d);
        portContainer.add(portField);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        add(portContainer, constraints);

        if (!isWindows) {
            // select service user
            FlowLayout userLayout = new FlowLayout();
            Container userContainer = new Container();
            userContainer.setLayout(userLayout);
            userContainer.add(
                createLabel("user", PANEL_NAME, "open", LEFT, false)
            );
            userField = new JTextField(System.getProperty("user.name"), 0);
            d = userField.getPreferredSize();
            d.width = 120;
            userField.setPreferredSize(d);
            userContainer.add(userField);

            constraints.gridy++;
            add(userContainer, constraints);
        }

        ButtonGroup group = new ButtonGroup();
        serviceRadio = new JRadioButton("Run as service", false);
        updateServiceRadio(isSuperUser);
        group.add(serviceRadio);

        cmdRadio = new JRadioButton(getString("Run in terminal"), true);
        group.add(cmdRadio);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        add(serviceRadio, constraints);

        ButtonFactory.useButtonIcons(true);
        uninstallButton = ButtonFactory.createButton("", getInstallerFrame().getIcons().get("UninstallService"), installData.buttonsHColor);
        uninstallButton.setToolTipText("Uninstall Service");
        uninstallButton.addActionListener(this);
        uninstallButton.setEnabled(false);
        ButtonFactory.useButtonIcons(false);
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.EAST;
        add(uninstallButton, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        add(cmdRadio, constraints);

        constraints.gridy++;
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        launchButton = ButtonFactory.createButton("Run Timebase", getInstallerFrame().getIcons().get("open"), installData.buttonsHColor);
        launchButton.addActionListener(this);
        add(launchButton, constraints);
    }

    private void updateServiceRadio(boolean isSuperUser) {
        updateServiceRadio(isSuperUser, null, null);
    }

    private void updateServiceRadio(boolean isSuperUser, String id, String status) {
        if (serviceRadio != null) {
            if (!isSuperUser) {
                serviceRadio.setEnabled(false);
                serviceRadio.setText("Run as service (only for " + (isWindows ? "administrator)" : "root user)"));
            } else if (status != null && !status.isEmpty()) {
                serviceRadio.setEnabled(true);
                serviceRadio.setText("Run as service (" + id + " : " + status + ")");
            } else {
                serviceRadio.setEnabled(true);
                serviceRadio.setText("Run as service");
            }
        }

        updateUninstallServiceButton(status);
    }

    private void updateUninstallServiceButton(String status) {
        if (uninstallButton != null) {
            if ("active".equalsIgnoreCase(status) || "running".equalsIgnoreCase(status)) {
                uninstallButton.setVisible(true);
                uninstallButton.setEnabled(true);
            } else {
                uninstallButton.setEnabled(false);
            }
        }
    }

    @Override
    public boolean isValidated() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == launchButton) {
            if (new File(homeSelectionPanel.getPath()).isFile()) {
                emitError("Warning", "Selected Timebase home is file");
                return;
            }
            if (!isNumber(portField.getText())) {
                emitError("Warning", "Invalid Timebase port");
                return;
            }

            try {
                prepareHome();

                String installPath = installData.getVariable(Utils.INSTALL_PATH_VAR);
                if (cmdRadio.isSelected()) {
                    startCmd(installPath);
                } else if (serviceRadio.isSelected()) {
                    startService(installPath);
                }
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to launch TimeBase", t);
                emitError("Warning: " + t.getClass().getName(), t.getMessage());
            }
        } else if (source == uninstallButton) {
            if (!isNumber(portField.getText())) {
                emitError("Warning", "Invalid TimeBase port");
                return;
            }

            try {
                uninstallService();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to uninstall TimeBase service", t);
                emitError("Warning: " + t.getClass().getName(), t.getMessage());
            }
        }
    }

    private void prepareHome() throws IOException {
        String home = homeSelectionPanel.getPath();
        File destDir = new File(home, "timebase");

        copySamples(destDir);
    }

    private static void copySamples(File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(
            TimebasePanel.class.getResourceAsStream(SAMPLE_DATA_RESOURCE))
        ) {
            ZipEntry zipEntry = zis.getNextEntry();
            Set<String> skipMap = new HashSet<>();
            while (zipEntry != null) {
                try {
                    if (shouldSkip(skipMap, zipEntry)) {
                        continue;
                    }

                    if (zipEntry.isDirectory()) {
                        if (firstLevelDirExists(destDir, zipEntry)) {
                            skipMap.add(zipEntry.getName());
                            continue;
                        }

                        new File(destDir, zipEntry.getName()).mkdirs();
                    } else {
                        try (FileOutputStream fos = new FileOutputStream(newFile(destDir, zipEntry))) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                } finally {
                    zipEntry = zis.getNextEntry();
                }
            }
            zis.closeEntry();
        }
    }

    private static boolean firstLevelDirExists(File destDir, ZipEntry zipEntry) {
        String parent = new File(zipEntry.getName()).getParent();
        if (parent == null || parent.isEmpty()) {
            if (new File(destDir, zipEntry.getName()).exists()) {
                return true;
            }
        }

        return false;
    }

    private static boolean shouldSkip(Set<String> skipMap, ZipEntry zipEntry) {
        boolean skip = false;
        for (String skipDir : skipMap) {
            if (zipEntry.getName().startsWith(skipDir)) {
                skip = true;
                break;
            }
        }

        return skip;
    }

    public static File newFile(File destDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destDir, zipEntry.getName());

        destFile.getParentFile().mkdirs();
        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private void startUpdateServiceStatusThread() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (panelActive) {
                    try {
                        sleepAwhile(2000);
                        if (serviceControl != null && portField != null && serviceRadio != null) {
                            if (isNumber(portField.getText())) {
                                String id = getServiceId(portField.getText());
                                String newStatus = getServiceStatus(id);
                                SwingUtilities.invokeLater(() -> {
                                    updateServiceRadio(isSuperUser, id, newStatus);
                                });
                            }
                        }
                    } catch (Throwable t) {
                    }
                }
            }
        });
    }

    private String getServiceStatus(String id) {
        try {
            return serviceControl.queryStatusName(id);
        } catch (Throwable t) {
        }

        return "inactive";
    }

    private void sleepAwhile(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startCmd(String installPath) throws IOException {
        try {
            if (isWindows) {
                WindowsOS.execWindowsCmd(TimebaseLauncherUtil.generateWindowsCmd(
                    installPath,
                    homeSelectionPanel.getPath(),
                    portField.getText()
                ), "Timebase on " + portField.getText());
            } else {
                LinuxOS.execLinuxCmd(TimebaseLauncherUtil.generateLinuxCmd(
                    installPath,
                    homeSelectionPanel.getPath(),
                    portField.getText()
                ), "Timebase on " + portField.getText());
            }
        } catch (IOException ioException) {
            throw ioException;
        }
    }

    private void startService(String installPath) throws IOException, InterruptedException {
        try {
            String port = portField.getText();
            String id = getServiceId(port);

            boolean reinstall = true;

            if (serviceControl.exists(id)) {
                reinstall = services.contains(id);
                if (!reinstall) {
                    reinstall = emitWarning(
                        "Service " + id,
                        "Service on port " + port + " is already installed. Do you want to reinstall?"
                    );
                }
                if (reinstall) {
                    serviceControl.stopAndWait(id, true, 10000);
                    serviceControl.delete(id);
                    services.remove(id);
                }
            }

            if (!reinstall) {
                return;
            }

            if (isWindows) {
                File binFile = TimebaseLauncherUtil.generateWindowsService(
                    installPath, homeSelectionPanel.getPath(), port
                );

                ServiceControl.CreationParameters params = new ServiceControl.CreationParameters();
                params.displayName = "Timebase on " + port;
                params.startMode = ServiceControl.StartMode.auto;
                params.servicePath = null;

                serviceControl.create(
                    id,
                    params.displayName,
                    "\"" + binFile.getAbsolutePath() + "\" -tb -sid \"" + id + "\" -port " + port,
                    params
                );
                serviceControl.start(id);
            } else {
                File binFile = TimebaseLauncherUtil.generateLinuxService(
                    installPath, homeSelectionPanel.getPath(), port, userField.getText()
                );

                serviceControl.create(
                    id,
                    "Timebase on " + port,
                    binFile.getAbsolutePath(),
                    null
                );
                serviceControl.start(id);
            }

            services.add(id);
        } catch (IOException | InterruptedException ioException) {
            throw ioException;
        } finally {
            updateUninstallData();
        }
    }

    private void uninstallService() throws IOException, InterruptedException {
        try {
            String port = portField.getText();
            String id = getServiceId(port);

            if (!emitWarning(
                "Service " + id,
                "Do you want to uninstall TimeBase service on port " + port + "?"
                ))
            {
                return;
            }

            serviceControl.stopAndWait(id, true, 10000);
            serviceControl.delete(id);
            services.remove(id);
        } catch (IOException | InterruptedException ioException) {
            throw ioException;
        } finally {
            updateUninstallData();
        }
    }

    private void updateUninstallData() {
        uninstallData.addAdditionalData(
            UNINSTALL_DATA_SERVICES, services.toArray(new String[0])
        );
    }

    private String getServiceId(String port) {
        return isWindows ? "tb." + port : "tb" + port;
    }

    private boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
