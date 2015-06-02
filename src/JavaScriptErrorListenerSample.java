/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.ScriptErrorEvent;
import com.teamdev.jexplorer.ScriptErrorListener;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to listen to JavaScript errors.
 */
public class JavaScriptErrorListenerSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.addScriptErrorListener(new ScriptErrorListener() {
            @Override
            public void errorOccured(ScriptErrorEvent event) {
                System.out.println("event = " + event);
            }
        });

        browser.executeScript("a.unknown;");
    }
}
