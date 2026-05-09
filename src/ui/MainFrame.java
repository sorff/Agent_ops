package ui;

import db.DatabaseHelper;
import models.User;
import ui.panels.*;
import utils.NotificationHelper;
import utils.SimulationEngine;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * The main application window.
 *
 * Layout (3 zones):
 *   LEFT   — dark sidebar with navigation items (220px wide)
 *   TOP    — white top bar with page title + user info (60px tall)
 *   CENTER — content area that switches panels based on selected nav item
 *
 * The content area uses CardLayout so switching pages is instant
 * (like tab navigation, but styled as a sidebar).
 *
 * SimulationEngine runs in the background and updates agent states
 * every few seconds, making the dashboard feel "live".
 */
public class MainFrame extends JFrame {

    // The currently logged-in user — passed to panels that need to know who's logged in
    private final User currentUser;

    // The layout that controls which panel is currently visible
    private CardLayout contentLayout;
    private JPanel     contentPanel;

    // Top bar title — changes when you switch pages
    private JLabel pageTitleLabel;

    // Sidebar buttons — kept in an array so we can highlight the selected one
    private JPanel[] navItems;
    private int selectedNavIndex = 0;

    // The simulation engine that makes agents look "alive"
    private SimulationEngine simulationEngine;

    // All panel instances — created once and reused
    private DashboardPanel     dashboardPanel;
    private OrgChartPanel      orgChartPanel;
    private DepartmentsPanel   departmentsPanel;
    private TasksPanel         tasksPanel;
    private ApprovalCenterPanel approvalPanel;
    private CommunicationPanel communicationPanel;
    private AlertsPanel        alertsPanel;
    private ReportsPanel       reportsPanel;
    private MarketplacePanel   marketplacePanel;
    private SettingsPanel      settingsPanel;
    private ActivityLogPanel   activityLogPanel;

