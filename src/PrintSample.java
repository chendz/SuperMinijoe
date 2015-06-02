/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.Callback;
import com.teamdev.jexplorer.PrintEventAdapter;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeoutException;

/**
 * The sample demonstrates how to print currently loaded web page.
 */
public class PrintSample {
    public static void main(String[] args) throws TimeoutException {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Browser.invokeAndWaitLoadMainFrame(browser, new Callback<Browser>() {
            @Override
            public void call(Browser value) {
                value.navigate("http://www.google.com");
            }
        });
        browser.addPrintEventListener(new PrintEventAdapter() {
            @Override
            public void onPrintTemplateTeardown() {
                // Print Dialog is closed.
            }
        });
        browser.print(true, "My Header", "My Footer");
    }
}
