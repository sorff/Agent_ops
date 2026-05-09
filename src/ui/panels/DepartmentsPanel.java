package ui.panels;

import db.DatabaseHelper;
import models.Agent;
import models.Department;
import models.Goal;
import models.User;
import ui.MainFrame;
import ui.components.RoundedPanel;
import ui.components.StatusDot;
import utils.DateUtils;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Departments overview page.
 *
 * Main view: a grid of department cards, each showing name, agent count,
 * budget, health status, and goal progress.
 *
 * Click a card → slides into Department Detail view showing:
 *   • Roster of agents with status
 *   • Department goals with progress bars
 *   • Budget usage
 */
public class DepartmentsPanel extends JPanel {

    private final User currentUser;
    private final MainFrame mainFrame;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    // We use CardLayout to switch between the list view and a detail view
    private CardLayout layout;
    private JPanel container;

    // Keep a reference to the list panel so refreshData() can rebuild it
    private JPanel deptGridPanel;

    public DepartmentsPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame   = mainFrame;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout());

        layout    = new CardLayout();
        container = new JPanel(layout);
        container.setBackground(ThemeColors.CONTENT_BG);

        container.add(buildListView(), "LIST");
        add(container, BorderLayout.CENTER);

        layout.show(container, "LIST");
    }

    // ── List view: all departments as cards ───────────────────

    private JPanel buildListView() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeColors.CONTENT_BG);
        header.setBorder(BorderFactory.createEmptyBorder(24, 24, 12, 24));

        JLabel title = new JLabel("Departments");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JButton addBtn = accentButton("+ New Department");
        addBtn.addActionListener(e -> showNewDeptDialog());

        header.add(title,  BorderLayout.WEST);
        if (currentUser.isAdmin()) header.add(addBtn, BorderLayout.EAST);

        // Grid of dept cards
        deptGridPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        deptGridPanel.setBackground(ThemeColors.CONTENT_BG);
        deptGridPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        JScrollPane scroll = new JScrollPane(deptGridPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    /** Reloads department data and rebuilds the grid */
    public void refreshData() {
        deptGridPanel.removeAll();
        List<Department> departments = db.getAllDepartments();

        String[] healthStatuses = {"Healthy", "Healthy", "Warning", "Healthy", "Critical", "Warning"};
        String[] emojis = {"💼", "📣", "🎧", "👥", "💰", "🚚"};
        int idx = 0;

        for (Department dept : departments) {
            String health = idx < healthStatuses.length ? healthStatuses[idx] : "Healthy";
            String emoji  = idx < emojis.length ? emojis[idx] : "🏢";
            deptGridPanel.add(buildDeptCard(dept, health, emoji));
            idx++;
        }

        if (departments.isEmpty()) {
            JLabel empty = new JLabel("No departments yet. Click 'New Department' to get started.");
            empty.setFont(ThemeColors.FONT_BODY);
            empty.setForeground(ThemeColors.TEXT_MUTED);
            deptGridPanel.add(empty);
        }

        deptGridPanel.revalidate();
        deptGridPanel.repaint();
    }

    /** Builds one department summary card */
    private JPanel buildDeptCard(Department dept, String health, String emoji) {
        RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── Top row: emoji icon + health badge ────────────────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        // Big emoji in a colored badge
        JPanel iconBadge = createIconBadge(emoji, dept.getId());
        topRow.add(iconBadge, BorderLayout.WEST);

        Color hColor = "Healthy".equals(health) ? ThemeColors.STATUS_ACTIVE
                     : "Warning".equals(health)  ? ThemeColors.STATUS_IDLE
                     :                             ThemeColors.STATUS_ERROR;
        JLabel healthBadge = pillLabel(health, hColor,
            new Color(hColor.getRed(), hColor.getGreen(), hColor.getBlue(), 20));
        topRow.add(healthBadge, BorderLayout.EAST);

        // ── Dept name ─────────────────────────────────────────
        JLabel nameLabel = new JLabel(dept.getName());
        nameLabel.setFont(ThemeColors.FONT_SUBTITLE);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // ── Stats row ─────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        statsRow.setOpaque(false);

        statsRow.add(statBlock("Agents", String.valueOf(dept.getAgentCount())));
        statsRow.add(statBlock("Budget", "$" + formatBudget(dept.getBudget())));

        // ── Goal progress bar ─────────────────────────────────
        List<Goal> goals = db.getGoalsByDepartment(dept.getId());
        JPanel goalsPanel = buildGoalSummary(goals);

        card.add(topRow,    BorderLayout.NORTH);
        card.add(nameLabel, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(statsRow);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(goalsPanel);
        card.add(bottom, BorderLayout.SOUTH);

        // Click → open detail
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openDeptDetail(dept); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackgroundColor(new Color(0xF5, 0xF3, 0xFF)); }
            @Override public void mouseExited(MouseEvent e)  { card.setBackgroundColor(ThemeColors.CARD_BG); }
        });

        return card;
    }

    /** Shows a short summary of dept goals (first 2 goals) */
    private JPanel buildGoalSummary(List<Goal> goals) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        int shown = 0;
        for (Goal g : goals) {
            if (shown++ >= 2) break;
            JLabel goalLabel = new JLabel(g.getTitle());
            goalLabel.setFont(ThemeColors.FONT_SMALL);
            goalLabel.setForeground(ThemeColors.TEXT_MUTED);
            goalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel barBg = progressBar((int) g.getProgressPercent(), ThemeColors.ACCENT);
            barBg.setAlignmentX(Component.LEFT_ALIGNMENT);

            panel.add(goalLabel);
            panel.add(Box.createVerticalStrut(3));
            panel.add(barBg);
            panel.add(Box.createVerticalStrut(6));
        }
        return panel;
    }

    // ── Department Detail View ────────────────────────────────

    /** Opens the detail view for a specific department */
    private void openDeptDetail(Department dept) {
        JPanel detail = buildDetailView(dept);
        container.add(detail, "DETAIL_" + dept.getId());
        layout.show(container, "DETAIL_" + dept.getId());
    }

    private JPanel buildDetailView(Department dept) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(ThemeColors.CONTENT_BG);

        // Header with back button
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(ThemeColors.CONTENT_BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JButton backBtn = new JButton("← Back to Departments");
        backBtn.setFont(ThemeColors.FONT_BODY);
        backBtn.setForeground(ThemeColors.ACCENT);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            layout.show(container, "LIST");
            refreshData();
        });

        JLabel deptTitle = new JLabel(dept.getName() + " Department");
        deptTitle.setFont(ThemeColors.FONT_TITLE);
        deptTitle.setForeground(ThemeColors.TEXT_PRIMARY);

        header.add(backBtn,   BorderLayout.NORTH);
        header.add(deptTitle, BorderLayout.CENTER);

        // Content area: two columns
        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setBackground(ThemeColors.CONTENT_BG);
        content.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        content.add(buildAgentRoster(dept));
        content.add(buildDeptGoals(dept));

        panel.add(header,  BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /** Roster of agents in the department */
    private JPanel buildAgentRoster(Department dept) {
        RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Agent Roster (" + dept.getAgentCount() + ")");
        title.setFont(ThemeColors.FONT_SUBTITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JPanel rosterPanel = new JPanel();
        rosterPanel.setLayout(new BoxLayout(rosterPanel, BoxLayout.Y_AXIS));
        rosterPanel.setOpaque(false);

        List<Agent> agents = db.getAgentsByDepartment(dept.getId());
        for (Agent ag : agents) {
            rosterPanel.add(buildAgentRow(ag));
            rosterPanel.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(rosterPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CARD_BG);

        card.add(title,  BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** One agent row in the roster */
    private JPanel buildAgentRow(Agent ag) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(new Color(0xF9, 0xFA, 0xFB));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new StatusDot(ag.getStatus(), 9));

        JLabel nameLabel = new JLabel(ag.getName().split("—")[0].trim());
        nameLabel.setFont(ThemeColors.FONT_BOLD);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        left.add(nameLabel);

        JLabel roleLabel = new JLabel(ag.getRoleLabel());
        roleLabel.setFont(ThemeColors.FONT_SMALL);
        roleLabel.setForeground(ThemeColors.TEXT_MUTED);

        row.add(left,      BorderLayout.WEST);
        row.add(roleLabel, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.openAgentDetail(ag.getId());
            }
            @Override public void mouseEntered(MouseEvent e) {
                row.setBackground(ThemeColors.ACCENT_VERY_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setBackground(new Color(0xF9, 0xFA, 0xFB));
            }
        });
        return row;
    }

    /** Goals section with progress bars */
    private JPanel buildDeptGoals(Department dept) {
        RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Department Goals");
        title.setFont(ThemeColors.FONT_SUBTITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JPanel goalsPanel = new JPanel();
        goalsPanel.setLayout(new BoxLayout(goalsPanel, BoxLayout.Y_AXIS));
        goalsPanel.setOpaque(false);

        List<Goal> goals = db.getGoalsByDepartment(dept.getId());
        for (Goal g : goals) {
            goalsPanel.add(buildGoalRow(g));
            goalsPanel.add(Box.createVerticalStrut(16));
        }

        if (goals.isEmpty()) {
            JLabel empty = new JLabel("No goals set for this department.");
            empty.setFont(ThemeColors.FONT_BODY);
            empty.setForeground(ThemeColors.TEXT_MUTED);
            goalsPanel.add(empty);
        }

        JScrollPane scroll = new JScrollPane(goalsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CARD_BG);

        card.add(title,  BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** One goal with a progress bar */
    private JPanel buildGoalRow(Goal g) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(g.getTitle());
        titleLabel.setFont(ThemeColors.FONT_BOLD);
        titleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        int pct = (int) g.getProgressPercent();
        JLabel pctLabel = new JLabel(pct + "%");
        pctLabel.setFont(ThemeColors.FONT_SMALL);
        pctLabel.setForeground(pct >= 80 ? ThemeColors.STATUS_ACTIVE
                             : pct >= 50  ? ThemeColors.STATUS_IDLE
                             :              ThemeColors.STATUS_ERROR);
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(pctLabel,   BorderLayout.EAST);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JPanel bar = progressBar(pct, pct >= 80 ? ThemeColors.STATUS_ACTIVE
                                     : pct >= 50  ? ThemeColors.STATUS_IDLE
                                     :              ThemeColors.STATUS_ERROR);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel deadlineLabel = new JLabel("Deadline: " + DateUtils.formatDate(g.getDeadline()));
        deadlineLabel.setFont(ThemeColors.FONT_SMALL);
        deadlineLabel.setForeground(ThemeColors.TEXT_MUTED);
        deadlineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(titleRow);
        row.add(Box.createVerticalStrut(4));
        row.add(bar);
        row.add(Box.createVerticalStrut(3));
        row.add(deadlineLabel);
        return row;
    }

    // ── Helper components ─────────────────────────────────────

    private JPanel createIconBadge(String emoji, int deptId) {
        Color color = ThemeColors.DEPT_COLORS[Math.abs(deptId - 1) % ThemeColors.DEPT_COLORS.length];
        JPanel badge = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(44, 44));
        JLabel lbl = new JLabel(emoji);
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        badge.add(lbl);
        return badge;
    }

    private JLabel pillLabel(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel("  " + text + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(ThemeColors.FONT_SMALL);
        lbl.setForeground(fg);
        lbl.setOpaque(false);
        return lbl;
    }

    private JPanel statBlock(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 18));
        val.setForeground(ThemeColors.TEXT_PRIMARY);
        JLabel lbl = new JLabel(label);
        lbl.setFont(ThemeColors.FONT_SMALL);
        lbl.setForeground(ThemeColors.TEXT_MUTED);
        p.add(val);
        p.add(lbl);
        return p;
    }

    /** Draws a thin horizontal progress bar */
    private JPanel progressBar(int percent, Color fillColor) {
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeColors.BORDER);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 4, 4));
                int fillW = (int)(getWidth() * Math.min(percent, 100) / 100.0);
                g2.setColor(fillColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, fillW, getHeight(), 4, 4));
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 6));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        return bar;
    }

    private JButton accentButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ThemeColors.ACCENT_LIGHT : ThemeColors.ACCENT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(Color.WHITE);
                g2.setFont(ThemeColors.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                              (getHeight()-fm.getHeight())/2+fm.getAscent());
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(160, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showNewDeptDialog() {
        JTextField nameField = new JTextField();
        JTextField descField = new JTextField();
        Object[] fields = {"Department Name:", nameField, "Description:", descField};
        int r = JOptionPane.showConfirmDialog(this, fields,
            "New Department", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            db.createDepartment(nameField.getText().trim(), descField.getText().trim(), "CUSTOM", 0);
            refreshData();
        }
    }

    private String formatBudget(double budget) {
        if (budget >= 1000) return String.format("%.0fK", budget / 1000);
        return String.format("%.0f", budget);
    }
}
