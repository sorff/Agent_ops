package utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Shows animated toast notifications in the bottom-right corner of the screen.
 *
 * Usage (call from anywhere in the app):
 *   NotificationHelper.showSuccess("Agent hired successfully!");
 *   NotificationHelper.showError("Failed to connect to database.");
 *   NotificationHelper.showInfo("3 tasks are awaiting your approval.");
 *   NotificationHelper.showWarning("Agent NOVA is in error state.");
 *
 * The toast slides in, stays for a few seconds, then fades out automatically.
 */
public class NotificationHelper {

    // How long the toast stays visible (milliseconds)
    private static final int DISPLAY_DURATION_MS = 3500;

    // ── Public API ────────────────────────────────────────────

    public static void showSuccess(String message) {
        show(message, "SUCCESS");
    }

    public static void showError(String message) {
        show(message, "ERROR");
    }

    public static void showInfo(String message) {
        show(message, "INFO");
    }

    public static void showWarning(String message) {
        show(message, "WARNING");
    }

    // ── Internal toast creation ───────────────────────────────

    /**
     * Creates and displays a toast window.
     * This always runs on the Swing Event Dispatch Thread (EDT) for thread safety.
     */
    private static void show(String message, String type) {
        // SwingUtilities.invokeLater ensures UI updates happen on the right thread
        SwingUtilities.invokeLater(() -> {
            ToastWindow toast = new ToastWindow(message, type);
            toast.display();
        });
    }

    // ── Inner class: the actual toast window ─────────────────

    /**
     * A lightweight, undecorated (no title bar) window that shows the message.
     */
    private static class ToastWindow extends JWindow {

        private float opacity = 0f;   // starts invisible, fades in
        private final String message;
        private final String type;

        ToastWindow(String message, String type) {
            this.message = message;
            this.type    = type;
            setSize(320, 60);
            setOpacity(0f);  // start fully transparent
        }

        /** Positions the toast, builds its content, and starts the animation */
        void display() {
            // Position in bottom-right corner of the screen
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width  - getWidth()  - 20;
            int y = screenSize.height - getHeight() - 60;
            setLocation(x, y);

            // Build the content panel
            JPanel content = buildContentPanel();
            add(content);
            setVisible(true);

            // Fade in over 300ms
            Timer fadeIn = new Timer(30, null);
            fadeIn.addActionListener(e -> {
                opacity += 0.1f;
                if (opacity >= 1f) {
                    opacity = 1f;
                    fadeIn.stop();
                    // After the toast is fully visible, schedule the auto-dismiss
                    scheduleDisappear();
                }
                setOpacity(Math.min(opacity, 1f));
            });
            fadeIn.start();
        }

        /** After DISPLAY_DURATION_MS ms, fade out and close */
        private void scheduleDisappear() {
            Timer wait = new Timer(DISPLAY_DURATION_MS, null);
            wait.setRepeats(false);
            wait.addActionListener(e -> fadeOut());
            wait.start();
        }

        /** Fades out and then disposes this window */
        private void fadeOut() {
            Timer fadeOut = new Timer(30, null);
            fadeOut.addActionListener(e -> {
                opacity -= 0.1f;
                if (opacity <= 0f) {
                    opacity = 0f;
                    fadeOut.stop();
                    dispose();
                } else {
                    setOpacity(Math.max(opacity, 0f));
                }
            });
            fadeOut.start();
        }

        /** Builds the colored panel with icon + message */
        private JPanel buildContentPanel() {
            // Choose colors based on type
            Color bgColor, borderColor;
            String icon;
            switch (type) {
                case "SUCCESS":
                    bgColor = ThemeColors.TOAST_SUCCESS_BG;
                    borderColor = ThemeColors.TOAST_SUCCESS_BORDER;
                    icon = "✓";
                    break;
                case "ERROR":
                    bgColor = ThemeColors.TOAST_ERROR_BG;
                    borderColor = ThemeColors.TOAST_ERROR_BORDER;
                    icon = "✕";
                    break;
                case "WARNING":
                    bgColor = ThemeColors.TOAST_WARN_BG;
                    borderColor = ThemeColors.TOAST_WARN_BORDER;
                    icon = "⚠";
                    break;
                default: // INFO
                    bgColor = ThemeColors.TOAST_INFO_BG;
                    borderColor = ThemeColors.TOAST_INFO_BORDER;
                    icon = "ℹ";
                    break;
            }

            // The panel draws a rounded rectangle with a colored left border
            JPanel panel = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    // White rounded background
                    g2.setColor(bgColor);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    // Colored left accent strip
                    g2.setColor(borderColor);
                    g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

            // Icon label
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            iconLabel.setForeground(borderColor);

            // Message label
            JLabel msgLabel = new JLabel("<html>" + message + "</html>");
            msgLabel.setFont(ThemeColors.FONT_BODY);
            msgLabel.setForeground(ThemeColors.TEXT_PRIMARY);

            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(msgLabel,  BorderLayout.CENTER);
            return panel;
        }
    }
}
