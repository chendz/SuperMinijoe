/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.BrowserFeatures;
import com.teamdev.jexplorer.BrowserMode;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to configure MS IE 11 Browser Mode.
 */
public class BrowserModeSample {
    public static void main(String[] args) {
        // Enables MS IE 11 Browser Mode
        BrowserFeatures.enableBrowserMode(BrowserMode.IE11);
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://www.google.com");
    }
}
