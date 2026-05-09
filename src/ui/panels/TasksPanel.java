package ui.panels;

import db.DatabaseHelper;
import models.Agent;
import models.Task;
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
 * Kanban board for managing tasks.
 *
 * Five columns (in order):
 *   Backlog → Assigned → In Progress → Awaiting Approval → Done
 *
 * Each card shows: title, agent name, priority badge, deadline.
 * Arrow buttons on each card let you move it to the next/previous column.
 * The "+ New Task" button opens a dialog to create a new task.
 */
public class TasksPanel extends JPanel {

    private final User currentUser;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    // The 5 columns — each is a JPanel holding task cards
    private JPanel[] columns;

    private static final String[] STATUSES = {
        Task.STATUS_BACKLOG,
        Task.STATUS_ASSIGNED,
        Task.STATUS_IN_PROGRESS,
        Task.STATUS_AWAITING_APPROVAL,
        Task.STATUS_DONE
    };
    private static final String[] COLUMN_TITLES = {
        "📥 Backlog", "📌 Assigned", "⚙ In Progress", "👁 Awaiting Approval", "✅ Done"
    };
    private static final Color[] COLUMN_COLORS = {
        new Color(0x6B, 0x72, 0x80),  // gray
        new Color(0x3B, 0x82, 0xF6),  // blue
        new Color(0xF5, 0x9E, 0x0B),  // amber
        new Color(0x8B, 0x5C, 0xF6),  // purple
        new Color(0x10, 0xB9, 0x81),  // green
    };

