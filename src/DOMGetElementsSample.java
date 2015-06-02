/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.dom.DOMDocument;
import com.teamdev.jexplorer.dom.DOMNodeList;
import com.teamdev.jexplorer.event.NavigationAdapter;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to get all elements with tag A and print their text content.
 */
public class DOMGetElementsSample {
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
                DOMDocument document = browser.getDocument();
                DOMNodeList elements = document.getElementsByTagName("a");
                int length = elements.getLength();
                for (int i = 0; i < length; i++) {
                    System.out.println("Text Content: " + elements.item(i).getTextContent());
                }
            }
        });
        browser.navigate("http://www.google.com");
    }
}
