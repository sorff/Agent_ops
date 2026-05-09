package ui.panels;

import db.DatabaseHelper;
import models.ActivityLog;
import utils.DateUtils;
import utils.ThemeColors;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Activity Log — full audit trail of everything that happened in the system.
 *
 * Features:
 *   • Search bar to filter by action text
 *   • Actor-type filter chips: All / User / Agent / System
 *   • JTable with custom row renderer (colored actor badge, action text, timestamp)
 *   • "Refresh" button to reload latest entries
 *   • Up to 500 most-recent entries shown
 */
public class ActivityLogPanel extends JPanel {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private JTable logTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private String actorFilter = "ALL";
    private JLabel[] filterBtns;

    // Snapshot of all loaded entries (pre-filter)
    private List<ActivityLog> allLogs;

    public ActivityLogPanel() {
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        wrapper.add(buildTopBar(),  BorderLayout.NORTH);
        wrapper.add(buildTable(),   BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    // ── Top bar: title + search + filters ────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(ThemeColors.CONTENT_BG);
        bar.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout(0, 4));
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Activity Log");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sub = new JLabel("Full audit trail — every user action, agent event, and system event.");
        sub.setFont(ThemeColors.FONT_BODY);
        sub.setForeground(ThemeColors.TEXT_SECONDARY);

        titleRow.add(title, BorderLayout.NORTH);
        titleRow.add(sub,   BorderLayout.SOUTH);

        // Controls row: search + refresh
        JPanel controlRow = new JPanel(new BorderLayout(12, 0));
        controlRow.setOpaque(false);
        controlRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        searchField = buildSearchField();
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilters(); }
        });

        JButton refreshBtn = refreshButton();
        refreshBtn.addActionListener(e -> refreshData());

        // Filter chips
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        String[] actors = {"ALL", "USER", "AGENT", "SYSTEM"};
        String[] labels = {"All",  "👤 User", "🤖 Agent", "⚙️ System"};
        filterBtns = new JLabel[actors.length];

        for (int i = 0; i < actors.length; i++) {
            final String f = actors[i];
            JLabel chip = filterChip(labels[i], f.equals(actorFilter));
            filterBtns[i] = chip;
            chip.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    actorFilter = f;
                    refreshFilterChips(actors);
                    applyFilters();
                }
            });
            filterRow.add(chip);
        }

        JPanel searchAndBtn = new JPanel(new BorderLayout(8, 0));
        searchAndBtn.setOpaque(false);
        searchAndBtn.add(searchField,  BorderLayout.CENTER);
        searchAndBtn.add(refreshBtn,   BorderLayout.EAST);

        controlRow.add(filterRow,   BorderLayout.WEST);
        controlRow.add(searchAndBtn, BorderLayout.EAST);

        bar.add(titleRow,    BorderLayout.NORTH);
        bar.add(controlRow,  BorderLayout.SOUTH);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────

    private JPanel buildTable() {
        String[] cols = {"#", "Actor", "Actor Type", "Action", "Details", "Time"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        logTable = new JTable(tableModel);
        logTable.setFont(ThemeColors.FONT_BODY);
        logTable.setForeground(ThemeColors.TEXT_PRIMARY);
        logTable.setBackground(ThemeColors.CARD_BG);
        logTable.setGridColor(ThemeColors.BORDER);
        logTable.setRowHeight(36);
        logTable.setShowVerticalLines(false);
        logTable.setIntercellSpacing(new Dimension(0, 1));
        logTable.setSelectionBackground(new Color(0xEE, 0xEF, 0xFF));
        logTable.setSelectionForeground(ThemeColors.TEXT_PRIMARY);

        // Column widths
        int[] widths = {40, 130, 80, 260, 200, 110};
        for (int i = 0; i < widths.length; i++) {
            logTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Header styling
        JTableHeader header = logTable.getTableHeader();
        header.setFont(ThemeColors.FONT_BOLD);
        header.setBackground(new Color(0xF9, 0xFA, 0xFB));
        header.setForeground(ThemeColors.TEXT_SECONDARY);
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
            .setHorizontalAlignment(JLabel.LEFT);

        // Custom row renderer
        logTable.setDefaultRenderer(Object.class, new ActivityRowRenderer());

        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.BORDER));
        scroll.getViewport().setBackground(ThemeColors.CARD_BG);
        scroll.setBorder(null);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeColors.CONTENT_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
        wrapper.add(scroll);
        return wrapper;
    }

    // ── Data loading ──────────────────────────────────────────

    public void refreshData() {
        allLogs = db.getActivityLogs(500);
        applyFilters();
    }

    private void applyFilters() {
        if (allLogs == null) return;

        String query = searchField.getText().trim().toLowerCase();

        List<ActivityLog> filtered = allLogs.stream()
            .filter(log -> {
                if (!"ALL".equals(actorFilter) && !actorFilter.equals(log.getActorType())) return false;
                if (!query.isEmpty()) {
                    String action  = log.getAction()  != null ? log.getAction().toLowerCase()  : "";
                    String details = log.getDetails() != null ? log.getDetails().toLowerCase() : "";
                    if (!action.contains(query) && !details.contains(query)) return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        tableModel.setRowCount(0);
        for (ActivityLog log : filtered) {
            String actorName = resolveActorName(log);
            String timeStr   = log.getTimestamp() != null
                ? DateUtils.timeAgo(log.getTimestamp()) : "—";
            tableModel.addRow(new Object[]{
                log.getId(),
                actorName,
                log.getActorType() != null ? log.getActorType() : "—",
                log.getAction()    != null ? log.getAction()    : "—",
                log.getDetails()   != null ? log.getDetails()   : "",
                timeStr
            });
        }
    }

    private String resolveActorName(ActivityLog log) {
        if (log.getActorType() == null) return "System";
        switch (log.getActorType()) {
            case "USER":   return log.getActorId() > 0 ? "User #"  + log.getActorId() : "Unknown User";
            case "AGENT":  return log.getActorId() > 0 ? "Agent #" + log.getActorId() : "Unknown Agent";
            case "SYSTEM": return "System";
            default:       return log.getActorType();
        }
    }

    // ── Custom cell renderer ──────────────────────────────────

    private static class ActivityRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {

            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            if (!isSelected) {
                lbl.setBackground(row % 2 == 0 ? ThemeColors.CARD_BG
                    : new Color(0xF9, 0xFA, 0xFB));
            }

            // Actor-type column: colored badge
            if (col == 2 && value != null) {
                String type = value.toString();
                Color badgeColor;
                switch (type) {
                    case "USER":   badgeColor = ThemeColors.ACCENT;        break;
                    case "AGENT":  badgeColor = ThemeColors.STATUS_ACTIVE; break;
                    case "SYSTEM": badgeColor = ThemeColors.STATUS_IDLE;   break;
                    default:       badgeColor = ThemeColors.TEXT_MUTED;    break;
                }
                lbl.setForeground(isSelected ? ThemeColors.TEXT_PRIMARY : badgeColor);
                lbl.setFont(ThemeColors.FONT_BOLD);
            } else {
                lbl.setFont(col == 0 ? ThemeColors.FONT_SMALL : ThemeColors.FONT_BODY);
                if (!isSelected) lbl.setForeground(
                    col == 0 ? ThemeColors.TEXT_MUTED : ThemeColors.TEXT_PRIMARY);
            }
            return lbl;
        }
    }

    // ── Chip & button helpers ─────────────────────────────────

    private JLabel filterChip(String text, boolean active) {
        JLabel chip = new JLabel("  " + text + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setFont(ThemeColors.FONT_SMALL);
        chip.setForeground(active ? Color.WHITE : ThemeColors.TEXT_SECONDARY);
        chip.setBackground(active ? ThemeColors.ACCENT : new Color(0xF3, 0xF4, 0xF6));
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return chip;
    }

    private void refreshFilterChips(String[] actors) {
        for (int i = 0; i < filterBtns.length; i++) {
            boolean active = actors[i].equals(actorFilter);
            filterBtns[i].setForeground(active ? Color.WHITE : ThemeColors.TEXT_SECONDARY);
            filterBtns[i].setBackground(active ? ThemeColors.ACCENT : new Color(0xF3, 0xF4, 0xF6));
            filterBtns[i].repaint();
        }
    }

    private JTextField buildSearchField() {
        JTextField field = new JTextField(24) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ThemeColors.TEXT_MUTED);
                    g2.setFont(ThemeColors.FONT_BODY);
                    g2.drawString("Search actions...", 10, getHeight() / 2 + 5);
                    g2.dispose();
                }
            }
        };
        field.setFont(ThemeColors.FONT_BODY);
        field.setForeground(ThemeColors.TEXT_PRIMARY);
        field.setBackground(new Color(0xF9, 0xFA, 0xFB));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        field.setPreferredSize(new Dimension(260, 34));
        return field;
    }

    private JButton refreshButton() {
        JButton btn = new JButton("↻  Refresh") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                    ? ThemeColors.ACCENT_LIGHT : ThemeColors.ACCENT);
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
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
