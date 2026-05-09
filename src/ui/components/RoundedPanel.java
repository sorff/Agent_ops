package ui.components;

import utils.ThemeColors;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A JPanel with rounded corners and an optional drop shadow.
 * Used throughout the app as "card" containers (agent cards, KPI cards, modals).
 *
 * Usage:
 *   RoundedPanel card = new RoundedPanel(12, ThemeColors.CARD_BG);
 *   card.add(someLabel);
 */
public class RoundedPanel extends JPanel {

    private int cornerRadius;
    private Color backgroundColor;
    private boolean showShadow;

    /**
     * Creates a rounded panel with the given corner radius and background color.
     * @param cornerRadius  pixels — higher = more rounded (12 is a good default)
     * @param backgroundColor  fill color for the panel
     */
    public RoundedPanel(int cornerRadius, Color backgroundColor) {
        this(cornerRadius, backgroundColor, true);
    }

    public RoundedPanel(int cornerRadius, Color backgroundColor, boolean showShadow) {
        this.cornerRadius    = cornerRadius;
        this.backgroundColor = backgroundColor;
        this.showShadow      = showShadow;
        setOpaque(false);  // we draw our own background, so don't let Swing draw it
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    }

    /**
     * Custom painting — draws a rounded rectangle with optional drop shadow.
     * Graphics2D antialiasing makes the edges smooth.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Turn on smooth (anti-aliased) rendering so curves don't look jagged
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width  = getWidth();
        int height = getHeight();

        // Draw a subtle shadow by painting a slightly offset dark rounded rect
        if (showShadow) {
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fill(new RoundRectangle2D.Float(2, 3, width - 4, height - 2, cornerRadius, cornerRadius));
        }

        // Draw the main background
        g2.setColor(backgroundColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width - 2, height - 2, cornerRadius, cornerRadius));

        g2.dispose();
    }

    /** Changes the background color at runtime (e.g., for hover effects) */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }
}
