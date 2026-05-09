package ui.panels;

import db.DatabaseHelper;
import models.User;
import ui.components.RoundedPanel;
import utils.NotificationHelper;
import utils.ThemeColors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.security.MessageDigest;
import java.util.List;

/**
 * Settings page — user profile, notifications, working hours, and admin tools.
 *
 * Sections:
 *   1. Profile  — edit name, email, change password
 *   2. Notifications — toggles for email/desktop/weekly digest
 *   3. Working Hours — operational hours for agent simulation
 *   4. Role Management (Admin only) — create / deactivate supervisor accounts
 *   5. Danger Zone (Admin only) — reset simulation data
 */
public class SettingsPanel extends JPanel {

    private final User currentUser;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    // Profile fields
    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField currentPassField;
    private JPasswordField newPassField;
    private JPasswordField confirmPassField;

    // Notification toggles
    private JCheckBox emailAlertsBox;
    private JCheckBox desktopAlertsBox;
    private JCheckBox weeklyDigestBox;
    private JCheckBox criticalOnlyBox;

    // Working hours
    private JComboBox<String> startHourBox;
    private JComboBox<String> endHourBox;
    private JCheckBox[] workDayBoxes;

    public SettingsPanel(User currentUser) {
        this.currentUser = currentUser;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(ThemeColors.CONTENT_BG);
        scrollContent.setBorder(BorderFactory.createEmptyBorder(0, 24, 32, 24));

        scrollContent.add(buildProfileSection());
        scrollContent.add(Box.createVerticalStrut(20));
        scrollContent.add(buildNotificationsSection());
        scrollContent.add(Box.createVerticalStrut(20));
        scrollContent.add(buildWorkingHoursSection());

        if (currentUser.isAdmin()) {
            scrollContent.add(Box.createVerticalStrut(20));
            scrollContent.add(buildRoleManagementSection());
            scrollContent.add(Box.createVerticalStrut(20));
            scrollContent.add(buildDangerZoneSection());
        }

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        wrapper.add(scroll, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── Header ────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeColors.CONTENT_BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel title = new JLabel("Settings");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sub = new JLabel("Manage your profile, notifications, and system preferences.");
        sub.setFont(ThemeColors.FONT_BODY);
        sub.setForeground(ThemeColors.TEXT_SECONDARY);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(sub,   BorderLayout.SOUTH);

        header.add(top, BorderLayout.WEST);
        return header;
    }

    // ── Profile Section ───────────────────────────────────────

    private JPanel buildProfileSection() {
        JPanel section = sectionCard("👤  Profile");

        // Avatar row
        JPanel avatarRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        avatarRow.setOpaque(false);

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeColors.ACCENT);
                g2.fillOval(0, 0, 64, 64);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                String initials = getInitials(currentUser.getName());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (64 - fm.stringWidth(initials)) / 2,
                        (64 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(64, 64));
        avatar.setOpaque(false);

        JPanel avatarInfo = new JPanel();
        avatarInfo.setLayout(new BoxLayout(avatarInfo, BoxLayout.Y_AXIS));
        avatarInfo.setOpaque(false);
        JLabel nameDisplay = new JLabel(currentUser.getName());
        nameDisplay.setFont(ThemeColors.FONT_SUBTITLE);
        nameDisplay.setForeground(ThemeColors.TEXT_PRIMARY);
        JLabel roleDisplay = new JLabel(currentUser.getRole() + " · " + currentUser.getEmail());
        roleDisplay.setFont(ThemeColors.FONT_SMALL);
        roleDisplay.setForeground(ThemeColors.TEXT_MUTED);
        avatarInfo.add(nameDisplay);
        avatarInfo.add(Box.createVerticalStrut(4));
        avatarInfo.add(roleDisplay);

        avatarRow.add(avatar);
        avatarRow.add(avatarInfo);

        // Form fields
        nameField  = styledTextField(currentUser.getName());
        emailField = styledTextField(currentUser.getEmail());
        emailField.setEditable(false);
        emailField.setForeground(ThemeColors.TEXT_MUTED);

        JPanel formGrid = new JPanel(new GridLayout(0, 2, 16, 12));
        formGrid.setOpaque(false);
        formGrid.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        formGrid.add(labeledField("Display Name", nameField));
        formGrid.add(labeledField("Email Address", emailField));

        // Password change sub-section
        JLabel passHeader = new JLabel("Change Password");
        passHeader.setFont(ThemeColors.FONT_BOLD);
        passHeader.setForeground(ThemeColors.TEXT_PRIMARY);
        passHeader.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));

        currentPassField = styledPasswordField();
        newPassField     = styledPasswordField();
        confirmPassField = styledPasswordField();

        JPanel passGrid = new JPanel(new GridLayout(0, 3, 16, 12));
        passGrid.setOpaque(false);
        passGrid.add(labeledField("Current Password", currentPassField));
        passGrid.add(labeledField("New Password",     newPassField));
        passGrid.add(labeledField("Confirm Password", confirmPassField));

        JButton saveProfileBtn = primaryButton("Save Changes");
        saveProfileBtn.addActionListener(e -> saveProfile());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        btnRow.add(saveProfileBtn);

        section.add(avatarRow);
        section.add(formGrid);
        section.add(passHeader);
        section.add(passGrid);
        section.add(btnRow);
        return section;
    }

    private void saveProfile() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            NotificationHelper.showWarning("Name cannot be empty.");
            return;
        }

        db.updateUserName(currentUser.getId(), newName);

        // Handle password change if the user filled the fields
        String curPass     = new String(currentPassField.getPassword());
        String newPass     = new String(newPassField.getPassword());
        String confirmPass = new String(confirmPassField.getPassword());

        if (!curPass.isEmpty() || !newPass.isEmpty()) {
            if (!md5(curPass).equals(currentUser.getPasswordHash())) {
                NotificationHelper.showError("Current password is incorrect.");
                return;
            }
            if (newPass.length() < 6) {
                NotificationHelper.showWarning("New password must be at least 6 characters.");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                NotificationHelper.showWarning("Passwords do not match.");
                return;
            }
            db.updateUserPassword(currentUser.getId(), md5(newPass));
            currentPassField.setText("");
            newPassField.setText("");
            confirmPassField.setText("");
        }

        db.logActivity("USER", currentUser.getId(), "Updated profile settings", null);
        NotificationHelper.showSuccess("Profile saved successfully.");
    }

    // ── Notifications Section ─────────────────────────────────

    private JPanel buildNotificationsSection() {
        JPanel section = sectionCard("🔔  Notifications");

        emailAlertsBox   = styledCheckBox("Email alerts for critical events",    true);
        desktopAlertsBox = styledCheckBox("Desktop toast notifications",          true);
        weeklyDigestBox  = styledCheckBox("Weekly performance digest report",     false);
        criticalOnlyBox  = styledCheckBox("Only notify for CRITICAL severity alerts", false);

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 8));
        grid.setOpaque(false);
        grid.add(emailAlertsBox);
        grid.add(desktopAlertsBox);
        grid.add(weeklyDigestBox);
        grid.add(criticalOnlyBox);

        JButton saveBtn = primaryButton("Save Preferences");
        saveBtn.addActionListener(e -> {
            db.logActivity("USER", currentUser.getId(), "Updated notification preferences", null);
            NotificationHelper.showSuccess("Notification preferences saved.");
        });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        btnRow.add(saveBtn);

        section.add(grid);
        section.add(btnRow);
        return section;
    }

    // ── Working Hours Section ─────────────────────────────────

    private JPanel buildWorkingHoursSection() {
        JPanel section = sectionCard("🕐  Agent Working Hours");

        JLabel desc = new JLabel("Set the operational hours during which agents are actively simulated.");
        desc.setFont(ThemeColors.FONT_BODY);
        desc.setForeground(ThemeColors.TEXT_SECONDARY);
        desc.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) {
            hours[i] = String.format("%02d:00", i);
        }
        startHourBox = new JComboBox<>(hours);
        startHourBox.setSelectedIndex(9); // 09:00 default
        endHourBox   = new JComboBox<>(hours);
        endHourBox.setSelectedIndex(18);  // 18:00 default
        styleComboBox(startHourBox);
        styleComboBox(endHourBox);

        JPanel hourRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        hourRow.setOpaque(false);
        hourRow.add(labeledField("Start Time", startHourBox));
        hourRow.add(labeledField("End Time",   endHourBox));

        // Work days
        JPanel daysRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        daysRow.setOpaque(false);
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        boolean[] defaults = {true, true, true, true, true, false, false};
        workDayBoxes = new JCheckBox[days.length];
        for (int i = 0; i < days.length; i++) {
            workDayBoxes[i] = styledCheckBox(days[i], defaults[i]);
            daysRow.add(workDayBoxes[i]);
        }

        JButton saveBtn = primaryButton("Save Hours");
        saveBtn.addActionListener(e -> {
            db.logActivity("USER", currentUser.getId(), "Updated working hours settings", null);
            NotificationHelper.showSuccess("Working hours updated.");
        });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        btnRow.add(saveBtn);

        section.add(desc);
        section.add(hourRow);
        section.add(daysRow);
        section.add(btnRow);
        return section;
    }

    // ── Role Management Section (Admin only) ──────────────────

    private JPanel buildRoleManagementSection() {
        JPanel section = sectionCard("👥  User Management  (Admin)");

        JLabel desc = new JLabel("Create supervisor accounts or manage existing users.");
        desc.setFont(ThemeColors.FONT_BODY);
        desc.setForeground(ThemeColors.TEXT_SECONDARY);
        desc.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Existing users table
        List<models.User> users = db.getAllUsers();
        String[] cols = {"Name", "Email", "Role", "Joined"};
        Object[][] rows = users.stream().map(u -> new Object[]{
            u.getName(), u.getEmail(), u.getRole(),
            u.getCreatedAt() != null ? u.getCreatedAt().toString().substring(0, 10) : "—"
        }).toArray(Object[][]::new);

        JTable userTable = new JTable(rows, cols) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        userTable.setFont(ThemeColors.FONT_BODY);
        userTable.setForeground(ThemeColors.TEXT_PRIMARY);
        userTable.setBackground(ThemeColors.CARD_BG);
        userTable.setGridColor(ThemeColors.BORDER);
        userTable.setRowHeight(32);
        userTable.getTableHeader().setFont(ThemeColors.FONT_BOLD);
        userTable.getTableHeader().setBackground(new Color(0xF9, 0xFA, 0xFB));
        JScrollPane tableScroll = new JScrollPane(userTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true));
        tableScroll.setPreferredSize(new Dimension(0, 140));

        // Create new supervisor form
        JLabel newUserLabel = new JLabel("Create New Supervisor");
        newUserLabel.setFont(ThemeColors.FONT_BOLD);
        newUserLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        newUserLabel.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));

        JTextField newNameField  = styledTextField("");
        JTextField newEmailField = styledTextField("");
        JPasswordField newPasswordField = styledPasswordField();
        newNameField.putClientProperty("placeholder", "Full Name");
        newEmailField.putClientProperty("placeholder", "Email Address");

        JPanel newUserGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        newUserGrid.setOpaque(false);
        newUserGrid.add(labeledField("Name",     newNameField));
        newUserGrid.add(labeledField("Email",    newEmailField));
        newUserGrid.add(labeledField("Password", newPasswordField));

        JButton createBtn = primaryButton("Create Supervisor");
        createBtn.addActionListener(e -> {
            String name  = newNameField.getText().trim();
            String email = newEmailField.getText().trim();
            String pass  = new String(newPasswordField.getPassword());
            if (name.isEmpty() || email.isEmpty() || pass.length() < 6) {
                NotificationHelper.showWarning("Please fill all fields (password ≥ 6 chars).");
                return;
            }
            db.createUser(name, email, md5(pass), "SUPERVISOR");
            db.logActivity("USER", currentUser.getId(), "Created supervisor account: " + email, null);
            newNameField.setText("");
            newEmailField.setText("");
            newPasswordField.setText("");
            NotificationHelper.showSuccess("Supervisor account created for " + email);
        });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        btnRow.add(createBtn);

        section.add(desc);
        section.add(tableScroll);
        section.add(newUserLabel);
        section.add(newUserGrid);
        section.add(btnRow);
        return section;
    }

    // ── Danger Zone Section (Admin only) ──────────────────────

    private JPanel buildDangerZoneSection() {
        RoundedPanel section = new RoundedPanel(12, new Color(0xFF, 0xF5, 0xF5));
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xFF, 0xCC, 0xCC), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel header = new JLabel("⚠️  Danger Zone");
        header.setFont(ThemeColors.FONT_SUBTITLE);
        header.setForeground(ThemeColors.STATUS_ERROR);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel desc = new JLabel("<html>These actions are irreversible. Use with caution in production environments.</html>");
        desc.setFont(ThemeColors.FONT_BODY);
        desc.setForeground(ThemeColors.TEXT_SECONDARY);
        desc.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JButton resetAlertsBtn  = dangerButton("Clear All Alerts");
        JButton resetLogsBtn    = dangerButton("Clear Activity Log");
        JButton resetAgentsBtn  = dangerButton("Reset All Agent Statuses");

        resetAlertsBtn.addActionListener(e -> confirmAndRun(
            "Clear ALL system alerts?",
            () -> { db.clearAllAlerts(); NotificationHelper.showSuccess("All alerts cleared."); }
        ));

        resetLogsBtn.addActionListener(e -> confirmAndRun(
            "Clear ALL activity logs?",
            () -> { db.clearActivityLog(); NotificationHelper.showSuccess("Activity log cleared."); }
        ));

        resetAgentsBtn.addActionListener(e -> confirmAndRun(
            "Reset ALL agents to IDLE status?",
            () -> { db.resetAllAgentStatuses(); NotificationHelper.showSuccess("Agent statuses reset to IDLE."); }
        ));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.add(resetAlertsBtn);
        btnRow.add(resetLogsBtn);
        btnRow.add(resetAgentsBtn);

        section.add(header);
        section.add(desc);
        section.add(btnRow);
        return section;
    }

    private void confirmAndRun(String message, Runnable action) {
        int choice = JOptionPane.showConfirmDialog(this,
            message + "\nThis cannot be undone.",
            "Confirm Action", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            action.run();
        }
    }

    // ── refreshData (called by MainFrame on tab switch) ───────

    public void refreshData() {
        // Fields are pre-populated in constructor; nothing to reload
    }

    // ── Helpers ───────────────────────────────────────────────

    private JPanel sectionCard(String title) {
        RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel header = new JLabel(title);
        header.setFont(ThemeColors.FONT_SUBTITLE);
        header.setForeground(ThemeColors.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        return card;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(ThemeColors.FONT_SMALL);
        lbl.setForeground(ThemeColors.TEXT_SECONDARY);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl,   BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JTextField styledTextField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(ThemeColors.FONT_BODY);
        field.setForeground(ThemeColors.TEXT_PRIMARY);
        field.setBackground(new Color(0xF9, 0xFA, 0xFB));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(0, 36));
        return field;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(ThemeColors.FONT_BODY);
        field.setBackground(new Color(0xF9, 0xFA, 0xFB));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(0, 36));
        return field;
    }

    private JCheckBox styledCheckBox(String label, boolean selected) {
        JCheckBox box = new JCheckBox(label, selected);
        box.setFont(ThemeColors.FONT_BODY);
        box.setForeground(ThemeColors.TEXT_SECONDARY);
        box.setOpaque(false);
        return box;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(ThemeColors.FONT_BODY);
        box.setBackground(new Color(0xF9, 0xFA, 0xFB));
        box.setPreferredSize(new Dimension(120, 36));
    }

    private JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ThemeColors.ACCENT_LIGHT : ThemeColors.ACCENT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(Color.WHITE);
                g2.setFont(ThemeColors.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(160, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton dangerButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? new Color(0xFF, 0xCC, 0xCC) : new Color(0xFF, 0xE4, 0xE4);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(ThemeColors.STATUS_ERROR);
                g2.setFont(ThemeColors.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(190, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
