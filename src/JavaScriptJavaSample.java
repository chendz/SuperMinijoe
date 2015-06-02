/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.BrowserFunction;
import com.teamdev.jexplorer.JSValue;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to register Java function callback to invoke Java
 * code from JavaScript.
 */
public class JavaScriptJavaSample {
    public static void main(String[] args) {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.registerFunction("MyFunction", new BrowserFunction() {
            @Override
            public Object invoke(Object... args) {
                for (Object arg : args) {
                    System.out.println("arg = " + arg);
                }
                return "MyString";
            }
        });
        JSValue returnValue = browser.executeScript("MyFunction('abc', 1, true);");
        if (returnValue.isString()) {
            System.out.println("Function returns string: " + returnValue.getString());
        }
    }
}
