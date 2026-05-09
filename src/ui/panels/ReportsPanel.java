package ui.panels;

import db.DatabaseHelper;
import models.Agent;
import models.Department;
import utils.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Reports & Analytics page.
 *
 * Uses custom Java2D painting (no external chart library needed) to draw:
 *   1. Bar chart   — Active agents per department
 *   2. Pie chart   — Agent status distribution (Active / Idle / Paused / Error)
 *   3. Bar chart   — Top 5 agents by performance score
 *   4. Line chart  — Mock task completion trend over the last 7 days
 *
 * All charts read live data from the database.
 */
public class ReportsPanel extends JPanel {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    private BarChart  deptBarChart;
    private PieChart  statusPieChart;
    private BarChart  topAgentsChart;
    private LineChart taskTrendChart;

    public ReportsPanel() {
        setBackground(ThemeColors.CONTENT_BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeColors.CONTENT_BG);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeColors.CONTENT_BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Chart grid: 2×2
        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setBackground(ThemeColors.CONTENT_BG);
        grid.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        deptBarChart    = new BarChart("Agents per Department",  new String[0], new int[0],  ThemeColors.DEPT_COLORS);
        statusPieChart  = new PieChart("Agent Status Distribution", new String[0], new int[0]);
        topAgentsChart  = new BarChart("Top 5 Agents by Performance", new String[0], new int[0],
                            new Color[]{ThemeColors.ACCENT, ThemeColors.ACCENT_LIGHT,
                                        ThemeColors.STATUS_ACTIVE, ThemeColors.STATUS_IDLE,
                                        ThemeColors.STATUS_ERROR});
        taskTrendChart  = new LineChart("Task Completions — Last 7 Days");

        grid.add(wrapInCard(deptBarChart,   "📊 Agents per Department"));
        grid.add(wrapInCard(statusPieChart, "🍕 Agent Status Distribution"));
        grid.add(wrapInCard(topAgentsChart, "🏆 Top 5 Agents by Performance"));
        grid.add(wrapInCard(taskTrendChart, "📈 Task Completion Trend"));

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeColors.CONTENT_BG);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    /** Reloads data and repaints all charts */
    public void refreshData() {
        List<Department> depts  = db.getAllDepartments();
        List<Agent>      agents = db.getAllAgents();

        // Dept bar chart
        String[] deptNames  = depts.stream().map(Department::getName).toArray(String[]::new);
        int[]    deptCounts = depts.stream().mapToInt(Department::getAgentCount).toArray();
        deptBarChart.setData(deptNames, deptCounts);

        // Status pie chart
        long active = agents.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count();
        long idle   = agents.stream().filter(a -> "IDLE".equals(a.getStatus())).count();
        long paused = agents.stream().filter(a -> "PAUSED".equals(a.getStatus())).count();
        long error  = agents.stream().filter(a -> "ERROR".equals(a.getStatus())).count();
        statusPieChart.setData(
            new String[]{"Active", "Idle", "Paused", "Error"},
            new int[]{(int)active, (int)idle, (int)paused, (int)error}
        );

        // Top 5 agents by performance
        List<Agent> top5 = agents.stream()
            .sorted((a, b) -> Double.compare(b.getPerformanceScore(), a.getPerformanceScore()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        String[] topNames  = top5.stream()
            .map(a -> a.getName().split("—")[0].trim())
            .toArray(String[]::new);
        int[]    topScores = top5.stream()
            .mapToInt(a -> (int) a.getPerformanceScore())
            .toArray();
        topAgentsChart.setData(topNames, topScores);

        // Task trend — mock values showing a realistic upward trend
        int[] trend = {8, 12, 7, 15, 11, 18, 14};
        taskTrendChart.setData(trend);

        repaint();
    }

    /** Wraps a chart in a rounded card with a title label */
    private JPanel wrapInCard(JComponent chart, String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(ThemeColors.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(ThemeColors.FONT_BOLD);
        titleLbl.setForeground(ThemeColors.TEXT_PRIMARY);
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(chart,    BorderLayout.CENTER);
        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  CHART COMPONENTS (custom Java2D, no external library)
    // ══════════════════════════════════════════════════════════

    /** A simple vertical bar chart */
    static class BarChart extends JPanel {
        private String[] labels = {};
        private int[]    values = {};
        private Color[]  colors;

        BarChart(String title, String[] labels, int[] values, Color[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            setBackground(ThemeColors.CARD_BG);
            setPreferredSize(new Dimension(0, 200));
        }

        void setData(String[] labels, int[] values) {
            this.labels = labels;
            this.values = values;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (labels.length == 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 30, padB = 36, padT = 12, padR = 10;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Y-axis
            g2.setColor(ThemeColors.BORDER);
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            int maxVal = Arrays.stream(values).max().orElse(1);
            if (maxVal == 0) maxVal = 1;

            int barW = (chartW / labels.length) - 8;
            barW = Math.min(barW, 50);

            for (int i = 0; i < labels.length; i++) {
                int x     = padL + i * (chartW / labels.length) + 4;
                int barH  = (int)((double) values[i] / maxVal * chartH);
                int y     = padT + chartH - barH;
                Color col = i < colors.length ? colors[i] : ThemeColors.ACCENT;

                // Bar fill
                g2.setColor(col);
                g2.fill(new RoundRectangle2D.Float(x, y, barW, barH, 4, 4));

                // Value label on top
                g2.setFont(ThemeColors.FONT_SMALL);
                g2.setColor(ThemeColors.TEXT_SECONDARY);
                String val = String.valueOf(values[i]);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(val, x + (barW - fm.stringWidth(val))/2, y - 3);

                // X label
                g2.setColor(ThemeColors.TEXT_MUTED);
                String lbl = labels[i].length() > 5 ? labels[i].substring(0, 5) : labels[i];
                g2.drawString(lbl, x + (barW - fm.stringWidth(lbl))/2, padT + chartH + 16);
            }
            g2.dispose();
        }
    }

    /** A simple pie / donut chart */
    static class PieChart extends JPanel {
        private String[] labels = {};
        private int[]    values = {};
        private static final Color[] PIE_COLORS = {
            ThemeColors.STATUS_ACTIVE, ThemeColors.STATUS_IDLE,
            ThemeColors.STATUS_PAUSED, ThemeColors.STATUS_ERROR
        };

        PieChart(String title, String[] labels, int[] values) {
            this.labels = labels;
            this.values = values;
            setBackground(ThemeColors.CARD_BG);
            setPreferredSize(new Dimension(0, 200));
        }

        void setData(String[] labels, int[] values) {
            this.labels = labels;
            this.values = values;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int total = Arrays.stream(values).sum();
            if (total == 0) { g2.dispose(); return; }

            int w = getWidth(), h = getHeight();
            int size  = Math.min(w, h) - 40;
            int x     = (w - size) / 2 - 40;
            int y     = (h - size) / 2;
            int hole  = size / 3;

            double startAngle = 0;
            for (int i = 0; i < values.length; i++) {
                double arc = 360.0 * values[i] / total;
                g2.setColor(i < PIE_COLORS.length ? PIE_COLORS[i] : ThemeColors.ACCENT);
                g2.fill(new Arc2D.Double(x, y, size, size, startAngle, arc, Arc2D.PIE));
                startAngle += arc;
            }

            // Donut hole
            g2.setColor(ThemeColors.CARD_BG);
            g2.fillOval(x + hole/2, y + hole/2, size - hole, size - hole);

            // Total label in center
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g2.setColor(ThemeColors.TEXT_PRIMARY);
            String tot = String.valueOf(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tot, x + size/2 - fm.stringWidth(tot)/2,
                          y + size/2 + fm.getAscent()/2 - 4);

            // Legend on the right
            int legX = x + size + 16;
            int legY = y + 10;
            for (int i = 0; i < labels.length && i < values.length; i++) {
                g2.setColor(i < PIE_COLORS.length ? PIE_COLORS[i] : ThemeColors.ACCENT);
                g2.fillRoundRect(legX, legY + i*22, 12, 12, 4, 4);
                g2.setFont(ThemeColors.FONT_SMALL);
                g2.setColor(ThemeColors.TEXT_SECONDARY);
                g2.drawString(labels[i] + " (" + values[i] + ")",
                    legX + 18, legY + i*22 + 11);
            }
            g2.dispose();
        }
    }

    /** A simple line chart for time-series data */
    static class LineChart extends JPanel {
        private int[] values = {8, 12, 7, 15, 11, 18, 14};
        private final String[] dayLabels = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};

        LineChart(String title) {
            setBackground(ThemeColors.CARD_BG);
            setPreferredSize(new Dimension(0, 200));
        }

        void setData(int[] values) {
            this.values = values;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 32, padB = 32, padT = 16, padR = 16;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Grid lines
            g2.setColor(new Color(0xF3, 0xF4, 0xF6));
            for (int r = 0; r <= 4; r++) {
                int gy = padT + r * chartH / 4;
                g2.drawLine(padL, gy, padL + chartW, gy);
            }

            // Axes
            g2.setColor(ThemeColors.BORDER);
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            if (values.length < 2) { g2.dispose(); return; }

            int maxVal = Arrays.stream(values).max().orElse(1);

            // Build x, y points
            int[] px = new int[values.length];
            int[] py = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                px[i] = padL + i * chartW / (values.length - 1);
                py[i] = padT + chartH - (int)((double) values[i] / maxVal * chartH);
            }

            // Fill under the line
            int[] fillX = new int[values.length + 2];
            int[] fillY = new int[values.length + 2];
            System.arraycopy(px, 0, fillX, 0, values.length);
            System.arraycopy(py, 0, fillY, 0, values.length);
            fillX[values.length]     = px[values.length - 1];
            fillY[values.length]     = padT + chartH;
            fillX[values.length + 1] = px[0];
            fillY[values.length + 1] = padT + chartH;
            g2.setColor(new Color(0x4F, 0x46, 0xE5, 25));
            g2.fillPolygon(fillX, fillY, fillX.length);

            // Draw line
            g2.setColor(ThemeColors.ACCENT);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < values.length - 1; i++) {
                g2.drawLine(px[i], py[i], px[i+1], py[i+1]);
            }

            // Draw points + labels
            g2.setFont(ThemeColors.FONT_SMALL);
            for (int i = 0; i < values.length; i++) {
                g2.setColor(ThemeColors.CARD_BG);
                g2.fillOval(px[i]-4, py[i]-4, 8, 8);
                g2.setColor(ThemeColors.ACCENT);
                g2.drawOval(px[i]-4, py[i]-4, 8, 8);

                g2.setColor(ThemeColors.TEXT_MUTED);
                String dayLbl = i < dayLabels.length ? dayLabels[i] : "";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(dayLbl, px[i] - fm.stringWidth(dayLbl)/2, padT + chartH + 16);
            }
            g2.dispose();
        }
    }
}
