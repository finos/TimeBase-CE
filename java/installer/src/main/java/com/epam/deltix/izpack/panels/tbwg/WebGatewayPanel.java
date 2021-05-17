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
package com.epam.deltix.izpack.panels.tbwg;

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
import com.epam.deltix.izpack.panels.timebase.TimebasePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
public class WebGatewayPanel extends IzPanel implements ActionListener {

    public final static Logger LOGGER = Logger.getLogger(WebGatewayPanel.class.getName());

    private enum WebGatewayStatus {
        RUNNING,
        INACTIVE;
    }

    private final Log izpackLog;
    private final UninstallData uninstallData;

    private static final String PANEL_NAME = "WebGatewayPanel";

    private JTextField portField;
    private JLabel statusLabel;
    private JLabel timebasePortLabel;
    private JButton startButton;
    private JButton openInBrowserButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final boolean isWindows;
    private final boolean isSuperUser;

    private String timebasePort;
    private volatile WebGatewayStatus status = WebGatewayStatus.INACTIVE;

    private volatile boolean panelActive = true;
    private boolean layoutInitialized;

    public WebGatewayPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, UninstallData uninstallData, Resources resources, Log log) {
        super(panel, parent, installData, new GridBagLayout(), resources);

        izpackLog = log;
        this.uninstallData = uninstallData;

        String platform = installData.getVariable(Utils.PLATFORM_VAR);
        isWindows = Utils.WINDOWS_PLATFORM.equalsIgnoreCase(platform);
        if (isWindows) {
            isSuperUser = true;
        } else {
            isSuperUser = LinuxOS.isSuperUser();
        }
    }

    @Override
    public void panelActivate() {
        panelActive = true;

        this.parent.lockNextButton();
        this.parent.getNavigator().setNextVisible(false);
        this.parent.setQuitButtonText(this.getI18nStringForClass("done"));
        this.parent.setQuitButtonIcon("done");

        timebasePort = installData.getVariable(TimebasePanel.TB_PORT_PROP);
        if (!isNumber(timebasePort)) {
            timebasePort = "8011";
        }
        updateTimebasePortLabel();
        startUpdateAppStatusThread();

        if (layoutInitialized) {
            return;
        }

        buildLayout();
    }

    @Override
    public void panelDeactivate() {
        super.panelDeactivate();

        panelActive = false;
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
            if (this.installData.isInstallSuccess()) {
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
        // select tbwg port
        FlowLayout portLayout = new FlowLayout();
        Container portContainer = new Container();
        portContainer.setLayout(portLayout);
        portContainer.add(
            createLabel("port", PANEL_NAME, "open", LEFT, false)
        );
        portField = new JTextField("8099", 0);
        Dimension d = portField.getPreferredSize();
        d.width = 80;
        portField.setPreferredSize(d);
        portContainer.add(portField);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        add(portContainer, constraints);

        // TB port label
        constraints.gridy++;
        FlowLayout timebasePortLayout = new FlowLayout();
        Container timebasePortContainer = new Container();
        timebasePortContainer.setLayout(timebasePortLayout);
        timebasePortLabel = new JLabel();
        timebasePortContainer.add(timebasePortLabel);
        updateTimebasePortLabel();
        add(timebasePortContainer, constraints);

        // Status label
//        constraints.gridy++;
//        FlowLayout statusLabelLayout = new FlowLayout();
//        Container statusLabelContainer = new Container();
//        statusLabelContainer.setLayout(statusLabelLayout);
//        statusLabel = new JLabel();
//        statusLabelContainer.add(statusLabel);
//        add(statusLabelContainer, constraints);

        constraints.gridy++;
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        startButton = ButtonFactory.createButton("Run WebGateway", getInstallerFrame().getIcons().get("open"), installData.buttonsHColor);
        startButton.addActionListener(this);
        add(startButton, constraints);

        if (Desktop.isDesktopSupported()) {
            constraints.gridy++;
            JFrame frame = new JFrame("Links");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(100, 400);
            Container container = frame.getContentPane();
            container.setLayout(new GridBagLayout());
            openInBrowserButton = new JButton();
            openInBrowserButton.setText("<HTML><FONT color=\"#000099\"><U>Open in browser</U></FONT></HTML>");
            openInBrowserButton.setHorizontalAlignment(SwingConstants.RIGHT);
            openInBrowserButton.setBorderPainted(false);
            openInBrowserButton.setOpaque(false);
            openInBrowserButton.setBackground(Color.WHITE);
            openInBrowserButton.addActionListener(this);
            openInBrowserButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            add(openInBrowserButton, constraints);
        }

        updateStatusLabel();
    }

    private void updateTimebasePortLabel() {
        if (timebasePortLabel != null) {
            timebasePortLabel.setText("TimeBase port: " + timebasePort);
        }
    }

    private void updateStatusLabel() {
        if (statusLabel != null) {
            statusLabel.setText("Status: " + status);
        }

        updateOpenInBrowserButton();
    }

    private void updateOpenInBrowserButton() {
        if (openInBrowserButton != null) {
            if (status == WebGatewayStatus.RUNNING) {
                openInBrowserButton.setEnabled(true);
            } else {
                openInBrowserButton.setEnabled(false);
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

        if (source == startButton) {
            if (!isNumber(portField.getText())) {
                emitError("Warning", "Invalid WebGateway port");
                return;
            }

            try {
                String installPath = installData.getVariable(Utils.INSTALL_PATH_VAR);
                startCmd(installPath);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to launch WebGateway", t);
                emitError("Warning: " + t.getClass().getName(), t.getMessage());
            }
        } else if (source == openInBrowserButton) {
            try {
                Desktop.getDesktop().browse(new URI(getWebGatewaryUrl()));
            } catch (Exception t) {
                LOGGER.log(Level.SEVERE, "Failed to open WebGateway in browser", t);
                emitError("Warning: " + t.getClass().getName(), t.getMessage());
            }
        }
    }

    private void startUpdateAppStatusThread() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (panelActive) {
                    try {
                        sleepAwhile(2000);

                        HttpURLConnection connection = (HttpURLConnection) new URL(
                             getWebGatewaryUrl() + "/ping"
                        ).openConnection();
                        connection.setRequestMethod("HEAD");
                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            status = WebGatewayStatus.RUNNING;
                        } else {
                            status = WebGatewayStatus.INACTIVE;
                        }
                    } catch (Throwable t) {
                        status = WebGatewayStatus.INACTIVE;
                    } finally {
                        SwingUtilities.invokeLater(() -> {
                            updateStatusLabel();
                            openInBrowserButton.setToolTipText(getWebGatewaryUrl());
                        });
                    }
                }
            }
        });
    }

    private void sleepAwhile(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startCmd(String installPath) throws IOException {
        // timebase port
        try {
            if (isWindows) {
                WindowsOS.execWindowsCmd(WebGatewayLauncherUtil.generateWindowsCmd(
                    installPath,
                    portField.getText(),
                    getTimebaseUrl()
                ), "WebGateway on " + portField.getText());
            } else {
                LinuxOS.execLinuxCmd(WebGatewayLauncherUtil.generateLinuxCmd(
                    installPath,
                    portField.getText(),
                    getTimebaseUrl()
                ), "WebGateway on " + portField.getText());
            }
        } catch (IOException ioException) {
            throw ioException;
        }
    }

    private String getTimebaseUrl() {
        return "dxtick://localhost:" + timebasePort;
    }

    private String getWebGatewaryUrl() {
        return "http://localhost:" + portField.getText();
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
