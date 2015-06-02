/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package com.teamdev.jexplorer.demo;

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.DefaultPopupHandler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author TeamDev Ltd.
 */
public final class TabFactory {

    public static Tab createFirstTab() {
        return createTab("http://www.teamdev.com/jexplorer");
    }

    public static Tab createTab() {
        return createTab("about:blank");
    }

    public static Tab createTab(String url) {
        Browser browser = new Browser();
        TabContent tabContent = new TabContent(browser);

        browser.setPopupHandler(new DefaultPopupHandler());

        final TabCaption tabCaption = new TabCaption();
        tabCaption.setTitle("about:blank");

        tabContent.addPropertyChangeListener("PageTitleChanged", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                tabCaption.setTitle((String) evt.getNewValue());
            }
        });

        browser.navigate(url);
        return new Tab(tabCaption, tabContent);
    }
}
