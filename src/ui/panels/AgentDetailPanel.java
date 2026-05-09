package ui.panels;

import db.DatabaseHelper;
import models.Agent;
import models.Task;
import models.User;
import ui.MainFrame;
import ui.components.RoundedPanel;
import ui.components.StatusDot;
import utils.DateUtils;
import utils.NotificationHelper;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Full profile page for a single AI agent.
 *
 * Shown when you click any agent card, org-chart node, or roster row.
 *
 * Tabs:
 *   Overview      — capabilities, current task, performance score ring
 *   Task History  — table of completed/failed/in-progress tasks
 *   Outputs       — mock output documents the agent "produced"
 *   Settings      — pause/resume, autonomy level slider
 *   Logs          — recent activity log entries for this agent
 */
public class AgentDetailPanel extends JPanel {

    private final int agentId;
    private final User currentUser;
    private final MainFrame mainFrame;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private Agent agent;

    // Profile header labels — updated when agent data refreshes
    private JLabel nameLabel, roleLabel, deptLabel, statusLabel;
    private StatusDot statusDot;
    private JLabel currentTaskLabel;

    // The tab pane
    private JTabbedPane tabs;

    public AgentDetailPanel(int agentId, User currentUser, MainFrame mainFrame) {
        this.agentId     = agentId;
        this.currentUser = currentUser;
        this.mainFrame   = mainFrame;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        loadAgent();
        buildUI();
    }

    /** Loads the agent from DB — called at construction and on refresh */
    private void loadAgent() {
        agent = db.getAgentById(agentId);
    }

