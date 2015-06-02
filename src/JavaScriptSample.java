/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.JSValue;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to execute JavaScript code on the currently loaded web page.
 */
public class JavaScriptSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.executeScript("document.write('<html><title>My Title</title><body>Hello from JExplorer!</body></html>');");
        JSValue returnValue = browser.executeScript("document.title");
        if (returnValue.isString()) {
            System.out.println("Title = " + returnValue.getString());
        }
    }
}
