/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.ConnectionType;
import com.teamdev.jexplorer.ProxyConfiguration;
import com.teamdev.jexplorer.ServerType;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to set proxy settings for Browser instance.
 */
public class ProxySample {
    public static void main(String[] args) {
        ProxyConfiguration configuration = new ProxyConfiguration();
        configuration.setConnectionType(ConnectionType.PROXY);
        configuration.setProxy("127.0.0.1:8088", ServerType.HTTP);
        configuration.setProxy("127.0.0.1:8088", ServerType.SECURE);

        Browser browser = new Browser();
        browser.setProxy(configuration);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://www.google.com");
    }
}
