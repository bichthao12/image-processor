package com.flashtech.imagetool;

import com.flashtech.imagetool.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        ImageIO.scanForPlugins();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}