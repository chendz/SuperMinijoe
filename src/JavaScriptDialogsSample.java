/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.DialogHandler;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to handle JavaScript dialogs such as Alert or Confirmation.
 */
public class JavaScriptDialogsSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.setDialogHandler(new DialogHandler() {
            @Override
            public int showDialog(String title, String text, int type) {
                System.out.println("title = " + title);
                System.out.println("text = " + text);
                // Suppress dialog and close it right away without displaying UI
                return DialogHandler.IDOK;
            }
        });

        browser.executeScript("alert('test');");
    }
}
