/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.Browser;
import com.teamdev.jexplorer.Callback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * The sample demonstrates how to get image of the currently loaded web page.
 */
public class HTMLToImageSample {
    public static void main(String[] args) throws IOException, TimeoutException {
        Browser browser = new Browser();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Browser.invokeAndWaitLoadMainFrame(browser, new Callback<Browser>() {
            @Override
            public void call(Browser browser) {
                browser.navigate("http://www.google.com");
            }
        });

        Image image = browser.getScreenShot(new Dimension(1280, 1024));
        ImageIO.write((RenderedImage) image, "PNG", new File("image.png"));
    }
}
