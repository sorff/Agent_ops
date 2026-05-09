package ui.components;

import models.Agent;
import utils.ThemeColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A compact card that displays one agent's summary information.
 * Used in the OrgChart and in department roster lists.
 *
 * Shows:
 *   • Agent name and role badge
 *   • Colored status dot + status text
 *   • Current task (truncated if long)
 *   • Performance score bar
 *
 * Usage:
 *   AgentCard card = new AgentCard(agent, () -> openAgentDetail(agent));
 */
public class AgentCard extends RoundedPanel {

    private StatusDot statusDot;
    private JLabel statusLabel;
    private JLabel taskLabel;

    /**
     * @param agent     The Agent model to display
     * @param onClick   Runnable that runs when the user clicks the card
     */
    public AgentCard(Agent agent, Runnable onClick) {
        super(10, ThemeColors.CARD_BG);
        setLayout(new BorderLayout(10, 4));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── Top row: avatar + name + role badge ───────────────
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        // Avatar — a colored circle with the agent's initials
        JPanel avatar = createAvatar(agent);

        // Name + role column
        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);

        // Just the actual name part (before the em-dash separator)
        String displayName = agent.getName().contains("—")
            ? agent.getName().split("—")[0].trim()
            : agent.getName();

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(ThemeColors.FONT_BOLD);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // Role badge — small colored pill
        JLabel roleBadge = createRoleBadge(agent.getRoleType());

        nameCol.add(nameLabel);
        nameCol.add(Box.createVerticalStrut(3));
        nameCol.add(roleBadge);

        topRow.add(avatar,  BorderLayout.WEST);
        topRow.add(nameCol, BorderLayout.CENTER);

        // ── Middle row: status dot + status text ──────────────
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusRow.setOpaque(false);
        statusDot   = new StatusDot(agent.getStatus(), 9);
        statusLabel = new JLabel(agent.getStatus());
        statusLabel.setFont(ThemeColors.FONT_SMALL);
        statusLabel.setForeground(ThemeColors.getStatusColor(agent.getStatus()));
        statusRow.add(statusDot);
        statusRow.add(statusLabel);

        // ── Bottom: current task ──────────────────────────────
        String task = agent.getCurrentTask() != null ? agent.getCurrentTask() : "No active task";
        taskLabel = new JLabel(truncate(task, 45));
        taskLabel.setFont(ThemeColors.FONT_SMALL);
        taskLabel.setForeground(ThemeColors.TEXT_MUTED);
        taskLabel.setToolTipText(task);  // show full text on hover

        // ── Bottom right: performance score ───────────────────
        JLabel scoreLabel = new JLabel(String.format("%.0f%%", agent.getPerformanceScore()));
        scoreLabel.setFont(ThemeColors.FONT_SMALL);
        scoreLabel.setForeground(getScoreColor(agent.getPerformanceScore()));
        topRow.add(scoreLabel, BorderLayout.EAST);

        add(topRow,    BorderLayout.NORTH);
        add(statusRow, BorderLayout.CENTER);
        add(taskLabel, BorderLayout.SOUTH);

        // ── Hover effect ──────────────────────────────────────
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                setBackgroundColor(ThemeColors.ACCENT_VERY_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                setBackgroundColor(ThemeColors.CARD_BG);
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
    }

    /** Creates a colored avatar circle with the agent's initials */
    private JPanel createAvatar(Agent agent) {
        // Pick a color based on the department
        Color[] colors = ThemeColors.DEPT_COLORS;
        Color color = colors[Math.abs(agent.getDepartmentId()) % colors.length];

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String initials = getInitials(agent.getName());
                FontMetrics fm = g2.getFontMetrics();
                int x = (36 - fm.stringWidth(initials)) / 2;
                int y = (36 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);
        return avatar;
    }

    /** Creates a small pill badge for MANAGER or SPECIALIST role */
    private JLabel createRoleBadge(String roleType) {
        boolean isManager = Agent.ROLE_MANAGER.equals(roleType);
        JLabel badge = new JLabel(isManager ? "  Manager  " : "  Specialist  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isManager
                    ? new Color(0x4F, 0x46, 0xE5, 25)
                    : new Color(0x10, 0xB9, 0x81, 25);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(ThemeColors.FONT_SMALL);
        badge.setForeground(isManager ? ThemeColors.ACCENT : ThemeColors.STATUS_ACTIVE);
        badge.setOpaque(false);
        return badge;
    }

    /** Returns the first 2 initials from an agent name (e.g. "ARIA" → "AR") */
    private String getInitials(String name) {
        String[] parts = name.split("[\\s—]+");
        if (parts.length >= 2) {
            return parts[0].substring(0, 1) + parts[1].substring(0, 1);
        }
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    /** Returns green/amber/red depending on how good the score is */
    private Color getScoreColor(double score) {
        if (score >= 85) return ThemeColors.STATUS_ACTIVE;
        if (score >= 70) return ThemeColors.STATUS_IDLE;
        return ThemeColors.STATUS_ERROR;
    }

    /** Shortens text that's too long to fit in a card */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    /** Refreshes the card's status display (called by simulation engine) */
    public void updateStatus(String status, String currentTask) {
        statusDot.setStatus(status);
        statusLabel.setText(status);
        statusLabel.setForeground(ThemeColors.getStatusColor(status));
        String task = currentTask != null ? currentTask : "No active task";
        taskLabel.setText(truncate(task, 45));
        taskLabel.setToolTipText(task);
    }
}
