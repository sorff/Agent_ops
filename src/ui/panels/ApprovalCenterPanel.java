package ui.panels;

import db.DatabaseHelper;
import models.Approval;
import models.User;
import ui.components.RoundedPanel;
import utils.DateUtils;
import utils.NotificationHelper;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Approval Center — the human-in-the-loop control room.
 *
 * Left panel: pending approvals queue (actionable items)
 * Right panel: audit trail of past decisions
 *
 * Each pending item shows:
 *   • Agent name and department
 *   • The proposed action (what the agent wants to do)
 *   • Timestamp
 *   • [Approve] [Reject] buttons
 */
public class ApprovalCenterPanel extends JPanel {

    private final User currentUser;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private JPanel pendingPanel;
    private JPanel historyPanel;
    private JLabel pendingCountLabel;

    public ApprovalCenterPanel(User currentUser) {
        this.currentUser = currentUser;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        // ── Top bar ───────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ThemeColors.CONTENT_BG);
        topBar.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Approval Center");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);
        pendingCountLabel = new JLabel("0 pending");
        pendingCountLabel.setFont(ThemeColors.FONT_BODY);
        pendingCountLabel.setForeground(ThemeColors.STATUS_IDLE);
        titleRow.add(title);
        titleRow.add(pendingCountLabel);

        topBar.add(titleRow, BorderLayout.WEST);

        // ── Two-column content ────────────────────────────────
        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setBackground(ThemeColors.CONTENT_BG);
        content.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        // Left: pending queue
        JPanel leftCard = new JPanel(new BorderLayout(0, 8));
        leftCard.setOpaque(false);

        JLabel pendingTitle = sectionTitle("⏳ Awaiting Your Decision");
        pendingPanel = new JPanel();
        pendingPanel.setLayout(new BoxLayout(pendingPanel, BoxLayout.Y_AXIS));
        pendingPanel.setBackground(ThemeColors.CONTENT_BG);

        JScrollPane pendingScroll = new JScrollPane(pendingPanel);
        pendingScroll.setBorder(null);
        pendingScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);

        leftCard.add(pendingTitle,  BorderLayout.NORTH);
        leftCard.add(pendingScroll, BorderLayout.CENTER);

        // Right: audit trail
        JPanel rightCard = new JPanel(new BorderLayout(0, 8));
        rightCard.setOpaque(false);

        JLabel historyTitle = sectionTitle("📋 Decision History");
        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setBackground(ThemeColors.CONTENT_BG);

