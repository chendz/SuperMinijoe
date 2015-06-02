/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to override default user-agent string.
 */
public class UserAgentSample {
    public static void main(String[] args) {
        Browser browser = new Browser();
        browser.setUserAgent("MyUserAgent");

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://whatsmyuseragent.com/");
    }
}
