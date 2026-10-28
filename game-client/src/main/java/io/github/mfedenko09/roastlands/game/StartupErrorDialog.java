package io.github.mfedenko09.roastlands.game;

import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;

final class StartupErrorDialog {

    private StartupErrorDialog() {
    }

    static void show(String message) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
            return;
        }
        JOptionPane.showMessageDialog(null, message, DisplaySettings.TITLE, JOptionPane.ERROR_MESSAGE);
    }
}
