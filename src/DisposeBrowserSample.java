/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.event.DisposeListener;

/**
 * The sample demonstrates how to dispose Browser instance when you don't need to use it anymore.
 */
public class DisposeBrowserSample {
    public static void main(String[] args) {
        Browser browser = new Browser();
        browser.addDisposeListener(new DisposeListener() {
            @Override
            public void onDispose(Browser browser) {
                // Browser instance was disposed.
            }
        });
        browser.dispose();
    }
}