    // Navigation items: { icon, label, card name }
    private static final Object[][] NAV_ITEMS = {
        { "🏠", "Dashboard",       "DASHBOARD"   },
        { "🌐", "Org Chart",       "ORG_CHART"   },
        { "🏢", "Departments",     "DEPARTMENTS" },
        { "📋", "Tasks & Goals",   "TASKS"       },
        { "✅", "Approvals",       "APPROVALS"   },
        { "💬", "Communication",   "COMMUNICATION"},
        { "🔔", "Alerts",          "ALERTS"      },
        { "📊", "Reports",         "REPORTS"     },
        { "🛒", "Marketplace",     "MARKETPLACE" },
        { "⚙", "Settings",        "SETTINGS"    },
        { "📜", "Activity Log",    "ACTIVITY"    },
    };

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("AgentOps — " + user.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        buildUI();
        startSimulation();

        // Shut down cleanly when the window closes
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (simulationEngine != null) simulationEngine.stop();
                DatabaseHelper.getInstance().close();
            }
        });
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ThemeColors.CONTENT_BG);

        // Build the three zones
        root.add(buildSidebar(),     BorderLayout.WEST);
        root.add(buildMainArea(),    BorderLayout.CENTER);

        setContentPane(root);
    }

    // ── Sidebar ───────────────────────────────────────────────

    /** Builds the dark left sidebar with logo, nav items, and user badge */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(ThemeColors.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(220, 0));

        // ── Logo area ─────────────────────────────────────────
        JPanel logoArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 18));
        logoArea.setBackground(ThemeColors.SIDEBAR_BG);
        logoArea.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                           new Color(255, 255, 255, 20)));

        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel logoText = new JLabel("AgentOps");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoText.setForeground(Color.WHITE);

        logoArea.add(logoIcon);
        logoArea.add(logoText);

        // ── Nav items ─────────────────────────────────────────
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(ThemeColors.SIDEBAR_BG);
        navPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        navItems = new JPanel[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            navItems[i] = buildNavItem(i,
                (String) NAV_ITEMS[i][0],
                (String) NAV_ITEMS[i][1],
                (String) NAV_ITEMS[i][2]);
            navPanel.add(navItems[i]);

            // Small section divider before "Settings"
            if (i == 7) {
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(255, 255, 255, 15));
                sep.setBackground(ThemeColors.SIDEBAR_BG);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                navPanel.add(Box.createVerticalStrut(6));
                navPanel.add(sep);
                navPanel.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane navScroll = new JScrollPane(navPanel);
        navScroll.setBackground(ThemeColors.SIDEBAR_BG);
        navScroll.setBorder(null);
        navScroll.getViewport().setBackground(ThemeColors.SIDEBAR_BG);
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // ── User badge at the bottom ──────────────────────────
        JPanel userBadge = buildUserBadge();

        sidebar.add(logoArea,  BorderLayout.NORTH);
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(userBadge, BorderLayout.SOUTH);

        return sidebar;
    }

    /** Builds one clickable navigation item in the sidebar */
    private JPanel buildNavItem(int index, String icon, String label, String cardName) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (selectedNavIndex == index) {
                    // Accent background for selected item
                    g2.setColor(ThemeColors.SIDEBAR_SELECTED);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // Left border accent strip
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, 3, getHeight());
                } else if (getMousePosition() != null) {
                    g2.setColor(ThemeColors.SIDEBAR_HOVER);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2.setColor(ThemeColors.SIDEBAR_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JLabel labelLabel = new JLabel(label);
        labelLabel.setFont(selectedNavIndex == index
            ? ThemeColors.FONT_SIDEBAR_BOLD : ThemeColors.FONT_SIDEBAR);
        labelLabel.setForeground(selectedNavIndex == index
            ? ThemeColors.SIDEBAR_TEXT_ACTIVE : ThemeColors.SIDEBAR_TEXT);

        item.add(iconLabel);
        item.add(labelLabel);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                switchTo(index, label, cardName);
            }
            @Override public void mouseEntered(MouseEvent e) { item.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { item.repaint(); }
        });

        return item;
    }

    /** Small panel at the bottom of the sidebar showing the logged-in user */
    private JPanel buildUserBadge() {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        badge.setBackground(new Color(0x0F, 0x0F, 0x20));
        badge.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                        new Color(255, 255, 255, 20)));

        // Avatar circle with initials
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeColors.ACCENT);
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String init = getInitials(currentUser.getName());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, (32 - fm.stringWidth(init))/2,
                              (32 - fm.getHeight())/2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(32, 32));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(currentUser.getName());
        nameLabel.setFont(ThemeColors.FONT_SIDEBAR_BOLD);
        nameLabel.setForeground(Color.WHITE);

        JLabel roleLabel = new JLabel(currentUser.getRole());
        roleLabel.setFont(ThemeColors.FONT_SMALL);
        roleLabel.setForeground(new Color(0x99, 0xA5, 0xB4));

        // Logout button
        JLabel logout = new JLabel("Log out");
        logout.setFont(ThemeColors.FONT_SMALL);
        logout.setForeground(new Color(0xFF, 0x75, 0x75));
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                handleLogout();
            }
        });

        info.add(nameLabel);
        info.add(roleLabel);
        info.add(logout);

        badge.add(avatar);
        badge.add(info);
        return badge;
    }

    // ── Main area (top bar + content) ─────────────────────────

    private JPanel buildMainArea() {
        JPanel area = new JPanel(new BorderLayout(0, 0));
        area.setBackground(ThemeColors.CONTENT_BG);

        area.add(buildTopBar(),     BorderLayout.NORTH);
        area.add(buildContentArea(),BorderLayout.CENTER);

        return area;
    }

    /** Top bar — shows page title + notification bell + user greeting */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(ThemeColors.TOPBAR_BG);
        bar.setPreferredSize(new Dimension(0, 64));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(0, 24, 0, 24)
        ));

        pageTitleLabel = new JLabel("Dashboard");
        pageTitleLabel.setFont(ThemeColors.FONT_SUBTITLE);
        pageTitleLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // Right side: notification bell + greeting
        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightSide.setOpaque(false);

        JLabel bellBtn = new JLabel("🔔");
        bellBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        bellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellBtn.setToolTipText("Notifications");
        bellBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                switchTo(6, "Alerts", "ALERTS");
            }
        });

        JLabel greeting = new JLabel("Hi, " + getFirstName(currentUser.getName()) + " 👋");
        greeting.setFont(ThemeColors.FONT_BODY);
        greeting.setForeground(ThemeColors.TEXT_SECONDARY);

        rightSide.add(bellBtn);
        rightSide.add(greeting);

        bar.add(pageTitleLabel, BorderLayout.WEST);
        bar.add(rightSide,      BorderLayout.EAST);
        return bar;
    }

    /**
     * Creates the content area where panels are shown.
     * CardLayout lets us swap panels without recreating them.
     */
    private JPanel buildContentArea() {
        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(ThemeColors.CONTENT_BG);

        // Create all panels (done once at startup — they get refreshed when shown)
        dashboardPanel    = new DashboardPanel(currentUser, this);
        orgChartPanel     = new OrgChartPanel(currentUser, this);
        departmentsPanel  = new DepartmentsPanel(currentUser, this);
        tasksPanel        = new TasksPanel(currentUser);
        approvalPanel     = new ApprovalCenterPanel(currentUser);
        communicationPanel= new CommunicationPanel(currentUser);
        alertsPanel       = new AlertsPanel();
        reportsPanel      = new ReportsPanel();
        marketplacePanel  = new MarketplacePanel(currentUser, this);
        settingsPanel     = new SettingsPanel(currentUser);
        activityLogPanel  = new ActivityLogPanel();

        contentPanel.add(dashboardPanel,     "DASHBOARD");
        contentPanel.add(orgChartPanel,      "ORG_CHART");
        contentPanel.add(departmentsPanel,   "DEPARTMENTS");
        contentPanel.add(tasksPanel,         "TASKS");
        contentPanel.add(approvalPanel,      "APPROVALS");
        contentPanel.add(communicationPanel, "COMMUNICATION");
        contentPanel.add(alertsPanel,        "ALERTS");
        contentPanel.add(reportsPanel,       "REPORTS");
        contentPanel.add(marketplacePanel,   "MARKETPLACE");
        contentPanel.add(settingsPanel,      "SETTINGS");
        contentPanel.add(activityLogPanel,   "ACTIVITY");

        // Show dashboard by default
        contentLayout.show(contentPanel, "DASHBOARD");

        return contentPanel;
    }

    // ── Navigation ────────────────────────────────────────────

    /**
     * Switches the content area to the given panel and highlights the sidebar item.
     * Call this from any panel that needs to navigate to another panel
     * (e.g., clicking an agent name in DashboardPanel navigates to OrgChartPanel).
     */
    public void switchTo(int navIndex, String title, String cardName) {
        selectedNavIndex = navIndex;

        // Update page title
        pageTitleLabel.setText(title);

        // Refresh the sidebar to show the new selection
        for (JPanel item : navItems) item.repaint();

        // Update nav item font styles
        for (int i = 0; i < navItems.length; i++) {
            Component[] comps = navItems[i].getComponents();
            for (Component c : comps) {
                if (c instanceof JLabel && !(((JLabel) c).getFont().getSize() >= 16)) {
                    ((JLabel) c).setFont(i == navIndex
                        ? ThemeColors.FONT_SIDEBAR_BOLD : ThemeColors.FONT_SIDEBAR);
                    ((JLabel) c).setForeground(i == navIndex
                        ? ThemeColors.SIDEBAR_TEXT_ACTIVE : ThemeColors.SIDEBAR_TEXT);
                }
            }
        }

        // Show the target panel
        contentLayout.show(contentPanel, cardName);

        // Refresh the panel's data so it shows fresh information
        refreshCurrentPanel(cardName);
    }

    /** Calls the refresh method on whichever panel was just switched to */
    private void refreshCurrentPanel(String cardName) {
        switch (cardName) {
            case "DASHBOARD":    dashboardPanel.refreshData();     break;
            case "ORG_CHART":    orgChartPanel.refreshData();      break;
            case "DEPARTMENTS":  departmentsPanel.refreshData();   break;
            case "TASKS":        tasksPanel.refreshData();         break;
            case "APPROVALS":    approvalPanel.refreshData();      break;
            case "COMMUNICATION":communicationPanel.refreshData(); break;
            case "ALERTS":       alertsPanel.refreshData();        break;
            case "REPORTS":      reportsPanel.refreshData();       break;
            case "MARKETPLACE":  marketplacePanel.refreshData();   break;
            case "ACTIVITY":     activityLogPanel.refreshData();   break;
        }
    }

    /** Navigates to the Agent Detail view for a specific agent */
    public void openAgentDetail(int agentId) {
        // Find or create the AgentDetailPanel and add it to the card layout
        AgentDetailPanel detailPanel = new AgentDetailPanel(agentId, currentUser, this);
        contentPanel.add(detailPanel, "AGENT_DETAIL");
        pageTitleLabel.setText("Agent Profile");
        contentLayout.show(contentPanel, "AGENT_DETAIL");
        // Deselect the sidebar (no item matches "Agent Detail")
        selectedNavIndex = -1;
        for (JPanel item : navItems) item.repaint();
    }

    // ── Simulation ────────────────────────────────────────────

    /** Starts the background simulation that makes agents look alive */
    private void startSimulation() {
        simulationEngine = new SimulationEngine(() -> {
            // After each simulation tick, refresh the currently visible dashboard
            if (selectedNavIndex == 0) {
                dashboardPanel.refreshData();
            }
        });
        simulationEngine.start();
    }

    // ── Logout ────────────────────────────────────────────────

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Log out of AgentOps?", "Confirm Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (simulationEngine != null) simulationEngine.stop();
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return parts[0].substring(0,1) + parts[1].substring(0,1);
        return name.length() >= 2 ? name.substring(0,2).toUpperCase() : name.toUpperCase();
    }

    private String getFirstName(String name) {
        return name.split("\\s+")[0];
    }

    /** Returns the current logged-in user — used by panels that need to know the user */
    public User getCurrentUser() { return currentUser; }
}
