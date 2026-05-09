package ui.panels;

import db.DatabaseHelper;
import models.Agent;
import models.Message;
import models.User;
import ui.components.RoundedPanel;
import ui.components.StatusDot;
import utils.DateUtils;
import utils.ThemeColors;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Communication Hub — chat-style interface to message agents directly.
 *
 * Layout:
 *   LEFT (240px)  — contact list of all agents with last-message preview
 *   RIGHT         — chat window for the selected agent conversation
 *
 * Agent responses are mocked with realistic, context-aware replies.
 */
public class CommunicationPanel extends JPanel {

    private final User currentUser;
    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private JPanel contactListPanel;
    private JPanel chatPanel;
    private JPanel messagesArea;
    private JTextField messageInput;
    private JLabel chatHeaderLabel;
    private Agent selectedAgent = null;

    // Mock agent responses by department type
    private static final Map<String, String[]> RESPONSES = new HashMap<>();
    static {
        RESPONSES.put("Sales", new String[]{
            "I'm working on qualifying the latest batch of leads. I've identified 8 high-value prospects so far.",
            "The Acme Corp deal is looking promising! I'll have a proposal draft ready within the hour.",
            "Outreach campaign response rate is at 18%. Should I adjust the subject line strategy?",
            "I've updated all deal stages in the CRM. Do you need a pipeline summary report?"
        });
        RESPONSES.put("Marketing", new String[]{
            "The blog post on AI Trends is 80% complete. I'll have it ready for review by end of day.",
            "Our LinkedIn engagement is up 24% this week. The follower milestone post is pending your approval.",
            "Keyword analysis shows 3 high-opportunity gaps vs. competitors. Should I draft a content plan?",
            "I've scheduled 14 social posts for the next two weeks. Review them in the Approvals queue."
        });
        RESPONSES.put("Support", new String[]{
            "Ticket #1042 is nearly resolved — customer confirmed the fix works.",
            "CSAT score this week: 4.2/5 from 34 responses. Two follow-ups are needed.",
            "I've flagged Ticket #1083 for engineering escalation. Root cause appears to be a billing API bug.",
            "Response time this week averaged 4.2 minutes. Within SLA targets."
        });
        RESPONSES.put("HR", new String[]{
            "I've screened 12 applications for the Backend Dev role. 3 candidates are strong fits.",
            "Interview schedule for Round 2 is finalized. Sending calendar invites now.",
            "The offer letter for Maya Chen is ready — it's in your Approvals queue.",
            "Onboarding checklist for the new hire has been sent. IT setup is in progress."
        });
        RESPONSES.put("Finance", new String[]{
            "April reconciliation is 90% complete. Found one discrepancy of $240 — investigating.",
            "Invoice INV-2049 is 14 days overdue. I'll send a final reminder today.",
            "Q2 budget utilization is at 62%. We're on track for the quarter.",
            "The vendor payment for CloudFlare is in the Approvals queue. Due date is May 15."
        });
        RESPONSES.put("Logistics", new String[]{
            "7 shipments are currently in transit. 6 are on time, 1 (SHP-0892) is delayed by 3 days.",
            "SKU-4421 is critically low. The reorder request is pending your approval.",
            "Weekly KPI: 94.2% on-time delivery rate — up from 91% last week.",
            "Inventory audit complete. 2 SKUs need reordering before end of month."
        });
        RESPONSES.put("default", new String[]{
            "Understood! I'm on it.",
            "I'll process that and get back to you shortly.",
            "Task received. I'll prioritize this in my queue.",
            "Thanks for the update! I'll adjust my approach accordingly."
        });
    }

    public CommunicationPanel(User currentUser) {
        this.currentUser = currentUser;
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(260);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setBackground(ThemeColors.CONTENT_BG);

        split.setLeftComponent(buildContactList());
        split.setRightComponent(buildChatArea());

        add(split, BorderLayout.CENTER);
    }

    // ── Contact list (left panel) ─────────────────────────────

