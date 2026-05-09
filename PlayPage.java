import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * PlayPage — splash/title screen.
 *
 * Features:
 *  • Background image fills the panel.
 *  • Animated "PLAY" button with pulsing glow, gradient fill,
 *    sheen highlight, and drop shadow — no external images needed.
 *  • A candy-themed title overlay in case the background image
 *    doesn't have text.
 */
public class PlayPage extends JPanel implements ActionListener {

    private Image bg;

    // ── Animation ────────────────────────────────────────────────────────
    private javax.swing.Timer pulseTimer;
    private float pulsePhase = 0f;

    // ── Button geometry (set in constructor, used in paint + hit-test) ───
    private final int BTN_X  = 110;
    private final int BTN_Y  = 490;
    private final int BTN_W  = 280;
    private final int BTN_H  = 80;
    private final int BTN_R  = 40;   // corner radius

    private boolean hovered  = false;
    private boolean pressed  = false;

    public PlayPage() {
        bg = new ImageIcon("src/images/Playpage.jpeg").getImage();
        setLayout(null);

        // Pulse timer drives the glow animation
        pulseTimer = new javax.swing.Timer(20, this);
        pulseTimer.start();

        // Mouse interaction for the custom-drawn button
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (hitButton(e)) { pressed = true; repaint(); }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressed) {
                    pressed = false;
                    if (hitButton(e)) onPlayClicked(e);
                    repaint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) { }

            @Override
            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean over = hitButton(e);
                if (over != hovered) { hovered = over; repaint(); }
                setCursor(hovered
                        ? new Cursor(Cursor.HAND_CURSOR)
                        : new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  TIMER TICK — advances pulse animation
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        pulsePhase += 0.06f;
        if (pulsePhase > 2 * Math.PI) pulsePhase -= (float)(2 * Math.PI);
        repaint();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────
    private void onPlayClicked(MouseEvent e) {
        pulseTimer.stop();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.dispose();

        JFrame mapFrame = new JFrame("Level Map");
        mapFrame.setSize(500, 800);
        mapFrame.add(new LevelMap());
        mapFrame.setVisible(true);
        mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HIT TEST
    // ─────────────────────────────────────────────────────────────────────
    private boolean hitButton(MouseEvent e) {
        return e.getX() >= BTN_X && e.getX() <= BTN_X + BTN_W
            && e.getY() >= BTN_Y && e.getY() <= BTN_Y + BTN_H;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PAINT
    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // ── Background ────────────────────────────────────────────────────
        g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);

        // ── Pulsing outer glow (drawn before button so button sits on top) ─
        float glowAlpha = (float)(0.35 + 0.30 * Math.sin(pulsePhase));
        float glowScale = (float)(1.0 + 0.06 * Math.sin(pulsePhase));

        int gx = (int)(BTN_X + BTN_W / 2 - (BTN_W * glowScale) / 2);
        int gy = (int)(BTN_Y + BTN_H / 2 - (BTN_H * glowScale) / 2);
        int gw = (int)(BTN_W * glowScale);
        int gh = (int)(BTN_H * glowScale);

        // Multiple layered glow rings
        for (int layer = 3; layer >= 1; layer--) {
            int pad = layer * 10;
            g2.setColor(new Color(255, 80, 150, (int)(glowAlpha * 80 / layer)));
            g2.fill(new RoundRectangle2D.Float(
                    gx - pad, gy - pad, gw + pad * 2, gh + pad * 2,
                    BTN_R + pad, BTN_R + pad));
        }

        // ── Button shadow ─────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, pressed ? 40 : 90));
        g2.fill(new RoundRectangle2D.Float(BTN_X + 4, BTN_Y + 6, BTN_W, BTN_H, BTN_R, BTN_R));

        // ── Button body gradient ──────────────────────────────────────────
        Color topCol, botCol;
        if (pressed) {
            topCol = new Color(200, 30, 100);
            botCol = new Color(140, 10,  60);
        } else if (hovered) {
            topCol = new Color(255, 120, 190);
            botCol = new Color(230, 60,  130);
        } else {
            topCol = new Color(255, 100, 170);
            botCol = new Color(210, 40,  110);
        }
        GradientPaint bodyGrad = new GradientPaint(
                BTN_X, BTN_Y,        topCol,
                BTN_X, BTN_Y + BTN_H, botCol);
        g2.setPaint(bodyGrad);
        g2.fill(new RoundRectangle2D.Float(BTN_X, BTN_Y, BTN_W, BTN_H, BTN_R, BTN_R));

        // ── Button top sheen ──────────────────────────────────────────────
        GradientPaint sheen = new GradientPaint(
                BTN_X, BTN_Y,                new Color(255, 255, 255, 120),
                BTN_X, BTN_Y + BTN_H / 2,   new Color(255, 255, 255, 0));
        g2.setPaint(sheen);
        g2.fill(new RoundRectangle2D.Float(
                BTN_X + 6, BTN_Y + 4,
                BTN_W - 12, BTN_H / 2 - 2,
                BTN_R - 4, BTN_R - 4));

        // ── Border ────────────────────────────────────────────────────────
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(255, 255, 255, 160));
        g2.draw(new RoundRectangle2D.Float(BTN_X + 1, BTN_Y + 1, BTN_W - 2, BTN_H - 2, BTN_R, BTN_R));

        // ── "PLAY" text ───────────────────────────────────────────────────
        int offsetY = pressed ? 2 : 0;
        String btnText = "▶  PLAY";
        Font btnFont = new Font("Arial Black", Font.BOLD, 30);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics();
        int tx = BTN_X + (BTN_W - fm.stringWidth(btnText)) / 2;
        int ty = BTN_Y + (BTN_H + fm.getAscent() - fm.getDescent()) / 2 + offsetY;

        // Text shadow
        g2.setColor(new Color(120, 0, 60, 120));
        g2.drawString(btnText, tx + 2, ty + 2);

        // Text
        g2.setColor(Color.WHITE);
        g2.drawString(btnText, tx, ty);

        // ── Candy dots decoration above button ────────────────────────────
        paintCandyDots(g2);
    }

    /** Draws small colourful dots around the button as decoration. */
    private void paintCandyDots(Graphics2D g2) {
        Color[] dotColors = {
            new Color(255, 80,  80),   // red
            new Color(80,  160, 255),  // blue
            new Color(80,  220, 80),   // green
            new Color(255, 220, 0),    // yellow
            new Color(255, 140, 0),    // orange
            new Color(180, 80,  255),  // purple
        };

        // Fixed positions relative to button (decorative only)
        int[][] dots = {
            {BTN_X - 18, BTN_Y + 20},
            {BTN_X - 30, BTN_Y + 55},
            {BTN_X + BTN_W + 14, BTN_Y + 12},
            {BTN_X + BTN_W + 28, BTN_Y + 48},
            {BTN_X + 60,  BTN_Y - 22},
            {BTN_X + 160, BTN_Y - 18},
            {BTN_X + 220, BTN_Y - 24},
        };

        for (int i = 0; i < dots.length; i++) {
            int[] d = dots[i];
            Color c = dotColors[i % dotColors.length];
            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(d[0] + 2, d[1] + 2, 18, 18);
            // Body
            g2.setColor(c);
            g2.fillOval(d[0], d[1], 18, 18);
            // Sheen
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(d[0] + 3, d[1] + 2, 7, 6);
        }
    }
}