        JScrollPane historyScroll = new JScrollPane(historyPanel);
        historyScroll.setBorder(null);
        historyScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);

        rightCard.add(historyTitle,  BorderLayout.NORTH);
        rightCard.add(historyScroll, BorderLayout.CENTER);

        content.add(leftCard);
        content.add(rightCard);

        wrapper.add(topBar,   BorderLayout.NORTH);
        wrapper.add(content,  BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    /** Reloads all approval data from the database */
    public void refreshData() {
        List<Approval> all = db.getAllApprovals();

        List<Approval> pending = all.stream()
            .filter(Approval::isPending)
            .collect(Collectors.toList());
        List<Approval> decided = all.stream()
            .filter(a -> !a.isPending())
            .collect(Collectors.toList());

        pendingCountLabel.setText(pending.size() + " pending");
        pendingCountLabel.setForeground(pending.isEmpty()
            ? ThemeColors.STATUS_ACTIVE : ThemeColors.STATUS_IDLE);

        // Rebuild pending list
        pendingPanel.removeAll();
        for (Approval ap : pending) {
            pendingPanel.add(buildPendingCard(ap));
            pendingPanel.add(Box.createVerticalStrut(10));
        }
        if (pending.isEmpty()) {
            pendingPanel.add(emptyState("✅ All caught up! No pending approvals."));
        }
        pendingPanel.revalidate();
        pendingPanel.repaint();

        // Rebuild history list
        historyPanel.removeAll();
        for (Approval ap : decided.stream().limit(20).collect(Collectors.toList())) {
            historyPanel.add(buildHistoryRow(ap));
            historyPanel.add(Box.createVerticalStrut(6));
        }
        if (decided.isEmpty()) {
            historyPanel.add(emptyState("No decisions made yet."));
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    // ── Pending approval card ─────────────────────────────────

    private JPanel buildPendingCard(Approval ap) {
        RoundedPanel card = new RoundedPanel(10, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        // ── Header: agent name + action type ──────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        String agentName = ap.getAgentName() != null ? ap.getAgentName() : "Unknown Agent";
        JLabel agentLabel = new JLabel("🤖 " + agentName.split("—")[0].trim());
        agentLabel.setFont(ThemeColors.FONT_BOLD);
        agentLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel timeLabel = new JLabel(DateUtils.timeAgo(ap.getCreatedAt()));
        timeLabel.setFont(ThemeColors.FONT_SMALL);
        timeLabel.setForeground(ThemeColors.TEXT_MUTED);

        header.add(agentLabel, BorderLayout.WEST);
        header.add(timeLabel,  BorderLayout.EAST);

        // ── Action type badge ──────────────────────────────────
        JLabel actionType = new JLabel(formatActionType(ap.getActionType()));
        actionType.setFont(ThemeColors.FONT_SMALL);
        actionType.setForeground(ThemeColors.ACCENT);

        // ── Payload (the proposed action text) ────────────────
        String payload = ap.getPayload() != null ? ap.getPayload() : ap.getActionType();
        JTextArea payloadArea = new JTextArea(payload);
        payloadArea.setFont(ThemeColors.FONT_BODY);
        payloadArea.setForeground(ThemeColors.TEXT_SECONDARY);
        payloadArea.setBackground(new Color(0xF9, 0xFA, 0xFB));
        payloadArea.setEditable(false);
        payloadArea.setLineWrap(true);
        payloadArea.setWrapStyleWord(true);
        payloadArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        payloadArea.setRows(3);

        // ── Department label ───────────────────────────────────
        JLabel deptLabel = new JLabel(ap.getDepartmentName() != null
            ? "🏢 " + ap.getDepartmentName() : "");
        deptLabel.setFont(ThemeColors.FONT_SMALL);
        deptLabel.setForeground(ThemeColors.TEXT_MUTED);

        // ── Action buttons ────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton approveBtn = actionButton("✓ Approve", ThemeColors.STATUS_ACTIVE,
            new Color(0xDC, 0xFC, 0xE7));
        JButton rejectBtn  = actionButton("✕ Reject",  ThemeColors.STATUS_ERROR,
            new Color(0xFF, 0xE4, 0xE4));

        approveBtn.addActionListener(e -> decide(ap.getId(), "APPROVED", card));
        rejectBtn.addActionListener(e  -> decide(ap.getId(), "REJECTED", card));

        btnRow.add(rejectBtn);
        btnRow.add(approveBtn);

        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);
        bodyPanel.add(actionType);
        bodyPanel.add(Box.createVerticalStrut(6));
        bodyPanel.add(payloadArea);
        bodyPanel.add(Box.createVerticalStrut(6));
        bodyPanel.add(deptLabel);

        card.add(header,    BorderLayout.NORTH);
        card.add(bodyPanel, BorderLayout.CENTER);
        card.add(btnRow,    BorderLayout.SOUTH);
        return card;
    }

    /** Handles the approve/reject action and refreshes the UI */
    private void decide(int approvalId, String decision, JPanel card) {
        db.decideApproval(approvalId, decision, currentUser.getId());
        db.logActivity("USER", currentUser.getId(),
            decision + " approval #" + approvalId, null);

        if ("APPROVED".equals(decision)) {
            NotificationHelper.showSuccess("Action approved ✓");
        } else {
            NotificationHelper.showInfo("Action rejected — agent notified.");
        }
        refreshData();
    }

    // ── History row ───────────────────────────────────────────

    private JPanel buildHistoryRow(Approval ap) {
        RoundedPanel row = new RoundedPanel(8, ThemeColors.CARD_BG, false);
        row.setLayout(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Status icon
        boolean approved = "APPROVED".equals(ap.getStatus());
        JLabel statusIcon = new JLabel(approved ? "✓" : "✕");
        statusIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusIcon.setForeground(approved ? ThemeColors.STATUS_ACTIVE : ThemeColors.STATUS_ERROR);
        statusIcon.setPreferredSize(new Dimension(24, 24));

        // Agent + action
        String agentName  = ap.getAgentName() != null ? ap.getAgentName().split("—")[0].trim() : "Agent";
        String payloadSnip = ap.getPayload() != null && ap.getPayload().length() > 40
            ? ap.getPayload().substring(0, 40) + "…" : ap.getPayload();
        JLabel textLabel = new JLabel("<html><b>" + agentName + "</b>: " + payloadSnip + "</html>");
        textLabel.setFont(ThemeColors.FONT_SMALL);
        textLabel.setForeground(ThemeColors.TEXT_SECONDARY);

        // Time
        JLabel timeLabel = new JLabel(ap.getDecidedAt() != null
            ? DateUtils.timeAgo(ap.getDecidedAt()) : "");
        timeLabel.setFont(ThemeColors.FONT_SMALL);
        timeLabel.setForeground(ThemeColors.TEXT_MUTED);

        row.add(statusIcon, BorderLayout.WEST);
        row.add(textLabel,  BorderLayout.CENTER);
        row.add(timeLabel,  BorderLayout.EAST);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────

    private JButton actionButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(fg);
                g2.setFont(ThemeColors.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()-fm.getHeight())/2+fm.getAscent());
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(ThemeColors.FONT_SUBTITLE);
        l.setForeground(ThemeColors.TEXT_PRIMARY);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return l;
    }

    private JPanel emptyState(String msg) {
        RoundedPanel p = new RoundedPanel(10, ThemeColors.CARD_BG);
        p.setLayout(new GridBagLayout());
        JLabel lbl = new JLabel(msg);
        lbl.setFont(ThemeColors.FONT_BODY);
        lbl.setForeground(ThemeColors.TEXT_MUTED);
        p.add(lbl);
        p.setPreferredSize(new Dimension(Integer.MAX_VALUE, 80));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return p;
    }

    private String formatActionType(String actionType) {
        if (actionType == null) return "";
        return actionType.replace("_", " ").toLowerCase()
            .substring(0, 1).toUpperCase()
            + actionType.replace("_", " ").toLowerCase().substring(1);
    }
}
