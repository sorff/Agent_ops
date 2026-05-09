package ui.panels;

import db.DatabaseHelper;
import models.User;
import ui.MainFrame;
import ui.components.RoundedPanel;
import utils.NotificationHelper;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Agent Marketplace — browse and "hire" pre-built agent templates.
 *
 * Each template card shows:
 *   • Role name + emoji icon
 *   • Department fit
 *   • Capabilities list
 *   • Suggested KPIs
 *   • [Hire] button that adds the agent to the selected department
 *
 * Templates are grouped by industry / department type.
 * Admin-only feature (supervisors can browse but not hire).
 */
public class MarketplacePanel extends JPanel {

    private final User currentUser;
    private final MainFrame mainFrame;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private String currentFilter = "ALL";
    private JPanel gridPanel;

    // Agent template data: { icon, name, dept, capabilities[], kpis }
    private static final Object[][] TEMPLATES = {
        {"💼","Lead Qualifier",    "Sales",     new String[]{"Lead Scoring","CRM Integration","Email Outreach"},             "Lead Quality Rate ≥ 80%"},
        {"📞","Outreach Agent",    "Sales",     new String[]{"Cold Email Sequencing","Follow-up Scheduling","LinkedIn Auto"},"Reply Rate ≥ 12%"},
        {"🤝","Deal Closer",       "Sales",     new String[]{"Contract Drafting","Negotiation Assist","CRM Updates"},        "Win Rate ≥ 25%"},
        {"✍","Content Writer",    "Marketing", new String[]{"Blog Writing","SEO Copywriting","Social Captions"},            "Posts/Week ≥ 3"},
        {"🔍","SEO Analyst",       "Marketing", new String[]{"Keyword Research","On-Page SEO","Backlink Analysis"},         "Organic Traffic +15%"},
        {"📣","Social Media Agent","Marketing", new String[]{"Post Scheduling","Hashtag Strategy","Engagement Tracking"},   "Engagement Rate ≥ 4%"},
        {"🎧","Tier-1 Responder",  "Support",   new String[]{"FAQ Resolution","Chat Support","Ticket Triage"},              "CSAT ≥ 90%"},
        {"🚨","Escalation Handler","Support",   new String[]{"Complex Resolution","Refund Processing","VIP Support"},       "Resolution Rate ≥ 95%"},
        {"🔎","Recruiter",         "HR",        new String[]{"Job Posting","Resume Screening","Interview Scheduling"},      "Time-to-Hire ≤ 21 days"},
        {"👋","Onboarding Agent",  "HR",        new String[]{"New Hire Paperwork","IT Setup Requests","Day-1 Welcome"},     "Onboarding Score ≥ 4.5"},
        {"📒","Bookkeeper",        "Finance",   new String[]{"Transaction Logging","Bank Reconciliation","Expense Categor."},"Accuracy ≥ 99%"},
        {"🧾","Invoice Tracker",   "Finance",   new String[]{"Invoice Generation","Payment Follow-up","Vendor Mgmt"},       "Overdue Rate ≤ 5%"},
        {"📦","Inventory Monitor", "Logistics", new String[]{"Stock Level Monitoring","Reorder Alerts","Supplier Coord."},  "Stockout Rate ≤ 2%"},
        {"🚚","Shipment Tracker",  "Logistics", new String[]{"Carrier Integration","Delivery ETA Updates","Exceptions"},    "On-Time Delivery ≥ 95%"},
    };

    private static final String[] FILTER_OPTIONS = {"ALL","Sales","Marketing","Support","HR","Finance","Logistics"};

