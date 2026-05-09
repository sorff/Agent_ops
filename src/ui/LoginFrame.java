package ui;

import db.DatabaseHelper;
import models.User;
import utils.NotificationHelper;
import utils.ThemeColors;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.prefs.Preferences;

/**
 * The first screen users see when they open AgentOps.
 *
 * Features:
 *   • Email and password fields with validation
 *   • "Remember me" checkbox (saves email using Java Preferences)
 *   • Visual error messages shown inline (no popups)
 *   • After successful login, opens MainFrame (or OnboardingWizard on first login)
 *
 * Layout: centered card on a dark gradient background
 */
public class LoginFrame extends JFrame {

    // ── Preferences key — saves "remember me" email between sessions ──
    private static final String PREF_EMAIL    = "agentops_email";
    private static final String PREF_REMEMBER = "agentops_remember";

    private JTextField     emailField;
    private JPasswordField passwordField;
    private JCheckBox      rememberMeCheck;
    private JLabel         errorLabel;
    private JButton        loginButton;

    private final Preferences prefs = Preferences.userRoot().node("agentops");

    public LoginFrame() {
        setTitle("AgentOps — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);      // center on screen
        setResizable(false);
        setUndecorated(false);            // keep the title bar

        buildUI();

        // Pre-fill remembered email if the checkbox was checked last time
        if (prefs.getBoolean(PREF_REMEMBER, false)) {
            emailField.setText(prefs.get(PREF_EMAIL, ""));
            rememberMeCheck.setSelected(true);
            passwordField.requestFocusInWindow();
        }
    }

    /** Builds the entire login screen layout */
    private void buildUI() {
        // The root panel paints a dark gradient background
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Dark gradient from navy to deep indigo
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(0x0F, 0x0C, 0x29),
                    getWidth(), getHeight(), new Color(0x13, 0x02, 0x2E)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        // ── Left side: branding ────────────────────────────────
        JPanel brandPanel = buildBrandPanel();

        // ── Right side: login card ─────────────────────────────
        JPanel cardPanel = buildLoginCard();

        // Add both sides side by side
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.weightx = 0.5;
        gbc.gridx   = 0;
        root.add(brandPanel, gbc);

        gbc.weightx = 0.5;
        gbc.gridx   = 1;
        root.add(cardPanel, gbc);

        setContentPane(root);
    }

    /** Builds the left-side branding panel with the app name and tagline */
    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // App logo — big emoji as a placeholder icon
        JLabel logo = new JLabel("⚡");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // App name
        JLabel appName = new JLabel("AgentOps");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 42));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Tagline
        JLabel tagline = new JLabel("Business AI Workforce Management");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tagline.setForeground(new Color(0xC4, 0xB5, 0xFD));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Feature bullets
        String[] features = {
            "🤖  Hire and manage AI agents",
            "📊  Track KPIs in real time",
            "✅  Human-in-the-loop approvals",
            "🏢  Full department hierarchy"
        };

        JPanel bulletPanel = new JPanel();
        bulletPanel.setLayout(new BoxLayout(bulletPanel, BoxLayout.Y_AXIS));
        bulletPanel.setOpaque(false);
        bulletPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        for (String feature : features) {
            JLabel bullet = new JLabel(feature);
            bullet.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            bullet.setForeground(new Color(0xA5, 0xB4, 0xFC));
            bullet.setAlignmentX(Component.LEFT_ALIGNMENT);
            bullet.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            bulletPanel.add(bullet);
        }
        bulletPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(logo);
        content.add(Box.createVerticalStrut(12));
        content.add(appName);
        content.add(Box.createVerticalStrut(6));
        content.add(tagline);
        content.add(Box.createVerticalStrut(30));
        content.add(bulletPanel);

        panel.add(content);
        return panel;
    }

    /** Builds the right-side white login card with input fields */
    private JPanel buildLoginCard() {
        // Outer panel centers the card vertically + horizontally
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        // The card itself — white rounded rectangle
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 44, 40, 44));
        card.setPreferredSize(new Dimension(400, 460));

        // ── Card header ───────────────────────────────────────
        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcome.setForeground(ThemeColors.TEXT_PRIMARY);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to your AgentOps dashboard");
        subtitle.setFont(ThemeColors.FONT_BODY);
        subtitle.setForeground(ThemeColors.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Email field ───────────────────────────────────────
        JLabel emailLabel = fieldLabel("Email address");
        emailField = createTextField("admin@agentops.com");

        // ── Password field ────────────────────────────────────
        JLabel passLabel = fieldLabel("Password");
        passwordField = new JPasswordField();
        styleTextField(passwordField, "••••••••");
        passwordField.addActionListener(e -> attemptLogin()); // Enter key logs in

        // ── Remember me ───────────────────────────────────────
        rememberMeCheck = new JCheckBox("Remember me");
        rememberMeCheck.setFont(ThemeColors.FONT_BODY);
        rememberMeCheck.setForeground(ThemeColors.TEXT_SECONDARY);
        rememberMeCheck.setOpaque(false);
        rememberMeCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Error message (hidden until needed) ───────────────
        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeColors.FONT_SMALL);
        errorLabel.setForeground(ThemeColors.STATUS_ERROR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Login button ──────────────────────────────────────
        loginButton = createLoginButton();

        // ── Demo hint ─────────────────────────────────────────
        JLabel hint = new JLabel("Demo: admin@agentops.com / admin123");
        hint.setFont(ThemeColors.FONT_SMALL);
        hint.setForeground(ThemeColors.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Assemble card ─────────────────────────────────────
        card.add(welcome);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(emailLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(emailField);
        card.add(Box.createVerticalStrut(16));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(10));
        card.add(rememberMeCheck);
        card.add(Box.createVerticalStrut(8));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(20));
        card.add(hint);

        outer.add(card);
        return outer;
    }

    /** Creates a styled label for form fields */
    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeColors.FONT_BOLD);
        label.setForeground(ThemeColors.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Creates a styled text input field with placeholder text */
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        styleTextField(field, placeholder);
        return field;
    }

    /** Applies consistent styling to any text input (JTextField or JPasswordField) */
    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(ThemeColors.FONT_BODY);
        field.setForeground(ThemeColors.TEXT_SECONDARY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Focus effect — change border color when the field is active
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeColors.ACCENT, 2, true),
                    BorderFactory.createEmptyBorder(9, 13, 9, 13)
                ));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
                ));
            }
        });
    }

    /** Creates the main login button with a hover effect */
    private JButton createLoginButton() {
        JButton btn = new JButton("Sign In") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(0x43, 0x38, 0xCA));
                } else if (getModel().isRollover()) {
                    g2.setColor(ThemeColors.ACCENT_LIGHT);
                } else {
                    g2.setColor(ThemeColors.ACCENT);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> attemptLogin());
        return btn;
    }

    /**
     * Validates the form and attempts to log in via DatabaseHelper.
     * Shows an error message if login fails, opens MainFrame if it succeeds.
     */
    private void attemptLogin() {
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Basic form validation
        if (email.isEmpty()) {
            showError("Please enter your email address.");
            emailField.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            passwordField.requestFocusInWindow();
            return;
        }

        // Change button text while waiting
        loginButton.setEnabled(false);
        loginButton.repaint();

        // Run DB query in a background thread so the UI doesn't freeze
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return DatabaseHelper.getInstance().login(email, password);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    User user = get();
                    if (user != null) {
                        handleSuccessfulLogin(user);
                    } else {
                        showError("Incorrect email or password. Try: admin@agentops.com / admin123");
                        passwordField.setText("");
                        passwordField.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    showError("Connection error. Is the database running?");
                }
            }
        };
        worker.execute();
    }

    /** Called after a successful login — saves prefs and opens the main app */
    private void handleSuccessfulLogin(User user) {
        // Handle "Remember me"
        if (rememberMeCheck.isSelected()) {
            prefs.put(PREF_EMAIL, emailField.getText().trim());
            prefs.putBoolean(PREF_REMEMBER, true);
        } else {
            prefs.remove(PREF_EMAIL);
            prefs.putBoolean(PREF_REMEMBER, false);
        }

        NotificationHelper.showSuccess("Welcome back, " + user.getName() + "!");

        // Open the main application window
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
            dispose(); // close the login window
        });
    }

    /** Displays an error message below the form fields */
    private void showError(String message) {
        errorLabel.setText("⚠  " + message);
        // Shake the card briefly — gives visual feedback that something's wrong
        shakeComponent(loginButton);
    }

    /** Simple horizontal shake animation for the login button */
    private void shakeComponent(JComponent comp) {
        int originalX = comp.getLocation().x;
        Timer shake = new Timer(50, null);
        int[] moves = {-6, 6, -4, 4, -2, 2, 0};
        final int[] step = {0};
        shake.addActionListener(e -> {
            if (step[0] < moves.length) {
                comp.setLocation(originalX + moves[step[0]], comp.getLocation().y);
                step[0]++;
            } else {
                comp.setLocation(originalX, comp.getLocation().y);
                shake.stop();
            }
        });
        shake.start();
    }
}
