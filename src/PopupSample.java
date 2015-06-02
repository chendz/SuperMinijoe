/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.PopupContainer;
import com.teamdev.jexplorer.PopupHandler;
import com.teamdev.jexplorer.event.DisposeListener;
import com.teamdev.jexplorer.event.WindowAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * The sample demonstrates how to register your own popup handler to display popup windows
 * in the way you want.
 */
public class PopupSample {
    public static void main(String[] args) {
        Browser browser = new Browser();
        browser.setPopupHandler(new PopupHandler() {
            @Override
            public PopupContainer handlePopup() {
                return new PopupContainer() {
                    @Override
                    public void insertBrowser(final Browser browser) {
                        final JFrame frame = new JFrame();
                        frame.getContentPane().add(browser, BorderLayout.CENTER);
                        frame.setSize(800, 600);
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);

                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                browser.dispose();
                            }
                        });

                        browser.addDisposeListener(new DisposeListener() {
                            @Override
                            public void onDispose(Browser browser) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        frame.setVisible(false);
                                        frame.dispose();
                                    }
                                });
                            }
                        });

                        browser.addWindowListener(new WindowAdapter() {
                            @Override
                            public void onSetLeft(int left) {
                                Rectangle bounds = frame.getBounds();
                                bounds.x = left;
                                frame.setBounds(bounds);
                            }

                            @Override
                            public void onSetTop(int top) {
                                Rectangle bounds = frame.getBounds();
                                bounds.y = top;
                                frame.setBounds(bounds);
                            }

                            @Override
                            public void onSetWidth(int width) {
                                Rectangle bounds = frame.getBounds();
                                bounds.width = width;
                                frame.setBounds(bounds);
                            }

                            @Override
                            public void onSetHeight(int height) {
                                Rectangle bounds = frame.getBounds();
                                bounds.height = height;
                                frame.setBounds(bounds);
                            }

                            @Override
                            public void onResizable(boolean resizable) {
                                frame.setResizable(resizable);
                            }
                        });
                    }
                };
            }
        });

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://www.popuptest.com");
    }
}
