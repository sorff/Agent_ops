package main;

import db.DatabaseHelper;
import ui.LoginFrame;
import utils.MockDataGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Application entry point.
 *
 * Start-up sequence:
 *   1. Apply the system Look-and-Feel for native window decorations
 *   2. Apply a few global UI defaults (font, focus rings)
 *   3. Initialize the SQLite database and create tables if they don't exist
 *   4. Seed demo data on first run (MockDataGenerator)
 *   5. Show the LoginFrame on the Event Dispatch Thread
 */
public class Main {

    public static void main(String[] args) {

        // 1. Look and Feel — use the platform native L&F for proper OS integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to the cross-platform L&F if the system one is unavailable
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) { }
        }

        // 2. Global UI tweaks — disable the default dotted focus rectangle on buttons
        UIManager.put("Button.focus",        new Color(0, 0, 0, 0));
        UIManager.put("ToggleButton.focus",  new Color(0, 0, 0, 0));
        UIManager.put("CheckBox.focus",      new Color(0, 0, 0, 0));
        UIManager.put("ComboBox.focus",      new Color(0, 0, 0, 0));

        // 3. Initialize DB (creates tables if missing, opens the SQLite file)
        DatabaseHelper db = DatabaseHelper.getInstance();

        // 4. Seed demo data only on the very first run
        MockDataGenerator.seedIfEmpty();

        // 5. Launch the login window on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
