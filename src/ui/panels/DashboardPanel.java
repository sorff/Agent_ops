package ui.panels;

import db.DatabaseHelper;
import models.*;
import ui.MainFrame;
import ui.components.KpiCard;
import ui.components.RoundedPanel;
import ui.components.StatusDot;
import utils.DateUtils;
import utils.ThemeColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * The home screen of AgentOps.
 *
 * Layout:
 *   ┌──────────────────────────────────────────────────────┐
 *   │ [🤖 24 Agents] [📋 12 Active] [✅ 8 Pending] [🔔 5] │  ← KPI row
 *   ├────────────────────────────┬─────────────────────────┤
 *   │ Department Health Overview │  Pending Approvals      │
 *   │  [Sales ✅] [HR ⚠] ...     │  [Approval 1]           │
 *   ├────────────────────────────┤  [Approval 2]           │
 *   │ Live Activity Feed         │  ...                    │
 *   └────────────────────────────┴─────────────────────────┘
 */
public class DashboardPanel extends JPanel {

    private final User currentUser;
    private final MainFrame mainFrame;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    // KPI cards — stored so we can update their values on refresh
    private KpiCard agentCard, taskCard, approvalCard, alertCard;

    // Dynamic content areas — rebuilt on each refresh
    private JPanel deptHealthGrid;
    private JPanel activityFeedPanel;
    private JPanel pendingApprovalsPanel;

    public DashboardPanel(User user, MainFrame mainFrame) {
        this.currentUser = user;
        this.mainFrame   = mainFrame;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        // Wrap everything in a scroll pane so it works on smaller screens
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(ThemeColors.CONTENT_BG);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        content.add(buildKpiRow(),      BorderLayout.NORTH);
        content.add(buildMainSection(), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        refreshData();
    }

    // ── KPI Row ───────────────────────────────────────────────

    /** Builds the 4 KPI cards at the top of the dashboard */
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);

        agentCard    = new KpiCard("Total Agents",      "—", "🤖", "loading…",   ThemeColors.ACCENT);
        taskCard     = new KpiCard("Active Tasks",      "—", "📋", "in progress", ThemeColors.STATUS_ACTIVE);
        approvalCard = new KpiCard("Pending Approvals", "—", "✅", "need review", ThemeColors.STATUS_IDLE);
        alertCard    = new KpiCard("Open Alerts",       "—", "🔔", "unread",      ThemeColors.STATUS_ERROR);

