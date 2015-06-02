/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.BrowserContext;

/**
 * The sample demonstrates how to create two Browser instances that will run in one native process and
 * share session cookies and cache data. browserOne will access session cookies of browserTwo and vice versa.
 */
public class BrowserContextSample {
    public static void main(String[] args) {
        BrowserContext context = new BrowserContext();
        Browser browserOne = new Browser(context);
        Browser browserTwo = new Browser(context);
    }
}
