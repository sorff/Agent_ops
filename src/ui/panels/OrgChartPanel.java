package ui.panels;

import db.DatabaseHelper;
import models.Agent;
import models.Department;
import models.User;
import ui.MainFrame;
import ui.components.RoundedPanel;
import ui.components.StatusDot;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Visual hierarchy page — shows the Company → Department → Manager → Specialist tree.
 *
 * Features:
 *   • Drawn tree diagram with connecting lines
 *   • Node cards showing agent name, status, current task
 *   • Click any node to open the Agent Detail panel
 *   • Filter by department, status, role type
 *   • Toggle between Tree View / Grid View / List View
 */
public class OrgChartPanel extends JPanel {

    private final MainFrame mainFrame;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    // Current view mode
    private String viewMode = "TREE"; // "TREE", "GRID", "LIST"

    // Filters
    private String filterDept   = "All";
    private String filterStatus = "All";

    // Tree canvas — where the actual drawing happens
    private TreeCanvas treeCanvas;
    private JScrollPane treeScroll;

    // The grid and list panels (alternate views)
    private JPanel gridPanel;
    private JPanel listPanel;

    // Container panel that switches between the three views
    private CardLayout viewLayout;
    private JPanel viewContainer;

    public OrgChartPanel(User user, MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        // ── Top toolbar: filters + view toggle ────────────────
        add(buildToolbar(), BorderLayout.NORTH);

        // ── View container: Tree / Grid / List ────────────────
        viewLayout    = new CardLayout();
        viewContainer = new JPanel(viewLayout);
        viewContainer.setBackground(ThemeColors.CONTENT_BG);

        // Tree view
        treeCanvas = new TreeCanvas();
        treeScroll = new JScrollPane(treeCanvas);
        treeScroll.setBorder(null);
        treeScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        viewContainer.add(treeScroll, "TREE");

        // Grid view
        gridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        gridPanel.setBackground(ThemeColors.CONTENT_BG);
        JScrollPane gridScroll = new JScrollPane(gridPanel);
        gridScroll.setBorder(null);
        gridScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        viewContainer.add(gridScroll, "GRID");

        // List view
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(ThemeColors.CONTENT_BG);
        JScrollPane listScroll = new JScrollPane(listPanel);
        listScroll.setBorder(null);
        listScroll.getViewport().setBackground(ThemeColors.CONTENT_BG);
        viewContainer.add(listScroll, "LIST");

        add(viewContainer, BorderLayout.CENTER);

        refreshData();
    }

