package com.epam.deltix.izpack.panels.timebase;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.izforge.izpack.gui.ButtonFactory;
import com.izforge.izpack.gui.IzPanelConstraints;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.LayoutConstants;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.IzPanel;
import com.izforge.izpack.installer.gui.LayoutHelper;

public class TBHomeSelectionPanel extends JPanel implements ActionListener, LayoutConstants
{

    private static final long serialVersionUID = 3618700794577105718L;

    private JTextField textField;
    private JButton browseButton;
    private IzPanel parent;
    private GUIInstallData installData;
    private String targetPanel;
    private String variableName;
    private String defaultPanelName = "TargetPanel";
    private final Log log;

    public TBHomeSelectionPanel(IzPanel parent, GUIInstallData installData, String targetPanel, String variableName,
                                  Log log)
    {
        super();
        this.parent = parent;
        this.installData = installData;
        this.variableName = variableName;
        this.targetPanel = targetPanel;
        this.log = log;
        createLayout();
    }

    protected void createLayout()
    {
        // We woulduse the IzPanelLayout also in this "sub"panel.
        // In an IzPanel there are support of this layout manager at
        // more than one places. In this panel not, therefore we have
        // to make all things needed.
        // First create a layout helper.
        LayoutHelper layoutHelper = new LayoutHelper(this, installData);
        // Start the layout.
        layoutHelper.startLayout(new IzPanelLayout(log));
        // One of the rare points we need explicit a constraints.
        IzPanelConstraints ipc = IzPanelLayout.getDefaultConstraint(TEXT_CONSTRAINT);
        // The text field should be stretched.
        ipc.setXStretch(1.0);
        textField = new JTextField(installData.getVariable(variableName), 1);
        textField.addActionListener(this);
        parent.setInitialFocus(textField);
        add(textField, ipc);
        // We would have place between text field and button.
        add(IzPanelLayout.createHorizontalFiller(3));
        // No explicit constraints for the button (else implicit) because
        // defaults are OK.
        String buttonText = parent.getInstallerFrame().getMessages().get(targetPanel + ".browse");
        if (buttonText == null)
        {
            buttonText = parent.getInstallerFrame().getMessages().get(defaultPanelName + ".browse");
        }
        browseButton = ButtonFactory.createButton(buttonText, parent.getInstallerFrame().getIcons().get("open"),
            installData.buttonsHColor);
        browseButton.addActionListener(this);
        add(browseButton);
    }

    // There are problems with the size if no other component needs the
    // full size. Sometimes directly, somtimes only after a back step.

    public Dimension getMinimumSize()
    {
        Dimension preferredSize = super.getPreferredSize();
        Dimension retval = parent.getSize();
        retval.height = preferredSize.height;
        return (retval);
    }

    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == browseButton)
        {
            // The user wants to browse its filesystem

            // Prepares the file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(textField.getText()));
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());

            // Shows it
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                textField.setText(path);
            }

        }
        else
        {
            if (parent instanceof ActionListener)
            {
                ((ActionListener) parent).actionPerformed(e);
            }
        }
    }

    public String getPath()
    {
        return (textField.getText());
    }

    public void setPath(String path)
    {
        textField.setText(path);
    }

    public JTextField getPathInputField()
    {
        return textField;
    }

    public JButton getBrowseButton()
    {
        return browseButton;
    }
}

