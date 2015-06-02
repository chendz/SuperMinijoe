/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.event.NavigationAdapter;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to get HTML content of the currently loaded document.
 */
public class GetContentSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.addNavigationListener(new NavigationAdapter() {
            @Override
            public void mainDocumentCompleted(Browser browser, String url) {
                System.out.println("html = " + browser.getContent());
            }
        });
        browser.navigate("http://www.google.com");
    }
}
