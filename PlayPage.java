import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Start / play page with a fancy animated PLAY button.
 * The button pulses (scales up and down), has a gradient fill,
 * gloss layer, and a glow halo that brightens on hover.
 */
public class PlayPage extends JPanel {

    private Image bg;

    // Pulse animation
    private float   pulse     = 0f;
    private boolean hovered   = false;
    private Timer   animTimer;

    // Button bounds (centred, computed in paintComponent)
    private Rectangle btnBounds = new Rectangle(140, 490, 220, 75);

    public PlayPage() {
        bg = new ImageIcon("src/images/Playpage.jpeg").getImage();
        setLayout(null);

        // Pulse timer — runs at ~60 fps
        animTimer = new Timer(16, e -> {
            pulse = (float)(Math.sin(System.currentTimeMillis() / 600.0) * 0.5 + 0.5);
            repaint();
        });
        animTimer.start();

        // Mouse hover + click
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                boolean over = btnBounds.contains(e.getPoint());
                if (over != hovered) { hovered = over; repaint(); }
                setCursor(over
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (btnBounds.contains(e.getPoint())) goToMap();
            }
            @Override public void mouseExited(MouseEvent e) {
                hovered = false; repaint();
            }
        });
    }

    private void goToMap() {
        animTimer.stop();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.dispose();

        JFrame mapFrame = new JFrame("Level Map");
        mapFrame.setSize(500, 800);
        mapFrame.add(new LevelMap());
        mapFrame.setVisible(true);
        mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background image
        g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);

        // Centre the button
        int bw = 230, bh = 72;
        int bx = getWidth() / 2 - bw / 2;
        int by = (int)(getHeight() * 0.67f);
        btnBounds = new Rectangle(bx, by, bw, bh);

        drawPlayButton(g2, bx, by, bw, bh);
    }

    private void drawPlayButton(Graphics2D g2, int bx, int by, int bw, int bh) {
        // ── Outer glow halo ────────────────────────────────────────────────
        float glowAlpha = hovered ? 0.55f : 0.25f + pulse * 0.2f;
        int   glowSize  = hovered ? 18 : 10 + (int)(pulse * 6);
        for (int i = glowSize; i > 0; i -= 3) {
            float a = glowAlpha * ((float)(glowSize - i) / glowSize + 0.2f);
            g2.setColor(new Color(255, 200, 0, (int)(a * 255)));
            g2.fillRoundRect(bx - i, by - i, bw + i*2, bh + i*2, 50, 50);
        }

        // ── Drop shadow ───────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(bx + 4, by + 6, bw, bh, 40, 40);

        // ── Main body gradient ────────────────────────────────────────────
        Color topCol = hovered ? new Color(255, 130, 0)  : new Color(255, 180, 0);
        Color botCol = hovered ? new Color(200,  60, 0)  : new Color(220,  90, 0);
        GradientPaint gp = new GradientPaint(bx, by, topCol, bx, by + bh, botCol);
        g2.setPaint(gp);
        g2.fillRoundRect(bx, by, bw, bh, 40, 40);

        // ── Border ring ───────────────────────────────────────────────────
        g2.setColor(new Color(255, 240, 100, 180));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(bx + 1, by + 1, bw - 2, bh - 2, 40, 40);

        // ── Gloss (top half shine) ────────────────────────────────────────
        g2.setColor(new Color(255, 255, 255, hovered ? 70 : 100));
        g2.fillRoundRect(bx + 6, by + 5, bw - 12, bh / 2 - 4, 30, 30);

        // ── PLAY text with shadow ─────────────────────────────────────────
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        String txt = "▶  PLAY";
        int tx = bx + bw / 2 - fm.stringWidth(txt) / 2;
        int ty = by + bh / 2 + fm.getAscent() / 2 - 4;

        g2.setColor(new Color(120, 40, 0, 150)); // text shadow
        g2.drawString(txt, tx + 2, ty + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(txt, tx, ty);
    }
}
