package ui.components;

import utils.ThemeColors;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A KPI (Key Performance Indicator) card for the dashboard.
 * Shows a large number, a label, and a small icon/emoji.
 *
 * Example:  [🤖]  24        <-- big number
 *                 Total Agents  <-- label
 *                 +2 this week  <-- trend
 *
 * Usage:
 *   KpiCard card = new KpiCard("Total Agents", "24", "🤖", "+2 this week", ThemeColors.ACCENT);
 */
public class KpiCard extends RoundedPanel {

    private JLabel valueLabel;
    private JLabel trendLabel;

    /**
     * @param title      Short label below the big number (e.g., "Total Agents")
     * @param value      The big number/text to display (e.g., "24")
     * @param icon       Emoji or symbol shown at top (e.g., "🤖")
     * @param trend      Small text showing change (e.g., "+2 this week"), can be null
     * @param accentColor  Color used for the icon badge background
     */
    public KpiCard(String title, String value, String icon,
                   String trend, Color accentColor) {
        super(12, ThemeColors.CARD_BG);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setPreferredSize(new Dimension(200, 120));

        // ── Top row: icon badge on left, trend on right ───────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        // Icon badge — colored circle with emoji
        JPanel iconBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Very light version of the accent color as background
                Color bg = new Color(accentColor.getRed(), accentColor.getGreen(),
                                     accentColor.getBlue(), 30);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(40, 40));
        iconBadge.setLayout(new GridBagLayout()); // centers the emoji
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconBadge.add(iconLabel);

        // Trend label (e.g., "+3 today") — shown in green if positive, red if negative
        trendLabel = new JLabel(trend != null ? trend : "");
        trendLabel.setFont(ThemeColors.FONT_SMALL);
        if (trend != null && trend.startsWith("+")) {
            trendLabel.setForeground(ThemeColors.STATUS_ACTIVE);
        } else if (trend != null && trend.startsWith("-")) {
            trendLabel.setForeground(ThemeColors.STATUS_ERROR);
        } else {
            trendLabel.setForeground(ThemeColors.TEXT_MUTED);
        }

        topRow.add(iconBadge,  BorderLayout.WEST);
        topRow.add(trendLabel, BorderLayout.EAST);

        // ── Middle: the big value number ──────────────────────
        valueLabel = new JLabel(value);
        valueLabel.setFont(ThemeColors.FONT_KPI_NUMBER);
        valueLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        // ── Bottom: the title label ───────────────────────────
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeColors.FONT_BODY);
        titleLabel.setForeground(ThemeColors.TEXT_SECONDARY);

        add(topRow,    BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(titleLabel, BorderLayout.SOUTH);
    }

    /** Updates the displayed number — call this when data refreshes */
    public void setValue(String newValue) {
        valueLabel.setText(newValue);
    }

    /** Updates the trend text */
    public void setTrend(String newTrend) {
        trendLabel.setText(newTrend != null ? newTrend : "");
    }
}