    public TasksPanel(User currentUser) {
        this.currentUser = currentUser;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        // ── Top bar: title + new task button ──────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ThemeColors.CONTENT_BG);
        topBar.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel("Tasks & Goals");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JButton newTaskBtn = accentButton("+ New Task");
        newTaskBtn.addActionListener(e -> showNewTaskDialog());

        topBar.add(title,      BorderLayout.WEST);
        topBar.add(newTaskBtn, BorderLayout.EAST);

        // ── Kanban board ──────────────────────────────────────
        JPanel board = new JPanel(new GridLayout(1, 5, 12, 0));
        board.setBackground(ThemeColors.CONTENT_BG);
        board.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        columns = new JPanel[5];
        for (int i = 0; i < 5; i++) {
            columns[i] = buildColumn(i);
            board.add(columns[i]);
        }

        JScrollPane boardScroll = new JScrollPane(board);
        boardScroll.setBorder(null);
        boardScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        boardScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        wrapper.add(topBar,       BorderLayout.NORTH);
        wrapper.add(boardScroll,  BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        refreshData();
    }

    /** Builds the container for one Kanban column */
    private JPanel buildColumn(int colIndex) {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setBackground(new Color(0xF3, 0xF4, 0xF6));
        col.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 0, 12, 0)
        ));

        // Column header with colored top stripe
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(COLUMN_COLORS[colIndex]);
                g.fillRect(0, 0, getWidth(), 4);
            }
        };
        header.setBackground(new Color(0xF3, 0xF4, 0xF6));
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        JLabel titleLabel = new JLabel(COLUMN_TITLES[colIndex]);
        titleLabel.setFont(ThemeColors.FONT_BOLD);
        titleLabel.setForeground(COLUMN_COLORS[colIndex]);

        // Task count badge
        JLabel countBadge = new JLabel("0");
        countBadge.setFont(ThemeColors.FONT_SMALL);
        countBadge.setForeground(ThemeColors.TEXT_MUTED);
        countBadge.setName("COUNT_" + colIndex); // used for lookup

        header.add(titleLabel,  BorderLayout.WEST);
        header.add(countBadge,  BorderLayout.EAST);

        // Cards container — scrollable
        JPanel cards = new JPanel();
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setBackground(new Color(0xF3, 0xF4, 0xF6));
        cards.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        cards.setName("CARDS_" + colIndex); // used to find the right column

        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(0xF3, 0xF4, 0xF6));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        col.add(header, BorderLayout.NORTH);
        col.add(scroll, BorderLayout.CENTER);
        return col;
    }

    /** Loads tasks from DB and populates every column */
    public void refreshData() {
        List<Task> allTasks = db.getAllTasks();

        for (int i = 0; i < STATUSES.length; i++) {
            final int idx = i;
            String status = STATUSES[i];

            // Find the cards panel inside this column
            JScrollPane scroll = (JScrollPane) columns[i].getComponent(1);
            JPanel cards = (JPanel) scroll.getViewport().getView();
            cards.removeAll();

            List<Task> colTasks = allTasks.stream()
                .filter(t -> status.equals(t.getStatus()))
                .collect(Collectors.toList());

            // Update count badge
            JPanel header = (JPanel) columns[i].getComponent(0);
            ((JLabel) header.getComponent(1)).setText(String.valueOf(colTasks.size()));

            for (Task task : colTasks) {
                cards.add(buildTaskCard(task, idx));
                cards.add(Box.createVerticalStrut(8));
            }

            if (colTasks.isEmpty()) {
                JLabel empty = new JLabel("No tasks");
                empty.setFont(ThemeColors.FONT_SMALL);
                empty.setForeground(ThemeColors.TEXT_MUTED);
                empty.setBorder(BorderFactory.createEmptyBorder(16, 8, 0, 0));
                cards.add(empty);
            }

            cards.revalidate();
            cards.repaint();
        }
    }

    /** Builds one task card inside a column */
    private JPanel buildTaskCard(Task task, int colIndex) {
        RoundedPanel card = new RoundedPanel(8, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // ── Priority badge (left border stripe) ───────────────
        Color priorityColor = ThemeColors.getPriorityColor(task.getPriority());

        // Card with a colored left border
        JPanel wrapper = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeColors.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                // Left accent strip
                g2.setColor(priorityColor);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // Title
        JLabel titleLabel = new JLabel("<html><b>" + truncate(task.getTitle(), 30) + "</b></html>");
        titleLabel.setFont(ThemeColors.FONT_BODY);
        titleLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // Agent + deadline row
        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);

        String agentName = task.getAgentName() != null
            ? "🤖 " + task.getAgentName().split("—")[0].trim() : "Unassigned";
        JLabel agentLabel = new JLabel(agentName);
        agentLabel.setFont(ThemeColors.FONT_SMALL);
        agentLabel.setForeground(ThemeColors.TEXT_MUTED);

        JLabel deadlineLabel = new JLabel(task.getDeadline() != null
            ? "📅 " + DateUtils.formatDate(task.getDeadline()) : "");
        deadlineLabel.setFont(ThemeColors.FONT_SMALL);
        deadlineLabel.setForeground(ThemeColors.TEXT_MUTED);

        metaRow.add(agentLabel,   BorderLayout.WEST);
        metaRow.add(deadlineLabel,BorderLayout.EAST);

        // Priority badge
        JLabel priorityBadge = new JLabel(task.getPriorityLabel());
        priorityBadge.setFont(ThemeColors.FONT_SMALL);
        priorityBadge.setForeground(priorityColor);

        // Move buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.setOpaque(false);

        if (colIndex > 0) {
            JButton leftBtn = moveButton("←");
            leftBtn.addActionListener(e -> {
                db.updateTaskStatus(task.getId(), STATUSES[colIndex - 1]);
                db.logActivity("USER", currentUser.getId(),
                    "Moved task '" + task.getTitle() + "' to " + STATUSES[colIndex - 1], null);
                NotificationHelper.showInfo("Task moved to " + COLUMN_TITLES[colIndex - 1].split(" ")[1]);
                refreshData();
            });
            btnRow.add(leftBtn);
        }
        if (colIndex < STATUSES.length - 1) {
            JButton rightBtn = moveButton("→");
            rightBtn.addActionListener(e -> {
                db.updateTaskStatus(task.getId(), STATUSES[colIndex + 1]);
                db.logActivity("USER", currentUser.getId(),
                    "Moved task '" + task.getTitle() + "' to " + STATUSES[colIndex + 1], null);
                NotificationHelper.showInfo("Task moved to " + COLUMN_TITLES[colIndex + 1].split(" ")[1]);
                refreshData();
            });
            btnRow.add(rightBtn);
        }

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(priorityBadge, BorderLayout.WEST);
        bottom.add(btnRow,        BorderLayout.EAST);

        wrapper.add(titleLabel, BorderLayout.NORTH);
        wrapper.add(metaRow,    BorderLayout.CENTER);
        wrapper.add(bottom,     BorderLayout.SOUTH);
        return wrapper;
    }

    // ── New Task Dialog ───────────────────────────────────────

    private void showNewTaskDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Create New Task");
        dialog.setModal(true);
        dialog.setSize(440, 380);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JTextField titleField  = dialogField("Task Title");
        JTextField descField   = dialogField("Description (optional)");

        // Agent selector
        List<Agent> agents = db.getAllAgents();
        String[] agentNames = agents.stream()
            .map(a -> a.getName().split("—")[0].trim() + " (" + a.getDepartmentName() + ")")
            .toArray(String[]::new);
        JComboBox<String> agentCombo = new JComboBox<>(agentNames);
        agentCombo.setFont(ThemeColors.FONT_BODY);
        agentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        agentCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Priority selector
        JComboBox<String> priorityCombo = new JComboBox<>(
            new String[]{"LOW", "MEDIUM", "HIGH", "URGENT"});
        priorityCombo.setSelectedIndex(1);
        priorityCombo.setFont(ThemeColors.FONT_BODY);
        priorityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        priorityCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = accentButton("Create Task");
        createBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createBtn.addActionListener(e -> {
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a task title.");
                return;
            }
            int agentId = agents.get(agentCombo.getSelectedIndex()).getId();
            db.createTask(titleField.getText().trim(), descField.getText().trim(),
                agentId, (String) priorityCombo.getSelectedItem(),
                DateUtils.futureDate(7), currentUser.getId());
            db.logActivity("USER", currentUser.getId(),
                "Created task: " + titleField.getText().trim(), null);
            NotificationHelper.showSuccess("Task created and assigned!");
            refreshData();
            dialog.dispose();
        });

        panel.add(formLabel("Task Title")); panel.add(titleField);
        panel.add(Box.createVerticalStrut(12));
        panel.add(formLabel("Description")); panel.add(descField);
        panel.add(Box.createVerticalStrut(12));
        panel.add(formLabel("Assign to Agent")); panel.add(agentCombo);
        panel.add(Box.createVerticalStrut(12));
        panel.add(formLabel("Priority")); panel.add(priorityCombo);
        panel.add(Box.createVerticalStrut(20));
        panel.add(createBtn);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────

    private JTextField dialogField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(ThemeColors.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(ThemeColors.FONT_BOLD);
        l.setForeground(ThemeColors.TEXT_PRIMARY);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton moveButton(String arrow) {
        JButton btn = new JButton(arrow);
        btn.setFont(ThemeColors.FONT_SMALL);
        btn.setForeground(ThemeColors.TEXT_SECONDARY);
        btn.setBackground(ThemeColors.BORDER);
        btn.setPreferredSize(new Dimension(26, 22));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
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
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(130, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
