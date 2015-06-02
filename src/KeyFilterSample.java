/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.KeyFilter;
import com.teamdev.jexplorer.KeyFilterEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * The sample demonstrates how to filter key events to disable Ctrl+A shortcut.
 */
public class KeyFilterSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.setKeyFilter(new KeyFilter() {
            @Override
            public boolean isFilter(KeyFilterEvent evt) {
                // Disable Ctrl+A
                return evt.isControlPressed() && evt.getKeyCode() == KeyEvent.VK_A;
            }
        });
        browser.navigate("http://www.teamdev.com");
    }
}