    private void buildUI() {
        if (agent == null) {
            add(new JLabel("Agent not found (id=" + agentId + ")"), BorderLayout.CENTER);
            return;
        }

        JPanel scrollContent = new JPanel(new BorderLayout(0, 16));
        scrollContent.setBackground(ThemeColors.CONTENT_BG);
        scrollContent.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        scrollContent.add(buildBackButton(),  BorderLayout.NORTH);
        scrollContent.add(buildProfileCard(), BorderLayout.CENTER);

        // Tabs below the profile card
        tabs = buildTabs();
        scrollContent.add(tabs, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Back button ───────────────────────────────────────────

    private JPanel buildBackButton() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JButton back = new JButton("← Back to Org Chart");
        back.setFont(ThemeColors.FONT_BODY);
        back.setForeground(ThemeColors.ACCENT);
        back.setBorderPainted(false);
        back.setContentAreaFilled(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> mainFrame.switchTo(1, "Org Chart", "ORG_CHART"));
        row.add(back);
        return row;
    }

    // ── Profile header card ───────────────────────────────────

    private JPanel buildProfileCard() {
        RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Avatar — large colored circle with initials
        JPanel avatar = buildAvatar(64);

        // Info column
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String displayName = agent.getName().contains("—")
            ? agent.getName().split("—")[0].trim() : agent.getName();
        String agentRole   = agent.getName().contains("—")
            ? agent.getName().split("—", 2)[1].trim() : agent.getRoleLabel();

        nameLabel = new JLabel(displayName);
        nameLabel.setFont(ThemeColors.FONT_TITLE);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        roleLabel = new JLabel(agentRole);
        roleLabel.setFont(ThemeColors.FONT_BODY);
        roleLabel.setForeground(ThemeColors.TEXT_SECONDARY);

        deptLabel = new JLabel("📁 " + (agent.getDepartmentName() != null
            ? agent.getDepartmentName() : "Unassigned"));
        deptLabel.setFont(ThemeColors.FONT_BODY);
        deptLabel.setForeground(ThemeColors.TEXT_MUTED);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusRow.setOpaque(false);
        statusDot = new StatusDot(agent.getStatus(), 10);
        statusLabel = new JLabel(agent.getStatus());
        statusLabel.setFont(ThemeColors.FONT_BOLD);
        statusLabel.setForeground(ThemeColors.getStatusColor(agent.getStatus()));
        statusRow.add(statusDot);
        statusRow.add(statusLabel);

        currentTaskLabel = new JLabel(agent.getCurrentTask() != null
            ? "▶  " + agent.getCurrentTask() : "No active task");
        currentTaskLabel.setFont(ThemeColors.FONT_SMALL);
        currentTaskLabel.setForeground(ThemeColors.TEXT_MUTED);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(roleLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(deptLabel);
        info.add(Box.createVerticalStrut(8));
        info.add(statusRow);
        info.add(Box.createVerticalStrut(4));
        info.add(currentTaskLabel);

        // Right: performance ring + score
        JPanel scorePanel = buildScoreRing(agent.getPerformanceScore());

        card.add(avatar,     BorderLayout.WEST);
        card.add(info,       BorderLayout.CENTER);
        card.add(scorePanel, BorderLayout.EAST);

        return card;
    }

    /** Builds a large colored avatar circle with the agent's initials */
    private JPanel buildAvatar(int size) {
        Color[] colors = ThemeColors.DEPT_COLORS;
        Color color = colors[Math.abs(agent.getDepartmentId()) % colors.length];
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size / 3));
                String init = getInitials(agent.getName());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, (size - fm.stringWidth(init))/2,
                              (size - fm.getHeight())/2 + fm.getAscent());
                g2.dispose();
            }
        };
        av.setOpaque(false);
        av.setPreferredSize(new Dimension(size, size));
        return av;
    }

    /** Draws a circular progress ring showing the performance score */
    private JPanel buildScoreRing(double score) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sz = 90;
                int x  = (getWidth()  - sz) / 2;
                int y  = (getHeight() - sz) / 2;
                // Background arc
                g2.setColor(ThemeColors.BORDER);
                g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(x, y, sz, sz, 90, -360);
                // Filled arc
                Color arcColor = score >= 85 ? ThemeColors.STATUS_ACTIVE
                               : score >= 70 ? ThemeColors.STATUS_IDLE
                               :               ThemeColors.STATUS_ERROR;
                g2.setColor(arcColor);
                int degrees = (int)(360 * score / 100.0);
                g2.drawArc(x, y, sz, sz, 90, -degrees);
                // Score text
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(ThemeColors.TEXT_PRIMARY);
                String txt = String.format("%.0f%%", score);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(txt, getWidth()/2 - fm.stringWidth(txt)/2, getHeight()/2 + fm.getAscent()/2 - 4);
                g2.setFont(ThemeColors.FONT_SMALL);
                g2.setColor(ThemeColors.TEXT_MUTED);
                String sub = "Score";
                fm = g2.getFontMetrics();
                g2.drawString(sub, getWidth()/2 - fm.stringWidth(sub)/2, getHeight()/2 + fm.getAscent() + 10);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(110, 110));
        return panel;
    }

    // ── Tabs ──────────────────────────────────────────────────

    private JTabbedPane buildTabs() {
        JTabbedPane tp = new JTabbedPane();
        tp.setFont(ThemeColors.FONT_BODY);
        tp.setBackground(ThemeColors.CONTENT_BG);
        tp.setPreferredSize(new Dimension(0, 380));

        tp.addTab("📊 Overview",      buildOverviewTab());
        tp.addTab("📋 Task History",  buildTaskHistoryTab());
        tp.addTab("📄 Outputs",       buildOutputsTab());
        tp.addTab("⚙ Settings",      buildSettingsTab());
        tp.addTab("📜 Logs",          buildLogsTab());

        return tp;
    }

    // ── Tab: Overview ─────────────────────────────────────────

    private JPanel buildOverviewTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(ThemeColors.CONTENT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Capabilities card
        RoundedPanel capsCard = new RoundedPanel(10, ThemeColors.CARD_BG);
        capsCard.setLayout(new BorderLayout(0, 8));
        capsCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel capsTitle = new JLabel("Capabilities");
        capsTitle.setFont(ThemeColors.FONT_SUBTITLE);
        capsTitle.setForeground(ThemeColors.TEXT_PRIMARY);

        JPanel capsList = new JPanel();
        capsList.setLayout(new BoxLayout(capsList, BoxLayout.Y_AXIS));
        capsList.setOpaque(false);

        List<String> caps = agent.getCapabilities();
        if (caps.isEmpty()) caps = db.getCapabilities(agentId);
        for (String cap : caps) {
            JLabel capLabel = new JLabel("✓  " + cap);
            capLabel.setFont(ThemeColors.FONT_BODY);
            capLabel.setForeground(ThemeColors.TEXT_SECONDARY);
            capLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            capsList.add(capLabel);
        }
        if (caps.isEmpty()) {
            capsList.add(new JLabel("No capabilities listed."));
        }

        capsCard.add(capsTitle, BorderLayout.NORTH);
        capsCard.add(capsList,  BorderLayout.CENTER);

        // Stats card
        RoundedPanel statsCard = new RoundedPanel(10, ThemeColors.CARD_BG);
        statsCard.setLayout(new GridLayout(3, 2, 12, 12));
        statsCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        List<Task> tasks = db.getTasksByAgent(agentId);
        long doneTasks  = tasks.stream().filter(t -> Task.STATUS_DONE.equals(t.getStatus())).count();
        long failedTasks= tasks.stream().filter(t -> Task.STATUS_FAILED.equals(t.getStatus())).count();

        statsCard.add(statBox("Performance",  String.format("%.1f%%", agent.getPerformanceScore())));
        statsCard.add(statBox("Autonomy Level", agent.getAutonomyLevel() + " / 5"));
        statsCard.add(statBox("Tasks Done",   String.valueOf(doneTasks)));
        statsCard.add(statBox("Tasks Failed", String.valueOf(failedTasks)));
        statsCard.add(statBox("Role",         agent.getRoleLabel()));
        statsCard.add(statBox("Status",       agent.getStatus()));

        panel.add(capsCard);
        panel.add(statsCard);
        return panel;
    }

    private JPanel statBox(String label, String value) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(0xF9, 0xFA, 0xFB));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valLbl.setForeground(ThemeColors.TEXT_PRIMARY);
        JLabel lblLbl = new JLabel(label);
        lblLbl.setFont(ThemeColors.FONT_SMALL);
        lblLbl.setForeground(ThemeColors.TEXT_MUTED);
        box.add(valLbl);
        box.add(lblLbl);
        return box;
    }

    // ── Tab: Task History ─────────────────────────────────────

    private JPanel buildTaskHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeColors.CONTENT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        String[] cols = {"Title", "Status", "Priority", "Deadline", "Completed"};
        List<Task> tasks = db.getTasksByAgent(agentId);
        Object[][] data  = new Object[tasks.size()][5];
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            data[i][0] = t.getTitle();
            data[i][1] = t.getStatus();
            data[i][2] = t.getPriority();
            data[i][3] = DateUtils.formatDate(t.getDeadline());
            data[i][4] = t.getCompletedAt() != null ? DateUtils.timeAgo(t.getCompletedAt()) : "—";
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ── Tab: Outputs ──────────────────────────────────────────

    private JPanel buildOutputsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeColors.CONTENT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Mock outputs based on the agent's role
        String[][] outputs = getMockOutputs();
        for (String[] output : outputs) {
            RoundedPanel card = new RoundedPanel(8, ThemeColors.CARD_BG);
            card.setLayout(new BorderLayout(12, 0));
            card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

            JLabel icon  = new JLabel(output[0]);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            JLabel title = new JLabel("<html><b>" + output[1] + "</b><br><small style='color:gray'>"
                                      + output[2] + "</small></html>");
            title.setFont(ThemeColors.FONT_BODY);
            JLabel date  = new JLabel(output[3]);
            date.setFont(ThemeColors.FONT_SMALL);
            date.setForeground(ThemeColors.TEXT_MUTED);

            card.add(icon,  BorderLayout.WEST);
            card.add(title, BorderLayout.CENTER);
            card.add(date,  BorderLayout.EAST);
            panel.add(card);
            panel.add(Box.createVerticalStrut(8));
        }
        return panel;
    }

    private String[][] getMockOutputs() {
        String dept = agent.getDepartmentName() != null ? agent.getDepartmentName() : "";
        switch (dept) {
            case "Sales": return new String[][]{
                {"📊","Q2 Pipeline Report","31 qualified leads, 8 proposals sent","2 days ago"},
                {"📧","Outreach Campaign #88","120 emails sent, 18% open rate","4 days ago"},
                {"📄","Deal Memo — Acme Corp","$48K ARR proposal drafted","1 day ago"}};
            case "Marketing": return new String[][]{
                {"📝","Blog: AI Trends 2026","1,847 words, SEO score 81","Today"},
                {"📣","LinkedIn Post Draft","Follower milestone celebration","Pending approval"},
                {"📊","Monthly Analytics Report","Traffic +12%, leads +8%","5 days ago"}};
            case "Support": return new String[][]{
                {"🎫","Ticket Resolution Report","94 tickets closed this week","Today"},
                {"📋","CSAT Survey Summary","Avg score: 4.2/5 (88%)","3 days ago"}};
            default: return new String[][]{
                {"📄","Weekly Status Report","Routine operational summary","2 days ago"},
                {"📊","Performance Dashboard","KPI metrics for review","1 week ago"}};
        }
    }

    // ── Tab: Settings ─────────────────────────────────────────

    private JPanel buildSettingsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeColors.CONTENT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Pause / Resume agent
        boolean isPaused = Agent.STATUS_PAUSED.equals(agent.getStatus());
        JButton toggleBtn = new JButton(isPaused ? "▶  Resume Agent" : "⏸  Pause Agent");
        toggleBtn.setFont(ThemeColors.FONT_BOLD);
        toggleBtn.setForeground(isPaused ? ThemeColors.STATUS_ACTIVE : ThemeColors.STATUS_IDLE);
        toggleBtn.setBackground(ThemeColors.CARD_BG);
        toggleBtn.setPreferredSize(new Dimension(180, 40));
        toggleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggleBtn.addActionListener(e -> {
            String newStatus = isPaused ? "IDLE" : "PAUSED";
            db.updateAgentStatus(agentId, newStatus, null);
            NotificationHelper.showInfo("Agent " + agent.getName().split("—")[0].trim()
                + " is now " + newStatus);
        });

        // Autonomy slider
        JLabel autoTitle = new JLabel("Autonomy Level");
        autoTitle.setFont(ThemeColors.FONT_BOLD);
        autoTitle.setForeground(ThemeColors.TEXT_PRIMARY);
        autoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel autoDesc = new JLabel("<html><small>1 = needs approval for everything · 5 = fully autonomous</small></html>");
        autoDesc.setFont(ThemeColors.FONT_SMALL);
        autoDesc.setForeground(ThemeColors.TEXT_MUTED);
        autoDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider slider = new JSlider(1, 5, agent.getAutonomyLevel());
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setFont(ThemeColors.FONT_SMALL);
        slider.setBackground(ThemeColors.CONTENT_BG);
        slider.setMaximumSize(new Dimension(300, 60));
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveBtn = new JButton("Save Settings");
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.setFont(ThemeColors.FONT_BOLD);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(ThemeColors.ACCENT);
        saveBtn.setPreferredSize(new Dimension(160, 36));
        saveBtn.addActionListener(e -> {
            db.updateAgentAutonomy(agentId, slider.getValue());
            db.logActivity("USER", currentUser.getId(), "Updated autonomy for " + agent.getName(),
                "Level set to " + slider.getValue());
            NotificationHelper.showSuccess("Autonomy level saved.");
        });

        panel.add(autoTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(autoDesc);
        panel.add(Box.createVerticalStrut(10));
        panel.add(slider);
        panel.add(Box.createVerticalStrut(20));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(16));
        panel.add(new JLabel("Agent Controls"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(toggleBtn);
        panel.add(Box.createVerticalStrut(20));
        panel.add(saveBtn);
        return panel;
    }

    // ── Tab: Logs ─────────────────────────────────────────────

    private JPanel buildLogsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeColors.CONTENT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel logList = new JPanel();
        logList.setLayout(new BoxLayout(logList, BoxLayout.Y_AXIS));
        logList.setBackground(ThemeColors.CONTENT_BG);

        // Filter to logs for this agent
        db.getRecentActivityLogs().stream()
            .filter(l -> "AGENT".equals(l.getActorType()) && l.getActorId() == agentId)
            .limit(20)
            .forEach(log -> {
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(ThemeColors.CARD_BG);
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

                JLabel actionLbl = new JLabel(log.getAction());
                actionLbl.setFont(ThemeColors.FONT_BODY);
                actionLbl.setForeground(ThemeColors.TEXT_PRIMARY);

                JLabel timeLbl = new JLabel(DateUtils.timeAgo(log.getTimestamp()));
                timeLbl.setFont(ThemeColors.FONT_SMALL);
                timeLbl.setForeground(ThemeColors.TEXT_MUTED);

                row.add(actionLbl, BorderLayout.CENTER);
                row.add(timeLbl,   BorderLayout.EAST);
                logList.add(row);
            });

        if (logList.getComponentCount() == 0) {
            JLabel empty = new JLabel("No activity logged for this agent yet.");
            empty.setFont(ThemeColors.FONT_BODY);
            empty.setForeground(ThemeColors.TEXT_MUTED);
            logList.add(empty);
        }

        panel.add(new JScrollPane(logList), BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────

    private void styleTable(JTable table) {
        table.setFont(ThemeColors.FONT_BODY);
        table.setRowHeight(36);
        table.setGridColor(ThemeColors.BORDER);
        table.setBackground(ThemeColors.CARD_BG);
        table.setForeground(ThemeColors.TEXT_PRIMARY);
        table.getTableHeader().setFont(ThemeColors.FONT_BOLD);
        table.getTableHeader().setBackground(new Color(0xF3, 0xF4, 0xF6));
        table.getTableHeader().setForeground(ThemeColors.TEXT_SECONDARY);
        table.setSelectionBackground(ThemeColors.ACCENT_VERY_LIGHT);
        table.setFillsViewportHeight(true);
    }

    private String getInitials(String name) {
        String[] parts = name.split("[\\s—]+");
        if (parts.length >= 2) return parts[0].substring(0,1) + parts[1].substring(0,1);
        return name.length() >= 2 ? name.substring(0,2).toUpperCase() : name.toUpperCase();
    }

    /** No-op refresh — data is loaded fresh each time this panel is created */
    public void refreshData() { /* AgentDetailPanel is created fresh each time */ }
}
