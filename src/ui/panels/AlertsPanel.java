package ui.panels;

import db.DatabaseHelper;
import models.Alert;
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
 * Alerts & Notifications page.
 *
 * Shows all system alerts with severity levels (INFO / WARNING / CRITICAL).
 * Features:
 *   • Filter tabs: All / Critical / Warning / Info
 *   • Each alert shows severity badge, message, related entity, timestamp
 *   • "Acknowledge" button marks the alert as read
 *   • "Acknowledge All" button clears all alerts
 */
public class AlertsPanel extends JPanel {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private JPanel alertListPanel;
    private String currentFilter = "ALL";
    private JLabel[] filterBtns;

    public AlertsPanel() {
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        wrapper.add(buildTopBar(),    BorderLayout.NORTH);
        wrapper.add(buildAlertList(), BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ThemeColors.CONTENT_BG);
        bar.setBorder(BorderFactory.createEmptyBorder(20, 24, 0, 24));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Alerts & Notifications");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JButton ackAllBtn = smallButton("Acknowledge All");
        ackAllBtn.addActionListener(e -> {
            db.getAllAlerts().forEach(a -> db.acknowledgeAlert(a.getId()));
            NotificationHelper.showSuccess("All alerts acknowledged.");
            refreshData();
        });

        titleRow.add(title,     BorderLayout.WEST);
        titleRow.add(ackAllBtn, BorderLayout.EAST);

        // Filter tabs
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        filterRow.setOpaque(false);

        String[] filters = {"ALL", "CRITICAL", "WARNING", "INFO"};
        String[] labels  = {"All", "🔴 Critical", "🟠 Warning", "🔵 Info"};
        filterBtns = new JLabel[filters.length];

        for (int i = 0; i < filters.length; i++) {
            final String f = filters[i];
            JLabel btn = filterTab(labels[i], f.equals(currentFilter));
            filterBtns[i] = btn;
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    currentFilter = f;
                    refreshFilters();
                    refreshData();
                }
            });
            filterRow.add(btn);
        }

        bar.add(titleRow,  BorderLayout.NORTH);
        bar.add(filterRow, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildAlertList() {
        alertListPanel = new JPanel();
        alertListPanel.setLayout(new BoxLayout(alertListPanel, BoxLayout.Y_AXIS));
        alertListPanel.setBackground(ThemeColors.CONTENT_BG);
        alertListPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        JScrollPane scroll = new JScrollPane(alertListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        return (JPanel)(JPanel) new JPanel(new BorderLayout()) {{
            setOpaque(false);
            add(scroll);
        }};
    }

    /** Reloads alerts from DB and rebuilds the list */
    public void refreshData() {
        alertListPanel.removeAll();

        List<Alert> alerts = db.getAllAlerts();
        if (!"ALL".equals(currentFilter)) {
            alerts = alerts.stream()
                .filter(a -> a.getSeverity().equals(currentFilter))
                .collect(Collectors.toList());
        }

        if (alerts.isEmpty()) {
            RoundedPanel empty = new RoundedPanel(10, ThemeColors.CARD_BG);
            empty.setLayout(new GridBagLayout());
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            JLabel lbl = new JLabel("🎉 No alerts to display");
            lbl.setFont(ThemeColors.FONT_BODY);
            lbl.setForeground(ThemeColors.TEXT_MUTED);
            empty.add(lbl);
            alertListPanel.add(empty);
        } else {
            for (Alert alert : alerts) {
                alertListPanel.add(buildAlertRow(alert));
                alertListPanel.add(Box.createVerticalStrut(8));
            }
        }

        alertListPanel.revalidate();
        alertListPanel.repaint();
    }

    /** Builds one alert row */
    private JPanel buildAlertRow(Alert alert) {
        Color bgColor  = ThemeColors.getSeverityBgColor(alert.getSeverity());
        Color border   = ThemeColors.getSeverityColor(alert.getSeverity());

        // Dimmed if already acknowledged
        if (alert.isAcknowledged()) {
            bgColor = new Color(0xF9, 0xFA, 0xFB);
            border  = ThemeColors.BORDER;
        }

        final Color finalBg     = bgColor;
        final Color finalBorder = border;

        RoundedPanel row = new RoundedPanel(8, finalBg) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Left accent stripe
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(finalBorder);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        // Left: severity icon + text
        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);

        JLabel severityIcon = new JLabel(alert.getSeverityEmoji());
        severityIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        severityIcon.setPreferredSize(new Dimension(28, 28));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel msgLabel = new JLabel("<html><b>" + alert.getMessage() + "</b></html>");
        msgLabel.setFont(ThemeColors.FONT_BODY);
        msgLabel.setForeground(alert.isAcknowledged()
            ? ThemeColors.TEXT_MUTED : ThemeColors.TEXT_PRIMARY);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        metaRow.setOpaque(false);

        if (alert.getRelatedEntity() != null) {
            JLabel entityLabel = new JLabel(alert.getRelatedEntity());
            entityLabel.setFont(ThemeColors.FONT_SMALL);
            entityLabel.setForeground(ThemeColors.ACCENT);
            metaRow.add(entityLabel);
        }

        JLabel timeLabel = new JLabel(DateUtils.timeAgo(alert.getCreatedAt()));
        timeLabel.setFont(ThemeColors.FONT_SMALL);
        timeLabel.setForeground(ThemeColors.TEXT_MUTED);
        metaRow.add(timeLabel);

        textPanel.add(msgLabel);
        textPanel.add(metaRow);

        left.add(severityIcon, BorderLayout.WEST);
        left.add(textPanel,    BorderLayout.CENTER);

        // Right: acknowledge button (only for unread)
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);

        if (!alert.isAcknowledged()) {
            JButton ackBtn = ackButton("Mark Read");
            ackBtn.addActionListener(e -> {
                db.acknowledgeAlert(alert.getId());
                refreshData();
            });
            right.add(ackBtn);
        } else {
            JLabel readLabel = new JLabel("✓ Read");
            readLabel.setFont(ThemeColors.FONT_SMALL);
            readLabel.setForeground(ThemeColors.TEXT_MUTED);
            right.add(readLabel);
        }

        row.add(left,  BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void refreshFilters() {
        String[] filters = {"ALL", "CRITICAL", "WARNING", "INFO"};
        for (int i = 0; i < filterBtns.length; i++) {
            boolean active = filters[i].equals(currentFilter);
            filterBtns[i].setOpaque(active);
            filterBtns[i].setBackground(active ? ThemeColors.ACCENT : Color.WHITE);
            filterBtns[i].setForeground(active ? Color.WHITE : ThemeColors.TEXT_SECONDARY);
            filterBtns[i].repaint();
        }
    }

    private JLabel filterTab(String text, boolean active) {
        JLabel lbl = new JLabel("  " + text + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(ThemeColors.FONT_SMALL);
        lbl.setForeground(active ? Color.WHITE : ThemeColors.TEXT_SECONDARY);
        lbl.setBackground(active ? ThemeColors.ACCENT : new Color(0xF3, 0xF4, 0xF6));
        lbl.setOpaque(false);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return lbl;
    }

    private JButton ackButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeColors.FONT_SMALL);
        btn.setForeground(ThemeColors.TEXT_SECONDARY);
        btn.setBackground(new Color(0xF3, 0xF4, 0xF6));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(90, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton smallButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xE5, 0xE7, 0xEB) : new Color(0xF3, 0xF4, 0xF6));
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(ThemeColors.TEXT_SECONDARY);
                g2.setFont(ThemeColors.FONT_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()-fm.getHeight())/2+fm.getAscent());
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(140, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
