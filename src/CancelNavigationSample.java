/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.DefaultEventsHandler;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to cancel navigation to a web addresses that contains "google.com".
 */
public class CancelNavigationSample {
    public static void main(String[] args) {
        Browser browser = new Browser();
        browser.setEventsHandler(new DefaultEventsHandler() {
            @Override
            public boolean beforeNavigate(Browser browser, String url, String targetFrameName, byte[] postData, String headers) {
                return url.contains("google.com");
            }
        });

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://www.google.com");
    }
}
