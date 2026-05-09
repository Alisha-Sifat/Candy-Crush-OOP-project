import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * LevelMap — candy-land world map with circular level nodes.
 *
 * ── Fine-tuning positions ────────────────────────────────────────────────
 *  Run the app, click anywhere, and the console prints the exact
 *  X / Y you clicked. Paste those into NODE_POS to snap nodes onto
 *  the candy-cane centre.
 */
public class LevelMap extends JPanel {

    // ── Node centre (x, y) in the 500 × 800 panel ────────────────────────
    // The candy-cane path runs through approximately x = 220-270 (left-centre).
    // These positions trace the cane's gentle zigzag from bottom to top.
    private static final int[][] NODE_POS = {
        { 245, 685 },   // Level 1  (bottom)
        { 250, 565 },   // Level 2
        { 240, 448 },   // Level 3
        { 260, 335 },   // Level 4
        { 245, 225 },   // Level 5
        { 255, 130 },   // Level 6  (top)
    };

    private static final int R        = 30;   // node radius
    private static final int RING_PAD =  5;   // extra radius for the gold ring
    private static final int STAR_GAP = 14;   // px between star centres

    // Colour theme per level: { topColour, bottomColour }
    private static final Color[][] COLOURS = {
        { new Color(255, 110, 160), new Color(200,  40, 100) },  // L1 Hot pink
        { new Color( 80, 165, 255), new Color( 20, 100, 210) },  // L2 Sky blue
        { new Color( 90, 215, 100), new Color( 30, 155,  50) },  // L3 Lime green
        { new Color(255, 205,  50), new Color(200, 140,   0) },  // L4 Golden yellow
        { new Color(185,  90, 255), new Color(120,  30, 200) },  // L5 Purple
        { new Color(255, 175,  30), new Color(210, 110,   0) },  // L6 Deep gold
    };

    private Image mapImage;
    private int   unlockedUpTo;
    private int   hoveredLevel = -1;

    // ─────────────────────────────────────────────────────────────────────
    //  CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────────
    public LevelMap() {
        mapImage     = new ImageIcon("src/images/Map.png").getImage();
        unlockedUpTo = SaveManager.loadLevel();   // App.java resets to 1 every launch

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Prints coords so you can fine-tune NODE_POS
                System.out.println("Clicked  X: " + e.getX() + "  Y: " + e.getY());
                int lvl = levelAt(e.getX(), e.getY());
                if (lvl > 0 && lvl <= unlockedUpTo) launchLevel(lvl);
            }
            @Override
            public void mouseExited(MouseEvent e) { hoveredLevel = -1; repaint(); }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int lvl = levelAt(e.getX(), e.getY());
                if (lvl != hoveredLevel) { hoveredLevel = lvl; repaint(); }
                boolean active = lvl > 0 && lvl <= unlockedUpTo;
                setCursor(active ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                 : Cursor.getDefaultCursor());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────
    private void launchLevel(int levelNum) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.dispose();
        new Game().startLevel(LevelManager.getLevels().get(levelNum - 1));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HIT TEST  (+10 px generous margin around each node)
    // ─────────────────────────────────────────────────────────────────────
    private int levelAt(int mx, int my) {
        for (int i = 0; i < NODE_POS.length; i++) {
            int dx = mx - NODE_POS[i][0], dy = my - NODE_POS[i][1];
            if (dx * dx + dy * dy <= (R + 10) * (R + 10)) return i + 1;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PAINT
    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);
        drawPathDots(g2);
        for (int i = 0; i < NODE_POS.length; i++)
            drawNode(g2, i + 1, NODE_POS[i][0], NODE_POS[i][1]);
    }

