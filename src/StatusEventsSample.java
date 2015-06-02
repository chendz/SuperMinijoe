/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.event.StatusAdapter;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to get status text change events.
 */
public class StatusEventsSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.addStatusListener(new StatusAdapter() {
            @Override
            public void statusTextChanged(String text) {
                System.out.println("Status Text = " + text);
            }
        });

        browser.navigate("http://www.google.com");
    }
}
