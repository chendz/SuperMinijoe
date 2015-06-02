/*
 * Copyright (c) 2000-2014 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package com.teamdev.jexplorer.demo.resources;

import javax.swing.*;

public class Resources {

    public static ImageIcon getIcon(String fileName) {
        return new ImageIcon(Resources.class.getResource(fileName));
    }
}
