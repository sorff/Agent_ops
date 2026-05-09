package ui;

import db.DatabaseHelper;
import models.User;
import utils.NotificationHelper;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 3-step setup wizard shown to new users the first time they log in.
 *
 * Step 1 — Choose your industry (loads department templates)
 * Step 2 — Pick which departments to activate
 * Step 3 — Review and confirm (app is ready to use)
 *
 * This prevents users from facing an empty dashboard with no guidance.
 * Once completed, we set up the basic org structure and go to MainFrame.
 */
public class OnboardingWizard extends JDialog {

    private int currentStep = 0;
    private final User user;

    // Step navigation panels
    private JPanel stepContainer;
    private CardLayout stepLayout;

    // Step indicators at the top
    private JLabel[] stepDots;

    // Selected industry and departments
    private String selectedIndustry = "Technology";
    private final List<String> selectedDepts = new ArrayList<>();

    // Industry templates: each maps to default departments
    private static final String[][] INDUSTRY_TEMPLATES = {
        { "Technology",     "Sales", "Marketing", "Support", "HR", "Finance" },
        { "Retail",         "Sales", "Logistics", "Marketing", "Support", "Finance" },
        { "Healthcare",     "Support", "HR", "Finance", "Logistics" },
        { "E-Commerce",     "Sales", "Marketing", "Logistics", "Support", "Finance" },
        { "Manufacturing",  "Logistics", "Finance", "HR", "Sales" },
        { "Agency",         "Sales", "Marketing", "Support", "HR" }
    };

