/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package com.teamdev.jexplorer.demo;

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.DefaultEventsHandler;
import com.teamdev.jexplorer.demo.resources.Resources;
import com.teamdev.jexplorer.event.NavigationAdapter;
import com.teamdev.jexplorer.event.StatusAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author TeamDev Ltd.
 */
public class ToolBar extends JPanel {
    private static final String RUN_JAVASCRIPT = "Run JavaScript...";
    private static final String CLOSE_JAVASCRIPT = "Close JavaScript Console";
    private static final String DEFAULT_URL = "about:blank";

    private JButton backwardButton;
    private JButton forwardButton;
    private JButton refreshButton;
    private JButton stopButton;
    private JMenuItem consoleMenuItem;

    private final JTextField addressBar;
    private final Browser browser;

    public ToolBar(Browser browser) {
        this.browser = browser;
        addressBar = createAddressBar();
        setLayout(new GridBagLayout());
        add(createActionsPane(), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        add(addressBar, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(4, 0, 4, 5), 0, 0));
        add(createMenuButton(), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 5), 0, 0));
    }

    public void didJSConsoleClose() {
        consoleMenuItem.setText(RUN_JAVASCRIPT);
    }

    private JPanel createActionsPane() {
        backwardButton = createBackwardButton(browser);
        forwardButton = createForwardButton(browser);
        refreshButton = createRefreshButton(browser);
        stopButton = createStopButton(browser);

        JPanel actionsPanel = new JPanel();
        actionsPanel.add(backwardButton);
        actionsPanel.add(forwardButton);
        actionsPanel.add(refreshButton);
        actionsPanel.add(stopButton);
        return actionsPanel;
    }

    private JTextField createAddressBar() {
        final JTextField result = new JTextField(DEFAULT_URL);
        result.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browser.navigate(result.getText());
            }
        });

        browser.setEventsHandler(new DefaultEventsHandler() {
            @Override
            public boolean beforeNavigate(Browser browser, String url, String targetFrameName, byte[] postData, String headers) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        refreshButton.setEnabled(false);
                        stopButton.setEnabled(true);
                    }
                });
                return super.beforeNavigate(browser, url, targetFrameName, postData, headers);
            }
        });

        browser.addStatusListener(new StatusAdapter() {
            @Override
            public void backButtonEnabled(final boolean enabled) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        backwardButton.setEnabled(enabled);
                    }
                });
            }

            @Override
            public void forwardButtonEnabled(final boolean enabled) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        forwardButton.setEnabled(enabled);
                    }
                });
            }
        });

        browser.addNavigationListener(new NavigationAdapter() {
            @Override
            public void mainDocumentCompleted(Browser browser, final String url) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        result.setText(url);
                        result.setCaretPosition(result.getText().length());
                        refreshButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    }
                });
            }
        });
        return result;
    }

    private static JButton createBackwardButton(final Browser browser) {
        return createButton("Back", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                browser.goBack();
            }
        });
    }

    private static JButton createForwardButton(final Browser browser) {
        return createButton("Forward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                browser.goForward();
            }
        });
    }

    private static JButton createRefreshButton(final Browser browser) {
        return createButton("Refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                browser.refresh();
            }
        });
    }

    private static JButton createStopButton(final Browser browser) {
        return createButton("Stop", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                browser.stop();
            }
        });
    }

    private static JButton createButton(String caption, Action action) {
        ActionButton button = new ActionButton(caption, action);
        String imageName = caption.toLowerCase();
        button.setIcon(Resources.getIcon(imageName + ".png"));
        button.setRolloverIcon(Resources.getIcon(imageName + "-selected.png"));
        return button;
    }

    private JComponent createMenuButton() {
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(createConsoleMenuItem());
        popupMenu.add(createGetHTMLMenuItem());
        popupMenu.add(createPopupsMenuItem());
        popupMenu.add(createJavaScriptDialogsMenuItem());
        popupMenu.addSeparator();
        popupMenu.add(createAboutMenuItem());

        final ActionButton button = new ActionButton("Preferences", null);
        button.setIcon(Resources.getIcon("gear.png"));
        button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
                    popupMenu.show(e.getComponent(), 0, button.getHeight());
                } else {
                    popupMenu.setVisible(false);
                }
            }
        });
        return button;
    }

    private Component createJavaScriptDialogsMenuItem() {
        JMenuItem menuItem = new JMenuItem("JavaScript Dialogs");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browser.navigate("http://www.javascripter.net/faq/alert.htm");
            }
        });
        return menuItem;
    }

    private Component createGetHTMLMenuItem() {
        JMenuItem menuItem = new JMenuItem("Get HTML");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String html = browser.getContent();
                Window window = SwingUtilities.getWindowAncestor(browser);
                JDialog dialog = new JDialog(window);
                dialog.setModal(true);
                dialog.setContentPane(new JScrollPane(new JTextArea(html)));
                dialog.setSize(700, 500);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

            }
        });
        return menuItem;
    }

    private JMenuItem createConsoleMenuItem() {
        consoleMenuItem = new JMenuItem(RUN_JAVASCRIPT);
        consoleMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (RUN_JAVASCRIPT.equals(consoleMenuItem.getText())) {
                    consoleMenuItem.setText(CLOSE_JAVASCRIPT);
                    firePropertyChange("JSConsoleDisplayed", false, true);
                } else {
                    consoleMenuItem.setText(RUN_JAVASCRIPT);
                    firePropertyChange("JSConsoleClosed", false, true);
                }
            }
        });
        return consoleMenuItem;
    }

    private JMenuItem createPopupsMenuItem() {
        JMenuItem menuItem = new JMenuItem("Popup Windows");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browser.navigate("http://www.popuptest.com");
            }
        });
        return menuItem;
    }

    private JMenuItem createAboutMenuItem() {
        JMenuItem menuItem = new JMenuItem("About JExplorer Demo");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(ToolBar.this);
                AboutDialog aboutDialog = new AboutDialog(parentFrame);
                aboutDialog.setVisible(true);
            }
        });
        return menuItem;
    }

    private boolean isFocusRequired() {
        String url = addressBar.getText();
        return url.isEmpty() || url.equals(DEFAULT_URL);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (isFocusRequired()) {
                    addressBar.requestFocus();
                    addressBar.selectAll();
                }
            }
        });
    }

    private static class ActionButton extends JButton {
        private ActionButton(String hint, Action action) {
            super(action);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setBorderPainted(false);
            setRolloverEnabled(true);
            setToolTipText(hint);
            setText(null);
            setFocusable(false);
            setDefaultCapable(false);
        }
    }
}
