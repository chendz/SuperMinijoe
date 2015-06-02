/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.InternetFeature;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to configure extended Internet Explorer features.
 */
public class BrowserFeaturesSample {
    public static void main(String[] args) {
        Browser browser = new Browser();
        browser.setInternetFeatureEnabled(InternetFeature.DISABLE_NAVIGATION_SOUNDS, true);
        browser.setInternetFeatureEnabled(InternetFeature.RESTRICT_FILEDOWNLOAD, true);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://www.google.com");
    }
}