    public OnboardingWizard(JFrame parent, User user) {
        super(parent, "Welcome to AgentOps — Setup Wizard", true);
        this.user = user;
        setSize(720, 560);
        setLocationRelativeTo(parent);
        setResizable(false);
        setUndecorated(true);    // clean look, no system title bar
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));

        // ── Header ─────────────────────────────────────────────
        root.add(buildHeader(), BorderLayout.NORTH);

        // ── Step content area ──────────────────────────────────
        stepLayout    = new CardLayout();
        stepContainer = new JPanel(stepLayout);
        stepContainer.setBackground(Color.WHITE);
        stepContainer.add(buildStep1(), "STEP1");
        stepContainer.add(buildStep2(), "STEP2");
        stepContainer.add(buildStep3(), "STEP3");
        root.add(stepContainer, BorderLayout.CENTER);

        // ── Footer with navigation buttons ────────────────────
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Top header with step indicator dots */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeColors.ACCENT);
        header.setPreferredSize(new Dimension(720, 70));
        header.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));

        JLabel title = new JLabel("⚡ AgentOps Setup Wizard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        // Step dots: 3 circles representing the 3 steps
        JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dotsPanel.setOpaque(false);
        stepDots = new JLabel[3];
        String[] stepNames = {"Industry", "Departments", "Confirm"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            stepDots[i] = new JLabel(stepNames[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean active = currentStep >= idx;
                    g2.setColor(active ? Color.WHITE : new Color(255, 255, 255, 80));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            stepDots[i].setFont(ThemeColors.FONT_SMALL);
            stepDots[i].setForeground(new Color(0x4F, 0x46, 0xE5));
            stepDots[i].setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            stepDots[i].setOpaque(false);
            dotsPanel.add(stepDots[i]);
            if (i < 2) {
                JLabel arrow = new JLabel("›");
                arrow.setForeground(new Color(255, 255, 255, 120));
                dotsPanel.add(arrow);
            }
        }

        header.add(title,     BorderLayout.WEST);
        header.add(dotsPanel, BorderLayout.EAST);
        return header;
    }

    // ── Step 1: Choose Industry ─────────────────────────────────

    private JPanel buildStep1() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JLabel heading = new JLabel("What industry are you in?");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sub = new JLabel(
            "We'll suggest the right department templates for your business.");
        sub.setFont(ThemeColors.FONT_BODY);
        sub.setForeground(ThemeColors.TEXT_SECONDARY);

        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 12));
        grid.setBackground(Color.WHITE);

        ButtonGroup group = new ButtonGroup();
        String[] icons = {"💻", "🛒", "🏥", "📦", "🏭", "🎨"};

        for (int i = 0; i < INDUSTRY_TEMPLATES.length; i++) {
            String industry = INDUSTRY_TEMPLATES[i][0];
            String icon     = icons[i];
            JToggleButton btn = createIndustryButton(industry, icon,
                                                      industry.equals(selectedIndustry));
            group.add(btn);
            btn.addActionListener(e -> {
                selectedIndustry = industry;
                preselectDepartments();
            });
            grid.add(btn);
        }

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(Color.WHITE);
        topSection.add(heading);
        topSection.add(Box.createVerticalStrut(6));
        topSection.add(sub);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(grid,       BorderLayout.CENTER);
        return panel;
    }

    private JToggleButton createIndustryButton(String name, String icon, boolean selected) {
        JToggleButton btn = new JToggleButton(icon + "  " + name) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isSelected()
                    ? ThemeColors.ACCENT_VERY_LIGHT
                    : (getModel().isRollover() ? new Color(0xF9, 0xFA, 0xFB) : Color.WHITE);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                Color border = isSelected() ? ThemeColors.ACCENT : ThemeColors.BORDER;
                g2.setColor(border);
                g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setSelected(selected);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(ThemeColors.FONT_BODY);
        btn.setForeground(ThemeColors.TEXT_PRIMARY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 60));
        return btn;
    }

    // ── Step 2: Choose Departments ──────────────────────────────

    private JPanel step2Panel;
    private JPanel deptCheckboxPanel;

    private JPanel buildStep2() {
        step2Panel = new JPanel(new BorderLayout(0, 16));
        step2Panel.setBackground(Color.WHITE);
        step2Panel.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JLabel heading = new JLabel("Which departments do you want to activate?");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sub = new JLabel("You can always add more departments later from Settings.");
        sub.setFont(ThemeColors.FONT_BODY);
        sub.setForeground(ThemeColors.TEXT_SECONDARY);

        deptCheckboxPanel = new JPanel();
        deptCheckboxPanel.setBackground(Color.WHITE);
        deptCheckboxPanel.setLayout(new GridLayout(0, 2, 12, 8));

        preselectDepartments(); // fill based on default industry

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(Color.WHITE);
        topSection.add(heading);
        topSection.add(Box.createVerticalStrut(6));
        topSection.add(sub);

        step2Panel.add(topSection,        BorderLayout.NORTH);
        step2Panel.add(deptCheckboxPanel, BorderLayout.CENTER);
        return step2Panel;
    }

    /** Refreshes the department checkboxes based on the selected industry */
    private void preselectDepartments() {
        selectedDepts.clear();
        if (deptCheckboxPanel == null) return;
        deptCheckboxPanel.removeAll();

        // Find the template for the selected industry
        String[] template = INDUSTRY_TEMPLATES[0]; // default
        for (String[] t : INDUSTRY_TEMPLATES) {
            if (t[0].equals(selectedIndustry)) { template = t; break; }
        }

        String[] allDepts = {"Sales", "Marketing", "Support", "HR", "Finance", "Logistics"};
        String[] deptIcons= {"💼",    "📣",        "🎧",      "👥",  "💰",      "🚚"};

        for (int i = 0; i < allDepts.length; i++) {
            String dept = allDepts[i];
            String icon = deptIcons[i];
            boolean recommended = isDeptInTemplate(dept, template);

            JCheckBox cb = new JCheckBox(icon + "  " + dept + (recommended ? "  ✓" : ""));
            cb.setFont(ThemeColors.FONT_BODY);
            cb.setForeground(ThemeColors.TEXT_PRIMARY);
            cb.setBackground(Color.WHITE);
            cb.setOpaque(false);
            cb.setSelected(recommended);
            if (recommended) selectedDepts.add(dept);

            cb.addActionListener(e -> {
                if (cb.isSelected()) selectedDepts.add(dept);
                else selectedDepts.remove(dept);
            });
            deptCheckboxPanel.add(cb);
        }
        deptCheckboxPanel.revalidate();
        deptCheckboxPanel.repaint();
    }

    private boolean isDeptInTemplate(String dept, String[] template) {
        for (int i = 1; i < template.length; i++) {
            if (template[i].equals(dept)) return true;
        }
        return false;
    }

    // ── Step 3: Confirm ──────────────────────────────────────────

    private JPanel buildStep3() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JLabel heading = new JLabel("You're all set! 🎉");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sub = new JLabel("<html>AgentOps will create your departments and populate them<br>" +
                                "with pre-built AI agents so your workforce is ready to go.</html>");
        sub.setFont(ThemeColors.FONT_BODY);
        sub.setForeground(ThemeColors.TEXT_SECONDARY);

        JPanel summaryBox = new JPanel();
        summaryBox.setLayout(new BoxLayout(summaryBox, BoxLayout.Y_AXIS));
        summaryBox.setBackground(ThemeColors.ACCENT_VERY_LIGHT);
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.ACCENT, 1, true),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel summaryTitle = new JLabel("Summary:");
        summaryTitle.setFont(ThemeColors.FONT_BOLD);
        summaryTitle.setForeground(ThemeColors.ACCENT);

        String deptList = selectedDepts.isEmpty() ? "(none selected)" : String.join(", ", selectedDepts);
        JLabel summaryIndustry = new JLabel("Industry: " + selectedIndustry);
        summaryIndustry.setFont(ThemeColors.FONT_BODY);
        JLabel summaryDepts = new JLabel("Departments: " + deptList);
        summaryDepts.setFont(ThemeColors.FONT_BODY);
        JLabel summaryAgents = new JLabel("Agents: " + (selectedDepts.size() * 4) + " pre-built agents will be created");
        summaryAgents.setFont(ThemeColors.FONT_BODY);

        summaryBox.add(summaryTitle);
        summaryBox.add(Box.createVerticalStrut(8));
        summaryBox.add(summaryIndustry);
        summaryBox.add(Box.createVerticalStrut(4));
        summaryBox.add(summaryDepts);
        summaryBox.add(Box.createVerticalStrut(4));
        summaryBox.add(summaryAgents);

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(Color.WHITE);
        topSection.add(heading);
        topSection.add(Box.createVerticalStrut(8));
        topSection.add(sub);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(summaryBox, BorderLayout.CENTER);
        return panel;
    }

    // ── Footer with navigation buttons ───────────────────────────

    private JButton backBtn, nextBtn;

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(16, 32, 16, 32)
        ));

        backBtn = styledButton("← Back",   ThemeColors.CARD_BG,   ThemeColors.TEXT_SECONDARY);
        nextBtn = styledButton("Next →",   ThemeColors.ACCENT,    Color.WHITE);

        backBtn.setEnabled(false); // disabled on step 1
        backBtn.addActionListener(e -> navigateBack());
        nextBtn.addActionListener(e -> navigateNext());

        JLabel stepIndicator = new JLabel("Step " + (currentStep + 1) + " of 3");
        stepIndicator.setFont(ThemeColors.FONT_SMALL);
        stepIndicator.setForeground(ThemeColors.TEXT_MUTED);

        footer.add(backBtn,        BorderLayout.WEST);
        footer.add(stepIndicator,  BorderLayout.CENTER);
        footer.add(nextBtn,        BorderLayout.EAST);
        return footer;
    }

    private JButton styledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? bg : ThemeColors.BORDER);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(isEnabled() ? fg : ThemeColors.TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Moves to the previous step */
    private void navigateBack() {
        if (currentStep > 0) {
            currentStep--;
            updateStep();
        }
    }

    /**
     * Moves to the next step.
     * On the final step, saves the setup and launches the main app.
     */
    private void navigateNext() {
        if (currentStep < 2) {
            currentStep++;
            updateStep();
        } else {
            // Final step — complete the setup
            finishSetup();
        }
    }

    /** Updates the UI to reflect the current step */
    private void updateStep() {
        stepLayout.show(stepContainer, "STEP" + (currentStep + 1));
        backBtn.setEnabled(currentStep > 0);
        nextBtn.repaint();
        if (currentStep == 2) {
            nextBtn.setText("Launch App 🚀");
        } else {
            nextBtn.setText("Next →");
        }
        // Update the step dot visuals
        for (JLabel dot : stepDots) dot.repaint();
    }

    /** Saves the wizard choices and opens MainFrame */
    private void finishSetup() {
        NotificationHelper.showSuccess("Workspace created! Welcome to AgentOps.");
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
        });
    }
}
