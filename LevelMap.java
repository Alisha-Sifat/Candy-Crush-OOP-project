import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Level map panel.
 *
 * Circles are positioned by measuring the actual pixel centres in the
 * background image (858 x 1337 original) and scaling to the 500 x 800
 * game window.
 *
 * All drawing is done in paintComponent — no invisible JButtons, so the
 * number/lock icon is always perfectly centred inside its circle.
 */
public class LevelMap extends JPanel {

    private Image mapImage;

    // ── Circle centres in the 500 x 800 window ───────────────────────────
    // Measured from the screenshot and scaled: original(x,y) * (500/858, 800/1337)
    // Level 1 = bottom green circle, Level 6 = topmost circle
    private static final int[][] POS = {
        {229, 660},   // Level 1 — bottom green circle
        {246, 574},   // Level 2
        {289, 498},   // Level 3
        {291, 426},   // Level 4
        {310, 354},   // Level 5
        {329, 279},   // Level 6 — top circle
    };
    private static final int R = 36; // circle radius in pixels

    private int hoveredLevel = -1; // 1-based, -1 = none

    public LevelMap() {
        mapImage = new ImageIcon("src/images/LevelMap.jpeg").getImage();
        setLayout(null);

        // ── Hover ─────────────────────────────────────────────────────────
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int prev = hoveredLevel;
                hoveredLevel = hitTest(e.getX(), e.getY());
                if (hoveredLevel != prev) repaint();
                boolean hand = hoveredLevel != -1
                               && hoveredLevel <= SaveManager.loadLevel();
                setCursor(Cursor.getPredefinedCursor(
                    hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        // ── Click ─────────────────────────────────────────────────────────
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int lvl      = hitTest(e.getX(), e.getY());
                int unlocked = SaveManager.loadLevel();
                if (lvl == -1 || lvl > unlocked) return;

                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(LevelMap.this);
                frame.dispose();
                new Game().startLevel(LevelManager.getLevels().get(lvl - 1));
            }
            @Override public void mouseExited(MouseEvent e) {
                hoveredLevel = -1; repaint();
            }
        });
    }

    private int hitTest(int mx, int my) {
        for (int i = 0; i < POS.length; i++) {
            double d = Math.hypot(mx - POS[i][0], my - POS[i][1]);
            if (d <= R) return i + 1;
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background image scaled to panel size
        g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);

        int unlocked = SaveManager.loadLevel();

        for (int i = 0; i < POS.length; i++) {
            drawCircle(g2, POS[i][0], POS[i][1], i + 1,
                       i + 1 <= unlocked, i + 1 == hoveredLevel && i + 1 <= unlocked);
        }
    }

    private void drawCircle(Graphics2D g2, int cx, int cy,
                             int lvl, boolean open, boolean hovered) {
        int r = R + (hovered ? 5 : 0);

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(cx - r + 4, cy - r + 5, r * 2, r * 2);

        // Outer ring
        g2.setColor(open ? new Color(255, 200, 30) : new Color(90, 90, 90));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Inner gradient
        int ir = r - 6;
        Color topC = open ? (hovered ? new Color(100, 230, 60) : new Color(60, 200, 40))
                          : new Color(65, 65, 65);
        Color botC = open ? (hovered ? new Color(30, 160, 10) : new Color(20, 140, 10))
                          : new Color(38, 38, 38);
        GradientPaint gp = new GradientPaint(cx, cy - ir, topC, cx, cy + ir, botC);
        g2.setPaint(gp);
        g2.fillOval(cx - ir, cy - ir, ir * 2, ir * 2);

        // Gloss
        g2.setColor(new Color(255, 255, 255, hovered ? 60 : 80));
        g2.fillOval(cx - ir / 2, cy - ir + 3, ir, ir / 2);

        if (open) {
            // Level number — perfectly centred using FontMetrics
            String txt = String.valueOf(lvl);
            g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int tx = cx - fm.stringWidth(txt) / 2;
            int ty = cy + fm.getAscent() / 2 - 3;

            g2.setColor(new Color(0, 0, 0, 100)); // shadow
            g2.drawString(txt, tx + 1, ty + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(txt, tx, ty);
        } else {
            drawLock(g2, cx, cy, ir);
        }
    }

    private void drawLock(Graphics2D g2, int cx, int cy, int ir) {
        int bw = (int)(ir * 0.9f);
        int bh = (int)(ir * 0.65f);
        int bx = cx - bw / 2;
        int by = cy - bh / 4;

        // Body
        g2.setColor(new Color(210, 210, 210));
        g2.fillRoundRect(bx, by, bw, bh, 7, 7);
        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bx, by, bw, bh, 7, 7);

        // Shackle
        int sw = (int)(bw * 0.55f);
        g2.setColor(new Color(210, 210, 210));
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - sw / 2, by - (int)(ir * 0.45f), sw, (int)(ir * 0.65f), 0, 180);

        // Keyhole
        g2.setColor(new Color(90, 90, 90));
        g2.fillOval(cx - 4, by + bh / 2 - 6, 8, 8);
        g2.fillRect(cx - 2, by + bh / 2 + 1, 4, 5);
        g2.setStroke(new BasicStroke(1f));
    }
}
