/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

import com.teamdev.jexplorer.AuthenticationHandler;
import com.teamdev.jexplorer.Browser;

import javax.swing.*;
import java.awt.*;

/**
 * The sample demonstrates how to handle Basic, NTLM and Digest authentication.
 */
public class AuthenticationSample {
    public static void main(String[] args) {
        final Browser browser = new Browser();
        browser.setAuthenticationHandler(new AuthenticationHandler() {
            @Override
            public void onAuthenticate() {
            }

            @Override
            public String getUserName() {
                return "user";
            }

            @Override
            public String getPassword() {
                return "passwd";
            }
        });

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(browser, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        browser.navigate("http://httpbin.org/basic-auth/user/passwd");
    }
}