        // Clicking on the Approvals KPI card navigates to the Approval Center
        approvalCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        approvalCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.switchTo(4, "Approvals", "APPROVALS");
            }
        });

        // Clicking on Alerts card navigates to Alerts
        alertCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        alertCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.switchTo(6, "Alerts", "ALERTS");
            }
        });

        row.add(agentCard);
        row.add(taskCard);
        row.add(approvalCard);
        row.add(alertCard);
        return row;
    }

    // ── Main section ──────────────────────────────────────────

    /** Builds the two-column main section */
    private JPanel buildMainSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.insets  = new Insets(0, 0, 0, 0);
        gbc.weighty = 1.0;

        // Left column (70%)
        JPanel leftCol = buildLeftColumn();
        gbc.weightx = 0.68;
        gbc.gridx   = 0;
        gbc.insets  = new Insets(0, 0, 0, 16);
        section.add(leftCol, gbc);

        // Right column (30%)
        JPanel rightCol = buildRightColumn();
        gbc.weightx = 0.32;
        gbc.gridx   = 1;
        gbc.insets  = new Insets(0, 0, 0, 0);
        section.add(rightCol, gbc);

        return section;
    }

    /** Left column: department health + activity feed */
    private JPanel buildLeftColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Department health section
        JLabel deptTitle = sectionTitle("Department Health Overview");
        deptHealthGrid = new JPanel(new GridLayout(2, 3, 12, 12));
        deptHealthGrid.setOpaque(false);

        JPanel deptSection = new JPanel(new BorderLayout(0, 10));
        deptSection.setOpaque(false);
        deptSection.add(deptTitle,    BorderLayout.NORTH);
        deptSection.add(deptHealthGrid, BorderLayout.CENTER);

        // Activity feed section
        JLabel activityTitle = sectionTitle("Live Activity Feed");
        activityFeedPanel = new JPanel();
        activityFeedPanel.setLayout(new BoxLayout(activityFeedPanel, BoxLayout.Y_AXIS));
        activityFeedPanel.setOpaque(false);

        JScrollPane feedScroll = new JScrollPane(activityFeedPanel);
        feedScroll.setBorder(null);
        feedScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        feedScroll.setPreferredSize(new Dimension(0, 250));

        JPanel activitySection = new JPanel(new BorderLayout(0, 10));
        activitySection.setOpaque(false);
        activitySection.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        activitySection.add(activityTitle, BorderLayout.NORTH);
        activitySection.add(feedScroll,    BorderLayout.CENTER);

        col.add(deptSection);
        col.add(activitySection);
        return col;
    }

    /** Right column: pending approvals queue */
    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 10));
        col.setOpaque(false);

        JLabel title = sectionTitle("Pending Approvals");

        pendingApprovalsPanel = new JPanel();
        pendingApprovalsPanel.setLayout(new BoxLayout(pendingApprovalsPanel, BoxLayout.Y_AXIS));
        pendingApprovalsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(pendingApprovalsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);

        JButton viewAllBtn = smallLinkButton("View all in Approval Center →");
        viewAllBtn.addActionListener(e -> mainFrame.switchTo(4, "Approvals", "APPROVALS"));

        col.add(title,      BorderLayout.NORTH);
        col.add(scroll,     BorderLayout.CENTER);
        col.add(viewAllBtn, BorderLayout.SOUTH);
        return col;
    }

    // ── Data refresh ──────────────────────────────────────────

    /**
     * Reloads all data from the database and updates the UI.
     * Called on initial load and after every simulation tick.
     */
    public void refreshData() {
        // Update KPI values
        int agents    = db.getTotalAgentCount();
        int tasks     = db.getActiveTaskCount();
        int approvals = db.getPendingApprovalCount();
        int alerts    = db.getUnacknowledgedAlertCount();

        agentCard.setValue(String.valueOf(agents));
        taskCard.setValue(String.valueOf(tasks));
        approvalCard.setValue(String.valueOf(approvals));
        alertCard.setValue(String.valueOf(alerts));

        // Rebuild department health grid
        refreshDeptHealth();

        // Rebuild activity feed
        refreshActivityFeed();

        // Rebuild pending approvals list
        refreshPendingApprovals();
    }

    /** Refreshes the department health cards */
    private void refreshDeptHealth() {
        deptHealthGrid.removeAll();
        List<Department> departments = db.getAllDepartments();

        String[] healthStatuses = {"Healthy", "Healthy", "Warning", "Healthy", "Critical", "Warning"};
        int idx = 0;

        for (Department dept : departments) {
            String health = idx < healthStatuses.length ? healthStatuses[idx++] : "Healthy";
            deptHealthGrid.add(buildDeptHealthCard(dept, health));
        }
        deptHealthGrid.revalidate();
        deptHealthGrid.repaint();
    }

    /** Builds one department health card */
    private JPanel buildDeptHealthCard(Department dept, String health) {
        RoundedPanel card = new RoundedPanel(10, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Dept name + health badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel nameLabel = new JLabel(dept.getName());
        nameLabel.setFont(ThemeColors.FONT_BOLD);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        Color healthColor = "Healthy".equals(health)  ? ThemeColors.STATUS_ACTIVE
                          : "Warning".equals(health)   ? ThemeColors.STATUS_IDLE
                          :                              ThemeColors.STATUS_ERROR;
        String healthIcon = "Healthy".equals(health)  ? "🟢"
                          : "Warning".equals(health)   ? "🟡"
                          :                              "🔴";

        JLabel healthLabel = new JLabel(healthIcon + " " + health);
        healthLabel.setFont(ThemeColors.FONT_SMALL);
        healthLabel.setForeground(healthColor);

        topRow.add(nameLabel,  BorderLayout.WEST);
        topRow.add(healthLabel,BorderLayout.EAST);

        JLabel agentCount = new JLabel(dept.getAgentCount() + " agents");
        agentCount.setFont(ThemeColors.FONT_SMALL);
        agentCount.setForeground(ThemeColors.TEXT_MUTED);

        card.add(topRow,     BorderLayout.NORTH);
        card.add(agentCount, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.switchTo(2, "Departments", "DEPARTMENTS");
            }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBackgroundColor(ThemeColors.ACCENT_VERY_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBackgroundColor(ThemeColors.CARD_BG);
            }
        });

        return card;
    }

    /** Refreshes the live activity feed */
    private void refreshActivityFeed() {
        activityFeedPanel.removeAll();
        List<ActivityLog> logs = db.getRecentActivityLogs();
        // Show only the 15 most recent entries
        int count = 0;
        for (ActivityLog log : logs) {
            if (count++ >= 15) break;
            activityFeedPanel.add(buildFeedItem(log));
            activityFeedPanel.add(Box.createVerticalStrut(4));
        }

        if (logs.isEmpty()) {
            JLabel empty = new JLabel("No recent activity yet.");
            empty.setFont(ThemeColors.FONT_BODY);
            empty.setForeground(ThemeColors.TEXT_MUTED);
            activityFeedPanel.add(empty);
        }

        activityFeedPanel.revalidate();
        activityFeedPanel.repaint();
    }

    /** Builds one activity feed row */
    private JPanel buildFeedItem(ActivityLog log) {
        RoundedPanel item = new RoundedPanel(6, ThemeColors.CARD_BG, false);
        item.setLayout(new BorderLayout(10, 0));
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // Actor type icon
        String icon = "AGENT".equals(log.getActorType()) ? "🤖" : "👤";
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        // Action text
        JLabel actionLabel = new JLabel(
            "<html><b>" + (log.getActorName() != null ? log.getActorName() : log.getActorType())
            + "</b> " + log.getAction() + "</html>");
        actionLabel.setFont(ThemeColors.FONT_SMALL);
        actionLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // Timestamp
        JLabel timeLabel = new JLabel(DateUtils.timeAgo(log.getTimestamp()));
        timeLabel.setFont(ThemeColors.FONT_SMALL);
        timeLabel.setForeground(ThemeColors.TEXT_MUTED);

        item.add(iconLabel,   BorderLayout.WEST);
        item.add(actionLabel, BorderLayout.CENTER);
        item.add(timeLabel,   BorderLayout.EAST);
        return item;
    }

    /** Refreshes the pending approvals list in the right sidebar */
    private void refreshPendingApprovals() {
        pendingApprovalsPanel.removeAll();
        List<Approval> pending = db.getPendingApprovals();

        int count = 0;
        for (Approval ap : pending) {
            if (count++ >= 6) break;
            pendingApprovalsPanel.add(buildApprovalSnippet(ap));
            pendingApprovalsPanel.add(Box.createVerticalStrut(8));
        }

        if (pending.isEmpty()) {
            RoundedPanel empty = new RoundedPanel(8, ThemeColors.CARD_BG);
            empty.setLayout(new BorderLayout());
            JLabel msg = new JLabel("✅ No pending approvals");
            msg.setFont(ThemeColors.FONT_BODY);
            msg.setForeground(ThemeColors.STATUS_ACTIVE);
            msg.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(msg);
            pendingApprovalsPanel.add(empty);
        }

        pendingApprovalsPanel.revalidate();
        pendingApprovalsPanel.repaint();
    }

    /** Builds a compact approval item for the sidebar */
    private JPanel buildApprovalSnippet(Approval ap) {
        RoundedPanel card = new RoundedPanel(8, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel agentLabel = new JLabel(ap.getAgentName() != null ? ap.getAgentName() : "Agent");
        agentLabel.setFont(ThemeColors.FONT_BOLD);
        agentLabel.setForeground(ThemeColors.ACCENT);

        String payload = ap.getPayload() != null ? ap.getPayload() : ap.getActionType();
        if (payload.length() > 50) payload = payload.substring(0, 50) + "…";
        JLabel payloadLabel = new JLabel("<html>" + payload + "</html>");
        payloadLabel.setFont(ThemeColors.FONT_SMALL);
        payloadLabel.setForeground(ThemeColors.TEXT_SECONDARY);

        JLabel timeLabel = new JLabel(DateUtils.timeAgo(ap.getCreatedAt()));
        timeLabel.setFont(ThemeColors.FONT_SMALL);
        timeLabel.setForeground(ThemeColors.TEXT_MUTED);

        card.add(agentLabel,   BorderLayout.NORTH);
        card.add(payloadLabel, BorderLayout.CENTER);
        card.add(timeLabel,    BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.switchTo(4, "Approvals", "APPROVALS");
            }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBackgroundColor(ThemeColors.ACCENT_VERY_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBackgroundColor(ThemeColors.CARD_BG);
            }
        });

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────

    /** Creates a styled section title label */
    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeColors.FONT_SUBTITLE);
        label.setForeground(ThemeColors.TEXT_PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return label;
    }

    /** Creates a small "view all" link button */
    private JButton smallLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeColors.FONT_SMALL);
        btn.setForeground(ThemeColors.ACCENT);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