    /** Builds the toolbar with department filter, status filter, and view toggles */
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ThemeColors.CONTENT_BG);
        bar.setBorder(BorderFactory.createEmptyBorder(16, 24, 12, 24));

        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSide.setOpaque(false);

        // Department filter
        leftSide.add(filterLabel("Department:"));
        JComboBox<String> deptFilter = styledCombo(
            new String[]{"All", "Sales", "Marketing", "Support", "HR", "Finance", "Logistics"});
        deptFilter.addActionListener(e -> {
            filterDept = (String) deptFilter.getSelectedItem();
            refreshData();
        });
        leftSide.add(deptFilter);

        // Status filter
        leftSide.add(Box.createHorizontalStrut(16));
        leftSide.add(filterLabel("Status:"));
        JComboBox<String> statusFilter = styledCombo(
            new String[]{"All", "ACTIVE", "IDLE", "PAUSED", "ERROR"});
        statusFilter.addActionListener(e -> {
            filterStatus = (String) statusFilter.getSelectedItem();
            refreshData();
        });
        leftSide.add(statusFilter);

        // View toggle buttons on the right
        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        toggles.setOpaque(false);

        for (String[] v : new String[][]{{"🌲", "TREE"}, {"⊞", "GRID"}, {"☰", "LIST"}}) {
            JToggleButton btn = viewToggleButton(v[0], v[1]);
            if (v[1].equals(viewMode)) btn.setSelected(true);
            toggles.add(btn);
        }

        bar.add(leftSide, BorderLayout.WEST);
        bar.add(toggles,  BorderLayout.EAST);
        return bar;
    }

    /** Creates a view toggle button (Tree/Grid/List) */
    private JToggleButton viewToggleButton(String icon, String mode) {
        JToggleButton btn = new JToggleButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isSelected() ? ThemeColors.ACCENT : ThemeColors.CARD_BG;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setForeground(ThemeColors.TEXT_SECONDARY);
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            viewMode = mode;
            viewLayout.show(viewContainer, mode);
            refreshData();
        });
        return btn;
    }

    /** Reloads all agent data from the DB and refreshes whichever view is active */
    public void refreshData() {
        List<Department> departments = db.getAllDepartments();
        List<Agent> agents = db.getAllAgents();

        // Apply filters
        List<Agent> filtered = agents.stream().filter(a -> {
            boolean deptMatch   = "All".equals(filterDept)   || filterDept.equals(a.getDepartmentName());
            boolean statusMatch = "All".equals(filterStatus) || filterStatus.equals(a.getStatus());
            return deptMatch && statusMatch;
        }).collect(Collectors.toList());

        switch (viewMode) {
            case "TREE": treeCanvas.setData(departments, filtered); break;
            case "GRID": buildGridView(filtered); break;
            case "LIST": buildListView(filtered); break;
        }
    }

    /** Populates the grid view with AgentCards */
    private void buildGridView(List<Agent> agents) {
        gridPanel.removeAll();
        for (Agent ag : agents) {
            JPanel card = buildGridCard(ag);
            gridPanel.add(card);
        }
        if (agents.isEmpty()) {
            JLabel empty = new JLabel("No agents match the current filters.");
            empty.setFont(ThemeColors.FONT_BODY);
            empty.setForeground(ThemeColors.TEXT_MUTED);
            gridPanel.add(empty);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    /** Builds one card for the grid view */
    private JPanel buildGridCard(Agent agent) {
        RoundedPanel card = new RoundedPanel(10, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setPreferredSize(new Dimension(210, 110));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel name = new JLabel(agent.getName().split("—")[0].trim());
        name.setFont(ThemeColors.FONT_BOLD);
        name.setForeground(ThemeColors.TEXT_PRIMARY);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusRow.setOpaque(false);
        statusRow.add(new StatusDot(agent.getStatus(), 8));
        JLabel statusLabel = new JLabel(agent.getStatus());
        statusLabel.setFont(ThemeColors.FONT_SMALL);
        statusLabel.setForeground(ThemeColors.getStatusColor(agent.getStatus()));
        statusRow.add(statusLabel);

        JLabel dept = new JLabel(agent.getDepartmentName() != null ? agent.getDepartmentName() : "");
        dept.setFont(ThemeColors.FONT_SMALL);
        dept.setForeground(ThemeColors.TEXT_MUTED);

        card.add(name,      BorderLayout.NORTH);
        card.add(statusRow, BorderLayout.CENTER);
        card.add(dept,      BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.openAgentDetail(agent.getId());
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

    /** Populates the list view (table-like rows) */
    private void buildListView(List<Agent> agents) {
        listPanel.removeAll();
        listPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        // Header row
        JPanel header = new JPanel(new GridLayout(1, 5));
        header.setBackground(new Color(0xF9, 0xFA, 0xFB));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        for (String h : new String[]{"Name", "Role", "Department", "Status", "Performance"}) {
            JLabel lbl = new JLabel(h);
            lbl.setFont(ThemeColors.FONT_BOLD);
            lbl.setForeground(ThemeColors.TEXT_SECONDARY);
            header.add(lbl);
        }
        listPanel.add(header);

        for (Agent ag : agents) {
            JPanel row = buildListRow(ag);
            listPanel.add(row);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildListRow(Agent agent) {
        JPanel row = new JPanel(new GridLayout(1, 5));
        row.setBackground(ThemeColors.CARD_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String name = agent.getName().split("—")[0].trim();
        row.add(labelCell(name, ThemeColors.TEXT_PRIMARY, true));
        row.add(labelCell(agent.getRoleLabel(), ThemeColors.TEXT_SECONDARY, false));
        row.add(labelCell(agent.getDepartmentName() != null ? agent.getDepartmentName() : "—",
                          ThemeColors.TEXT_SECONDARY, false));

        JPanel statusCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusCell.setOpaque(false);
        statusCell.add(new StatusDot(agent.getStatus(), 8));
        statusCell.add(labelCell(agent.getStatus(), ThemeColors.getStatusColor(agent.getStatus()), false));
        row.add(statusCell);

        row.add(labelCell(String.format("%.0f%%", agent.getPerformanceScore()),
                          getScoreColor(agent.getPerformanceScore()), false));

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.openAgentDetail(agent.getId());
            }
            @Override public void mouseEntered(MouseEvent e) {
                row.setBackground(ThemeColors.ACCENT_VERY_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setBackground(ThemeColors.CARD_BG);
            }
        });
        return row;
    }

    // ── Helper methods ────────────────────────────────────────

    private JLabel labelCell(String text, Color color, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(bold ? ThemeColors.FONT_BOLD : ThemeColors.FONT_BODY);
        lbl.setForeground(color);
        return lbl;
    }

    private Color getScoreColor(double score) {
        if (score >= 85) return ThemeColors.STATUS_ACTIVE;
        if (score >= 70) return ThemeColors.STATUS_IDLE;
        return ThemeColors.STATUS_ERROR;
    }

    private JLabel filterLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeColors.FONT_BODY);
        lbl.setForeground(ThemeColors.TEXT_SECONDARY);
        return lbl;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(ThemeColors.FONT_BODY);
        combo.setBackground(ThemeColors.CARD_BG);
        combo.setPreferredSize(new Dimension(130, 32));
        return combo;
    }

    // ── Tree Canvas: custom-drawn org chart ───────────────────

    /**
     * A JPanel subclass that draws the tree hierarchy using Java2D graphics.
     * The tree is:   Company → Departments → Manager Agents → Specialist Agents
     */
    private class TreeCanvas extends JPanel {

        private List<Department> departments = new ArrayList<>();
        private List<Agent>      agents      = new ArrayList<>();

        // Node layout info (x, y, width, height per node)
        private final List<NodeInfo> nodes = new ArrayList<>();

        // Dimensions
        private static final int NODE_W  = 170;
        private static final int NODE_H  = 64;
        private static final int H_GAP   = 24;
        private static final int V_GAP   = 60;

        TreeCanvas() {
            setBackground(ThemeColors.CONTENT_BG);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    handleClick(e.getX(), e.getY());
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    boolean onNode = nodes.stream().anyMatch(n ->
                        e.getX() >= n.x && e.getX() <= n.x + NODE_W &&
                        e.getY() >= n.y && e.getY() <= n.y + NODE_H);
                    setCursor(Cursor.getPredefinedCursor(
                        onNode ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            });
        }

        void setData(List<Department> depts, List<Agent> agentList) {
            this.departments = depts;
            this.agents      = agentList;
            nodes.clear();
            calculateLayout();
            repaint();
        }

        /** Calculates x,y positions for every node in the tree */
        private void calculateLayout() {
            if (departments.isEmpty()) return;

            int startX = 40;
            int startY = 40;
            int currentX = startX;

            // Company root node
            nodes.add(new NodeInfo(null, "Company", "ROOT", "root", 0, 0));

            for (Department dept : departments) {
                List<Agent> deptAgents = agents.stream()
                    .filter(a -> a.getDepartmentId() == dept.getId())
                    .collect(Collectors.toList());

                // Find manager
                Agent manager = deptAgents.stream()
                    .filter(a -> Agent.ROLE_MANAGER.equals(a.getRoleType()))
                    .findFirst().orElse(null);

                // Find specialists
                List<Agent> specialists = deptAgents.stream()
                    .filter(a -> Agent.ROLE_SPECIALIST.equals(a.getRoleType()))
                    .collect(Collectors.toList());

                int numSpecialists = Math.max(1, specialists.size());
                int deptWidth = numSpecialists * (NODE_W + H_GAP);

                // Department node (row 1)
                int deptX = currentX + deptWidth / 2 - NODE_W / 2;
                nodes.add(new NodeInfo(dept, dept.getName(), "DEPT",
                                       dept.getName(), deptX, startY + V_GAP + NODE_H));

                // Manager node (row 2)
                if (manager != null) {
                    nodes.add(new NodeInfo(manager, getShortName(manager.getName()), "MANAGER",
                                           dept.getName(), deptX, startY + 2 * (V_GAP + NODE_H)));
                }

                // Specialist nodes (row 3)
                int specX = currentX;
                for (Agent spec : specialists) {
                    nodes.add(new NodeInfo(spec, getShortName(spec.getName()), "SPECIALIST",
                                           dept.getName(), specX, startY + 3 * (V_GAP + NODE_H)));
                    specX += NODE_W + H_GAP;
                }

                currentX += deptWidth + 40;
            }

            // Set preferred size so the scroll pane can scroll
            int totalWidth  = currentX + 40;
            int totalHeight = startY + 4 * (V_GAP + NODE_H) + 60;
            setPreferredSize(new Dimension(totalWidth, totalHeight));

            // Position the company root node at center
            if (!nodes.isEmpty()) {
                NodeInfo rootNode = nodes.get(0);
                rootNode.x = totalWidth / 2 - NODE_W / 2;
                rootNode.y = startY;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw connecting lines first (behind nodes)
            g2.setColor(new Color(0xD1, 0xD5, 0xDB));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawConnectingLines(g2);

            // Draw all nodes on top of lines
            for (NodeInfo node : nodes) {
                drawNode(g2, node);
            }

            g2.dispose();
        }

        /** Draws the connecting lines between parent and child nodes */
        private void drawConnectingLines(Graphics2D g2) {
            if (nodes.size() < 2) return;

            NodeInfo root = nodes.get(0);
            int rootCX = root.x + NODE_W / 2;
            int rootBY = root.y + NODE_H;

            // Root → each dept
            for (NodeInfo node : nodes) {
                if ("DEPT".equals(node.type)) {
                    int cx = node.x + NODE_W / 2;
                    int ty = node.y;
                    // Elbow line: down from root, then horizontal, then up
                    g2.drawLine(rootCX, rootBY, rootCX, rootBY + V_GAP/2);
                    g2.drawLine(rootCX, rootBY + V_GAP/2, cx, rootBY + V_GAP/2);
                    g2.drawLine(cx, rootBY + V_GAP/2, cx, ty);
                }
            }

            // Dept → Manager
            for (NodeInfo deptNode : nodes) {
                if (!"DEPT".equals(deptNode.type)) continue;
                int dcx = deptNode.x + NODE_W / 2;
                int dby = deptNode.y + NODE_H;
                for (NodeInfo mgr : nodes) {
                    if ("MANAGER".equals(mgr.type) && mgr.deptName.equals(deptNode.deptName)) {
                        int mcx = mgr.x + NODE_W / 2;
                        g2.drawLine(dcx, dby, dcx, dby + V_GAP/2);
                        g2.drawLine(dcx, dby + V_GAP/2, mcx, dby + V_GAP/2);
                        g2.drawLine(mcx, dby + V_GAP/2, mcx, mgr.y);

                        // Manager → each specialist in same dept
                        for (NodeInfo spec : nodes) {
                            if ("SPECIALIST".equals(spec.type) && spec.deptName.equals(mgr.deptName)) {
                                int scx = spec.x + NODE_W / 2;
                                int mby = mgr.y + NODE_H;
                                g2.drawLine(mcx, mby, mcx, mby + V_GAP/2);
                                g2.drawLine(mcx, mby + V_GAP/2, scx, mby + V_GAP/2);
                                g2.drawLine(scx, mby + V_GAP/2, scx, spec.y);
                            }
                        }
                    }
                }
            }
        }

        /** Draws one node (a rounded rectangle card) */
        private void drawNode(Graphics2D g2, NodeInfo node) {
            // Pick background color by type
            Color bg, border, textColor;
            switch (node.type) {
                case "ROOT":
                    bg = ThemeColors.ACCENT;
                    border = ThemeColors.ACCENT;
                    textColor = Color.WHITE;
                    break;
                case "DEPT":
                    bg = new Color(0xEE, 0xF2, 0xFF);
                    border = ThemeColors.ACCENT;
                    textColor = ThemeColors.ACCENT;
                    break;
                case "MANAGER":
                    bg = ThemeColors.CARD_BG;
                    border = new Color(0x10, 0xB9, 0x81);
                    textColor = ThemeColors.TEXT_PRIMARY;
                    break;
                default: // SPECIALIST
                    bg = ThemeColors.CARD_BG;
                    border = ThemeColors.BORDER;
                    textColor = ThemeColors.TEXT_PRIMARY;
                    break;
            }

            // Draw shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fill(new RoundRectangle2D.Float(node.x + 2, node.y + 3, NODE_W, NODE_H, 10, 10));

            // Draw background
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(node.x, node.y, NODE_W, NODE_H, 10, 10));

            // Draw border
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(node.x + 0.5f, node.y + 0.5f,
                                               NODE_W - 1, NODE_H - 1, 10, 10));

            // Draw agent name
            g2.setColor(textColor);
            g2.setFont(ThemeColors.FONT_BOLD);
            String name = node.label.length() > 18 ? node.label.substring(0, 18) + "…" : node.label;
            g2.drawString(name, node.x + 12, node.y + 24);

            // Draw status dot + agent status for agent nodes
            if (node.agent != null) {
                Color statusColor = ThemeColors.getStatusColor(node.agent.getStatus());
                g2.setColor(statusColor);
                g2.fill(new Ellipse2D.Float(node.x + 12, node.y + 36, 8, 8));
                g2.setColor(ThemeColors.TEXT_MUTED);
                g2.setFont(ThemeColors.FONT_SMALL);
                g2.drawString(node.agent.getStatus(), node.x + 26, node.y + 44);
                // Performance score on the right
                g2.drawString(String.format("%.0f%%", node.agent.getPerformanceScore()),
                              node.x + NODE_W - 36, node.y + 44);
            }
        }

        /** Called when the user clicks on the canvas — checks if they clicked a node */
        private void handleClick(int mx, int my) {
            for (NodeInfo node : nodes) {
                if (mx >= node.x && mx <= node.x + NODE_W &&
                    my >= node.y && my <= node.y + NODE_H) {
                    if (node.agent != null) {
                        mainFrame.openAgentDetail(node.agent.getId());
                    }
                    return;
                }
            }
        }

        private String getShortName(String name) {
            return name.contains("—") ? name.split("—")[0].trim() : name;
        }
    }

    /** Holds position and data for one tree node */
    private static class NodeInfo {
        Agent agent;       // null for dept/root nodes
        String label;      // display text
        String type;       // "ROOT", "DEPT", "MANAGER", "SPECIALIST"
        String deptName;   // which department this belongs to
        int x, y;

        NodeInfo(Object obj, String label, String type, String deptName, int x, int y) {
            this.agent    = obj instanceof Agent ? (Agent) obj : null;
            this.label    = label;
            this.type     = type;
            this.deptName = deptName;
            this.x        = x;
            this.y        = y;
        }
    }

}

/**
 * A FlowLayout that wraps components to the next line when the container is too narrow.
 * Used for the grid view of agent cards.
 */
class WrapLayout extends FlowLayout {
    WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }
    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
            int hgap = getHgap(), vgap = getVgap();
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;
            int width = 0, height = 0, rowWidth = 0, rowHeight = 0;
            for (Component comp : target.getComponents()) {
                if (!comp.isVisible()) continue;
                Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                    width = Math.max(width, rowWidth);
                    height += rowHeight + vgap;
                    rowWidth = 0; rowHeight = 0;
                }
                rowWidth += d.width + hgap;
                rowHeight = Math.max(rowHeight, d.height);
            }
            width = Math.max(width, rowWidth);
            height += rowHeight + insets.top + insets.bottom + vgap * 2;
            return new Dimension(width, height);
        }
    }
}