    /** Small dotted connectors between consecutive nodes. */
    private void drawPathDots(Graphics2D g2) {
        for (int i = 0; i < NODE_POS.length - 1; i++) {
            int x1 = NODE_POS[i][0],   y1 = NODE_POS[i][1];
            int x2 = NODE_POS[i+1][0], y2 = NODE_POS[i+1][1];
            for (int s = 1; s < 5; s++) {
                float t  = s / 5f;
                int   dx = (int)(x1 + t * (x2 - x1));
                int   dy = (int)(y1 + t * (y2 - y1));
                g2.setColor(new Color(255, 255, 255, 130));
                g2.fillOval(dx - 4, dy - 4, 8, 8);
                g2.setColor(new Color(255, 180, 210, 160));
                g2.fillOval(dx - 3, dy - 3, 6, 6);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NODE DISPATCH
    // ─────────────────────────────────────────────────────────────────────
    private void drawNode(Graphics2D g2, int level, int cx, int cy) {
        boolean unlocked = level <= unlockedUpTo;
        boolean hov      = unlocked && level == hoveredLevel;
        if (unlocked) drawUnlockedNode(g2, level, cx, cy, hov);
        else          drawLockedNode  (g2,         cx, cy);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UNLOCKED NODE
    // ─────────────────────────────────────────────────────────────────────
    private void drawUnlockedNode(Graphics2D g2, int level, int cx, int cy, boolean hov) {
        int   r    = R + (hov ? 4 : 0);
        Color topC = COLOURS[level - 1][0];
        Color botC = COLOURS[level - 1][1];

        // Hover glow rings
        if (hov) {
            for (int layer = 3; layer >= 1; layer--) {
                int pad = layer * 7;
                g2.setColor(new Color(topC.getRed(), topC.getGreen(), topC.getBlue(), 45 / layer));
                g2.fillOval(cx - r - pad, cy - r - pad, (r + pad) * 2, (r + pad) * 2);
            }
        }

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(cx - r + 3, cy - r + 5, r * 2, r * 2);

        // Gold outer ring
        g2.setPaint(new GradientPaint(cx - r, cy - r, new Color(255, 230, 80),
                                      cx + r, cy + r, new Color(185, 125,  0)));
        g2.fillOval(cx - r - RING_PAD, cy - r - RING_PAD,
                   (r + RING_PAD) * 2, (r + RING_PAD) * 2);

        // White spacer
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2);

        // Coloured body
        g2.setPaint(new GradientPaint(cx - r, cy - r, topC, cx + r, cy + r, botC));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Top sheen
        g2.setPaint(new GradientPaint(cx, cy - r,     new Color(255, 255, 255, 140),
                                      cx, cy - r / 2, new Color(255, 255, 255,   0)));
        g2.fillOval(cx - r + 6, cy - r + 4, r * 2 - 12, r - 2);

        // Level number
        String      txt = String.valueOf(level);
        g2.setFont(new Font("Arial Black", Font.BOLD, 20));
        FontMetrics fm  = g2.getFontMetrics();
        int tx = cx - fm.stringWidth(txt) / 2;
        int ty = cy + (fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 90)); g2.drawString(txt, tx + 1, ty + 1);
        g2.setColor(Color.WHITE);            g2.drawString(txt, tx, ty);

        // Three stars below
        drawStarRow(g2, cx, cy + r + RING_PAD + 12, 3);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOCKED NODE
    // ─────────────────────────────────────────────────────────────────────
    private void drawLockedNode(Graphics2D g2, int cx, int cy) {
        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(cx - R + 2, cy - R + 4, R * 2, R * 2);

        // Grey outer ring
        g2.setPaint(new GradientPaint(cx - R, cy - R, new Color(170, 170, 170),
                                      cx + R, cy + R, new Color( 95,  95,  95)));
        g2.fillOval(cx - R - RING_PAD, cy - R - RING_PAD,
                   (R + RING_PAD) * 2, (R + RING_PAD) * 2);

        // White spacer
        g2.setColor(new Color(210, 210, 210));
        g2.fillOval(cx - R - 2, cy - R - 2, (R + 2) * 2, (R + 2) * 2);

        // Grey body
        g2.setPaint(new GradientPaint(cx - R, cy - R, new Color(175, 175, 175),
                                      cx + R, cy + R, new Color(105, 105, 105)));
        g2.fillOval(cx - R, cy - R, R * 2, R * 2);

        // Sheen
        g2.setColor(new Color(255, 255, 255, 65));
        g2.fillOval(cx - R + 6, cy - R + 4, R * 2 - 12, R - 2);

        // Padlock (shape-drawn — no emoji dependency)
        drawLockIcon(g2, cx, cy);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PADLOCK SHAPES
    // ─────────────────────────────────────────────────────────────────────
    private void drawLockIcon(Graphics2D g2, int cx, int cy) {
        int bw = 18, bh = 14;
        int bx = cx - bw / 2, by = cy - 1;

        g2.setColor(new Color(255, 255, 255, 210));
        g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 5, 5));

        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - 6, by - 9, 12, 11, 0, 180);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(110, 110, 110));
        g2.fillOval(cx - 3, by + 3, 6, 6);
        g2.fillRect(cx - 2, by + 7, 4, 4);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  STARS
    // ─────────────────────────────────────────────────────────────────────
    private void drawStarRow(Graphics2D g2, int cx, int cy, int count) {
        int startX = cx - ((count - 1) * STAR_GAP) / 2;
        for (int i = 0; i < count; i++)
            drawStar(g2, startX + i * STAR_GAP, cy, 6,
                     new Color(255, 230, 0), new Color(215, 155, 0));
    }

    private void drawStar(Graphics2D g2, int cx, int cy, int outerR,
                          Color fill, Color outline) {
        int pts = 5, innerR = outerR / 2;
        int[] xs = new int[pts * 2], ys = new int[pts * 2];

        for (int i = 0; i < pts * 2; i++) {
            double a = Math.PI / pts * i - Math.PI / 2;
            int    r = (i % 2 == 0) ? outerR : innerR;
            xs[i] = cx + (int)(r * Math.cos(a));
            ys[i] = cy + (int)(r * Math.sin(a));
        }
        g2.setColor(outline); g2.fillPolygon(xs, ys, pts * 2);

        for (int i = 0; i < pts * 2; i++) {
            double a = Math.PI / pts * i - Math.PI / 2;
            int    r = (i % 2 == 0) ? outerR - 1 : innerR;
            xs[i] = cx + (int)(r * Math.cos(a));
            ys[i] = cy + (int)(r * Math.sin(a));
        }
        g2.setColor(fill); g2.fillPolygon(xs, ys, pts * 2);
    }
}