    private JPanel buildContactList() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeColors.CARD_BG);
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeColors.BORDER));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeColors.CARD_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(16, 16, 12, 16)
        ));
        JLabel title = new JLabel("💬 Messages");
        title.setFont(ThemeColors.FONT_SUBTITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Contact list
        contactListPanel = new JPanel();
        contactListPanel.setLayout(new BoxLayout(contactListPanel, BoxLayout.Y_AXIS));
        contactListPanel.setBackground(ThemeColors.CARD_BG);

        JScrollPane scroll = new JScrollPane(contactListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CARD_BG);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** Populates the contact list with all agents */
    private void buildContactItems() {
        contactListPanel.removeAll();
        List<Agent> agents = db.getAllAgents();
        for (Agent ag : agents) {
            JPanel item = buildContactItem(ag);
            contactListPanel.add(item);
        }
        contactListPanel.revalidate();
        contactListPanel.repaint();
    }

    /** Builds one contact row (avatar + name + last message) */
    private JPanel buildContactItem(Agent agent) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setBackground(selectedAgent != null && selectedAgent.getId() == agent.getId()
            ? ThemeColors.ACCENT_VERY_LIGHT : ThemeColors.CARD_BG);
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF3, 0xF4, 0xF6)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Avatar circle
        Color color = ThemeColors.DEPT_COLORS[Math.abs(agent.getDepartmentId()) % ThemeColors.DEPT_COLORS.length];
        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String init = agent.getName().substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, (36 - fm.stringWidth(init))/2, (36 - fm.getHeight())/2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(36, 36));

        // Name + last message snippet
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String shortName = agent.getName().contains("—")
            ? agent.getName().split("—")[0].trim() : agent.getName();
        JLabel nameLabel = new JLabel(shortName);
        nameLabel.setFont(ThemeColors.FONT_BOLD);
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel previewLabel = new JLabel(agent.getDepartmentName() != null
            ? agent.getDepartmentName() : "Agent");
        previewLabel.setFont(ThemeColors.FONT_SMALL);
        previewLabel.setForeground(ThemeColors.TEXT_MUTED);

        info.add(nameLabel);
        info.add(previewLabel);

        // Status dot
        StatusDot dot = new StatusDot(agent.getStatus(), 8);

        item.add(avatar, BorderLayout.WEST);
        item.add(info,   BorderLayout.CENTER);
        item.add(dot,    BorderLayout.EAST);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectedAgent = agent;
                buildContactItems(); // refresh to highlight selection
                openChatWith(agent);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (selectedAgent == null || selectedAgent.getId() != agent.getId())
                    item.setBackground(new Color(0xF9, 0xFA, 0xFB));
            }
            @Override public void mouseExited(MouseEvent e) {
                if (selectedAgent == null || selectedAgent.getId() != agent.getId())
                    item.setBackground(ThemeColors.CARD_BG);
            }
        });
        return item;
    }

    // ── Chat area (right panel) ───────────────────────────────

    private JPanel buildChatArea() {
        chatPanel = new JPanel(new BorderLayout(0, 0));
        chatPanel.setBackground(ThemeColors.CONTENT_BG);

        // Chat header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(ThemeColors.CARD_BG);
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(16, 20, 14, 20)
        ));
        chatHeaderLabel = new JLabel("Select an agent to start chatting");
        chatHeaderLabel.setFont(ThemeColors.FONT_SUBTITLE);
        chatHeaderLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        chatHeader.add(chatHeaderLabel, BorderLayout.WEST);

        // Messages area
        messagesArea = new JPanel();
        messagesArea.setLayout(new BoxLayout(messagesArea, BoxLayout.Y_AXIS));
        messagesArea.setBackground(new Color(0xF8, 0xF9, 0xFA));
        messagesArea.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JScrollPane msgScroll = new JScrollPane(messagesArea);
        msgScroll.setBorder(null);
        msgScroll.getViewport().setBackground(new Color(0xF8, 0xF9, 0xFA));

        // Input area
        JPanel inputArea = buildInputArea();

        // Empty state
        JLabel emptyState = new JLabel("← Select an agent from the list to chat");
        emptyState.setFont(ThemeColors.FONT_BODY);
        emptyState.setForeground(ThemeColors.TEXT_MUTED);
        emptyState.setHorizontalAlignment(SwingConstants.CENTER);
        messagesArea.add(Box.createVerticalGlue());
        messagesArea.add(emptyState);
        messagesArea.add(Box.createVerticalGlue());

        chatPanel.add(chatHeader, BorderLayout.NORTH);
        chatPanel.add(msgScroll,  BorderLayout.CENTER);
        chatPanel.add(inputArea,  BorderLayout.SOUTH);
        return chatPanel;
    }

    /** Builds the message input bar at the bottom */
    private JPanel buildInputArea() {
        JPanel area = new JPanel(new BorderLayout(8, 0));
        area.setBackground(ThemeColors.CARD_BG);
        area.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        messageInput = new JTextField();
        messageInput.setFont(ThemeColors.FONT_BODY);
        messageInput.setForeground(ThemeColors.TEXT_PRIMARY);
        messageInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        messageInput.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("Send") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? ThemeColors.ACCENT : ThemeColors.BORDER);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(Color.WHITE);
                g2.setFont(ThemeColors.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()-fm.getHeight())/2+fm.getAscent());
                g2.dispose();
            }
        };
        sendBtn.setContentAreaFilled(false); sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setPreferredSize(new Dimension(80, 38));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        area.add(messageInput, BorderLayout.CENTER);
        area.add(sendBtn,      BorderLayout.EAST);
        return area;
    }

    /** Opens the conversation with a specific agent */
    private void openChatWith(Agent agent) {
        chatHeaderLabel.setText("🤖 " + agent.getName().split("—")[0].trim()
            + "  ·  " + agent.getStatus());

        messagesArea.removeAll();
        messagesArea.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        // Load existing messages from DB
        List<Message> messages = db.getMessageThread(currentUser.getId(), agent.getId());

        if (messages.isEmpty()) {
            // Add a greeting from the agent
            messagesArea.add(buildBubble(
                "Hello! I'm " + agent.getName().split("—")[0].trim() + ". How can I help you today?",
                false, agent));
            messagesArea.add(Box.createVerticalStrut(12));
        } else {
            for (Message msg : messages) {
                boolean isUser = "USER".equals(msg.getSenderType());
                messagesArea.add(buildBubble(msg.getBody(), isUser, agent));
                messagesArea.add(Box.createVerticalStrut(10));
            }
        }

        messagesArea.add(Box.createVerticalGlue());
        messagesArea.revalidate();
        messagesArea.repaint();
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) chatPanel.getComponent(1);
            JScrollBar sb  = sp.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    /** Sends a message and shows a mock agent reply */
    private void sendMessage() {
        if (selectedAgent == null) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        // Save user message
        db.sendMessage("USER", currentUser.getId(), "AGENT", selectedAgent.getId(), text);

        // Show user bubble
        messagesArea.add(buildBubble(text, true, selectedAgent));
        messagesArea.add(Box.createVerticalStrut(10));
        messageInput.setText("");

        // After a brief delay, show a mock agent reply
        Timer replyTimer = new Timer(1200, e -> {
            String reply = getMockReply(selectedAgent);
            db.sendMessage("AGENT", selectedAgent.getId(), "USER", currentUser.getId(), reply);
            messagesArea.add(buildBubble(reply, false, selectedAgent));
            messagesArea.add(Box.createVerticalStrut(10));
            messagesArea.revalidate();
            messagesArea.repaint();
        });
        replyTimer.setRepeats(false);
        replyTimer.start();

        messagesArea.revalidate();
        messagesArea.repaint();
    }

    /** Builds one chat bubble (user = right/indigo, agent = left/white) */
    private JPanel buildBubble(String text, boolean isUser, Agent agent) {
        JPanel row = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isUser ? ThemeColors.ACCENT : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),16,16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setLayout(new BorderLayout());
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel textLabel = new JLabel("<html><div style='max-width:260px'>" + text + "</div></html>");
        textLabel.setFont(ThemeColors.FONT_BODY);
        textLabel.setForeground(isUser ? Color.WHITE : ThemeColors.TEXT_PRIMARY);
        bubble.add(textLabel, BorderLayout.CENTER);
        bubble.setMaximumSize(new Dimension(320, Integer.MAX_VALUE));

        row.add(bubble);
        return row;
    }

    /** Returns a random mock reply from the agent's department pool */
    private String getMockReply(Agent agent) {
        String dept = agent.getDepartmentName() != null ? agent.getDepartmentName() : "default";
        String[] pool = RESPONSES.getOrDefault(dept, RESPONSES.get("default"));
        return pool[new Random().nextInt(pool.length)];
    }

    /** Reload contact list and current chat */
    public void refreshData() {
        buildContactItems();
        if (selectedAgent != null) {
            // Refresh the selected agent from DB (status may have changed)
            selectedAgent = db.getAgentById(selectedAgent.getId());
            if (selectedAgent != null) openChatWith(selectedAgent);
        }
    }
}
