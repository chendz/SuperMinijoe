/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package com.teamdev.jexplorer.demo;

import com.teamdev.jexplorer.BrowserFeatures;
import com.teamdev.jexplorer.BrowserMode;
import com.teamdev.jexplorer.demo.resources.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author TeamDev Ltd.
 */
public class JExplorerDemo {

    public static void main(String[] args) throws Exception {
        configureBrowserMode();
        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        String str = UIManager.getSystemLookAndFeelClassName();//com.sun.java.swing.plaf.windows.WindowsLookAndFeel
        UIManager.setLookAndFeel(str);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initAndDisplayUI();
            }
        });
    }

    /**
     * Configure Browser Mode according to MS Internet Explorer guide:
     * http://msdn.microsoft.com/en-us/library/ie/ee330730(v=vs.85).aspx
     */
    private static void configureBrowserMode() {
        String ieVersion = BrowserFeatures.getIEVersion();
        int index = ieVersion.indexOf('.');
        if (index != -1) {
            String version = ieVersion.substring(0, index);
            BrowserFeatures.enableBrowserMode(BrowserMode.valueOf(Integer.valueOf(version) * 1000));
        }
    }

    private static void initAndDisplayUI() {
        final TabbedPane tabbedPane = new TabbedPane();
        insertTab(tabbedPane, TabFactory.createFirstTab());
        insertNewTabButton(tabbedPane);

        JFrame frame = new JFrame("JExplorer Demo");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tabbedPane.disposeAllTabs();
            }
        });
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(Resources.getIcon("jexplorer16x16.png").getImage());
        frame.setVisible(true);
    }

    private static void insertNewTabButton(final TabbedPane tabbedPane) {
        TabButton button = new TabButton(Resources.getIcon("new-tab.png"), "New tab");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                insertTab(tabbedPane, TabFactory.createTab());
            }
        });
        tabbedPane.addTabButton(button);
    }

    private static void insertTab(TabbedPane tabbedPane, Tab tab) {
        tabbedPane.addTab(tab);
        tabbedPane.selectTab(tab);
    }
}