    public MarketplacePanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame   = mainFrame;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        wrapper.add(buildHeader(), BorderLayout.NORTH);
        wrapper.add(buildGrid(),   BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setBackground(ThemeColors.CONTENT_BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel("Agent Marketplace");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sub = new JLabel("Browse and hire pre-built AI agents for your departments.");
        sub.setFont(ThemeColors.FONT_BODY);
        sub.setForeground(ThemeColors.TEXT_SECONDARY);

        // Filter tabs
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        for (String f : FILTER_OPTIONS) {
            filters.add(filterChip(f));
        }

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);
        topSection.add(title, BorderLayout.NORTH);
        topSection.add(sub,   BorderLayout.CENTER);

        header.add(topSection, BorderLayout.NORTH);
        header.add(filters,    BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildGrid() {
        gridPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        gridPanel.setBackground(ThemeColors.CONTENT_BG);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll);
        return wrapper;
    }

    /** Rebuilds the grid with templates matching the current filter */
    public void refreshData() {
        gridPanel.removeAll();
        for (Object[] tmpl : TEMPLATES) {
            String dept = (String) tmpl[2];
            if (!"ALL".equals(currentFilter) && !dept.equals(currentFilter)) continue;
            gridPanel.add(buildTemplateCard(tmpl));
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    /** Builds one agent template card */
    private JPanel buildTemplateCard(Object[] tmpl) {
        String icon  = (String) tmpl[0];
        String name  = (String) tmpl[1];
        String dept  = (String) tmpl[2];
        String[] caps = (String[]) tmpl[3];
        String kpi   = (String) tmpl[4];

        // Dept color
        String[] depts = {"Sales","Marketing","Support","HR","Finance","Logistics"};
        int deptIdx = Arrays.asList(depts).indexOf(dept);
        Color deptColor = deptIdx >= 0 ? ThemeColors.DEPT_COLORS[deptIdx] : ThemeColors.ACCENT;

        RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ── Top: icon + badge ─────────────────────────────────
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        // Icon badge
        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(deptColor.getRed(), deptColor.getGreen(), deptColor.getBlue(), 25));
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(48, 48));
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconBadge.add(iconLbl);

        // Dept pill
        JLabel deptPill = new JLabel("  " + dept + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(deptColor.getRed(), deptColor.getGreen(), deptColor.getBlue(), 20));
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        deptPill.setFont(ThemeColors.FONT_SMALL);
        deptPill.setForeground(deptColor);
        deptPill.setOpaque(false);

        top.add(iconBadge, BorderLayout.WEST);
        top.add(deptPill,  BorderLayout.EAST);

        // ── Name ──────────────────────────────────────────────
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(ThemeColors.FONT_SUBTITLE);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // ── Capabilities ──────────────────────────────────────
        JPanel capsPanel = new JPanel();
        capsPanel.setLayout(new BoxLayout(capsPanel, BoxLayout.Y_AXIS));
        capsPanel.setOpaque(false);
        for (String cap : caps) {
            JLabel capLbl = new JLabel("✓  " + cap);
            capLbl.setFont(ThemeColors.FONT_SMALL);
            capLbl.setForeground(ThemeColors.TEXT_SECONDARY);
            capsPanel.add(capLbl);
        }

        // ── KPI label ─────────────────────────────────────────
        JLabel kpiLabel = new JLabel("📊 KPI: " + kpi);
        kpiLabel.setFont(ThemeColors.FONT_SMALL);
        kpiLabel.setForeground(ThemeColors.TEXT_MUTED);

        // ── Hire button ───────────────────────────────────────
        JButton hireBtn = hireButton(currentUser.isAdmin());
        hireBtn.addActionListener(e -> hireAgent(name, dept, caps, hireBtn));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(nameLabel);
        body.add(Box.createVerticalStrut(8));
        body.add(capsPanel);
        body.add(Box.createVerticalStrut(8));
        body.add(kpiLabel);

        card.add(top,     BorderLayout.NORTH);
        card.add(body,    BorderLayout.CENTER);
        card.add(hireBtn, BorderLayout.SOUTH);
        return card;
    }

    /** Hires (creates) the agent in the appropriate department */
    private void hireAgent(String roleName, String deptName, String[] caps, JButton hireBtn) {
        if (!currentUser.isAdmin()) {
            NotificationHelper.showWarning("Only Admins can hire agents.");
            return;
        }
        // Find the department ID
        int deptId = db.getAllDepartments().stream()
            .filter(d -> d.getName().equalsIgnoreCase(deptName))
            .mapToInt(d -> d.getId())
            .findFirst().orElse(1);

        // Find the manager agent for that dept
        int managerId = db.getAgentsByDepartment(deptId).stream()
            .filter(a -> "MANAGER".equals(a.getRoleType()))
            .mapToInt(a -> a.getId())
            .findFirst().orElse(0);

        // Generate a unique name with a random code
        String agentCode = new String[]{"NOVA","ECHO","MUSE","FLUX","APEX","BOLT","ZETA","ORION"}
            [new java.util.Random().nextInt(8)];
        String fullName  = agentCode + " — " + roleName;

        int newId = db.createAgent(fullName, "SPECIALIST", deptId,
                                   managerId > 0 ? managerId : null, 3);
        for (String cap : caps) db.addCapability(newId, cap);

        db.logActivity("USER", currentUser.getId(),
            "Hired agent: " + fullName + " in " + deptName, null);

        NotificationHelper.showSuccess("🎉 " + agentCode + " hired as " + roleName + " in " + deptName + "!");
        hireBtn.setText("✓ Hired!");
        hireBtn.setEnabled(false);
    }

    private JLabel filterChip(String filter) {
        boolean active = filter.equals(currentFilter);
        JLabel chip = new JLabel("  " + filter + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
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
        chip.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                currentFilter = filter;
                refreshData();
            }
        });
        return chip;
    }

    private JButton hireButton(boolean enabled) {
        JButton btn = new JButton(enabled ? "Hire Agent" : "View Only") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean canHire = isEnabled() && currentUser.isAdmin();
                g2.setColor(canHire
                    ? (getModel().isRollover() ? ThemeColors.ACCENT_LIGHT : ThemeColors.ACCENT)
                    : ThemeColors.BORDER);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(canHire ? Color.WHITE : ThemeColors.TEXT_MUTED);
                g2.setFont(ThemeColors.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()-fm.getHeight())/2+fm.getAscent());
                g2.dispose();
            }
        };
        btn.setEnabled(enabled);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
