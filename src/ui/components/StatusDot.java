package ui.components;

import utils.ThemeColors;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * A small colored circle that indicates an agent's status.
 * Colors come from ThemeColors so they're consistent everywhere:
 *   ACTIVE = green, IDLE = amber, PAUSED = gray, ERROR = red.
 *
 * Usage:
 *   StatusDot dot = new StatusDot("ACTIVE", 10);  // 10px diameter
 *   panel.add(dot);
 */
public class StatusDot extends JComponent {

    private String status;
    private int diameter;
    private boolean pulse;  // if true, adds a subtle pulsing ring (for ACTIVE)

    /** Creates a status dot with the given status string and diameter in pixels */
    public StatusDot(String status, int diameter) {
        this.status   = status;
        this.diameter = diameter;
        setPreferredSize(new Dimension(diameter + 6, diameter + 6));
        setOpaque(false);
    }

    /** Updates the status and repaints the dot */
    public void setStatus(String status) {
        this.status = status;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color dotColor = ThemeColors.getStatusColor(status);
        int offset = 3; // small offset to center in the component

        // For ACTIVE status, draw a faint halo ring
        if ("ACTIVE".equalsIgnoreCase(status)) {
            Color halo = new Color(dotColor.getRed(), dotColor.getGreen(),
                                   dotColor.getBlue(), 60);
            g2.setColor(halo);
            g2.fill(new Ellipse2D.Float(offset - 2, offset - 2,
                                        diameter + 4, diameter + 4));
        }

        // Draw the filled circle
        g2.setColor(dotColor);
        g2.fill(new Ellipse2D.Float(offset, offset, diameter, diameter));

        g2.dispose();
    }
}
